package com.fitmentor.auth;

import jakarta.validation.Valid;
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
}
