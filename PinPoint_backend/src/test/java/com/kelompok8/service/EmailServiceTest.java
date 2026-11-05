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
    @DisplayName("Email OTP dipanggil secara asinkron tanpa memblokir thread pemanggil")
    void testSendOtpEmailAsyncNonBlocking() {
        assertDoesNotThrow(() -> {
            emailService.sendOtpEmail("qa.tester.pinpoint@example.com", "849201", "QA Tester");
        });
    }

    @Test
    @DisplayName("Email Welcome dipanggil secara asinkron tanpa memblokir thread pemanggil")
    void testSendWelcomeEmailAsyncNonBlocking() {
        assertDoesNotThrow(() -> {
            emailService.sendWelcomeEmail("qa.tester.pinpoint@example.com", "QA Tester");
        });
    }
}
