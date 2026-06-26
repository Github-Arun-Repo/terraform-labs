package com.terraformlabs.ums.service;

import com.terraformlabs.ums.dto.CreateUserRequest;
import com.terraformlabs.ums.dto.RegisterUserRequest;
import com.terraformlabs.ums.dto.UpdateUserRolesRequest;
import com.terraformlabs.ums.dto.UserResponse;
import com.terraformlabs.ums.entity.AppUser;
import com.terraformlabs.ums.entity.Role;
import com.terraformlabs.ums.entity.UserStatus;
import com.terraformlabs.ums.exception.BadRequestException;
import com.terraformlabs.ums.exception.ResourceNotFoundException;
import com.terraformlabs.ums.repository.UserRepository;
import com.terraformlabs.ums.security.AppUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("User already exists with this email");
        }

        AppUser user = new AppUser();
        user.setEmail(request.email().toLowerCase());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(request.roles());
        user.setStatus(UserStatus.ACTIVE);

        AppUser saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse registerUser(RegisterUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("User already exists with this email");
        }

        AppUser user = new AppUser();
        user.setEmail(request.email().toLowerCase());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        // Self-registration is intentionally least-privilege.
        user.setRoles(Set.of(Role.SUPPLIER));
        user.setStatus(UserStatus.ACTIVE);

        AppUser saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        AppUser user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateRoles(Long userId, UpdateUserRolesRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setRoles(request.roles());
        return toResponse(userRepository.save(user));
    }

    public UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                user.getRoles(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
