package com.studioflow.controller;

import com.studioflow.dto.conflict.ConflictDetail;
import com.studioflow.dto.scene.SceneRequest;
import com.studioflow.dto.scene.SceneResponse;
import com.studioflow.dto.scene.SceneStatusUpdateRequest;
import com.studioflow.exception.BadRequestException;
import com.studioflow.service.SceneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/scenes")
@RequiredArgsConstructor
public class SceneController {

    private final SceneService sceneService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PRODUCER', 'DIRECTOR')")
    public ResponseEntity<SceneResponse> createScene(
            @RequestParam(required = false) Long productionId,
            @Valid @RequestBody SceneRequest request
    ) {
        Long targetProdId = productionId != null ? productionId : request.getProductionId();
        if (targetProdId == null) {
            throw new BadRequestException("El id de la producción es obligatorio (por query param o en el cuerpo de la petición)");
        }
        SceneResponse response = sceneService.createScene(targetProdId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRODUCER', 'DIRECTOR', 'CREW', 'ACTOR')")
    public ResponseEntity<SceneResponse> getSceneById(@PathVariable Long id) {
        SceneResponse response = sceneService.getSceneById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRODUCER', 'DIRECTOR')")
    public ResponseEntity<SceneResponse> updateScene(
            @PathVariable Long id,
            @Valid @RequestBody SceneRequest request
    ) {
        SceneResponse response = sceneService.updateScene(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PRODUCER', 'DIRECTOR')")
    public ResponseEntity<SceneResponse> updateSceneStatus(
            @PathVariable Long id,
            @Valid @RequestBody SceneStatusUpdateRequest request
    ) {
        SceneResponse response = sceneService.updateSceneStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PRODUCER')")
    public ResponseEntity<Void> deleteScene(@PathVariable Long id) {
        sceneService.deleteScene(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate-conflicts")
    @PreAuthorize("hasAnyRole('PRODUCER', 'DIRECTOR')")
    public ResponseEntity<List<ConflictDetail>> validateConflicts(
            @RequestParam(required = false) Long productionId,
            @Valid @RequestBody SceneRequest request
    ) {
        Long targetProdId = productionId != null ? productionId : request.getProductionId();
        List<ConflictDetail> conflicts = sceneService.validateConflicts(targetProdId, request);
        return ResponseEntity.ok(conflicts);
    }

    @GetMapping("/actor/{actorId}")
    @PreAuthorize("hasAnyRole('PRODUCER', 'DIRECTOR', 'CREW', 'ACTOR')")
    public ResponseEntity<List<SceneResponse>> getActorSchedule(@PathVariable Long actorId) {
        List<SceneResponse> scenes = sceneService.getActorSchedule(actorId);
        return ResponseEntity.ok(scenes);
    }
}
