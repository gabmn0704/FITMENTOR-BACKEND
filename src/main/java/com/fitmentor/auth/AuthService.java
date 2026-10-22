package com.fitmentor.auth;

import com.fitmentor.api.error.ApiException;
import com.fitmentor.security.JwtService;
import com.fitmentor.user.UserEntity;
import com.fitmentor.user.UserResponse;
import com.fitmentor.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Login use cases: Google sign-in and guest accounts. Both return a JWT and the public user.
 * Credentials and tokens are never logged.
 */
@Service
public class AuthService {

    private final GoogleTokenVerifier googleVerifier;
    private final UserService users;
    private final JwtService jwt;

    public AuthService(GoogleTokenVerifier googleVerifier, UserService users, JwtService jwt) {
        this.googleVerifier = googleVerifier;
        this.users = users;
        this.jwt = jwt;
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

    private AuthResult result(UserEntity user) {
        return new AuthResult(jwt.issue(user.getId(), user.isGuest()), UserResponse.from(user));
    }

    /** Token and public user returned to the client. */
    public record AuthResult(String token, UserResponse user) {
    }
}
