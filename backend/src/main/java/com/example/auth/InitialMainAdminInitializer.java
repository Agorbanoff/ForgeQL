package com.example.auth;

import com.example.persistence.Enums.GlobalRole;
import com.example.persistence.model.UserAccountEntity;
import com.example.persistence.repository.UserAccountRepository;
import org.springframework.core.env.Environment;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InitialMainAdminInitializer implements ApplicationRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public InitialMainAdminInitializer(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            Environment environment
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userAccountRepository.existsByGlobalRole(GlobalRole.MAIN_ADMIN)) {
            return;
        }

        Map<String, String> dotenvValues = loadDotenvValues();
        String email = requireConfigured("INITIAL_MAIN_ADMIN_EMAIL", dotenvValues);
        String password = requireConfigured("INITIAL_MAIN_ADMIN_PASSWORD", dotenvValues);
        String username = requireConfigured("INITIAL_MAIN_ADMIN_USERNAME", dotenvValues);

        if (userAccountRepository.existsByEmail(email)) {
            throw new IllegalStateException("Cannot bootstrap MAIN_ADMIN: email is already used by another account");
        }

        UserAccountEntity user = new UserAccountEntity();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setGlobalRole(GlobalRole.MAIN_ADMIN);

        userAccountRepository.save(user);
    }

    private String requireConfigured(String key, Map<String, String> dotenvValues) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            value = dotenvValues.get(key);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value.trim();
    }

    private Map<String, String> loadDotenvValues() {
        for (Path dotenvPath : candidateDotenvPaths()) {
            if (!Files.exists(dotenvPath)) {
                continue;
            }

            try {
                List<String> lines = Files.readAllLines(dotenvPath);
                Map<String, String> values = new HashMap<>();
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }

                    int separatorIndex = trimmed.indexOf('=');
                    if (separatorIndex <= 0) {
                        continue;
                    }

                    String key = trimmed.substring(0, separatorIndex).trim();
                    String value = trimmed.substring(separatorIndex + 1).trim();
                    values.put(key, value);
                }
                return values;
            } catch (IOException exception) {
                throw new UncheckedIOException("Failed to read " + dotenvPath, exception);
            }
        }

        return Map.of();
    }

    private List<Path> candidateDotenvPaths() {
        List<Path> candidates = new ArrayList<>();
        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();

        candidates.add(workingDirectory.resolve(".env"));
        candidates.add(workingDirectory.resolve("..").resolve(".env").normalize());
        candidates.add(workingDirectory.resolve("backend").resolve("..").resolve(".env").normalize());

        return candidates.stream().distinct().toList();
    }
}
