package com.fitmentor.user;

import com.fitmentor.api.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
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
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        UserEntity user = users.findByGoogleId(googleId)
            .or(() -> users.findByEmailIgnoreCase(normalizedEmail).map(this::revokePassword))
            .orElseGet(() -> new UserEntity(UUID.randomUUID(), displayName(name, normalizedEmail), normalizedEmail,
                false, clock.instant()));
        user.setGoogleId(googleId);
        user.setEmail(normalizedEmail);
        if (name != null && !name.isBlank()) {
            user.setName(name.trim());
        }
        user.setPicture(picture);
        return users.save(user);
    }

    /** Case-insensitive lookup by email, used by the email/password flows. */
    @Transactional(readOnly = true)
    public Optional<UserEntity> findByEmail(String email) {
        return users.findByEmailIgnoreCase(email);
    }

    /**
     * Creates an email/password user. The caller hashes the password and normalizes the email.
     * Flushes immediately so a concurrent duplicate surfaces as a data integrity error in the caller.
     */
    @Transactional
    public UserEntity createPasswordUser(String name, String email, String passwordHash) {
        UserEntity user = new UserEntity(UUID.randomUUID(), name.trim(), email, false, clock.instant());
        user.setPasswordHash(passwordHash);
        return users.saveAndFlush(user);
    }

    /** Creates a guest with a unique synthetic email, as agreed with the database schema. */
    @Transactional
    public UserEntity createGuest(String name) {
        UUID id = UUID.randomUUID();
        String email = "guest-" + id + "@guest.fitmentor.local";
        return users.save(new UserEntity(id, displayName(name, "Invitado"), email, true, clock.instant()));
    }

    /**
     * Google linked to an account that was created with email/password. Google has verified the
     * address, so the real owner takes the account over: the password stops working and every token
     * issued before now (including one an attacker obtained with that password) stops working too.
     * The instant is truncated to milliseconds, the precision of the token marker.
     */
    private UserEntity revokePassword(UserEntity existing) {
        existing.setPasswordHash(null);
        existing.setSessionsValidAfter(clock.instant().truncatedTo(ChronoUnit.MILLIS));
        return existing;
    }

    private static String displayName(String name, String fallback) {
        return name == null || name.isBlank() ? fallback : name.trim();
    }
}
