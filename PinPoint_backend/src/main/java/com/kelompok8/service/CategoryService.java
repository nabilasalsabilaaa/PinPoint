package com.kelompok8.service;

import com.kelompok8.model.Kategori;
import com.kelompok8.repository.CategoryRepository;
import com.kelompok8.repository.LaporanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LaporanRepository laporanRepository;

    public List<Kategori> getAllCategories() {
        List<Kategori> list = categoryRepository.findAll();
        for (Kategori k : list) {
            if (k.getId() != null) {
                k.setConnectedReportsCount((int) laporanRepository.countByIdKategori(k.getId()));
            }
        }
        return list;
    }

    public Kategori createCategory(Kategori kategori) {
        try {
            categoryRepository.syncSequence();
        } catch (Exception ignored) {}
        kategori.setId(null);
        if (kategori.getAktif() == null) {
            kategori.setAktif(true);
        }
        if (kategori.getIkon() == null || kategori.getIkon().isBlank()) {
            kategori.setIkon("jalan");
        }
        Kategori saved = categoryRepository.save(kategori);
        saved.setConnectedReportsCount(0);
        return saved;
    }

    public Kategori updateCategory(Long id, Kategori updatedData) {
        Kategori kategori = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kategori tidak ditemukan"));
        
        if (updatedData.getNamaKategori() != null) {
            kategori.setNamaKategori(updatedData.getNamaKategori());
        }
        if (updatedData.getDeskripsi() != null) {
            kategori.setDeskripsi(updatedData.getDeskripsi());
        }
        if (updatedData.getIkon() != null) {
            kategori.setIkon(updatedData.getIkon());
        }
        if (updatedData.getAktif() != null) {
            kategori.setAktif(updatedData.getAktif());
        }
        
        Kategori saved = categoryRepository.save(kategori);
        saved.setConnectedReportsCount((int) laporanRepository.countByIdKategori(saved.getId()));
        return saved;
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}