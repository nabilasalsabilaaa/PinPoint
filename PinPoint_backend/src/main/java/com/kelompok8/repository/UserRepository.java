package com.kelompok8.repository;

import com.kelompok8.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByNoTelepon(String noTelepon);
    // Gunakan query eksplisit ini agar pencarian hibrida email / no_telepon 100% akurat
    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.noTelepon = :identifier")
    Optional<User> findByEmailOrNoTelepon(@Param("identifier") String identifier1, @Param("identifier") String identifier2);

    boolean existsByEmail(String email);
    boolean existsByNoTelepon(String noTelepon);
}