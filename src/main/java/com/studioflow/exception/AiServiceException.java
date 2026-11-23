package com.studioflow.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Fallo al hablar con el proveedor de IA; el estado indica al cliente si puede reintentar. */
@Getter
public class AiServiceException extends RuntimeException {

    private final HttpStatus status;

    public AiServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public AiServiceException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
