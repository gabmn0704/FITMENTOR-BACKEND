package com.fitmentor.coach;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** One earlier message of the conversation, so the coach can follow the thread. Role is "user" or "coach". */
public record CoachTurn(
    @NotBlank @Pattern(regexp = "user|coach", message = "role must be user or coach")
    String role,

    @NotBlank @Size(max = 400, message = "text must be at most 400 characters")
    String text
) {
}
