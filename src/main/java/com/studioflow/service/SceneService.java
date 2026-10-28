package com.studioflow.service;

import com.studioflow.dto.conflict.ConflictDetail;
import com.studioflow.dto.scene.DailyScheduleResponse;
import com.studioflow.dto.scene.SceneRequest;
import com.studioflow.dto.scene.SceneResponse;
import com.studioflow.dto.scene.SceneStatusUpdateRequest;

import java.time.LocalDate;
import java.util.List;

public interface SceneService {

    SceneResponse createScene(Long productionId, SceneRequest request);

    SceneResponse getSceneById(Long id);

    List<SceneResponse> getScenesByProduction(Long productionId);

    SceneResponse updateScene(Long id, SceneRequest request);

    SceneResponse updateSceneStatus(Long id, SceneStatusUpdateRequest request);

    void deleteScene(Long id);

    DailyScheduleResponse getDailySchedule(Long productionId, LocalDate date);

    List<ConflictDetail> validateConflicts(Long productionId, SceneRequest request);

    List<ConflictDetail> getProductionConflicts(Long productionId);

    List<SceneResponse> getActorSchedule(Long actorId);
}
