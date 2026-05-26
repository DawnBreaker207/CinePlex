package com.dawn.identity.controller;

import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.identity.dto.request.UserRequest;
import com.dawn.identity.dto.response.UserResponse;
import com.dawn.identity.model.Role;
import com.dawn.identity.service.UserService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@Tag(name = "User", description = "Operations related to user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("")
    @RateLimiter(name = "limit")
//    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseObject<ResponsePage<UserResponse>> getAll(Pageable pageable) {
        return ResponseObject.success(userService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseObject<UserResponse> getOne(@PathVariable Long id) {
        return ResponseObject.success(userService.findOne(id));
    }

    @GetMapping("/email/{email}")
    public ResponseObject<UserResponse> getEmail(@PathVariable String email) {
        return ResponseObject.success(userService.findByEmail(email));
    }

    @PostMapping("/batch")
    public ResponseObject<List<UserResponse>> findAllByIds(@RequestBody List<Long> ids) {
        return ResponseObject.success(userService.findAllByIds(ids));
    }

    @PutMapping("/update/{id}/status")
    @PreAuthorize("@roleSecurity.canUpdate(#id, authentication)")
    public ResponseObject<UserResponse> updateStatus(@PathVariable Long id, @RequestBody Boolean status) {
        return ResponseObject.success(userService.updateStatus(id, status));
    }

    @PutMapping("/update/{id}/profile")
    public ResponseObject<UserResponse> updateUserInfo(@PathVariable Long id, @RequestBody UserRequest req) {
        return ResponseObject.success(userService.update(id, req));
    }

    @GetMapping("/role/{roleName}")
    public ResponseObject<Role> getRoleName(@PathVariable String roleName) {
        return ResponseObject.success(userService.findByRoleName(roleName));
    }

    @GetMapping("/role/existed/{roleName}")
    public ResponseObject<Boolean> checkExistsByRoleName(@PathVariable String roleName) {
        return ResponseObject.success(userService.existsByRolesName(roleName));
    }

}
