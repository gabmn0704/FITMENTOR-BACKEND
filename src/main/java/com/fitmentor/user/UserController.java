package com.fitmentor.user;

import com.fitmentor.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Current user profile. */
@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return UserResponse.from(users.require(principal.id()));
    }

    @PatchMapping
    public UserResponse update(@AuthenticationPrincipal AuthenticatedUser principal,
                               @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(users.updateProfile(principal.id(), request));
    }
}
