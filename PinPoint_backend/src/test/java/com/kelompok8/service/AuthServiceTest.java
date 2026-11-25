package com.kelompok8.service;

import com.kelompok8.model.User;
import com.kelompok8.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User activeUser;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(15L);
        activeUser.setIdRole(2L);
        activeUser.setNama("Salsabila");
        activeUser.setEmail("salsa@gmail.com");
        activeUser.setNoTelepon("085214736999");
        activeUser.setStatusAkun("ACTIVE");
        activeUser.setPassword(encoder.encode("rahasia123"));
    }

    @Test
    void authenticate_withValidEmailAndPassword_succeeds() {
        when(userRepository.findByEmailIgnoreCase("salsa@gmail.com")).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.authenticate("salsa@gmail.com", "rahasia123");

        assertNotNull(result);
        assertEquals("salsa@gmail.com", result.getEmail());
        assertNotNull(result.getTerakhirLogin());
        verify(userRepository).save(activeUser);
    }

    @Test
    void authenticate_withPhone62Format_matchesLocalZeroUser() {
        when(userRepository.findFirstByNoTeleponIn(anyCollection())).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.authenticate("6285214736999", "rahasia123");

        assertNotNull(result);
        assertEquals("085214736999", result.getNoTelepon());
    }

    @Test
    void authenticate_withWrongPassword_returnsNull() {
        when(userRepository.findByEmailIgnoreCase("salsa@gmail.com")).thenReturn(Optional.of(activeUser));

        User result = authService.authenticate("salsa@gmail.com", "salahpassword");

        assertNull(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_withBlockedUser_returnsNull() {
        activeUser.setStatusAkun("BLOCKED");
        when(userRepository.findByEmailIgnoreCase("salsa@gmail.com")).thenReturn(Optional.of(activeUser));

        User result = authService.authenticate("salsa@gmail.com", "rahasia123");

        assertNull(result);
    }

    @Test
    void requestRegisterOtp_rejectsDuplicatePhoneDifferentFormat() {
        User req = new User();
        req.setNama("Budi");
        req.setEmail("budi.baru@gmail.com");
        req.setNoTelepon("+6285214736999");
        req.setPassword("password123");

        when(emailService.isEmailActiveAndValid("budi.baru@gmail.com")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("budi.baru@gmail.com")).thenReturn(false);
        when(userRepository.findByEmailOrNoTelepon("budi.baru@gmail.com", "budi.baru@gmail.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByNoTeleponIn(anyCollection())).thenReturn(true);

        String response = authService.requestRegisterOtp(req);

        assertTrue(response.startsWith("Error:"));
        assertTrue(response.toLowerCase().contains("nomor telepon"));
        verify(emailService, never()).sendOtpEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void requestRegisterOtp_withValidData_normalizesPhoneToZeroAndSendsOtp() {
        User req = new User();
        req.setNama("Budi");
        req.setEmail("budi.baru@gmail.com");
        req.setNoTelepon("6281234567890");
        req.setPassword("password123");

        when(emailService.isEmailActiveAndValid("budi.baru@gmail.com")).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("budi.baru@gmail.com")).thenReturn(false);
        when(userRepository.findByEmailOrNoTelepon("budi.baru@gmail.com", "budi.baru@gmail.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByNoTeleponIn(anyCollection())).thenReturn(false);

        String response = authService.requestRegisterOtp(req);

        assertFalse(response.startsWith("Error:"));
        assertEquals("081234567890", req.getNoTelepon());
        verify(emailService).sendOtpEmail(eq("budi.baru@gmail.com"), eq("Budi"), anyString(), eq("Registrasi Akun Baru"));
    }
}
