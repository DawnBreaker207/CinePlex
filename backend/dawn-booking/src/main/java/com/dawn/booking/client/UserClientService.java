package com.dawn.booking.client;


import com.dawn.booking.dto.response.RoleDTO;
import com.dawn.booking.dto.response.UserDTO;

public interface UserClientService {

    boolean existsByRolesName(String roleName);

    RoleDTO findByRoleName(String roleName);

    UserDTO findWithEmail(String email);

    UserDTO findById(Long id);
}
