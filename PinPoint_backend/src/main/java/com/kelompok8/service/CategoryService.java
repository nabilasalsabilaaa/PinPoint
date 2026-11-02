package com.kelompok8.service;

import com.kelompok8.model.Kategori;
import com.kelompok8.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Kategori> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Kategori createCategory(Kategori kategori) {
        return categoryRepository.save(kategori);
    }

    public Kategori updateCategory(Long id, Kategori updatedData) {
        Kategori kategori = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kategori tidak ditemukan"));
        
        kategori.setNamaKategori(updatedData.getNamaKategori());
        kategori.setDeskripsi(updatedData.getDeskripsi());
        kategori.setIkon(updatedData.getIkon());
        kategori.setAktif(updatedData.getAktif());
        
        return categoryRepository.save(kategori);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}