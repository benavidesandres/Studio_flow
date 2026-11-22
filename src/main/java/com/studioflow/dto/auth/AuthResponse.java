package com.studioflow.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.studioflow.dto.user.UserResponse;
import com.studioflow.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String token;
    private UserResponse user;

    // Campos adicionales para compatibilidad retroactiva
    @Builder.Default
    private String tokenType = "Bearer";
    private Long userId;
    private String email;
    private String fullName;
    private Role role;
}
