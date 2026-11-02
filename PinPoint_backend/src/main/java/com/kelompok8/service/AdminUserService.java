package com.kelompok8.service;

import com.kelompok8.model.User;
import com.kelompok8.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminUserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User updateUserStatus(Long id, boolean active, String statusAkun) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pengguna tidak ditemukan"));
        
        if (statusAkun != null && !statusAkun.isBlank()) {
            user.setStatusAkun(statusAkun);
        } else {
            user.setActive(active);
        }
        
        return userRepository.save(user);
    }
}