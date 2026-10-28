package com.studioflow.dto.scene;

import com.studioflow.dto.conflict.ConflictDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyScheduleResponse {

    private LocalDate date;
    private Long productionId;
    private String productionTitle;
    private int totalScenes;
    private int readyCount;
    private int delayedCount;
    private int problemCount;
    private int completedCount;
    @Builder.Default
    private List<SceneResponse> scenes = new ArrayList<>();
    @Builder.Default
    private List<ConflictDetail> conflicts = new ArrayList<>();
}
