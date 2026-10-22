package com.fitmentor.training;

import com.fitmentor.domain.session.CompletedSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Row of the {@code training_sessions} table. {@code avg_angles} is stored as JSON. */
@Entity
@Table(name = "training_sessions")
public class TrainingSessionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "exercise", nullable = false, length = 60)
    private String exercise;

    @Column(name = "repetitions", nullable = false)
    private int repetitions;

    @Column(name = "accuracy", nullable = false)
    private double accuracy;

    @Column(name = "calories", nullable = false)
    private double calories;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "avg_angles", nullable = false)
    private Map<String, Double> avgAngles = new HashMap<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TrainingSessionEntity() {
    }

    public TrainingSessionEntity(UUID id, UUID userId, SessionSummary summary) {
        this.id = id;
        this.userId = userId;
        this.exercise = summary.exercise();
        this.repetitions = summary.reps();
        this.accuracy = summary.accuracy();
        this.calories = summary.calories();
        this.durationSeconds = summary.durationSeconds();
        this.avgAngles = new HashMap<>(summary.avgAngles());
        this.createdAt = summary.createdAt();
    }

    public SessionSummary toSummary() {
        return SessionSummary.builder()
            .id(id)
            .exercise(exercise)
            .reps(repetitions)
            .accuracy(accuracy)
            .calories(calories)
            .durationSeconds(durationSeconds)
            .avgAngles(avgAngles)
            .createdAt(createdAt)
            .build();
    }

    public CompletedSession toCompleted() {
        return new CompletedSession(exercise, repetitions, accuracy, calories, durationSeconds, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
}
