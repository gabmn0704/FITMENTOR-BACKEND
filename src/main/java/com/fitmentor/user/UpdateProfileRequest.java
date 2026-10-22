package com.fitmentor.user;

import com.fitmentor.domain.user.FitnessLevel;
import jakarta.validation.constraints.Size;

/** Body of {@code PATCH /api/me}. Null fields are left unchanged. */
public record UpdateProfileRequest(
    @Size(min = 1, max = 120, message = "name must be between 1 and 120 characters")
    String name,
    FitnessLevel level,
    @Size(max = 60, message = "goal must be at most 60 characters")
    String goal
) {
}
