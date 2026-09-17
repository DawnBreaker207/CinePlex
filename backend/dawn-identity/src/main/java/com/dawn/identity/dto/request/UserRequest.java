package com.dawn.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserRequest {

    @NotBlank(message = "Username is required")
    private String username;

    private String avatar;

    private String address;

    private String phone;
}
