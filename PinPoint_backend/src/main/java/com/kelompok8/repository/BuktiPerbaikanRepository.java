package com.kelompok8.repository;

import com.kelompok8.model.BuktiPerbaikan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BuktiPerbaikanRepository extends JpaRepository<BuktiPerbaikan, Long> {
    Optional<BuktiPerbaikan> findByIdLaporan(Long idLaporan);
}