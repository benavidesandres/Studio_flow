package com.studioflow.dto.scene;

import com.studioflow.model.SceneStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneRequest {

    private Long productionId;

    @NotNull(message = "El número de escena es obligatorio")
    @Min(value = 1, message = "El número de escena debe ser mayor a 0")
    private Integer sceneNumber;

    @NotBlank(message = "El título de la escena es obligatorio")
    private String title;

    @NotNull(message = "El horario programado es obligatorio")
    private LocalDateTime scheduledTime;

    @Builder.Default
    private SceneStatus status = SceneStatus.READY;

    @NotBlank(message = "La locación es obligatoria")
    private String location;

    @DecimalMin(value = "0.0", inclusive = true, message = "El coste no puede ser negativo")
    @Builder.Default
    private BigDecimal cost = BigDecimal.ZERO;

    @NotNull(message = "La duración en minutos es obligatoria")
    @Min(value = 1, message = "La duración debe ser de al menos 1 minuto")
    @Builder.Default
    private Integer durationMinutes = 60;

    @Builder.Default
    private Set<Long> actorIds = new HashSet<>();

    /**
     * Si es true, el sistema permite guardar aunque existan advertencias WARNING_LOCATION_CONFLICT.
     * Si existen conflictos críticos ACTOR_OVERLAP, siempre se rechaza.
     */
    @Builder.Default
    private boolean allowWarnings = false;
}
