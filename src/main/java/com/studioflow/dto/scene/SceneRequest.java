package com.studioflow.dto.scene;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.studioflow.model.SceneStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneRequest {

    private Long productionId;

    @NotNull(message = "El número de escena es obligatorio")
    private Object sceneNumber;

    @NotBlank(message = "El título de la escena es obligatorio")
    private String title;

    @NotBlank(message = "La locación es obligatoria")
    private String location;

    private LocalDate shootDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private LocalDateTime scheduledTime;

    private Integer durationMinutes;

    @Builder.Default
    private SceneStatus status = SceneStatus.READY;

    @DecimalMin(value = "0.0", inclusive = true, message = "El coste no puede ser negativo")
    @Builder.Default
    private BigDecimal cost = BigDecimal.ZERO;

    @Builder.Default
    private Set<Long> actorIds = new HashSet<>();

    /**
     * Si es true, el sistema permite guardar aunque existan advertencias WARNING_LOCATION_CONFLICT.
     * Si existen conflictos críticos ACTOR_OVERLAP, siempre se rechaza.
     */
    @Builder.Default
    private boolean allowWarnings = false;

    private String notes;

    public String resolveSceneNumber() {
        return sceneNumber != null ? String.valueOf(sceneNumber).trim() : "";
    }
}
