package com.kelompok8.service;

import com.kelompok8.PinPoint.PinPointApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(classes = PinPointApplication.class)
public class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Test
    @DisplayName("Test send registration OTP email")
    void testSendRegistrationOtpEmail() {
        assertDoesNotThrow(() -> {
            emailService.sendRegistrationOtpEmail("noreplay.pinpoint@gmail.com", "654321", "User Registrasi");
        });
    }

    @Test
    @DisplayName("Test send forgot password OTP email")
    void testSendForgotPasswordOtpEmail() {
        assertDoesNotThrow(() -> {
            emailService.sendForgotPasswordOtpEmail("noreplay.pinpoint@gmail.com", "123456", "User Lupa Password");
        });
    }
}


