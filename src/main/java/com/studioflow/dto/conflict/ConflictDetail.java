package com.studioflow.dto.conflict;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConflictDetail {

    private ConflictType type;
    private Long actorId;
    private String actorName;
    private Long conflictingSceneId;
    private String conflictingSceneTitle;
    private String message;

    // Campos complementarios útiles para cálculo y frontend
    private String conflictingSceneNumber;
    private LocalDateTime conflictingScheduledTime;
    private Integer conflictingDurationMinutes;
    private String conflictingLocation;
    private String newLocation;
    private Long gapMinutes;
    private Integer minimumRequiredMinutes;
}
