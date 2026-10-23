package com.fitmentor.auth;

import com.fitmentor.api.error.ApiException;
import com.fitmentor.security.JwtService;
import com.fitmentor.user.UserEntity;
import com.fitmentor.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTests {

    private static final String PASSWORD = "correct-horse";

    private UserService users;
    private JwtService jwt;
    private PasswordEncoder encoder;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        users = mock(UserService.class);
        jwt = mock(JwtService.class);
        // Low BCrypt cost keeps the unit tests fast; the production bean uses the default cost.
        encoder = spy(new BCryptPasswordEncoder(4));
        auth = new AuthService(mock(GoogleTokenVerifier.class), users, jwt, encoder);
        when(jwt.issue(any(UUID.class), anyBoolean())).thenReturn("token");
    }

    @Test
    void registerNormalizesEmailAndStoresOnlyTheHash() {
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        when(users.createPasswordUser(anyString(), anyString(), anyString())).thenAnswer(invocation ->
            passwordUser(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));

        AuthService.AuthResult result = auth.register("  Ana ", "  Ana.Lopez@Example.COM ", PASSWORD);

        verify(users).createPasswordUser(eq("Ana"), eq("ana.lopez@example.com"), anyString());
        assertThat(result.token()).isEqualTo("token");
        assertThat(result.user().guest()).isFalse();
        assertThat(result.user().email()).isEqualTo("ana.lopez@example.com");
        assertThat(result.user().toString()).doesNotContain(PASSWORD).doesNotContain("$2");
    }

    @Test
    void registerRejectsDuplicateEmailWithoutCreatingUser() {
        when(users.findByEmail("ana@example.com")).thenReturn(Optional.of(passwordUser("Ana", "ana@example.com", "x")));

        assertThatThrownBy(() -> auth.register("Ana", "ANA@example.com", PASSWORD))
            .isInstanceOf(ApiException.class)
            .satisfies(error -> {
                ApiException api = (ApiException) error;
                assertThat(api.status()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(api.code()).isEqualTo("email_taken");
                assertThat(api.getMessage()).isEqualTo("Ese correo ya está registrado");
            });
        verify(users, never()).createPasswordUser(anyString(), anyString(), anyString());
    }

    @Test
    void registerMapsConcurrentDuplicateToConflict() {
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        when(users.createPasswordUser(anyString(), anyString(), anyString()))
            .thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> auth.register("Ana", "ana@example.com", PASSWORD))
            .isInstanceOfSatisfying(ApiException.class, api -> assertThat(api.status()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void loginSucceedsWithCorrectPasswordAndIgnoresEmailCase() {
        UserEntity user = passwordUser("Ana", "ana@example.com", encoder.encode(PASSWORD));
        when(users.findByEmail("ana@example.com")).thenReturn(Optional.of(user));

        AuthService.AuthResult result = auth.loginWithPassword(" ANA@example.com ", PASSWORD);

        assertThat(result.token()).isEqualTo("token");
        assertThat(result.user().id()).isEqualTo(user.getId().toString());
    }

    @Test
    void wrongPasswordIsGenericUnauthorized() {
        UserEntity user = passwordUser("Ana", "ana@example.com", encoder.encode(PASSWORD));
        when(users.findByEmail("ana@example.com")).thenReturn(Optional.of(user));

        assertInvalidCredentials(() -> auth.loginWithPassword("ana@example.com", "wrong-password"));
    }

    @Test
    void unknownEmailIsIndistinguishableAndStillRunsBcrypt() {
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());

        assertInvalidCredentials(() -> auth.loginWithPassword("nobody@example.com", PASSWORD));
        // One comparison against the dummy hash, so the timing matches a wrong password.
        verify(encoder, times(1)).matches(eq(PASSWORD), anyString());
    }

    @Test
    void accountWithoutPasswordHashCannotUsePasswordLogin() {
        // Google and guest accounts have no password_hash.
        UserEntity googleUser = new UserEntity(UUID.randomUUID(), "Pat", "pat@example.com", false, Instant.EPOCH);
        when(users.findByEmail("pat@example.com")).thenReturn(Optional.of(googleUser));

        assertInvalidCredentials(() -> auth.loginWithPassword("pat@example.com", PASSWORD));
    }

    private static void assertInvalidCredentials(Runnable login) {
        assertThatThrownBy(login::run)
            .isInstanceOfSatisfying(ApiException.class, api -> {
                assertThat(api.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(api.code()).isEqualTo("invalid_credentials");
                assertThat(api.getMessage()).isEqualTo("Correo o contraseña incorrectos");
            });
    }

    private static UserEntity passwordUser(String name, String email, String hash) {
        UserEntity user = new UserEntity(UUID.randomUUID(), name, email, false, Instant.EPOCH);
        user.setPasswordHash(hash);
        return user;
    }
}
