package com.amcs.infrastructure.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Infrastructure configuration for secure password hashing.
 * Uses DelegatingPasswordEncoder with BCrypt as the primary encoding algorithm.
 */
@Configuration
public class SecurityCryptoConfig {

    @Bean
    public PasswordEncoder passwordEncoder(
        @Value("${amcs.security.bcrypt.strength:12}") int bcryptStrength
    ) {
        String encodingId = "bcrypt";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(encodingId, new BCryptPasswordEncoder(bcryptStrength));
        return new DelegatingPasswordEncoder(encodingId, encoders);
    }
}
