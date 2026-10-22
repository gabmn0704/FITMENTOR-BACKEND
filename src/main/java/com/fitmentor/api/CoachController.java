package com.fitmentor.api;

import com.fitmentor.coach.CoachService;
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
    public CoachService.CoachAnswer ask(@Valid @RequestBody CoachRequest request) {
        return coach.ask(request.question(), request.context());
    }

    /** Question of at most 500 characters and optional free context. */
    public record CoachRequest(
        @NotBlank(message = "question is required")
        @Size(max = 500, message = "question must be at most 500 characters")
        String question,

        @Size(max = 2000, message = "context must be at most 2000 characters")
        String context
    ) {
    }
}
