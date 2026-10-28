package com.studioflow.exception;

import com.studioflow.dto.conflict.ConflictDetail;
import lombok.Getter;

import java.util.List;

@Getter
public class ScheduleConflictException extends RuntimeException {

    private final List<ConflictDetail> conflicts;

    public ScheduleConflictException(String message, List<ConflictDetail> conflicts) {
        super(message);
        this.conflicts = conflicts;
    }

    public ScheduleConflictException(List<ConflictDetail> conflicts) {
        super("Se han detectado conflictos de programación en la escena");
        this.conflicts = conflicts;
    }
}
