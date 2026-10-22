package com.fitmentor.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserAchievementRepository extends JpaRepository<UserAchievementEntity, UserAchievementId> {

    List<UserAchievementEntity> findByIdUserId(UUID userId);
}
