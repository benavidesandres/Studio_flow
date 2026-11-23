package com.studioflow.dto.ai;

import java.util.List;

/**
 * Desglose de un guion: las escenas en orden y los personajes y locaciones que aparecen en ellas.
 * No crea nada: el cliente revisa el resultado y decide qué escenas programar.
 */
public record ScriptBreakdownResponse(
        List<BreakdownScene> scenes,
        List<String> characters,
        List<String> locations
) {

    /**
     * @param setting          INT, EXT o INT/EXT
     * @param timeOfDay        DÍA, NOCHE, AMANECER, ATARDECER o CONTINUO
     * @param estimatedMinutes tiempo de rodaje estimado
     */
    public record BreakdownScene(
            String sceneNumber,
            String title,
            String location,
            String setting,
            String timeOfDay,
            List<String> characters,
            Integer estimatedMinutes,
            String summary
    ) {
    }
}
