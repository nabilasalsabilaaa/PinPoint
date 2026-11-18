package com.kelompok8.repository;

import com.kelompok8.model.FotoBuktiPerbaikan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FotoBuktiPerbaikanRepository extends JpaRepository<FotoBuktiPerbaikan, Long> {
    List<FotoBuktiPerbaikan> findByIdBuktiOrderByIdAsc(Long idBukti);
}

