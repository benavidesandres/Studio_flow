package com.studioflow.dto.production;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionRequest {

    @NotBlank(message = "El título de la producción es obligatorio")
    private String title;

    private String description;

    @NotNull(message = "El presupuesto total es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El presupuesto debe ser mayor o igual a 0")
    private BigDecimal totalBudget;

    private LocalDate startDate;

    private LocalDate endDate;
}
