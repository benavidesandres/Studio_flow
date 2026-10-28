package com.studioflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studioflow.dto.auth.AuthResponse;
import com.studioflow.dto.auth.RegisterRequest;
import com.studioflow.dto.production.ProductionRequest;
import com.studioflow.dto.production.ProductionResponse;
import com.studioflow.dto.scene.SceneRequest;
import com.studioflow.dto.scene.SceneStatusUpdateRequest;
import com.studioflow.model.Role;
import com.studioflow.model.SceneStatus;
import com.studioflow.service.AuthService;
import com.studioflow.service.ProductionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SceneControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private ProductionService productionService;

    private String producerToken;
    private String directorToken;
    private String actorToken;
    private Long actorId;
    private Long productionId;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        // 1. Crear Productor
        AuthResponse producerAuth = authService.register(RegisterRequest.builder()
                .email("producer_" + ts + "@studioflow.com")
                .password("Pass123456")
                .fullName("Producer Test")
                .role(Role.PRODUCER)
                .build());
        producerToken = "Bearer " + producerAuth.getToken();

        // 2. Crear Director
        AuthResponse directorAuth = authService.register(RegisterRequest.builder()
                .email("director_" + ts + "@studioflow.com")
                .password("Pass123456")
                .fullName("Director Test")
                .role(Role.DIRECTOR)
                .build());
        directorToken = "Bearer " + directorAuth.getToken();

        // 3. Crear Actor
        AuthResponse actorAuth = authService.register(RegisterRequest.builder()
                .email("actor_" + ts + "@studioflow.com")
                .password("Pass123456")
                .fullName("Actor Test")
                .role(Role.ACTOR)
                .build());
        actorToken = "Bearer " + actorAuth.getToken();
        actorId = actorAuth.getUserId();

        // 4. Crear Producción
        ProductionResponse prod = productionService.createProduction(ProductionRequest.builder()
                .title("Interstellar II")
                .description("Sci-fi epic")
                .totalBudget(BigDecimal.valueOf(500000))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(3))
                .build());
        productionId = prod.getId();
    }

    @Test
    @DisplayName("Productor y Director deben poder crear una escena válida")
    void shouldCreateSceneSuccessfully() throws Exception {
        SceneRequest sceneRequest = SceneRequest.builder()
                .productionId(productionId)
                .sceneNumber(1)
                .title("Aterrizaje en planeta Miller")
                .scheduledTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0))
                .durationMinutes(120)
                .location("Set Agua - Stage 4")
                .cost(BigDecimal.valueOf(15000))
                .status(SceneStatus.READY)
                .actorIds(Set.of(actorId))
                .build();

        mockMvc.perform(post("/api/v1/scenes")
                        .header("Authorization", directorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sceneRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.sceneNumber", is(1)))
                .andExpect(jsonPath("$.title", is("Aterrizaje en planeta Miller")))
                .andExpect(jsonPath("$.actors", hasSize(1)));
    }

    @Test
    @DisplayName("Debe rechazar con 409 Conflict si se intenta solapar el horario de un actor")
    void shouldRejectSceneCreationWhenActorOverlapDetected() throws Exception {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0);

        // Crear primera escena: 14:00 - 16:00 (120 min)
        SceneRequest scene1 = SceneRequest.builder()
                .productionId(productionId)
                .sceneNumber(10)
                .title("Conversación en la nave")
                .scheduledTime(baseTime)
                .durationMinutes(120)
                .location("Set Interior Nave")
                .cost(BigDecimal.valueOf(5000))
                .actorIds(Set.of(actorId))
                .build();

        mockMvc.perform(post("/api/v1/scenes")
                        .header("Authorization", producerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scene1)))
                .andExpect(status().isCreated());

        // Intentar crear segunda escena que se solapa: 15:00 - 17:00 (120 min)
        SceneRequest scene2 = SceneRequest.builder()
                .productionId(productionId)
                .sceneNumber(11)
                .title("Acoplamiento orbital")
                .scheduledTime(baseTime.plusHours(1)) // 15:00 (se solapa!)
                .durationMinutes(120)
                .location("Set Simulador de Vuelo")
                .cost(BigDecimal.valueOf(8000))
                .actorIds(Set.of(actorId))
                .build();

        mockMvc.perform(post("/api/v1/scenes")
                        .header("Authorization", producerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scene2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Schedule Conflict")))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Actor no debe tener permisos para crear producciones (RBAC 403 Forbidden)")
    void shouldDenyActorFromCreatingProduction() throws Exception {
        ProductionRequest request = ProductionRequest.builder()
                .title("Película No Autorizada")
                .totalBudget(BigDecimal.valueOf(10000))
                .build();

        mockMvc.perform(post("/api/v1/productions")
                        .header("Authorization", actorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Director debe poder actualizar el estado de una escena")
    void shouldAllowDirectorToUpdateSceneStatus() throws Exception {
        // Crear escena
        SceneRequest sceneRequest = SceneRequest.builder()
                .productionId(productionId)
                .sceneNumber(20)
                .title("Secuencia de Acción")
                .scheduledTime(LocalDateTime.now().plusDays(3).withHour(10).withMinute(0))
                .durationMinutes(60)
                .location("Hangar Principal")
                .status(SceneStatus.READY)
                .build();

        String res = mockMvc.perform(post("/api/v1/scenes")
                        .header("Authorization", producerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sceneRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdSceneId = objectMapper.readTree(res).get("id").asLong();

        // Actualizar estado a DELAYED
        SceneStatusUpdateRequest updateRequest = SceneStatusUpdateRequest.builder()
                .status(SceneStatus.DELAYED)
                .notes("Retraso por ajuste de iluminación técnica")
                .build();

        mockMvc.perform(patch("/api/v1/scenes/" + createdSceneId + "/status")
                        .header("Authorization", directorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DELAYED")));
    }
}
