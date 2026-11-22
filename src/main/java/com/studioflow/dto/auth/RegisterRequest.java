package com.studioflow.dto.auth;

import com.studioflow.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato de email no es válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    private String name;

    private String fullName;

    @NotNull(message = "El rol es obligatorio (PRODUCER, DIRECTOR, CREW, ACTOR)")
    private Role role;

    private String phone;

    private String avatarUrl;

    private String characterName;

    public String resolveName() {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }
        return "";
    }
}
