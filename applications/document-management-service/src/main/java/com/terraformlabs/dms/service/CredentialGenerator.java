package com.terraformlabs.dms.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class CredentialGenerator {

    private static final String USER_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String PASS_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateUsername() {
        return "user_" + randomString(USER_CHARS, 8);
    }

    public String generatePassword() {
        return randomString(PASS_CHARS, 16);
    }

    private String randomString(String source, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(source.charAt(RANDOM.nextInt(source.length())));
        }
        return sb.toString();
    }
}
