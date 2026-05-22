package ru.danilavak.zizu.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.JwtException;
import ru.danilavak.zizu.model.SessionStatus;
import ru.danilavak.zizu.model.UserAccount;
import ru.danilavak.zizu.model.UserSession;
import ru.danilavak.zizu.repository.UserAccountRepository;
import ru.danilavak.zizu.repository.UserSessionRepository;

@Service
public class AuthTokenService {
    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final JwtTokenService jwtTokenService;

    public AuthTokenService(
            AuthenticationManager authenticationManager,
            UserAccountRepository userAccountRepository,
            UserSessionRepository userSessionRepository,
            JwtTokenService jwtTokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public TokenPair login(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> unauthorized("Invalid username or password"));

        UserSession session = new UserSession();
        session.setUserAccount(user);
        session.setRefreshTokenId(UUID.randomUUID().toString());
        session.setStatus(SessionStatus.ACTIVE);
        session.setExpiresAt(Instant.now().plus(jwtTokenService.getRefreshTokenLifetime()));
        userSessionRepository.save(session);

        return issuePair(user, session);
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        JwtTokenService.RefreshTokenPayload payload;
        try {
            payload = jwtTokenService.parseRefreshToken(refreshToken);
        } catch (JwtException ex) {
            throw unauthorized("Refresh token is invalid");
        }

        UserSession session = userSessionRepository.findById(payload.sessionId())
                .orElseThrow(() -> unauthorized("Refresh token is invalid"));

        if (!payload.refreshTokenId().equals(session.getRefreshTokenId())) {
            throw unauthorized("Refresh token is invalid");
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setStatus(SessionStatus.EXPIRED);
            session.setInvalidatedAt(Instant.now());
            throw unauthorized("Refresh token is expired");
        }
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw unauthorized("Refresh token is no longer active");
        }

        session.setStatus(SessionStatus.ROTATED);
        session.setInvalidatedAt(Instant.now());

        UserSession replacementSession = new UserSession();
        replacementSession.setUserAccount(session.getUserAccount());
        replacementSession.setRefreshTokenId(UUID.randomUUID().toString());
        replacementSession.setStatus(SessionStatus.ACTIVE);
        replacementSession.setExpiresAt(Instant.now().plus(jwtTokenService.getRefreshTokenLifetime()));
        userSessionRepository.save(replacementSession);

        return issuePair(session.getUserAccount(), replacementSession);
    }

    private TokenPair issuePair(UserAccount user, UserSession session) {
        JwtTokenService.TokenWithExpiry accessToken = jwtTokenService.createAccessToken(user, session.getId());
        JwtTokenService.TokenWithExpiry refreshToken = jwtTokenService.createRefreshToken(
                user,
                session.getId(),
                session.getRefreshTokenId()
        );
        return new TokenPair(
                accessToken.token(),
                refreshToken.token(),
                accessToken.expiresAt(),
                refreshToken.expiresAt()
        );
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    public record TokenPair(String accessToken, String refreshToken, Instant accessExpiresAt, Instant refreshExpiresAt) {
    }
}
