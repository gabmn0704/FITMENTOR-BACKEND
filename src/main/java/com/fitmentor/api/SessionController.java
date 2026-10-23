package com.fitmentor.api;

import com.fitmentor.security.AuthenticatedUser;
import com.fitmentor.training.SessionRequest;
import com.fitmentor.training.SessionSummary;
import com.fitmentor.training.TrainingFacade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Training sessions of the authenticated user. */
@RestController
@RequestMapping("/api/sessions")
@Validated
public class SessionController {

    private final TrainingFacade training;

    public SessionController(TrainingFacade training) {
        this.training = training;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionSummary create(@AuthenticationPrincipal AuthenticatedUser principal,
                                 @Valid @RequestBody SessionRequest request) {
        return training.recordSession(principal.id(), request);
    }

    @GetMapping
    public List<SessionSummary> recent(@AuthenticationPrincipal AuthenticatedUser principal,
                                       @RequestParam(name = "limit", defaultValue = "20")
                                       @Min(1) @Max(100) int limit) {
        return training.recentSessions(principal.id(), limit);
    }
}
