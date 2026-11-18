package com.kelompok8.repository;

import com.kelompok8.model.Kategori;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Kategori, Long> {
    @Query(value = "SELECT setval(pg_get_serial_sequence('kategori', 'id'), COALESCE((SELECT MAX(id) FROM kategori), 1))", nativeQuery = true)
    Long syncSequence();
}