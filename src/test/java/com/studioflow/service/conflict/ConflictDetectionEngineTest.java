package com.studioflow.service.conflict;

import com.studioflow.dto.conflict.ConflictDetail;
import com.studioflow.dto.conflict.ConflictType;
import com.studioflow.exception.ScheduleConflictException;
import com.studioflow.model.Role;
import com.studioflow.model.Scene;
import com.studioflow.model.SceneStatus;
import com.studioflow.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConflictDetectionEngineTest {

    private ConflictDetectionEngineImpl conflictEngine;

    private User actor1;
    private User actor2;

    @BeforeEach
    void setUp() {
        conflictEngine = new ConflictDetectionEngineImpl();
        ReflectionTestUtils.setField(conflictEngine, "minimumTravelMinutes", 45);

        actor1 = User.builder()
                .id(1L)
                .email("cillian@studioflow.com")
                .fullName("Cillian Murphy")
                .role(Role.ACTOR)
                .build();

        actor2 = User.builder()
                .id(2L)
                .email("emily@studioflow.com")
                .fullName("Emily Blunt")
                .role(Role.ACTOR)
                .build();
    }

    @Test
    @DisplayName("Debe detectar conflicto de solapamiento cuando un actor está citado en dos escenas simultáneas")
    void shouldDetectActorOverlapConflict() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 10, 10, 0);

        Scene existingScene = Scene.builder()
                .id(101L)
                .sceneNumber(1)
                .title("Discusión en el laboratorio")
                .scheduledTime(baseTime) // 10:00 - 11:30
                .durationMinutes(90)
                .location("Estudio A - Laboratorio")
                .status(SceneStatus.READY)
                .cost(BigDecimal.valueOf(5000))
                .actors(Set.of(actor1))
                .build();

        Scene candidateScene = Scene.builder()
                .id(102L)
                .sceneNumber(2)
                .title("Reunión con el comité")
                .scheduledTime(baseTime.plusMinutes(45)) // 10:45 - 11:45 (Solapa de 10:45 a 11:30)
                .durationMinutes(60)
                .location("Estudio B - Sala de Juntas")
                .status(SceneStatus.READY)
                .cost(BigDecimal.valueOf(3000))
                .actors(Set.of(actor1))
                .build();

        List<ConflictDetail> conflicts = conflictEngine.detectConflicts(candidateScene, List.of(existingScene));

        assertFalse(conflicts.isEmpty(), "Se debía haber detectado un conflicto");
        assertEquals(1, conflicts.size());
        assertEquals(ConflictType.ACTOR_OVERLAP, conflicts.get(0).getType());
        assertEquals(actor1.getId(), conflicts.get(0).getActorId());
        assertTrue(conflicts.get(0).getMessage().contains("SOLAPAMIENTO"));

        // Validar que validateAndDetect lanza excepción
        assertThrows(ScheduleConflictException.class, () ->
                conflictEngine.validateAndDetect(candidateScene, List.of(existingScene), false)
        );
    }

    @Test
    @DisplayName("Debe generar WARNING_LOCATION_CONFLICT si el tiempo entre locaciones distintas es menor a 45 min")
    void shouldDetectLocationTravelWarningWhenBufferIsInsufficient() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 10, 10, 0);

        Scene scene1 = Scene.builder()
                .id(201L)
                .sceneNumber(5)
                .title("Toma en exteriores bosque")
                .scheduledTime(baseTime) // 10:00 - 11:00
                .durationMinutes(60)
                .location("Parque Natural Chicaque")
                .status(SceneStatus.READY)
                .cost(BigDecimal.valueOf(4000))
                .actors(Set.of(actor1))
                .build();

        Scene scene2 = Scene.builder()
                .id(202L)
                .sceneNumber(6)
                .title("Entrevista televisiva")
                .scheduledTime(baseTime.plusMinutes(80)) // 11:20 - 12:20 (Solo 20 min de traslado después de las 11:00)
                .durationMinutes(60)
                .location("Estudios RCN Bogotá")
                .status(SceneStatus.READY)
                .cost(BigDecimal.valueOf(2500))
                .actors(Set.of(actor1))
                .build();

        List<ConflictDetail> conflicts = conflictEngine.detectConflicts(scene2, List.of(scene1));

        assertFalse(conflicts.isEmpty(), "Se debía haber detectado una advertencia de locación");
        assertEquals(ConflictType.WARNING_LOCATION_CONFLICT, conflicts.get(0).getType());
        assertEquals(20L, conflicts.get(0).getGapMinutes());
        assertEquals(45, conflicts.get(0).getMinimumRequiredMinutes());
        assertTrue(conflicts.get(0).getMessage().contains("WARNING_LOCATION_CONFLICT"));

        // Con allowLocationWarnings = false, debe rechazar
        assertThrows(ScheduleConflictException.class, () ->
                conflictEngine.validateAndDetect(scene2, List.of(scene1), false)
        );

        // Con allowLocationWarnings = true, no debe lanzar excepción pero sí retornar la advertencia
        List<ConflictDetail> warnings = conflictEngine.validateAndDetect(scene2, List.of(scene1), true);
        assertEquals(1, warnings.size());
    }

    @Test
    @DisplayName("No debe haber conflicto si el tiempo de traslado es suficiente (>= 45 minutos)")
    void shouldNotDetectConflictWhenTravelBufferIsSufficient() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 10, 10, 0);

        Scene scene1 = Scene.builder()
                .id(301L)
                .sceneNumber(10)
                .title("Escena Mañana")
                .scheduledTime(baseTime) // 10:00 - 11:00
                .durationMinutes(60)
                .location("Locación A")
                .actors(Set.of(actor1))
                .build();

        Scene scene2 = Scene.builder()
                .id(302L)
                .sceneNumber(11)
                .title("Escena Tarde")
                .scheduledTime(baseTime.plusMinutes(120)) // 12:00 - 13:00 (60 minutos de traslado entre 11:00 y 12:00)
                .durationMinutes(60)
                .location("Locación B")
                .actors(Set.of(actor1))
                .build();

        List<ConflictDetail> conflicts = conflictEngine.detectConflicts(scene2, List.of(scene1));
        assertTrue(conflicts.isEmpty(), "No debía haber conflictos pues el margen es de 60 min >= 45 min");
    }

    @Test
    @DisplayName("No debe generar conflicto de traslado si las escenas ocurren en la misma locación")
    void shouldNotDetectLocationConflictIfSameLocation() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 10, 10, 0);

        Scene scene1 = Scene.builder()
                .id(401L)
                .sceneNumber(20)
                .title("Plano general")
                .scheduledTime(baseTime) // 10:00 - 11:00
                .durationMinutes(60)
                .location("Set Central")
                .actors(Set.of(actor1))
                .build();

        Scene scene2 = Scene.builder()
                .id(402L)
                .sceneNumber(21)
                .title("Primer plano")
                .scheduledTime(baseTime.plusMinutes(70)) // 11:10 - 12:00 (10 min después pero mismo set)
                .durationMinutes(50)
                .location("Set Central")
                .actors(Set.of(actor1))
                .build();

        List<ConflictDetail> conflicts = conflictEngine.detectConflicts(scene2, List.of(scene1));
        assertTrue(conflicts.isEmpty(), "Misma locación no debe generar conflicto de traslado");
    }

    @Test
    @DisplayName("No debe detectar conflicto si los actores asignados a las escenas son distintos")
    void shouldNotDetectConflictIfDifferentActors() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 10, 10, 0);

        Scene scene1 = Scene.builder()
                .id(501L)
                .sceneNumber(30)
                .title("Escena Actor 1")
                .scheduledTime(baseTime)
                .durationMinutes(60)
                .location("Locación A")
                .actors(Set.of(actor1))
                .build();

        Scene scene2 = Scene.builder()
                .id(502L)
                .sceneNumber(31)
                .title("Escena Actor 2")
                .scheduledTime(baseTime) // Mismo horario pero actor distinto
                .durationMinutes(60)
                .location("Locación B")
                .actors(Set.of(actor2))
                .build();

        List<ConflictDetail> conflicts = conflictEngine.detectConflicts(scene2, List.of(scene1));
        assertTrue(conflicts.isEmpty(), "Actores distintos no interfieren entre sí");
    }
}
