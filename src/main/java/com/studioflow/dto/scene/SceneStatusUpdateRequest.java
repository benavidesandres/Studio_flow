package com.studioflow.dto.scene;

import com.studioflow.model.SceneStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneStatusUpdateRequest {

    @NotNull(message = "El nuevo estado de la escena es obligatorio (READY, DELAYED, PROBLEM, COMPLETED)")
    private SceneStatus status;

    private String notes;
}
