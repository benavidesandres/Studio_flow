package com.studioflow.dto.conflict;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictDetail {

    private ConflictType type;
    private Long actorId;
    private String actorName;
    private Long conflictingSceneId;
    private Integer conflictingSceneNumber;
    private String conflictingSceneTitle;
    private LocalDateTime conflictingScheduledTime;
    private Integer conflictingDurationMinutes;
    private String conflictingLocation;
    private String newLocation;
    private long gapMinutes;
    private int minimumRequiredMinutes;
    private String message;
}
