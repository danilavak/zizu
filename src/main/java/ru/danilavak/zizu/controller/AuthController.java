package ru.danilavak.zizu.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.danilavak.zizu.common.security.AuthenticatedUser;
import ru.danilavak.zizu.model.UserAccount;
import ru.danilavak.zizu.model.UserRole;
import ru.danilavak.zizu.service.AuthTokenService;
import ru.danilavak.zizu.service.UserAccountService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserAccountService userAccountService;
    private final AuthTokenService authTokenService;

    public AuthController(UserAccountService userAccountService, AuthTokenService authTokenService) {
        this.userAccountService = userAccountService;
        this.authTokenService = authTokenService;
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegistrationRequest request) {
        UserAccount user = userAccountService.register(
                request.username(),
                request.email(),
                request.password(),
                UserRole.USER
        );
        return UserResponse.from(user);
    }

    @PostMapping("/login")
    public TokenPairResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenPairResponse.from(authTokenService.login(request.username(), request.password()));
    }

    @PostMapping("/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return TokenPairResponse.from(authTokenService.refresh(request.refreshToken()));
    }

    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return new CurrentUserResponse(user.userId(), user.username(), user.role(), user.sessionId());
    }

    public record RegistrationRequest(
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(min = 8, max = 128) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record UserResponse(Long id, String username, String email, UserRole role) {
        static UserResponse from(UserAccount user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
        }
    }

    public record TokenPairResponse(
            String accessToken,
            String refreshToken,
            java.time.Instant accessExpiresAt,
            java.time.Instant refreshExpiresAt
    ) {
        static TokenPairResponse from(AuthTokenService.TokenPair pair) {
            return new TokenPairResponse(
                    pair.accessToken(),
                    pair.refreshToken(),
                    pair.accessExpiresAt(),
                    pair.refreshExpiresAt()
            );
        }
    }

    public record CurrentUserResponse(Long id, String username, UserRole role, Long sessionId) {
    }
}
