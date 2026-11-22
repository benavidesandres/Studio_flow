package com.studioflow.service.conflict;

import com.studioflow.dto.conflict.ConflictDetail;
import com.studioflow.dto.conflict.ConflictType;
import com.studioflow.exception.ScheduleConflictException;
import com.studioflow.model.Scene;
import com.studioflow.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class ConflictDetectionEngineImpl implements ConflictDetectionEngine {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Value("${studioflow.conflict.minimum-travel-minutes:45}")
    private int minimumTravelMinutes;

    @Override
    public List<ConflictDetail> detectConflicts(Scene candidateScene, List<Scene> existingScenes) {
        if (candidateScene == null || candidateScene.getActors() == null || candidateScene.getActors().isEmpty()) {
            return Collections.emptyList();
        }

        List<ConflictDetail> conflicts = new ArrayList<>();
        LocalDateTime candStart = candidateScene.getScheduledTime();
        LocalDateTime candEnd = candidateScene.getEndDateTime();

        if (candStart == null || candEnd == null) {
            return Collections.emptyList();
        }

        for (Scene existing : existingScenes) {
            // Ignorar la misma escena al actualizar
            if (candidateScene.getId() != null && candidateScene.getId().equals(existing.getId())) {
                continue;
            }

            LocalDateTime existStart = existing.getScheduledTime();
            LocalDateTime existEnd = existing.getEndDateTime();

            if (existStart == null || existEnd == null) {
                continue;
            }

            // Identificar actores en común
            Set<User> commonActors = findCommonActors(candidateScene.getActors(), existing.getActors());
            if (commonActors.isEmpty()) {
                continue;
            }

            for (User actor : commonActors) {
                String actorName = actor.getFullName() != null ? actor.getFullName() : actor.getName();
                String existSceneNum = existing.getSceneNumber() != null ? existing.getSceneNumber() : "SC-" + existing.getId();
                String existTitleDisplay = existSceneNum + ": " + existing.getTitle();

                // 1. Regla: Solapamiento simultáneo de actor (startA < endB && endA > startB)
                boolean isOverlapping = candStart.isBefore(existEnd) && existStart.isBefore(candEnd);

                if (isOverlapping) {
                    long overlapMinutes = Math.min(
                            Duration.between(candStart, existEnd).toMinutes(),
                            Duration.between(existStart, candEnd).toMinutes()
                    );

                    String message = String.format(
                            "El actor %s ya está citado en la escena %s (%s - %s). SOLAPAMIENTO de horario detectado.",
                            actorName,
                            existSceneNum,
                            existStart.format(TIME_FORMATTER),
                            existEnd.format(TIME_FORMATTER)
                    );

                    conflicts.add(ConflictDetail.builder()
                            .type(ConflictType.ACTOR_OVERLAP)
                            .actorId(actor.getId())
                            .actorName(actorName)
                            .conflictingSceneId(existing.getId())
                            .conflictingSceneNumber(existSceneNum)
                            .conflictingSceneTitle(existTitleDisplay)
                            .conflictingScheduledTime(existStart)
                            .conflictingDurationMinutes(existing.getDurationMinutes())
                            .conflictingLocation(existing.getLocation())
                            .newLocation(candidateScene.getLocation())
                            .gapMinutes(-overlapMinutes)
                            .minimumRequiredMinutes(0)
                            .message(message)
                            .build());

                } else if (candStart.toLocalDate().equals(existStart.toLocalDate())) {
                    // 2. Regla: Conflicto de Traslado/Locación en el mismo día (< 45 minutos de margen)
                    boolean differentLocation = !normalizeLocation(candidateScene.getLocation())
                            .equalsIgnoreCase(normalizeLocation(existing.getLocation()));

                    if (differentLocation) {
                        long gapMinutes;
                        if (!candEnd.isAfter(existStart)) {
                            // La escena candidata termina antes de que empiece la existente
                            gapMinutes = Duration.between(candEnd, existStart).toMinutes();
                        } else {
                            // La escena existente termina antes de que empiece la candidata
                            gapMinutes = Duration.between(existEnd, candStart).toMinutes();
                        }

                        if (gapMinutes >= 0 && gapMinutes < minimumTravelMinutes) {
                            String candidateSceneNum = candidateScene.getSceneNumber() != null ? candidateScene.getSceneNumber() : "nueva";
                            String message = String.format(
                                    "WARNING_LOCATION_CONFLICT: El actor %s tiene solo %d minutos de margen entre la escena %s y la escena %s en distinta locación (mínimo requerido: %d min).",
                                    actorName,
                                    gapMinutes,
                                    existSceneNum,
                                    candidateSceneNum,
                                    minimumTravelMinutes
                            );

                            conflicts.add(ConflictDetail.builder()
                                    .type(ConflictType.WARNING_LOCATION_CONFLICT)
                                    .actorId(actor.getId())
                                    .actorName(actorName)
                                    .conflictingSceneId(existing.getId())
                                    .conflictingSceneNumber(existSceneNum)
                                    .conflictingSceneTitle(existTitleDisplay)
                                    .conflictingScheduledTime(existStart)
                                    .conflictingDurationMinutes(existing.getDurationMinutes())
                                    .conflictingLocation(existing.getLocation())
                                    .newLocation(candidateScene.getLocation())
                                    .gapMinutes(gapMinutes)
                                    .minimumRequiredMinutes(minimumTravelMinutes)
                                    .message(message)
                                    .build());
                        }
                    }
                }
            }
        }

        return conflicts;
    }

    @Override
    public List<ConflictDetail> validateAndDetect(
            Scene candidateScene,
            List<Scene> existingScenes,
            boolean allowLocationWarnings
    ) {
        List<ConflictDetail> conflicts = detectConflicts(candidateScene, existingScenes);
        if (conflicts.isEmpty()) {
            return Collections.emptyList();
        }

        boolean hasOverlap = conflicts.stream()
                .anyMatch(c -> c.getType() == ConflictType.ACTOR_OVERLAP);

        if (hasOverlap) {
            log.warn("Rechazando guardado de escena por solapamiento de actor: {} conflicto(s) detectado(s)", conflicts.size());
            throw new ScheduleConflictException(
                    "Conflicto crítico de Solapamiento de Actor.",
                    conflicts
            );
        }

        boolean hasLocationWarning = conflicts.stream()
                .anyMatch(c -> c.getType() == ConflictType.WARNING_LOCATION_CONFLICT);

        if (hasLocationWarning && !allowLocationWarnings) {
            log.warn("Rechazando guardado de escena por margen insuficiente entre locaciones (allowWarnings=false)");
            throw new ScheduleConflictException(
                    "Advertencia de Traslado (WARNING_LOCATION_CONFLICT): Margen inferior a 45 minutos entre locaciones.",
                    conflicts
            );
        }

        return conflicts;
    }

    @Override
    public List<ConflictDetail> detectAllConflicts(List<Scene> scenes) {
        if (scenes == null || scenes.size() < 2) {
            return Collections.emptyList();
        }

        List<ConflictDetail> allConflicts = new ArrayList<>();
        Set<String> processedPairs = new HashSet<>();

        for (int i = 0; i < scenes.size(); i++) {
            Scene current = scenes.get(i);
            for (int j = i + 1; j < scenes.size(); j++) {
                Scene other = scenes.get(j);

                String pairKey = Math.min(current.getId(), other.getId()) + "-" + Math.max(current.getId(), other.getId());
                if (processedPairs.contains(pairKey)) {
                    continue;
                }
                processedPairs.add(pairKey);

                List<ConflictDetail> pairConflicts = detectConflicts(current, List.of(other));
                allConflicts.addAll(pairConflicts);
            }
        }

        return allConflicts;
    }

    private Set<User> findCommonActors(Set<User> actorsA, Set<User> actorsB) {
        if (actorsA == null || actorsB == null || actorsA.isEmpty() || actorsB.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> idsB = new HashSet<>();
        for (User u : actorsB) {
            if (u.getId() != null) idsB.add(u.getId());
        }

        Set<User> common = new HashSet<>();
        for (User u : actorsA) {
            if (u.getId() != null && idsB.contains(u.getId())) {
                common.add(u);
            }
        }
        return common;
    }

    private String normalizeLocation(String location) {
        return location != null ? location.trim() : "";
    }
}
