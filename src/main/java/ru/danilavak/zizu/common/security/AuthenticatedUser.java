package ru.danilavak.zizu.common.security;

import ru.danilavak.zizu.model.UserRole;

public record AuthenticatedUser(
        Long userId,
        String username,
        UserRole role,
        Long sessionId
) {
}
