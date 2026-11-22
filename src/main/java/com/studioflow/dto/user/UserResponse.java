package com.studioflow.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.studioflow.model.Role;
import com.studioflow.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private String phone;
    private String avatarUrl;
    private String characterName;

    // Helper method for backward compatibility
    public String getFullName() {
        return name;
    }

    public static UserResponse fromEntity(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .characterName(user.getCharacterName())
                .build();
    }
}
