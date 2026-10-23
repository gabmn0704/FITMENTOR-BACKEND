package com.fitmentor.security;

import java.util.UUID;

/**
 * Principal of an authenticated request, taken from the verified JWT. {@code sessionMarker} is the
 * revocation marker the token was issued with (0 = never revoked); see {@link SessionChecker}.
 */
public record AuthenticatedUser(UUID id, boolean guest, long sessionMarker) {

    public AuthenticatedUser(UUID id, boolean guest) {
        this(id, guest, 0L);
    }
}
