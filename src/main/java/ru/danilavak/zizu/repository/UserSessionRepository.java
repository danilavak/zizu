package ru.danilavak.zizu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.danilavak.zizu.model.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByRefreshTokenId(String refreshTokenId);

    List<UserSession> findAllByUserAccountUsernameOrderByIdAsc(String username);
}
