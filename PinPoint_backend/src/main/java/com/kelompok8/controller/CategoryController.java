package com.kelompok8.controller;

import com.kelompok8.model.Kategori;
import com.kelompok8.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Kategori>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @PostMapping
    public ResponseEntity<Kategori> createCategory(@RequestBody Kategori kategori) {
        return ResponseEntity.ok(categoryService.createCategory(kategori));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Kategori> updateCategory(@PathVariable Long id, @RequestBody Kategori kategori) {
        return ResponseEntity.ok(categoryService.updateCategory(id, kategori));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("Kategori berhasil dihapus.");
    }
}