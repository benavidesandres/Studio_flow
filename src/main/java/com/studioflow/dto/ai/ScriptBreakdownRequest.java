package com.studioflow.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScriptBreakdownRequest {

    /** Texto del guion, tal cual lo pega el usuario. La longitud máxima la controla el servicio. */
    @NotBlank(message = "El guion no puede estar vacío")
    private String script;
}
