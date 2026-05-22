package ru.danilavak.zizu.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ru.danilavak.zizu.model.UserRole;
import ru.danilavak.zizu.repository.UserAccountRepository;
import ru.danilavak.zizu.service.UserAccountService;

@Component
public class InitialUserLoader implements CommandLineRunner {
    private final Environment environment;
    private final UserAccountRepository repository;
    private final UserAccountService service;

    public InitialUserLoader(
            Environment environment,
            UserAccountRepository repository,
            UserAccountService service
    ) {
        this.environment = environment;
        this.repository = repository;
        this.service = service;
    }

    @Override
    public void run(String... args) {
        String username = environment.getProperty("APP_ADMIN_USERNAME");
        String email = environment.getProperty("APP_ADMIN_EMAIL");
        String password = environment.getProperty("APP_ADMIN_PASSWORD");
        if (isBlank(username) || isBlank(email) || isBlank(password) || repository.existsByUsername(username)) {
            return;
        }
        service.register(username, email, password, UserRole.ADMIN);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
