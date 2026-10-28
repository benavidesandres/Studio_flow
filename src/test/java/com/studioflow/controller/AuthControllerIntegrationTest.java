package com.studioflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studioflow.dto.auth.LoginRequest;
import com.studioflow.dto.auth.RegisterRequest;
import com.studioflow.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Debe registrar un nuevo usuario productor y retornar token JWT")
    void shouldRegisterProducerAndReturnJwtToken() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("nolan@studioflow.com")
                .password("Secr3t123!")
                .fullName("Christopher Nolan")
                .role(Role.PRODUCER)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("nolan@studioflow.com")))
                .andExpect(jsonPath("$.role", is("PRODUCER")));
    }

    @Test
    @DisplayName("Debe autenticar un usuario existente y retornar token JWT")
    void shouldLoginAndReturnJwtToken() throws Exception {
        // Primero registrar
        RegisterRequest regRequest = RegisterRequest.builder()
                .email("villeneuve@studioflow.com")
                .password("Dune2026!")
                .fullName("Denis Villeneuve")
                .role(Role.DIRECTOR)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        // Luego iniciar sesión
        LoginRequest loginRequest = LoginRequest.builder()
                .email("villeneuve@studioflow.com")
                .password("Dune2026!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.role", is("DIRECTOR")));
    }

    @Test
    @DisplayName("Debe rechazar el registro de un email duplicado con 409 Conflict")
    void shouldRejectDuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("duplicate@studioflow.com")
                .password("password123")
                .fullName("Test User")
                .role(Role.CREW)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Segundo intento con mismo email
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("duplicate@studioflow.com")));
    }

    @Test
    @DisplayName("Debe rechazar credenciales inválidas con 401 Unauthorized")
    void shouldRejectInvalidCredentials() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("nonexistent@studioflow.com")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Health endpoint debe ser público y responder UP")
    void shouldAccessHealthPublicly() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}
