package com.fitmentor.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoutinePlanRepository extends JpaRepository<RoutinePlanEntity, UUID> {
}
