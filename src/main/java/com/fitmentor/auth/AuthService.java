package com.fitmentor.auth;

import com.fitmentor.api.error.ApiException;
import com.fitmentor.security.JwtService;
import com.fitmentor.user.UserEntity;
import com.fitmentor.user.UserResponse;
import com.fitmentor.user.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Login use cases: Google sign-in, guest accounts and email/password. All return a JWT and the public user.
 * Credentials and tokens are never logged.
 */
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Correo o contraseña incorrectos";

    private final GoogleTokenVerifier googleVerifier;
    private final UserService users;
    private final JwtService jwt;
    private final PasswordEncoder passwordEncoder;
    private final String dummyHash;

    public AuthService(GoogleTokenVerifier googleVerifier, UserService users, JwtService jwt,
                       PasswordEncoder passwordEncoder) {
        this.googleVerifier = googleVerifier;
        this.users = users;
        this.jwt = jwt;
        this.passwordEncoder = passwordEncoder;
        // Hash of a random value, computed once. Unknown emails are checked against it so that
        // they cost the same BCrypt comparison as a wrong password (no timing-based email probing).
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public AuthResult loginWithGoogle(String credential) {
        GoogleProfile profile = googleVerifier.verify(credential);
        if (!profile.emailVerified() || profile.email() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unverified_google_email",
                "The Google account email is not verified.");
        }
        UserEntity user = users.upsertGoogleUser(profile.googleId(), profile.email(), profile.name(), profile.picture());
        return result(user);
    }

    public AuthResult loginAsGuest(String name) {
        return result(users.createGuest(name));
    }

    /** Creates an email/password account. The email is stored lowercase; the password only as a BCrypt hash. */
    public AuthResult register(String name, String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (users.findByEmail(normalizedEmail).isPresent()) {
            throw emailTaken();
        }
        String hash = passwordEncoder.encode(password);
        try {
            return result(users.createPasswordUser(name.trim(), normalizedEmail, hash));
        } catch (DataIntegrityViolationException violation) {
            // Only a concurrent registration of the same email is a conflict; anything else is rethrown.
            if (users.findByEmail(normalizedEmail).isPresent()) {
                throw emailTaken();
            }
            throw violation;
        }
    }

    /**
     * Email/password sign-in. Unknown emails, wrong passwords and accounts without a password
     * (Google or guest) all get the same 401. One BCrypt comparison runs in every case.
     */
    public AuthResult loginWithPassword(String email, String password) {
        Optional<UserEntity> found = users.findByEmail(normalizeEmail(email));
        String stored = found.map(UserEntity::getPasswordHash).orElse(null);
        boolean passwordMatches = passwordEncoder.matches(password, stored != null ? stored : dummyHash);
        if (stored == null || !passwordMatches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials", INVALID_CREDENTIALS_MESSAGE);
        }
        return result(found.get());
    }

    private AuthResult result(UserEntity user) {
        return new AuthResult(jwt.issue(user.getId(), user.isGuest(), user.sessionMarker()), UserResponse.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ApiException emailTaken() {
        return new ApiException(HttpStatus.CONFLICT, "email_taken", "Ese correo ya está registrado");
    }

    /** Token and public user returned to the client. */
    public record AuthResult(String token, UserResponse user) {
    }
}
