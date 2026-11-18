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
    // Gunakan query eksplisit ini agar pencarian hibrida email / no_telepon / nama 100% akurat
    @Query("SELECT u FROM User u WHERE u.email = :id1 OR u.noTelepon = :id2 OR LOWER(u.nama) = LOWER(:id1)")
    Optional<User> findByEmailOrNoTelepon(@Param("id1") String identifier1, @Param("id2") String identifier2);

    boolean existsByEmail(String email);
    boolean existsByNoTelepon(String noTelepon);
}