package com.terraformlabs.dms.service;

import com.terraformlabs.dms.dto.CreateUserResponse;
import com.terraformlabs.dms.entity.AppUser;
import com.terraformlabs.dms.exception.ResourceNotFoundException;
import com.terraformlabs.dms.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings("null") // Mockito any() matchers are untyped; save() is @NonNull in Spring Data
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private CredentialGenerator credentialGenerator;
    @Mock private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(appUserRepository, credentialGenerator, passwordEncoder);
    }

    // --- createUser ---

    @Test
    void createUser_persistsAndReturnsNewUser() {
        when(credentialGenerator.generateUsername()).thenReturn("user_abcd1234");
        when(appUserRepository.existsByUsername("user_abcd1234")).thenReturn(false);
        when(credentialGenerator.generatePassword()).thenReturn("RawPass123!");
        when(passwordEncoder.encode("RawPass123!")).thenReturn("$2a$encoded");

        AppUser saved = new AppUser();
        saved.setUsername("user_abcd1234");
        saved.setPasswordHash("$2a$encoded");
        // Simulate DB-assigned ID via reflection-free setter (ID stays null; we set it manually)
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            return u; // return same object; ID would normally be set by Hibernate
        });

        CreateUserResponse response = userService.createUser();

        assertThat(response.username()).isEqualTo("user_abcd1234");
        assertThat(response.password()).isEqualTo("RawPass123!");
        verify(appUserRepository).save(any(AppUser.class));
    }

    @Test
    void createUser_retriesUntilUniqueUsernameIsFound() {
        // First two attempts collide; third is unique
        when(credentialGenerator.generateUsername())
                .thenReturn("user_aaaaaaaa")
                .thenReturn("user_bbbbbbbb")
                .thenReturn("user_cccccccc");
        when(appUserRepository.existsByUsername("user_aaaaaaaa")).thenReturn(true);
        when(appUserRepository.existsByUsername("user_bbbbbbbb")).thenReturn(true);
        when(appUserRepository.existsByUsername("user_cccccccc")).thenReturn(false);
        when(credentialGenerator.generatePassword()).thenReturn("SomePass1!");
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateUserResponse response = userService.createUser();

        assertThat(response.username()).isEqualTo("user_cccccccc");
        verify(credentialGenerator, times(3)).generateUsername();
    }

    // --- getByUsername ---

    @Test
    void getByUsername_returnsUserWhenFound() {
        AppUser user = new AppUser();
        user.setUsername("alice");
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        AppUser result = userService.getByUsername("alice");

        assertThat(result.getUsername()).isEqualTo("alice");
    }

    @Test
    void getByUsername_throwsResourceNotFoundWhenUserMissing() {
        when(appUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByUsername("ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
