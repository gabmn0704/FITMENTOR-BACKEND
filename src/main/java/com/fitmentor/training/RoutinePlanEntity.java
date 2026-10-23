package com.fitmentor.training;

import com.fitmentor.domain.routine.RoutinePlan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Row of {@code routine_plans}: history of the adaptive routines generated for a user. */
@Entity
@Table(name = "routine_plans")
public class RoutinePlanEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "level", nullable = false, length = 30)
    private String level;

    @Column(name = "rest_seconds", nullable = false)
    private int restSeconds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", nullable = false)
    private List<RoutinePlan.RoutineItem> items;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RoutinePlanEntity() {
    }

    public RoutinePlanEntity(UUID userId, RoutinePlan plan, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.level = plan.level();
        this.restSeconds = plan.restSeconds();
        this.items = List.copyOf(plan.items());
        this.reason = plan.reason();
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
}
