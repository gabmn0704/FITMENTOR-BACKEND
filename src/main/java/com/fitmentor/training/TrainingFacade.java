package com.fitmentor.training;

import com.fitmentor.api.error.ApiException;
import com.fitmentor.domain.achievement.AchievementEvaluator;
import com.fitmentor.domain.achievement.AchievementInput;
import com.fitmentor.domain.achievement.AchievementStatus;
import com.fitmentor.domain.calories.CalorieCalculator;
import com.fitmentor.domain.exercise.Exercise;
import com.fitmentor.domain.exercise.ExerciseCatalog;
import com.fitmentor.domain.routine.RoutineContext;
import com.fitmentor.domain.routine.RoutinePlan;
import com.fitmentor.domain.routine.RoutineTemplate;
import com.fitmentor.domain.session.CompletedSession;
import com.fitmentor.domain.stats.StatsCalculator;
import com.fitmentor.domain.user.FitnessLevel;
import com.fitmentor.user.UserEntity;
import com.fitmentor.user.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Facade: the single entry point the controllers use for training data. It hides the
 * repositories and the domain calculators (calories, stats, routine, achievements) behind
 * a few use cases, and keeps every operation scoped to one user id.
 */
@Service
public class TrainingFacade {

    static final int ROUTINE_HISTORY = 10;

    private final TrainingSessionRepository sessions;
    private final UserAchievementRepository achievements;
    private final RoutinePlanRepository routines;
    private final UserService users;
    private final ExerciseCatalog catalog;
    private final RoutineTemplate routineTemplate;
    private final AchievementEvaluator achievementEvaluator;
    private final Clock clock;

    public TrainingFacade(TrainingSessionRepository sessions,
                          UserAchievementRepository achievements,
                          RoutinePlanRepository routines,
                          UserService users,
                          ExerciseCatalog catalog,
                          RoutineTemplate routineTemplate,
                          Clock clock) {
        this.sessions = sessions;
        this.achievements = achievements;
        this.routines = routines;
        this.users = users;
        this.catalog = catalog;
        this.routineTemplate = routineTemplate;
        this.achievementEvaluator = AchievementEvaluator.standard();
        this.clock = clock;
    }

    /** Stores a finished session. Calories come from the exercise MET and the 70 kg reference weight. */
    @Transactional
    public SessionSummary recordSession(UUID userId, SessionRequest request) {
        Exercise exercise = catalog.find(request.exercise())
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "unknown_exercise", "Unknown exercise."));
        SessionSummary summary = SessionSummary.builder()
            .exercise(exercise.id())
            .reps(request.reps())
            .accuracy(request.accuracy())
            .calories(CalorieCalculator.calories(exercise.met(), request.durationSeconds()))
            .durationSeconds(request.durationSeconds())
            .avgAngles(request.avgAngles())
            .createdAt(clock.instant())
            .build();
        sessions.save(new TrainingSessionEntity(summary.id(), userId, summary));
        return summary;
    }

    @Transactional(readOnly = true)
    public List<SessionSummary> recentSessions(UUID userId, int limit) {
        return sessions.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit)).stream()
            .map(TrainingSessionEntity::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public StatsCalculator.Stats stats(UUID userId) {
        UserEntity user = users.require(userId);
        return StatsCalculator.compute(completed(userId), clock.instant(), user.getLevel());
    }

    /** Builds the adaptive routine from the user's level and the last sessions, and keeps it in history. */
    @Transactional
    public RoutinePlan routine(UUID userId) {
        UserEntity user = users.require(userId);
        List<CompletedSession> history = completed(userId);
        Instant now = clock.instant();
        List<CompletedSession> recent = history.stream()
            .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
            .limit(ROUTINE_HISTORY)
            .toList();
        Double avgAccuracy = recent.isEmpty() ? null : StatsCalculator.average(recent);
        FitnessLevel level = FitnessLevel.parse(user.getLevel());
        RoutinePlan plan = routineTemplate.build(
            new RoutineContext(level, avgAccuracy, StatsCalculator.activeDaysInLastWeek(history, now)));
        routines.save(new RoutinePlanEntity(userId, plan, now));
        return plan;
    }

    /** Evaluates the achievements and records newly unlocked ones (unlock time is kept once). */
    @Transactional
    public List<AchievementStatus> achievements(UUID userId) {
        List<CompletedSession> history = completed(userId);
        AchievementInput input = new AchievementInput(
            history.size(),
            history.stream().mapToInt(CompletedSession::reps).sum(),
            StatsCalculator.currentStreak(history, clock.instant()),
            history.stream().mapToDouble(CompletedSession::accuracy).max().orElse(0.0),
            StatsCalculator.distinctExerciseCount(history));
        List<AchievementStatus> statuses = achievementEvaluator.evaluate(input);
        Set<String> alreadyUnlocked = achievements.findByIdUserId(userId).stream()
            .map(entry -> entry.getId().getAchievementId())
            .collect(Collectors.toSet());
        statuses.stream()
            .filter(status -> status.unlocked() && !alreadyUnlocked.contains(status.id()))
            .forEach(status -> achievements.save(new UserAchievementEntity(
                new UserAchievementId(userId, status.id()), clock.instant())));
        return statuses;
    }

    private List<CompletedSession> completed(UUID userId) {
        return sessions.findByUserId(userId).stream().map(TrainingSessionEntity::toCompleted).toList();
    }
}
