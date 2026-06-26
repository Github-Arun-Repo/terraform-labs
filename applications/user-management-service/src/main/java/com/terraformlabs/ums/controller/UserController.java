package com.terraformlabs.ums.controller;

import com.terraformlabs.ums.dto.CreateUserRequest;
import com.terraformlabs.ums.dto.UpdateUserRolesRequest;
import com.terraformlabs.ums.dto.UserResponse;
import com.terraformlabs.ums.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateRoles(@PathVariable Long userId, @Valid @RequestBody UpdateUserRolesRequest request) {
        return userService.updateRoles(userId, request);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.currentUser(authentication);
    }
}
