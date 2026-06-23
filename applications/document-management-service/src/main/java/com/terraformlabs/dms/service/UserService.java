package com.terraformlabs.dms.service;

import com.terraformlabs.dms.dto.CreateUserResponse;
import com.terraformlabs.dms.entity.AppUser;
import com.terraformlabs.dms.exception.ResourceNotFoundException;
import com.terraformlabs.dms.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;
    private final CredentialGenerator credentialGenerator;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository appUserRepository,
                       CredentialGenerator credentialGenerator,
                       PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.credentialGenerator = credentialGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    public CreateUserResponse createUser() {
        String username = generateUniqueUsername();
        String rawPassword = credentialGenerator.generatePassword();

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        AppUser saved = appUserRepository.save(user);
        return new CreateUserResponse(saved.getId(), username, rawPassword);
    }

    public AppUser getByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String generateUniqueUsername() {
        String username;
        do {
            username = credentialGenerator.generateUsername();
        } while (appUserRepository.existsByUsername(username));
        return username;
    }
}
