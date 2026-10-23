package com.fitmentor.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public login endpoints. Responses are {@code {token, user}}. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/google")
    public AuthService.AuthResult google(@Valid @RequestBody GoogleLoginRequest request) {
        return auth.loginWithGoogle(request.credential());
    }

    @PostMapping("/guest")
    public AuthService.AuthResult guest(@Valid @RequestBody(required = false) GuestLoginRequest request) {
        return auth.loginAsGuest(request == null ? null : request.name());
    }

    @PostMapping("/register")
    public AuthService.AuthResult register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request.name(), request.email(), request.password());
    }

    @PostMapping("/login")
    public AuthService.AuthResult login(@Valid @RequestBody LoginRequest request) {
        return auth.loginWithPassword(request.email(), request.password());
    }

    /** Google ID token obtained by the frontend (Sign in with Google). */
    public record GoogleLoginRequest(
        @NotBlank(message = "credential is required")
        @Size(max = 4096, message = "credential is too long")
        String credential) {
    }

    /** Optional display name for a guest: at most 120 characters, without control characters. */
    public record GuestLoginRequest(
        @Size(max = 120, message = "name must be at most 120 characters")
        @Pattern(regexp = "[^\\p{Cntrl}]*", message = "name contains invalid characters")
        String name) {
    }

    /** Email/password registration. The password is never echoed back or logged. */
    public record RegisterRequest(
        @NotBlank(message = "name is required")
        @Size(max = 60, message = "name must be at most 60 characters")
        @Pattern(regexp = "[^\\p{Cntrl}]*", message = "name contains invalid characters")
        String name,

        @NotBlank(message = "email is required")
        @Email(message = "email is not valid")
        @Size(max = 180, message = "email must be at most 180 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
            message = "password must be between 8 and 72 characters")
        String password) {

        @AssertTrue(message = "password is too long")
        public boolean isPasswordWithinBcryptLimit() {
            return PasswordPolicy.fitsBcryptLimit(password);
        }
    }

    /** Email/password login. Validation only checks the shape; wrong credentials get a generic 401. */
    public record LoginRequest(
        @NotBlank(message = "email is required")
        @Size(max = 180, message = "email must be at most 180 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(max = PasswordPolicy.MAX_LENGTH, message = "password must be at most 72 characters")
        String password) {

        @AssertTrue(message = "password is too long")
        public boolean isPasswordWithinBcryptLimit() {
            return PasswordPolicy.fitsBcryptLimit(password);
        }
    }
}
