package com.studioflow.dto.production;

import com.studioflow.model.Production;
import com.studioflow.model.Scene;
import com.studioflow.model.SceneStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal totalBudget;
    private BigDecimal spentBudget;
    private BigDecimal remainingBudget;
    private Integer progressPercentage;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalScenes;
    private int completedScenes;
    private int readyScenes;
    private int delayedScenes;
    private int problemScenes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductionResponse fromEntity(Production production) {
        if (production == null) return null;

        int totalScenes = production.getScenes() != null ? production.getScenes().size() : 0;
        int completed = 0;
        int ready = 0;
        int delayed = 0;
        int problem = 0;

        if (production.getScenes() != null) {
            for (Scene s : production.getScenes()) {
                if (s.getStatus() == SceneStatus.COMPLETED) completed++;
                else if (s.getStatus() == SceneStatus.READY) ready++;
                else if (s.getStatus() == SceneStatus.DELAYED) delayed++;
                else if (s.getStatus() == SceneStatus.PROBLEM) problem++;
            }
        }

        BigDecimal total = production.getTotalBudget() != null ? production.getTotalBudget() : BigDecimal.ZERO;
        BigDecimal spent = production.getSpentBudget() != null ? production.getSpentBudget() : BigDecimal.ZERO;
        BigDecimal remaining = total.subtract(spent);

        return ProductionResponse.builder()
                .id(production.getId())
                .title(production.getTitle())
                .description(production.getDescription())
                .totalBudget(total)
                .spentBudget(spent)
                .remainingBudget(remaining)
                .progressPercentage(production.getProgressPercentage() != null ? production.getProgressPercentage() : 0)
                .startDate(production.getStartDate())
                .endDate(production.getEndDate())
                .totalScenes(totalScenes)
                .completedScenes(completed)
                .readyScenes(ready)
                .delayedScenes(delayed)
                .problemScenes(problem)
                .createdAt(production.getCreatedAt())
                .updatedAt(production.getUpdatedAt())
                .build();
    }
}
