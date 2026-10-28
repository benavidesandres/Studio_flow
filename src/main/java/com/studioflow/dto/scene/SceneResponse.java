package com.studioflow.dto.scene;

import com.studioflow.dto.conflict.ConflictDetail;
import com.studioflow.dto.user.UserResponse;
import com.studioflow.model.Scene;
import com.studioflow.model.SceneStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneResponse {

    private Long id;
    private Long productionId;
    private String productionTitle;
    private Integer sceneNumber;
    private String title;
    private LocalDateTime scheduledTime;
    private LocalDateTime endTime;
    private SceneStatus status;
    private String location;
    private BigDecimal cost;
    private Integer durationMinutes;
    @Builder.Default
    private Set<UserResponse> actors = new HashSet<>();
    @Builder.Default
    private List<ConflictDetail> warnings = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SceneResponse fromEntity(Scene scene) {
        if (scene == null) return null;

        Set<UserResponse> actorDtos = scene.getActors() != null
                ? scene.getActors().stream().map(UserResponse::fromEntity).collect(Collectors.toSet())
                : new HashSet<>();

        return SceneResponse.builder()
                .id(scene.getId())
                .productionId(scene.getProduction() != null ? scene.getProduction().getId() : null)
                .productionTitle(scene.getProduction() != null ? scene.getProduction().getTitle() : null)
                .sceneNumber(scene.getSceneNumber())
                .title(scene.getTitle())
                .scheduledTime(scene.getScheduledTime())
                .endTime(scene.getEndTime())
                .status(scene.getStatus())
                .location(scene.getLocation())
                .cost(scene.getCost())
                .durationMinutes(scene.getDurationMinutes())
                .actors(actorDtos)
                .createdAt(scene.getCreatedAt())
                .updatedAt(scene.getUpdatedAt())
                .build();
    }
}
