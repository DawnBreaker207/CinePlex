package com.dawn.identity.dto.response;

import com.dawn.common.core.dto.response.BaseResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Set;


@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponse extends BaseResponse {

    private Long userId;

    private String avatar;

    private String username;

    private String email;

    private String phone;

    private String address;

    private Set<String> role;

    private Boolean isActive;
}
