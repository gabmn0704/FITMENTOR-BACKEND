package com.fitmentor.api;

import com.fitmentor.coach.CoachService;
import com.fitmentor.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Coach question endpoint. Requires authentication. */
@RestController
@RequestMapping("/api/coach")
public class CoachController {

    private final CoachService coach;

    public CoachController(CoachService coach) {
        this.coach = coach;
    }

    @PostMapping
    public CoachService.CoachAnswer ask(@AuthenticationPrincipal AuthenticatedUser principal,
                                        @Valid @RequestBody CoachRequest request) {
        return coach.ask(principal.id(), request.question().strip());
    }

    /**
     * Question of at most 500 characters. {@code context} is accepted for compatibility but not
     * forwarded: the server builds the coach context from the user's profile and last session.
     */
    public record CoachRequest(
        @NotBlank(message = "question is required")
        @Size(max = 500, message = "question must be at most 500 characters")
        String question,

        @Size(max = 4000, message = "context must be at most 4000 characters")
        String context
    ) {
    }
}
