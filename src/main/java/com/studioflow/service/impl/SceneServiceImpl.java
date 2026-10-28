package com.studioflow.service.impl;

import com.studioflow.dto.conflict.ConflictDetail;
import com.studioflow.dto.scene.DailyScheduleResponse;
import com.studioflow.dto.scene.SceneRequest;
import com.studioflow.dto.scene.SceneResponse;
import com.studioflow.dto.scene.SceneStatusUpdateRequest;
import com.studioflow.exception.BadRequestException;
import com.studioflow.exception.ResourceNotFoundException;
import com.studioflow.model.Production;
import com.studioflow.model.Role;
import com.studioflow.model.Scene;
import com.studioflow.model.SceneStatus;
import com.studioflow.model.User;
import com.studioflow.repository.ProductionRepository;
import com.studioflow.repository.SceneRepository;
import com.studioflow.repository.UserRepository;
import com.studioflow.service.ProductionService;
import com.studioflow.service.SceneService;
import com.studioflow.service.conflict.ConflictDetectionEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SceneServiceImpl implements SceneService {

    private final SceneRepository sceneRepository;
    private final ProductionRepository productionRepository;
    private final UserRepository userRepository;
    private final ConflictDetectionEngine conflictDetectionEngine;
    private final ProductionService productionService;

    @Override
    @Transactional
    public SceneResponse createScene(Long productionId, SceneRequest request) {
        log.info("Creando escena #{} para producción id: {}", request.getSceneNumber(), productionId);

        Production production = productionRepository.findById(productionId)
                .orElseThrow(() -> new ResourceNotFoundException("Producción", "id", productionId));

        Set<User> actors = resolveActors(request.getActorIds());

        Scene candidateScene = Scene.builder()
                .production(production)
                .sceneNumber(request.getSceneNumber())
                .title(request.getTitle().trim())
                .scheduledTime(request.getScheduledTime())
                .status(request.getStatus() != null ? request.getStatus() : SceneStatus.READY)
                .location(request.getLocation().trim())
                .cost(request.getCost() != null ? request.getCost() : BigDecimal.ZERO)
                .durationMinutes(request.getDurationMinutes())
                .actors(actors)
                .build();

        // Obtener escenas candidatas de los actores y de la producción para evaluar conflictos
        List<Scene> existingScenes = findCandidateScenesForEvaluation(candidateScene, actors, productionId);

        // Ejecutar motor de detección de conflictos
        List<ConflictDetail> warnings = conflictDetectionEngine.validateAndDetect(
                candidateScene,
                existingScenes,
                request.isAllowWarnings()
        );

        Scene savedScene = sceneRepository.save(candidateScene);
        productionService.recalculateMetrics(productionId);

        SceneResponse response = SceneResponse.fromEntity(savedScene);
        response.setWarnings(warnings);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public SceneResponse getSceneById(Long id) {
        Scene scene = sceneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escena", "id", id));
        return SceneResponse.fromEntity(scene);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SceneResponse> getScenesByProduction(Long productionId) {
        if (!productionRepository.existsById(productionId)) {
            throw new ResourceNotFoundException("Producción", "id", productionId);
        }
        return sceneRepository.findAllByProductionIdWithActors(productionId).stream()
                .map(SceneResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public SceneResponse updateScene(Long id, SceneRequest request) {
        log.info("Actualizando escena id: {}", id);
        Scene scene = sceneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escena", "id", id));

        Set<User> actors = resolveActors(request.getActorIds());

        // Actualizar datos preliminares en entidad para evaluación
        scene.setSceneNumber(request.getSceneNumber());
        scene.setTitle(request.getTitle().trim());
        scene.setScheduledTime(request.getScheduledTime());
        if (request.getStatus() != null) {
            scene.setStatus(request.getStatus());
        }
        scene.setLocation(request.getLocation().trim());
        if (request.getCost() != null) {
            scene.setCost(request.getCost());
        }
        scene.setDurationMinutes(request.getDurationMinutes());
        scene.setActors(actors);

        // Buscar escenas existentes para contraste
        List<Scene> existingScenes = findCandidateScenesForEvaluation(scene, actors, scene.getProduction().getId());

        // Validar conflictos
        List<ConflictDetail> warnings = conflictDetectionEngine.validateAndDetect(
                scene,
                existingScenes,
                request.isAllowWarnings()
        );

        Scene updatedScene = sceneRepository.save(scene);
        productionService.recalculateMetrics(scene.getProduction().getId());

        SceneResponse response = SceneResponse.fromEntity(updatedScene);
        response.setWarnings(warnings);
        return response;
    }

    @Override
    @Transactional
    public SceneResponse updateSceneStatus(Long id, SceneStatusUpdateRequest request) {
        log.info("Actualizando estado de escena id: {} a {}", id, request.getStatus());
        Scene scene = sceneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escena", "id", id));

        scene.setStatus(request.getStatus());
        Scene updatedScene = sceneRepository.save(scene);

        productionService.recalculateMetrics(scene.getProduction().getId());
        return SceneResponse.fromEntity(updatedScene);
    }

    @Override
    @Transactional
    public void deleteScene(Long id) {
        log.info("Eliminando escena id: {}", id);
        Scene scene = sceneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Escena", "id", id));
        Long productionId = scene.getProduction().getId();

        sceneRepository.delete(scene);
        productionService.recalculateMetrics(productionId);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyScheduleResponse getDailySchedule(Long productionId, LocalDate date) {
        Production production = productionRepository.findById(productionId)
                .orElseThrow(() -> new ResourceNotFoundException("Producción", "id", productionId));

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Scene> dailyScenes = sceneRepository.findByProductionIdAndScheduledTimeBetweenOrderByScheduledTimeAsc(
                productionId,
                startOfDay,
                endOfDay
        );

        int total = dailyScenes.size();
        int ready = 0;
        int delayed = 0;
        int problem = 0;
        int completed = 0;

        for (Scene s : dailyScenes) {
            if (s.getStatus() == SceneStatus.READY) ready++;
            else if (s.getStatus() == SceneStatus.DELAYED) delayed++;
            else if (s.getStatus() == SceneStatus.PROBLEM) problem++;
            else if (s.getStatus() == SceneStatus.COMPLETED) completed++;
        }

        // Evaluar conflictos entre las escenas del día
        List<ConflictDetail> conflicts = conflictDetectionEngine.detectAllConflicts(dailyScenes);

        List<SceneResponse> sceneResponses = dailyScenes.stream()
                .map(SceneResponse::fromEntity)
                .toList();

        return DailyScheduleResponse.builder()
                .date(date)
                .productionId(production.getId())
                .productionTitle(production.getTitle())
                .totalScenes(total)
                .readyCount(ready)
                .delayedCount(delayed)
                .problemCount(problem)
                .completedCount(completed)
                .scenes(sceneResponses)
                .conflicts(conflicts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConflictDetail> validateConflicts(Long productionId, SceneRequest request) {
        Set<User> actors = resolveActors(request.getActorIds());

        Scene candidateScene = Scene.builder()
                .sceneNumber(request.getSceneNumber())
                .title(request.getTitle())
                .scheduledTime(request.getScheduledTime())
                .location(request.getLocation())
                .durationMinutes(request.getDurationMinutes())
                .actors(actors)
                .build();

        List<Scene> existingScenes = findCandidateScenesForEvaluation(candidateScene, actors, productionId);
        return conflictDetectionEngine.detectConflicts(candidateScene, existingScenes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConflictDetail> getProductionConflicts(Long productionId) {
        if (!productionRepository.existsById(productionId)) {
            throw new ResourceNotFoundException("Producción", "id", productionId);
        }
        List<Scene> scenes = sceneRepository.findAllByProductionIdWithActors(productionId);
        return conflictDetectionEngine.detectAllConflicts(scenes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SceneResponse> getActorSchedule(Long actorId) {
        User user = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actorId));

        if (user.getRole() != Role.ACTOR) {
            throw new BadRequestException("El usuario con id " + actorId + " no es un ACTOR");
        }

        return sceneRepository.findScenesByActorId(actorId).stream()
                .map(SceneResponse::fromEntity)
                .toList();
    }

    private Set<User> resolveActors(Set<Long> actorIds) {
        if (actorIds == null || actorIds.isEmpty()) {
            return new HashSet<>();
        }

        List<User> users = userRepository.findAllById(actorIds);
        if (users.size() != actorIds.size()) {
            Set<Long> foundIds = users.stream().map(User::getId).collect(Collectors.toSet());
            Set<Long> missing = new HashSet<>(actorIds);
            missing.removeAll(foundIds);
            throw new BadRequestException("Los siguientes actores no existen: " + missing);
        }

        // Validar que todos tengan rol ACTOR
        for (User u : users) {
            if (u.getRole() != Role.ACTOR) {
                throw new BadRequestException(String.format(
                        "El usuario %s (%s) tiene el rol %s, no puede ser asignado como ACTOR en una escena",
                        u.getFullName(),
                        u.getEmail(),
                        u.getRole()
                ));
            }
        }

        return new HashSet<>(users);
    }

    private List<Scene> findCandidateScenesForEvaluation(Scene candidate, Set<User> actors, Long productionId) {
        if (candidate.getScheduledTime() == null) {
            return Collections.emptyList();
        }

        LocalDate sceneDate = candidate.getScheduledTime().toLocalDate();
        LocalDateTime windowStart = sceneDate.minusDays(1).atStartOfDay();
        LocalDateTime windowEnd = sceneDate.plusDays(1).atTime(LocalTime.MAX);

        Set<Scene> candidateSet = new HashSet<>();

        // Escenas de la misma producción en el rango de fechas
        if (productionId != null) {
            List<Scene> prodScenes = sceneRepository.findByProductionIdAndScheduledTimeBetweenOrderByScheduledTimeAsc(
                    productionId,
                    windowStart,
                    windowEnd
            );
            candidateSet.addAll(prodScenes);
        }

        // Escenas en las que participan los mismos actores (incluso si pertenecen a otra producción)
        if (actors != null && !actors.isEmpty()) {
            Set<Long> actorIds = actors.stream().map(User::getId).collect(Collectors.toSet());
            List<Scene> actorScenes = sceneRepository.findCandidateScenesForActors(actorIds, windowStart, windowEnd);
            candidateSet.addAll(actorScenes);
        }

        return new ArrayList<>(candidateSet);
    }
}
