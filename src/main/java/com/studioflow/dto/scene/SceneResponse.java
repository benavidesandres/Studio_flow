package com.studioflow.dto.scene;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.studioflow.dto.conflict.ConflictDetail;
import com.studioflow.dto.user.UserResponse;
import com.studioflow.model.Scene;
import com.studioflow.model.SceneStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SceneResponse {

    private Long id;
    private Long productionId;
    private String productionTitle;
    private String sceneNumber;
    private String title;
    private String location;
    private LocalDate shootDate;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
    private LocalDateTime scheduledTime;
    private Integer durationMinutes;
    private SceneStatus status;
    private BigDecimal cost;
    private String notes;
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

        LocalDate date = scene.getShootDate();
        LocalTime start = scene.getStartTime();
        LocalTime end = scene.getEndTime();

        if (date == null && scene.getScheduledTime() != null) {
            date = scene.getScheduledTime().toLocalDate();
        }
        if (start == null && scene.getScheduledTime() != null) {
            start = scene.getScheduledTime().toLocalTime();
        }
        if (end == null && start != null && scene.getDurationMinutes() != null) {
            end = start.plusMinutes(scene.getDurationMinutes());
        }

        return SceneResponse.builder()
                .id(scene.getId())
                .productionId(scene.getProduction() != null ? scene.getProduction().getId() : null)
                .productionTitle(scene.getProduction() != null ? scene.getProduction().getTitle() : null)
                .sceneNumber(scene.getSceneNumber())
                .title(scene.getTitle())
                .location(scene.getLocation())
                .shootDate(date)
                .startTime(start)
                .endTime(end)
                .scheduledTime(scene.getScheduledTime())
                .durationMinutes(scene.getDurationMinutes())
                .status(scene.getStatus())
                .cost(scene.getCost())
                .notes(scene.getNotes())
                .actors(actorDtos)
                .createdAt(scene.getCreatedAt())
                .updatedAt(scene.getUpdatedAt())
                .build();
    }
}
