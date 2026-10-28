package com.studioflow.service.conflict;

import com.studioflow.dto.conflict.ConflictDetail;
import com.studioflow.model.Scene;

import java.util.List;

public interface ConflictDetectionEngine {

    /**
     * Evalúa los conflictos potenciales de una escena contra una lista de escenas existentes.
     *
     * @param candidateScene Escena a evaluar (nueva o modificada)
     * @param existingScenes Escenas contra las cuales contrastar
     * @return Lista de conflictos detectados (solapamientos y advertencias de traslado)
     */
    List<ConflictDetail> detectConflicts(Scene candidateScene, List<Scene> existingScenes);

    /**
     * Valida que no existan conflictos o solo advertencias permitidas.
     * Lanza ScheduleConflictException si se detecta solapamiento crítico o si
     * existen advertencias de locación y allowWarnings es falso.
     *
     * @param candidateScene Escena a evaluar
     * @param existingScenes Escenas existentes
     * @param allowLocationWarnings Si true, permite advertencias de traslado pero no solapamientos
     * @return Lista de advertencias no bloqueantes si allowLocationWarnings es true
     */
    List<ConflictDetail> validateAndDetect(
            Scene candidateScene,
            List<Scene> existingScenes,
            boolean allowLocationWarnings
    );

    /**
     * Analiza todas las escenas de una producción para identificar todos los conflictos existentes.
     *
     * @param scenes Todas las escenas de la producción
     * @return Lista completa de conflictos detectados en la producción
     */
    List<ConflictDetail> detectAllConflicts(List<Scene> scenes);
}
