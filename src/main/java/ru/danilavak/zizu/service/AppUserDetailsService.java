package ru.danilavak.zizu.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import ru.danilavak.zizu.repository.UserAccountRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final UserAccountRepository repository;

    public AppUserDetailsService(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return repository.findByUsername(username)
                .map(user -> User.withUsername(user.getUsername())
                        .password(user.getPasswordHash())
                        .roles(user.getRole().name())
                        .accountExpired(user.isAccountExpired())
                        .accountLocked(user.isAccountLocked())
                        .credentialsExpired(user.isCredentialsExpired())
                        .disabled(!user.isEnabled())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
