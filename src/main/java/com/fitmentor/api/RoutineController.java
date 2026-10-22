package com.fitmentor.api;

import com.fitmentor.domain.routine.RoutinePlan;
import com.fitmentor.security.AuthenticatedUser;
import com.fitmentor.training.TrainingFacade;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adaptive routine of the authenticated user. */
@RestController
@RequestMapping("/api/routine")
public class RoutineController {

    private final TrainingFacade training;

    public RoutineController(TrainingFacade training) {
        this.training = training;
    }

    @GetMapping
    public RoutinePlan routine(@AuthenticationPrincipal AuthenticatedUser principal) {
        return training.routine(principal.id());
    }
}
