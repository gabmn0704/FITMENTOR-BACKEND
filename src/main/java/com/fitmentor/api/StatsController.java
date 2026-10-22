package com.fitmentor.api;

import com.fitmentor.domain.stats.StatsCalculator;
import com.fitmentor.security.AuthenticatedUser;
import com.fitmentor.training.TrainingFacade;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Aggregated statistics of the authenticated user. */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final TrainingFacade training;

    public StatsController(TrainingFacade training) {
        this.training = training;
    }

    @GetMapping
    public StatsCalculator.Stats stats(@AuthenticationPrincipal AuthenticatedUser principal) {
        return training.stats(principal.id());
    }
}
