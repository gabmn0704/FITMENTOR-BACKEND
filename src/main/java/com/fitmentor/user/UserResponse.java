package com.fitmentor.user;

import java.time.Instant;

/** Public view of a user, as returned by the auth and profile endpoints. */
public record UserResponse(
    String id,
    String name,
    String email,
    String picture,
    String level,
    String goal,
    boolean guest,
    Instant createdAt
) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(
            user.getId().toString(),
            user.getName(),
            user.getEmail(),
            user.getPicture(),
            user.getLevel(),
            user.getGoal(),
            user.isGuest(),
            user.getCreatedAt());
    }
}
