package ru.danilavak.zizu.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ru.danilavak.zizu.model.UserAccount;
import ru.danilavak.zizu.model.UserRole;
import ru.danilavak.zizu.repository.UserAccountRepository;

@Service
public class UserAccountService {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount register(String username, String email, String password, UserRole role) {
        validateRequired(username, "username");
        validateRequired(email, "email");
        validatePassword(password);
        if (repository.existsByUsername(username)) {
            throw badRequest("Username already exists");
        }
        if (repository.existsByEmail(email)) {
            throw badRequest("Email already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username.trim());
        user.setEmail(email.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role == null ? UserRole.USER : role);
        user.setEnabled(true);
        return repository.save(user);
    }

    private void validateRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw badRequest("Field " + field + " is required");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw badRequest("Password must contain at least 8 characters");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw badRequest("Password must contain a special character");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
