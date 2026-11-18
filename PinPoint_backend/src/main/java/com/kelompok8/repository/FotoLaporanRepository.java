package com.kelompok8.repository;

import com.kelompok8.model.FotoLaporan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FotoLaporanRepository extends JpaRepository<FotoLaporan, Long> {
    List<FotoLaporan> findByIdLaporanOrderByIdAsc(Long idLaporan);
}

