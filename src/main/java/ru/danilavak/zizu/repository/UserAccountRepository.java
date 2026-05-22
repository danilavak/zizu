package ru.danilavak.zizu.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.danilavak.zizu.model.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
