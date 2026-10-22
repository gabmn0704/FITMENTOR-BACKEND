package com.fitmentor.training;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrainingSessionRepository extends JpaRepository<TrainingSessionEntity, UUID> {

    /** Most recent sessions of one user. Every query is scoped by user id. */
    List<TrainingSessionEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<TrainingSessionEntity> findByUserId(UUID userId);
}
