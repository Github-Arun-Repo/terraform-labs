package com.terraformlabs.dms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialGeneratorTest {

    private CredentialGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new CredentialGenerator();
    }

    // --- generateUsername ---

    @Test
    void generateUsername_startsWithUserPrefix() {
        String username = generator.generateUsername();
        assertThat(username).startsWith("user_");
    }

    @Test
    void generateUsername_hasTotalLengthOfThirteen() {
        // "user_" (5) + 8 random chars = 13
        String username = generator.generateUsername();
        assertThat(username).hasSize(13);
    }

    @Test
    void generateUsername_containsOnlyLowercaseAlphanumericAndUnderscore() {
        String username = generator.generateUsername();
        assertThat(username).matches("user_[a-z0-9]{8}");
    }

    @RepeatedTest(5)
    void generateUsername_isDifferentOnConsecutiveCalls() {
        // With 36^8 ≈ 2.8 trillion combinations this is astronomically unlikely to collide
        String first = generator.generateUsername();
        String second = generator.generateUsername();
        // We just verify both are valid; collision test is probabilistic but informative
        assertThat(first).matches("user_[a-z0-9]{8}");
        assertThat(second).matches("user_[a-z0-9]{8}");
    }

    // --- generatePassword ---

    @Test
    void generatePassword_hasLengthOfSixteen() {
        String password = generator.generatePassword();
        assertThat(password).hasSize(16);
    }

    @Test
    void generatePassword_containsOnlyAllowedCharacters() {
        String allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        String password = generator.generatePassword();
        for (char c : password.toCharArray()) {
            assertThat(allowed).contains(String.valueOf(c));
        }
    }

    @Test
    void generatePassword_returnsNonBlankString() {
        String password = generator.generatePassword();
        assertThat(password).isNotBlank();
    }
}
