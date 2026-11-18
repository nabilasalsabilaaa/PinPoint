package com.kelompok8.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.regex.Pattern;

@Service
public class EmailService {

    public static final String SENDER_EMAIL = "noreplay.pinpoint@gmail.com";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public boolean isEmailActiveAndValid(String email) {
        if (email == null || email.isBlank()) return false;
        String trimmed = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        int atIndex = trimmed.indexOf('@');
        if (atIndex < 1 || atIndex == trimmed.length() - 1) return false;
        String domain = trimmed.substring(atIndex + 1).toLowerCase();

        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "2500");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            DirContext ictx = new InitialDirContext(env);
            Attributes attrs = ictx.getAttributes(domain, new String[]{"MX", "A"});
            Attribute mxAttr = attrs.get("MX");
            Attribute aAttr = attrs.get("A");
            return (mxAttr != null && mxAttr.size() > 0) || (aAttr != null && aAttr.size() > 0);
        } catch (NamingException e) {
            return domain.equals("gmail.com")
                    || domain.equals("yahoo.com")
                    || domain.equals("outlook.com")
                    || domain.equals("hotmail.com")
                    || domain.equals("icloud.com")
                    || domain.endsWith(".ac.id")
                    || domain.endsWith(".go.id")
                    || domain.endsWith(".co.id")
                    || domain.endsWith(".id");
        }
    }

    public void sendOtpEmail(String toEmail, String otpCode) {
        sendOtpEmail(toEmail, null, otpCode, "Reset Kata Sandi");
    }

    public void sendOtpEmail(String toEmail, String otpCode, String purpose) {
        sendOtpEmail(toEmail, null, otpCode, purpose);
    }

    public void sendOtpEmail(String toEmail, String recipientName, String otpCode, String purpose) {
        String displayName = (recipientName != null && !recipientName.isBlank())
                ? recipientName.trim()
                : (toEmail != null && toEmail.contains("@") ? toEmail.substring(0, toEmail.indexOf('@')) : "Pengguna");

        System.out.println("==================================================");
        System.out.println("[PINPOINT OTP] Pengirim : " + SENDER_EMAIL);
        System.out.println("[PINPOINT OTP] Tujuan   : " + toEmail + " (" + displayName + ")");
        System.out.println("[PINPOINT OTP] Keperluan: " + purpose);
        System.out.println("[PINPOINT OTP] Kode OTP : " + otpCode + " (Berlaku 15 Menit)");
        System.out.println("==================================================");

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(new InternetAddress(SENDER_EMAIL, "PinPoint"));
            helper.setTo(toEmail);
            helper.setSubject("\uD83D\uDD10 Kode OTP " + purpose + " - PinPoint");
            helper.setText(buildOtpHtmlTemplate(displayName, otpCode, purpose), true);

            mailSender.send(mimeMessage);
        } catch (MailAuthenticationException authEx) {
            if (mailPassword == null || mailPassword.isBlank() || "abcdefghijklmno".equals(mailPassword.trim())) {
                System.out.println("[PERINGATAN SMTP] MAIL_PASSWORD di .env masih menggunakan placeholder. "
                        + "Gunakan kode OTP di atas (" + otpCode + ") untuk pengujian, atau isi 16 digit App Password Gmail untuk "
                        + SENDER_EMAIL + " di file .env.");
            } else {
                throw new RuntimeException("Gagal autentikasi email pengirim (" + SENDER_EMAIL + "). Pastikan App Password Gmail sudah benar.");
            }
        } catch (Exception e) {
            if (mailPassword == null || mailPassword.isBlank() || "abcdefghijklmno".equals(mailPassword.trim())) {
                System.out.println("[INFO OTP LOKAL] Gunakan kode OTP: " + otpCode + " untuk email " + toEmail);
            } else {
                throw new RuntimeException("Gagal mengirim email OTP ke " + toEmail + ". Pastikan alamat email aktif!");
            }
        }
    }

    private String buildOtpHtmlTemplate(String userName, String otpCode, String purpose) {
        String cleanOtp = (otpCode != null && otpCode.length() == 6) ? otpCode : String.format("%-6s", otpCode).replace(' ', '0');
        boolean isRegister = purpose != null && purpose.toLowerCase().contains("registrasi");

        String introText = isRegister
                ? "Kami menerima permintaan pendaftaran akun baru PinPoint Anda. Masukkan kode OTP berikut pada aplikasi untuk melanjutkan proses verifikasi:"
                : "Kami menerima permintaan reset kata sandi untuk akun PinPoint Anda. Masukkan kode OTP berikut pada aplikasi untuk melanjutkan proses verifikasi:";

        String footerIgnoreText = isRegister
                ? "Jika Anda tidak meminta pendaftaran akun ini, abaikan email ini. Akun Anda tetap aman."
                : "Jika Anda tidak meminta reset kata sandi, abaikan email ini. Akun Anda tetap aman.";

        StringBuilder otpCells = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            char digit = cleanOtp.charAt(i);
            otpCells.append(
                    "<td style=\"padding: 0 4px;\">"
                    + "<div style=\"width: 44px; height: 52px; line-height: 52px; text-align: center; "
                    + "background-color: #F0F6FF; border: 1.5px solid #2563EB; border-radius: 10px; "
                    + "font-size: 22px; font-weight: 700; color: #1D4ED8; font-family: 'Segoe UI', Roboto, Arial, sans-serif;\">"
                    + digit
                    + "</div>"
                    + "</td>"
            );
        }

        return "<!DOCTYPE html>"
                + "<html lang=\"id\">"
                + "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>"
                + "<body style=\"margin: 0; padding: 24px 12px; background-color: #F4F6F9; font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;\">"
                + "  <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" width=\"100%\" style=\"max-width: 500px; margin: 0 auto; background-color: #FFFFFF; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(11, 28, 48, 0.08); border: 1px solid #E2E8F0;\">"
                + "    <!-- Header Biru PinPoint -->"
                + "    <tr>"
                + "      <td style=\"background-color: #1D63DC; padding: 28px 24px; text-align: center;\">"
                + "        <div style=\"font-size: 22px; font-weight: 700; color: #FFFFFF; letter-spacing: 0.3px;\">&#128205; PinPoint</div>"
                + "        <div style=\"font-size: 12.5px; color: #DBEAFE; margin-top: 6px;\">Sistem Pelaporan Infrastruktur Daerah</div>"
                + "      </td>"
                + "    </tr>"
                + "    <!-- Isi Pesan -->"
                + "    <tr>"
                + "      <td style=\"padding: 32px 32px 28px 32px; color: #334155;\">"
                + "        <p style=\"margin: 0 0 16px 0; font-size: 16px; color: #1E293B;\">Halo, <strong>" + escapeHtml(userName) + "</strong> &#128075;</p>"
                + "        <p style=\"margin: 0 0 28px 0; font-size: 14px; line-height: 1.6; color: #475569;\">"
                +            introText
                + "        </p>"
                + "        <!-- Label & 6 Kotak Kode OTP -->"
                + "        <div style=\"text-align: center; margin-bottom: 12px; font-size: 11px; font-weight: 700; letter-spacing: 1px; color: #64748B; text-transform: uppercase;\">"
                + "          KODE VERIFIKASI ANDA"
                + "        </div>"
                + "        <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" style=\"margin: 0 auto 28px auto;\">"
                + "          <tr>"
                +              otpCells
                + "          </tr>"
                + "        </table>"
                + "        <!-- Kotak Peringatan Kuning 15 Menit -->"
                + "        <div style=\"background-color: #FEF3C7; border-left: 4px solid #F59E0B; border-radius: 6px; padding: 14px 16px; margin-bottom: 28px; font-size: 12.5px; line-height: 1.5; color: #92400E;\">"
                + "          &#9432; <strong>Kode berlaku selama 15 menit</strong> sejak email ini dikirimkan. Jangan bagikan kode ini kepada siapapun, termasuk tim PinPoint."
                + "        </div>"
                + "        <hr style=\"border: none; border-top: 1px solid #F1F5F9; margin: 0 0 20px 0;\" />"
                + "        <!-- Footer -->"
                + "        <p style=\"margin: 0 0 12px 0; text-align: center; font-size: 12px; color: #94A3B8;\">"
                +            footerIgnoreText
                + "        </p>"
                + "        <p style=\"margin: 0; text-align: center; font-size: 12px; color: #94A3B8;\">"
                + "          Butuh bantuan? Hubungi <a href=\"mailto:support@pinpoint.id\" style=\"color: #2563EB; text-decoration: underline;\">support@pinpoint.id</a>"
                + "        </p>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
