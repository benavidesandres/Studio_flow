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
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${studioflow.conflict.minimum-travel-minutes:45}")
    private int minimumTravelMinutes;

    @Override
    public List<ConflictDetail> detectConflicts(Scene candidateScene, List<Scene> existingScenes) {
        if (candidateScene == null || candidateScene.getActors() == null || candidateScene.getActors().isEmpty()) {
            return Collections.emptyList();
        }

        List<ConflictDetail> conflicts = new ArrayList<>();
        LocalDateTime candStart = candidateScene.getScheduledTime();
        LocalDateTime candEnd = candidateScene.getEndTime();

        if (candStart == null || candEnd == null) {
            return Collections.emptyList();
        }

        for (Scene existing : existingScenes) {
            // Ignorar la misma escena al actualizar
            if (candidateScene.getId() != null && candidateScene.getId().equals(existing.getId())) {
                continue;
            }

            LocalDateTime existStart = existing.getScheduledTime();
            LocalDateTime existEnd = existing.getEndTime();

            if (existStart == null || existEnd == null) {
                continue;
            }

            // Identificar actores en común
            Set<User> commonActors = findCommonActors(candidateScene.getActors(), existing.getActors());
            if (commonActors.isEmpty()) {
                continue;
            }

            for (User actor : commonActors) {
                // 1. Regla: Solapamiento simultáneo de actor
                boolean isOverlapping = candStart.isBefore(existEnd) && existStart.isBefore(candEnd);

                if (isOverlapping) {
                    long overlapMinutes = Math.min(
                            Duration.between(candStart, existEnd).toMinutes(),
                            Duration.between(existStart, candEnd).toMinutes()
                    );

                    String message = String.format(
                            "CONFLICTO CRÍTICO (SOLAPAMIENTO): El actor %s está asignado a la Escena #%d ('%s') de %s a %s, lo cual se solapa con el horario programado (%s a %s).",
                            actor.getFullName(),
                            existing.getSceneNumber(),
                            existing.getTitle(),
                            existStart.format(TIME_FORMATTER),
                            existEnd.format(TIME_FORMATTER),
                            candStart.format(TIME_FORMATTER),
                            candEnd.format(TIME_FORMATTER)
                    );

                    conflicts.add(ConflictDetail.builder()
                            .type(ConflictType.ACTOR_OVERLAP)
                            .actorId(actor.getId())
                            .actorName(actor.getFullName())
                            .conflictingSceneId(existing.getId())
                            .conflictingSceneNumber(existing.getSceneNumber())
                            .conflictingSceneTitle(existing.getTitle())
                            .conflictingScheduledTime(existStart)
                            .conflictingDurationMinutes(existing.getDurationMinutes())
                            .conflictingLocation(existing.getLocation())
                            .newLocation(candidateScene.getLocation())
                            .gapMinutes(-overlapMinutes)
                            .minimumRequiredMinutes(0)
                            .message(message)
                            .build());

                } else if (candStart.toLocalDate().equals(existStart.toLocalDate())) {
                    // 2. Regla: Conflicto de Traslado/Locación en el mismo día
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
                            String message = String.format(
                                    "WARNING_LOCATION_CONFLICT: El actor %s tiene solo %d minutos de intervalo entre la Escena #%d en '%s' y la Escena #%d en '%s' (el tiempo mínimo requerido de traslado es de %d minutos).",
                                    actor.getFullName(),
                                    gapMinutes,
                                    existing.getSceneNumber(),
                                    existing.getLocation(),
                                    candidateScene.getSceneNumber() != null ? candidateScene.getSceneNumber() : 0,
                                    candidateScene.getLocation(),
                                    minimumTravelMinutes
                            );

                            conflicts.add(ConflictDetail.builder()
                                    .type(ConflictType.WARNING_LOCATION_CONFLICT)
                                    .actorId(actor.getId())
                                    .actorName(actor.getFullName())
                                    .conflictingSceneId(existing.getId())
                                    .conflictingSceneNumber(existing.getSceneNumber())
                                    .conflictingSceneTitle(existing.getTitle())
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
            log.warn("Rechazando guardado de escena por solapamiento de actor: {} conflictos detectados", conflicts.size());
            throw new ScheduleConflictException(
                    "Conflicto de Solapamiento de Actor: Un mismo actor no puede estar citado en dos escenas simultáneas.",
                    conflicts
            );
        }

        boolean hasLocationWarning = conflicts.stream()
                .anyMatch(c -> c.getType() == ConflictType.WARNING_LOCATION_CONFLICT);

        if (hasLocationWarning && !allowLocationWarnings) {
            log.warn("Rechazando guardado de escena por conflicto de traslado entre locaciones");
            throw new ScheduleConflictException(
                    "WARNING_LOCATION_CONFLICT: Margen de tiempo insuficiente para el traslado del actor entre locaciones distintas.",
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
