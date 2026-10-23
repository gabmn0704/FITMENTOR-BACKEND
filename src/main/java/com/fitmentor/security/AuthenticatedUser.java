package com.fitmentor.security;

import java.util.UUID;

/** Principal of an authenticated request, taken from the verified JWT. */
public record AuthenticatedUser(UUID id, boolean guest) {
}
