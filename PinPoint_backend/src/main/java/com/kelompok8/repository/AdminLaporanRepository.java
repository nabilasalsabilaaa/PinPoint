package com.kelompok8.repository;

import com.kelompok8.model.Laporan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminLaporanRepository extends JpaRepository<Laporan, Long> {
}