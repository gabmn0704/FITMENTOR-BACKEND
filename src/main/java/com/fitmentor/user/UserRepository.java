package com.fitmentor.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByGoogleId(String googleId);

    Optional<UserEntity> findByEmail(String email);

    /** Case-insensitive lookup: Google accounts may store mixed-case emails. */
    Optional<UserEntity> findByEmailIgnoreCase(String email);
}
