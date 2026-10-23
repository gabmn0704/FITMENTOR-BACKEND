package com.fitmentor.security;

import com.fitmentor.user.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides whether a verified token was revoked: when the user's current session marker differs
 * from the one the token was issued with. This is one primary-key read per authenticated request.
 * A deleted user is not reported here; services that load the user still answer
 * {@code unknown_user} (401), as before.
 */
@Component
public class SessionChecker {

    private final UserRepository users;

    public SessionChecker(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(AuthenticatedUser principal) {
        return users.findById(principal.id())
            .map(user -> user.sessionMarker() != principal.sessionMarker())
            .orElse(false);
    }
}
