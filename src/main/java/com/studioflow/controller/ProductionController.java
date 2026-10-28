package com.studioflow.controller;

import com.studioflow.dto.conflict.ConflictDetail;
import com.studioflow.dto.production.ProductionRequest;
import com.studioflow.dto.production.ProductionResponse;
import com.studioflow.dto.scene.DailyScheduleResponse;
import com.studioflow.dto.scene.SceneResponse;
import com.studioflow.service.ProductionService;
import com.studioflow.service.SceneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/productions")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;
    private final SceneService sceneService;

    @PostMapping
    @PreAuthorize("hasRole('PRODUCER')")
    public ResponseEntity<ProductionResponse> createProduction(@Valid @RequestBody ProductionRequest request) {
        ProductionResponse response = productionService.createProduction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PRODUCER', 'DIRECTOR', 'CREW')")
    public ResponseEntity<List<ProductionResponse>> getAllProductions() {
        List<ProductionResponse> list = productionService.getAllProductions();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRODUCER', 'DIRECTOR', 'CREW', 'ACTOR')")
    public ResponseEntity<ProductionResponse> getProductionById(@PathVariable Long id) {
        ProductionResponse response = productionService.getProductionById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PRODUCER')")
    public ResponseEntity<ProductionResponse> updateProduction(
            @PathVariable Long id,
            @Valid @RequestBody ProductionRequest request
    ) {
        ProductionResponse response = productionService.updateProduction(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PRODUCER')")
    public ResponseEntity<Void> deleteProduction(@PathVariable Long id) {
        productionService.deleteProduction(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/scenes")
    @PreAuthorize("hasAnyRole('PRODUCER', 'DIRECTOR', 'CREW', 'ACTOR')")
    public ResponseEntity<List<SceneResponse>> getScenesByProduction(@PathVariable Long id) {
        List<SceneResponse> scenes = sceneService.getScenesByProduction(id);
        return ResponseEntity.ok(scenes);
    }

    @GetMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('PRODUCER', 'DIRECTOR', 'CREW', 'ACTOR')")
    public ResponseEntity<DailyScheduleResponse> getDailySchedule(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        DailyScheduleResponse schedule = sceneService.getDailySchedule(id, queryDate);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/{id}/conflicts")
    @PreAuthorize("hasAnyRole('PRODUCER', 'DIRECTOR')")
    public ResponseEntity<List<ConflictDetail>> getProductionConflicts(@PathVariable Long id) {
        List<ConflictDetail> conflicts = sceneService.getProductionConflicts(id);
        return ResponseEntity.ok(conflicts);
    }

    @PostMapping("/{id}/recalculate")
    @PreAuthorize("hasAnyRole('PRODUCER', 'DIRECTOR')")
    public ResponseEntity<ProductionResponse> recalculateMetrics(@PathVariable Long id) {
        productionService.recalculateMetrics(id);
        ProductionResponse updated = productionService.getProductionById(id);
        return ResponseEntity.ok(updated);
    }
}
