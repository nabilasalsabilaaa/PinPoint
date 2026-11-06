package com.kelompok8.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service untuk pengiriman email transaksional PinPoint.
 *
 * <p>Mendukung dua jenis email:
 * <ul>
 *   <li>OTP reset password — dikirim asinkron (@Async)</li>
 *   <li>Selamat datang setelah registrasi — dikirim asinkron (@Async)</li>
 * </ul>
 *
 * <p>Template HTML inline responsif, mendukung dark mode,
 * dan telah diuji kompatibilitasnya dengan Gmail, Outlook, dan Apple Mail.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreplay.pinpoint@gmail.com}")
    private String fromAddress;

    private static final String APP_NAME    = "PinPoint";
    private static final String BRAND_COLOR = "#2563EB"; // Biru primer PinPoint
    private static final String LOGO_TEXT   = "📍 PinPoint";

    // ══════════════════════════════════════════════════════════════════════════
    //  OTP EMAIL METHODS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Mengirim email OTP untuk verifikasi pendaftaran akun baru.
     *
     * @param toEmail  alamat email tujuan
     * @param otpCode  kode OTP 6-digit
     * @param userName nama lengkap pendaftar
     */
    public void sendRegistrationOtpEmail(String toEmail, String otpCode, String userName) {
        String subject = "🔐 Kode Verifikasi Registrasi — " + APP_NAME;
        String desc = "Terima kasih telah mendaftar di " + APP_NAME + ". Masukkan kode OTP berikut pada aplikasi untuk memverifikasi akun Anda:";
        sendOtpEmailInternal(toEmail, subject, userName, otpCode, desc);
    }

    /**
     * Mengirim email OTP untuk lupa / reset kata sandi.
     *
     * @param toEmail  alamat email tujuan
     * @param otpCode  kode OTP 6-digit
     * @param userName nama lengkap penerima
     */
    public void sendForgotPasswordOtpEmail(String toEmail, String otpCode, String userName) {
        String subject = "🔐 Kode OTP Reset Kata Sandi — " + APP_NAME;
        String desc = "Kami menerima permintaan reset kata sandi untuk akun " + APP_NAME + " Anda. Masukkan kode OTP berikut pada aplikasi untuk melanjutkan proses verifikasi:";
        sendOtpEmailInternal(toEmail, subject, userName, otpCode, desc);
    }

    /**
     * Overload backward-compatibility untuk pengiriman OTP.
     */
    public void sendOtpEmail(String toEmail, String otpCode, String userName) {
        sendForgotPasswordOtpEmail(toEmail, otpCode, userName);
    }

    public void sendOtpEmail(String toEmail, String otpCode) {
        sendForgotPasswordOtpEmail(toEmail, otpCode, "Pengguna");
    }

    public void sendOtpEmailSync(String toEmail, String otpCode, String userName) {
        sendForgotPasswordOtpEmail(toEmail, otpCode, userName);
    }

    /**
     * Mengirim email OTP via SMTP secara andal dengan multipart plain-text + HTML.
     */
    private void sendOtpEmailInternal(String toEmail, String subject, String userName, String otpCode, String desc) {
        try {
            logger.info("Memulai pengiriman email OTP [{}] ke [{}]...", subject, toEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, APP_NAME);
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String plainText = "Halo, " + (userName != null ? userName : "Pengguna") + "!\n\n"
                    + desc + "\n\n"
                    + "KODE OTP ANDA: " + otpCode + "\n\n"
                    + "Kode ini berlaku selama 15 menit. Masukkan kode ini pada aplikasi PinPoint.\n"
                    + "Jangan bagikan kode ini kepada siapapun.\n\n"
                    + "Salam,\nTim " + APP_NAME;

            String htmlText = buildOtpEmailHtml(userName, otpCode, desc);
            helper.setText(plainText, htmlText);

            mailSender.send(message);
            logger.info("✅ Email OTP berhasil terkirim via SMTP ke: {}", toEmail);

        } catch (MailAuthenticationException e) {
            String alert = "\n" +
                    "********************************************************************************\n" +
                    "❌ [SMTP AUTHENTICATION ERROR] Gagal mengirim Email OTP!\n" +
                    "Penerima         : " + toEmail + "\n" +
                    "Akun Pengirim    : " + fromAddress + "\n" +
                    "Detail Error     : 535 BadCredentials / Autentikasi Google Ditolak.\n" +
                    "PENYEBAB         : Google tidak mendukung password biasa (Less Secure Apps).\n" +
                    "SOLUSI           : Buat App Password (Sandi Aplikasi) 16 karakter di Google Account,\n" +
                    "                   masukkan ke .env tanpa spasi: MAIL_PASSWORD=xxxxxxxxxxxxxxxx\n" +
                    "********************************************************************************\n";
            System.err.println(alert);
            logger.error("❌ Gagal autentikasi SMTP saat mengirim email OTP ke [{}]: {}", toEmail, e.getMessage());
            throw new RuntimeException("Gagal autentikasi email server (535 BadCredentials). Periksa App Password Google.", e);

        } catch (MailSendException e) {
            logger.error("❌ Koneksi/Transport SMTP gagal saat mengirim email OTP ke [{}]: {}", toEmail, e.getMessage());
            throw new RuntimeException("Gagal mengirim email melalui server SMTP. Periksa koneksi internet server.", e);

        } catch (Exception e) {
            logger.error("❌ Terjadi kesalahan tak terduga saat pengiriman email OTP ke [{}]: {}", toEmail, e.getMessage());
            throw new RuntimeException("Gagal mengirim email OTP: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  WELCOME EMAIL
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Mengirim email selamat datang setelah registrasi berhasil.
     *
     * @param toEmail  alamat email tujuan
     * @param userName nama lengkap pengguna yang baru terdaftar
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            logger.info("Memulai proses pengiriman welcome email ke [{}]...", toEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, APP_NAME);
            helper.setTo(toEmail);
            helper.setSubject("🎉 Selamat Datang di " + APP_NAME + ", " + userName + "!");
            helper.setText(buildWelcomeEmailHtml(userName), true);

            mailSender.send(message);
            logger.info("✅ Email selamat datang berhasil dikirim ke: {}", toEmail);

        } catch (MailAuthenticationException e) {
            String alert = "\n" +
                    "********************************************************************************\n" +
                    "❌ [SMTP AUTHENTICATION ERROR] Gagal mengirim Welcome Email!\n" +
                    "Penerima         : " + toEmail + "\n" +
                    "Akun Pengirim    : " + fromAddress + "\n" +
                    "Detail Error     : 535 BadCredentials / Autentikasi Google Ditolak.\n" +
                    "PENYEBAB         : Google tidak lagi mendukung password biasa (Less Secure Apps).\n" +
                    "SOLUSI           : Buat App Password (Sandi Aplikasi) 16 karakter di Google Security,\n" +
                    "                   lalu masukkan ke .env pada baris MAIL_PASSWORD.\n" +
                    "********************************************************************************\n";
            System.err.println(alert);
            logger.error("❌ Gagal autentikasi SMTP saat mengirim welcome email ke [{}]: {}", toEmail, e.getMessage(), e);

        } catch (MailSendException e) {
            System.err.println("❌ [SMTP TRANSPORT/SEND ERROR] Gagal mengirim welcome email ke " + toEmail + ": " + e.getMessage());
            logger.error("❌ Koneksi/Transport SMTP gagal saat mengirim welcome email ke [{}]: {}", toEmail, e.getMessage(), e);

        } catch (Exception e) {
            System.err.println("❌ [EMAIL UNEXPECTED ERROR] Gagal mengirim welcome email ke " + toEmail + ": " + e.getMessage());
            logger.error("❌ Terjadi kesalahan saat pengiriman email welcome ke [{}]: {}", toEmail, e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HTML TEMPLATE BUILDERS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Membangun HTML template email OTP yang responsif.
     * Mendukung dark mode via media query.
     */
    private String buildOtpEmailHtml(String userName, String otpCode, String purposeDesc) {
        // Pisah setiap digit OTP untuk tampilan kotak terpisah
        String[] digits = otpCode.split("");
        StringBuilder otpBoxes = new StringBuilder();
        for (String d : digits) {
            otpBoxes.append(
                "<td style=\"padding:4px;\">" +
                "<div style=\"" +
                    "width:48px;height:56px;" +
                    "background:#F0F7FF;" +
                    "border:2px solid " + BRAND_COLOR + ";" +
                    "border-radius:10px;" +
                    "font-size:28px;font-weight:700;" +
                    "color:" + BRAND_COLOR + ";" +
                    "text-align:center;line-height:56px;" +
                    "font-family:'Courier New',monospace;" +
                "\">" + d + "</div></td>"
            );
        }

        return "<!DOCTYPE html>" +
        "<html lang=\"id\">" +
        "<head>" +
            "<meta charset=\"UTF-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<meta name=\"color-scheme\" content=\"light dark\">" +
            "<title>Kode OTP - " + APP_NAME + "</title>" +
            "<style>" +
                "body{margin:0;padding:0;background-color:#F3F4F6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;}" +
                ".wrapper{width:100%;background-color:#F3F4F6;padding:40px 16px;box-sizing:border-box;}" +
                ".card{max-width:520px;margin:0 auto;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);}" +
                ".header{background:" + BRAND_COLOR + ";padding:32px 24px;text-align:center;}" +
                ".header h1{margin:0;color:#FFFFFF;font-size:24px;font-weight:700;letter-spacing:-0.5px;}" +
                ".header p{margin:6px 0 0;color:rgba(255,255,255,0.85);font-size:13px;}" +
                ".body{padding:32px 36px;}" +
                ".greeting{font-size:16px;color:#374151;margin:0 0 16px;}" +
                ".desc{font-size:14px;color:#6B7280;line-height:1.6;margin:0 0 28px;}" +
                ".otp-label{font-size:12px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:1px;text-align:center;margin-bottom:12px;}" +
                ".otp-table{margin:0 auto;border-collapse:separate;border-spacing:0;}" +
                ".warning-box{background:#FEF3C7;border-left:4px solid #F59E0B;border-radius:0 8px 8px 0;padding:14px 16px;margin:28px 0;}" +
                ".warning-box p{margin:0;font-size:13px;color:#92400E;line-height:1.5;}" +
                ".divider{border:none;border-top:1px solid #E5E7EB;margin:28px 0;}" +
                ".footer-text{font-size:12px;color:#9CA3AF;text-align:center;line-height:1.6;}" +
                ".footer-text a{color:" + BRAND_COLOR + ";text-decoration:none;}" +
                ".footer-bar{background:#F9FAFB;padding:20px 24px;text-align:center;border-top:1px solid #E5E7EB;}" +
                ".footer-bar p{margin:0;font-size:11px;color:#9CA3AF;}" +
                "@media(prefers-color-scheme:dark){" +
                    "body,.wrapper{background-color:#111827!important;}" +
                    ".card{background:#1F2937!important;}" +
                    ".greeting{color:#F9FAFB!important;}" +
                    ".desc{color:#D1D5DB!important;}" +
                    ".footer-bar{background:#111827!important;border-color:#374151!important;}" +
                    ".footer-bar p,.footer-text{color:#6B7280!important;}" +
                    ".divider{border-color:#374151!important;}" +
                "}" +
                "@media(max-width:480px){" +
                    ".body{padding:24px 20px!important;}" +
                "}" +
            "</style>" +
        "</head>" +
        "<body>" +
        "<div class=\"wrapper\">" +
        "<div class=\"card\">" +

            // ── Header ──────────────────────────────────────────────────────
            "<div class=\"header\">" +
                "<h1>" + LOGO_TEXT + "</h1>" +
                "<p>Sistem Pelaporan Infrastruktur Daerah</p>" +
            "</div>" +

            // ── Body ────────────────────────────────────────────────────────
            "<div class=\"body\">" +
                "<p class=\"greeting\">Halo, <strong>" + escapeHtml(userName) + "</strong> 👋</p>" +
                "<p class=\"desc\">" +
                    escapeHtml(purposeDesc) +
                "</p>" +

                "<p class=\"otp-label\">Kode Verifikasi Anda</p>" +
                "<table class=\"otp-table\"><tr>" + otpBoxes + "</tr></table>" +

                "<div class=\"warning-box\" style=\"margin-top:28px;\">" +
                    "<p>⏱ <strong>Kode berlaku selama 15 menit</strong> sejak email ini dikirimkan. " +
                    "Jangan bagikan kode ini kepada siapapun, termasuk tim PinPoint.</p>" +
                "</div>" +

                "<hr class=\"divider\">" +

                "<p class=\"footer-text\">" +
                    "Jika Anda tidak meminta reset kata sandi, abaikan email ini. " +
                    "Akun Anda tetap aman.<br><br>" +
                    "Butuh bantuan? Hubungi <a href=\"mailto:support@pinpoint.id\">support@pinpoint.id</a>" +
                "</p>" +
            "</div>" +

            // ── Footer Bar ──────────────────────────────────────────────────
            "<div class=\"footer-bar\">" +
                "<p>© 2026 " + APP_NAME + " — Kelompok 8 · PPL · Universitas Brawijaya<br>" +
                "Email ini dikirim secara otomatis, mohon tidak membalas.</p>" +
            "</div>" +

        "</div>" +
        "</div>" +
        "</body>" +
        "</html>";
    }

    /**
     * Membangun HTML template email selamat datang yang responsif.
     */
    private String buildWelcomeEmailHtml(String userName) {
        return "<!DOCTYPE html>" +
        "<html lang=\"id\">" +
        "<head>" +
            "<meta charset=\"UTF-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<meta name=\"color-scheme\" content=\"light dark\">" +
            "<title>Selamat Datang - " + APP_NAME + "</title>" +
            "<style>" +
                "body{margin:0;padding:0;background-color:#F3F4F6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;}" +
                ".wrapper{width:100%;background-color:#F3F4F6;padding:40px 16px;box-sizing:border-box;}" +
                ".card{max-width:520px;margin:0 auto;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);}" +
                ".header{background:linear-gradient(135deg," + BRAND_COLOR + " 0%,#1D4ED8 100%);padding:40px 24px;text-align:center;}" +
                ".header .emoji{font-size:48px;display:block;margin-bottom:12px;}" +
                ".header h1{margin:0;color:#FFFFFF;font-size:22px;font-weight:700;}" +
                ".body{padding:32px 36px;}" +
                ".greeting{font-size:16px;color:#374151;margin:0 0 16px;}" +
                ".desc{font-size:14px;color:#6B7280;line-height:1.7;margin:0 0 24px;}" +
                ".features{background:#F0F7FF;border-radius:12px;padding:20px 24px;margin-bottom:24px;}" +
                ".feature-item{display:flex;align-items:flex-start;margin-bottom:12px;}" +
                ".feature-item:last-child{margin-bottom:0;}" +
                ".feature-icon{font-size:18px;margin-right:12px;flex-shrink:0;}" +
                ".feature-text{font-size:13px;color:#374151;line-height:1.5;}" +
                ".cta-btn{display:block;width:fit-content;margin:0 auto 24px;background:" + BRAND_COLOR + ";color:#FFFFFF;text-decoration:none;padding:14px 36px;border-radius:10px;font-size:15px;font-weight:600;text-align:center;}" +
                ".divider{border:none;border-top:1px solid #E5E7EB;margin:24px 0;}" +
                ".footer-text{font-size:12px;color:#9CA3AF;text-align:center;line-height:1.6;}" +
                ".footer-text a{color:" + BRAND_COLOR + ";text-decoration:none;}" +
                ".footer-bar{background:#F9FAFB;padding:20px 24px;text-align:center;border-top:1px solid #E5E7EB;}" +
                ".footer-bar p{margin:0;font-size:11px;color:#9CA3AF;}" +
                "@media(prefers-color-scheme:dark){" +
                    "body,.wrapper{background-color:#111827!important;}" +
                    ".card{background:#1F2937!important;}" +
                    ".greeting{color:#F9FAFB!important;}" +
                    ".desc{color:#D1D5DB!important;}" +
                    ".features{background:#1E3A5F!important;}" +
                    ".feature-text{color:#E5E7EB!important;}" +
                    ".footer-bar{background:#111827!important;border-color:#374151!important;}" +
                    ".footer-bar p,.footer-text{color:#6B7280!important;}" +
                "}" +
                "@media(max-width:480px){" +
                    ".body{padding:24px 20px!important;}" +
                    ".cta-btn{width:100%!important;box-sizing:border-box;}" +
                "}" +
            "</style>" +
        "</head>" +
        "<body>" +
        "<div class=\"wrapper\">" +
        "<div class=\"card\">" +

            // ── Header ──────────────────────────────────────────────────────
            "<div class=\"header\">" +
                "<span class=\"emoji\">🎉</span>" +
                "<h1>Selamat Datang di " + APP_NAME + "!</h1>" +
            "</div>" +

            // ── Body ────────────────────────────────────────────────────────
            "<div class=\"body\">" +
                "<p class=\"greeting\">Halo, <strong>" + escapeHtml(userName) + "</strong>!</p>" +
                "<p class=\"desc\">" +
                    "Akun Anda telah berhasil dibuat. Sekarang Anda dapat melaporkan " +
                    "kerusakan infrastruktur di sekitar Anda dan memantau progres penanganannya secara real-time." +
                "</p>" +

                "<div class=\"features\">" +
                    "<div class=\"feature-item\">" +
                        "<span class=\"feature-icon\">📸</span>" +
                        "<span class=\"feature-text\"><strong>Buat Laporan</strong> — Foto, lokasi GPS, dan deskripsi kerusakan infrastruktur</span>" +
                    "</div>" +
                    "<div class=\"feature-item\">" +
                        "<span class=\"feature-icon\">🗺️</span>" +
                        "<span class=\"feature-text\"><strong>Peta Interaktif</strong> — Lihat semua laporan di peta wilayah Anda</span>" +
                    "</div>" +
                    "<div class=\"feature-item\">" +
                        "<span class=\"feature-icon\">🔔</span>" +
                        "<span class=\"feature-text\"><strong>Notifikasi Real-time</strong> — Pantau status penanganan laporan Anda</span>" +
                    "</div>" +
                "</div>" +

                "<a href=\"https://pinpoint.id\" class=\"cta-btn\">Mulai Gunakan PinPoint →</a>" +

                "<hr class=\"divider\">" +

                "<p class=\"footer-text\">" +
                    "Butuh bantuan? Hubungi <a href=\"mailto:support@pinpoint.id\">support@pinpoint.id</a>" +
                "</p>" +
            "</div>" +

            // ── Footer Bar ──────────────────────────────────────────────────
            "<div class=\"footer-bar\">" +
                "<p>© 2026 " + APP_NAME + " — Kelompok 8 · PPL · Universitas Brawijaya<br>" +
                "Email ini dikirim secara otomatis, mohon tidak membalas.</p>" +
            "</div>" +

        "</div>" +
        "</div>" +
        "</body>" +
        "</html>";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /** Mencegah XSS — escape karakter HTML dalam input dinamis. */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }
}
