package com.kelompok8.repository;

import com.kelompok8.model.RiwayatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RiwayatStatusRepository extends JpaRepository<RiwayatStatus, Long> {
    List<RiwayatStatus> findByIdLaporan(Long idLaporan);
}