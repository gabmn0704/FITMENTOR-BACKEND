package com.fitmentor.api;

import com.fitmentor.domain.achievement.AchievementStatus;
import com.fitmentor.security.AuthenticatedUser;
import com.fitmentor.training.TrainingFacade;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Achievements of the authenticated user with their progress. */
@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final TrainingFacade training;

    public AchievementController(TrainingFacade training) {
        this.training = training;
    }

    @GetMapping
    public List<AchievementStatus> achievements(@AuthenticationPrincipal AuthenticatedUser principal) {
        return training.achievements(principal.id());
    }
}
