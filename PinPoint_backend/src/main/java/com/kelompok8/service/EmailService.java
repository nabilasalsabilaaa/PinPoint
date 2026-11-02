package com.kelompok8.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreplay.pinpoint@gmail.com");
        message.setTo(toEmail);
        message.setSubject("Kode OTP Reset Kata Sandi - PinPoint");
        message.setText("Halo,\n\nKode OTP untuk mereset kata sandi akun PinPoint Anda adalah:\n\n" 
                        + otpCode + "\n\n" 
                        + "Kode ini berlaku selama 15 menit. Mohon untuk tidak membagikan kode ini kepada siapapun demi keamanan akun Anda.\n\n"
                        + "Salam,\nTim PinPoint");

        mailSender.send(message);
    }
}
