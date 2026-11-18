package com.kelompok8.repository;

import com.kelompok8.model.Laporan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LaporanRepository extends JpaRepository<Laporan, Long> {
    List<Laporan> findByIdUser(Long idUser);
    long countByIdKategori(Long idKategori);
    long countByIdUser(Long idUser);
}