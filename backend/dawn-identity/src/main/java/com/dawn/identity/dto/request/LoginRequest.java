package com.dawn.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LoginRequest {
    @NotBlank(message = "Username or email is not mandatory")
    @Size(min = 1, max = 100)
    private String identifier;

    @NotBlank(message = "Password is not mandatory")
    @Size(min = 8, message = "Password is required 8 characters above")
    private String password;

}
