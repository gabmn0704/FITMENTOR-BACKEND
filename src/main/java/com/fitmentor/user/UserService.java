package com.fitmentor.user;

import com.fitmentor.api.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** Loads and updates the profile of the authenticated user. Every call is scoped to one id. */
@Service
public class UserService {

    private final UserRepository users;
    private final Clock clock;

    public UserService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserEntity require(UUID id) {
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unknown_user",
            "The session user no longer exists."));
    }

    @Transactional
    public UserEntity updateProfile(UUID id, UpdateProfileRequest request) {
        UserEntity user = require(id);
        if (request.name() != null) {
            user.setName(request.name().trim());
        }
        if (request.level() != null) {
            user.setLevel(request.level().name());
        }
        if (request.goal() != null) {
            user.setGoal(request.goal().isBlank() ? null : request.goal().trim());
        }
        return users.save(user);
    }

    /** Creates a user or refreshes the Google profile of an existing one, matched by google id then email. */
    @Transactional
    public UserEntity upsertGoogleUser(String googleId, String email, String name, String picture) {
        UserEntity user = users.findByGoogleId(googleId)
            .or(() -> users.findByEmail(email))
            .orElseGet(() -> new UserEntity(UUID.randomUUID(), displayName(name, email), email, false, clock.instant()));
        user.setGoogleId(googleId);
        user.setEmail(email);
        if (name != null && !name.isBlank()) {
            user.setName(name.trim());
        }
        user.setPicture(picture);
        return users.save(user);
    }

    /** Creates a guest with a unique synthetic email, as agreed with the database schema. */
    @Transactional
    public UserEntity createGuest(String name) {
        UUID id = UUID.randomUUID();
        String email = "guest-" + id + "@guest.fitmentor.local";
        return users.save(new UserEntity(id, displayName(name, "Invitado"), email, true, clock.instant()));
    }

    private static String displayName(String name, String fallback) {
        return name == null || name.isBlank() ? fallback : name.trim();
    }
}
