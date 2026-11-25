package com.kelompok8.service;

import com.kelompok8.model.*;
import com.kelompok8.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class UserReportService {

    @Autowired
    private LaporanRepository laporanRepository;

    @Autowired
    private RiwayatStatusRepository riwayatStatusRepository;

    @Autowired
    private BuktiPerbaikanRepository buktiPerbaikanRepository;

    @Autowired
    private NotifikasiRepository notifikasiRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FotoLaporanRepository fotoLaporanRepository;

    @Autowired
    private AdminReportService adminReportService;

    // Menyimpan jumlah dukungan per laporan
    private final Map<Long, Set<Long>> reportSupporters = new ConcurrentHashMap<>();

    public Laporan createReport(String userEmail, Laporan laporan, List<String> rawPhotos) {
        User user = resolveUser(userEmail);

        laporan.setIdUser(user.getId());
        laporan.setIdStatus(1L); // ID 1 = Menunggu Verifikasi
        laporan.setTanggalLaporan(LocalDateTime.now());
        laporan.setTanggalUpdate(LocalDateTime.now());
        Laporan saved = laporanRepository.save(laporan);

        // Simpan foto-foto laporan (bisa lebih dari 1 foto)
        if (rawPhotos != null && !rawPhotos.isEmpty() && saved.getId() != null) {
            int index = 1;
            for (String raw : rawPhotos) {
                if (raw == null || raw.isBlank()) continue;
                String storedPath = storeReportPhoto(saved.getId(), index++, raw.trim());
                if (storedPath != null) {
                    FotoLaporan fl = new FotoLaporan();
                    fl.setIdLaporan(saved.getId());
                    fl.setPathFile(storedPath);
                    fl.setNamaFile(storedPath.substring(storedPath.lastIndexOf('/') + 1));
                    fl.setTipeFile("image/jpeg");
                    fl.setUkuranFile(0);
                    fl.setKeterangan("Foto laporan warga");
                    fl.setTanggalUpload(LocalDateTime.now());
                    fotoLaporanRepository.save(fl);
                }
            }
        }

        // Catat riwayat status awal: Laporan Masuk
        try {
            RiwayatStatus riwayat = new RiwayatStatus();
            riwayat.setIdLaporan(saved.getId());
            riwayat.setIdStatus(1L);
            riwayat.setIdPengubah(user.getId());
            riwayat.setKeterangan("Laporan masuk ke sistem dan menunggu verifikasi admin.");
            riwayatStatusRepository.save(riwayat);
        } catch (Exception ignored) {}

        // Buat notifikasi 1: Laporan Masuk
        try {
            Notifikasi notif = new Notifikasi();
            notif.setIdUser(user.getId());
            notif.setIdLaporan(saved.getId());
            notif.setJudul("Laporan Masuk");
            notif.setPesan("Laporan Anda '" + saved.getJudul() + "' telah diterima sistem dan sedang menunggu verifikasi admin.");
            notif.setTipe("MASUK");
            notif.setSudahDibaca(false);
            notif.setTanggalDibuat(LocalDateTime.now());
            notifikasiRepository.save(notif);
        } catch (Exception ignored) {}

        return enrichWithSupport(adminReportService.enrichSingleReport(saved));
    }

    public List<Laporan> getReportsByUser(String userEmail) {
        User user = resolveUser(userEmail);
        return adminReportService.getAllReports().stream()
                .filter(r -> Objects.equals(r.getIdUser(), user.getId()))
                .map(this::enrichWithSupport)
                .collect(Collectors.toList());
    }

    public List<Laporan> getAllReportsForMap() {
        List<Laporan> all = adminReportService.getAllReports().stream()
                .map(this::enrichWithSupport)
                .collect(Collectors.toList());
        all.sort(Comparator
                .comparing(Laporan::getSupportCount, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Laporan::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        return all;
    }

    public Optional<Laporan> getReportDetail(Long id) {
        return laporanRepository.findById(id)
                .map(adminReportService::enrichSingleReport)
                .map(this::enrichWithSupport);
    }

    public Map<String, Object> toggleSupportReport(Long idLaporan, String userEmail) {
        User user = resolveUser(userEmail);
        Set<Long> supporters = reportSupporters.computeIfAbsent(idLaporan, k -> ConcurrentHashMap.newKeySet());
        boolean supported;
        if (supporters.contains(user.getId())) {
            supporters.remove(user.getId());
            supported = false;
        } else {
            supporters.add(user.getId());
            supported = true;
        }
        Map<String, Object> res = new HashMap<>();
        res.put("reportId", idLaporan);
        res.put("supported", supported);
        res.put("supportCount", supporters.size());
        return res;
    }

    public List<RiwayatStatus> getStatusHistory(Long idLaporan) {
        return riwayatStatusRepository.findByIdLaporan(idLaporan);
    }

    public Optional<BuktiPerbaikan> getEvidence(Long idLaporan) {
        return buktiPerbaikanRepository.findByIdLaporan(idLaporan);
    }

    public List<Notifikasi> getUserNotifications(String userEmail) {
        User user = resolveUser(userEmail);
        syncNotificationsForUserReports(user);

        List<Notifikasi> list = new ArrayList<>(notifikasiRepository.findByIdUser(user.getId()));
        // Filter hanya 3 kategori notifikasi utama: Laporan Masuk, Diverifikasi Admin, Selesai Diperbaiki Pemerintah (atau Ditolak)
        list.sort(Comparator.comparing(Notifikasi::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    private void syncNotificationsForUserReports(User user) {
        try {
            List<Laporan> userReports = laporanRepository.findByIdUser(user.getId());
            List<Notifikasi> existing = notifikasiRepository.findByIdUser(user.getId());

            for (Laporan r : userReports) {
                if (r.getId() == null) continue;
                Long status = r.getIdStatus() != null ? r.getIdStatus() : 1L;

                // 1. Pastikan notifikasi "Laporan Masuk" ada
                boolean hasMasuk = existing.stream().anyMatch(n ->
                        Objects.equals(n.getIdLaporan(), r.getId()) &&
                                (n.getJudul() != null && n.getJudul().toLowerCase().contains("masuk")));
                if (!hasMasuk) {
                    Notifikasi n = new Notifikasi();
                    n.setIdUser(user.getId());
                    n.setIdLaporan(r.getId());
                    n.setJudul("Laporan Masuk");
                    n.setPesan("Laporan Anda '" + r.getJudul() + "' telah masuk ke sistem dan menunggu verifikasi admin.");
                    n.setTipe("MASUK");
                    n.setSudahDibaca(false);
                    n.setTanggalDibuat(r.getTanggalLaporan() != null ? r.getTanggalLaporan() : LocalDateTime.now());
                    notifikasiRepository.save(n);
                    existing.add(n);
                }

                // 2. Jika sudah diverifikasi admin (status 2, 4, atau 5), pastikan notifikasi "Laporan Diverifikasi" ada
                if (status == 2L || status == 4L || status == 5L) {
                    boolean hasVerif = existing.stream().anyMatch(n ->
                            Objects.equals(n.getIdLaporan(), r.getId()) &&
                                    (n.getJudul() != null && n.getJudul().toLowerCase().contains("diverifikasi")));
                    if (!hasVerif) {
                        Notifikasi n = new Notifikasi();
                        n.setIdUser(user.getId());
                        n.setIdLaporan(r.getId());
                        n.setJudul("Laporan Diverifikasi Admin");
                        n.setPesan("Laporan Anda '" + r.getJudul() + "' telah diverifikasi oleh admin dan diteruskan ke pihak pemerintah.");
                        n.setTipe("VERIFIKASI");
                        n.setSudahDibaca(false);
                        n.setTanggalDibuat(r.getTanggalUpdate() != null ? r.getTanggalUpdate() : LocalDateTime.now());
                        notifikasiRepository.save(n);
                        existing.add(n);
                    }
                }

                // 3. Jika sudah selesai diperbaiki pemerintah (status 5), pastikan notifikasi "Laporan Selesai" ada
                if (status == 5L) {
                    boolean hasSelesai = existing.stream().anyMatch(n ->
                            Objects.equals(n.getIdLaporan(), r.getId()) &&
                                    (n.getJudul() != null && n.getJudul().toLowerCase().contains("selesai")));
                    if (!hasSelesai) {
                        Notifikasi n = new Notifikasi();
                        n.setIdUser(user.getId());
                        n.setIdLaporan(r.getId());
                        n.setJudul("Laporan Selesai Diperbaiki");
                        n.setPesan("Laporan Anda '" + r.getJudul() + "' telah selesai diperbaiki oleh pihak pemerintah.");
                        n.setTipe("SELESAI");
                        n.setSudahDibaca(false);
                        n.setTanggalDibuat(r.getTanggalUpdate() != null ? r.getTanggalUpdate() : LocalDateTime.now());
                        notifikasiRepository.save(n);
                        existing.add(n);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public void markNotificationAsRead(Long notifId) {
        Notifikasi notif = notifikasiRepository.findById(notifId)
                .orElseThrow(() -> new RuntimeException("Notifikasi tidak ditemukan"));
        notif.setSudahDibaca(true);
        notifikasiRepository.save(notif);
    }

    private Laporan enrichWithSupport(Laporan r) {
        if (r != null && r.getId() != null) {
            Set<Long> s = reportSupporters.get(r.getId());
            r.setSupportCount(s != null ? s.size() : 0);
        }
        return r;
    }

    private String storeReportPhoto(Long idLaporan, int index, String raw) {
        try {
            if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("/api/files/")) {
                return raw.length() <= 500 ? raw : raw.substring(0, 500);
            }
            String base64Data = raw;
            if (raw.contains(",")) {
                base64Data = raw.substring(raw.indexOf(',') + 1);
            }
            if (base64Data.length() > 120 && !raw.startsWith("file:") && !raw.startsWith("content:")) {
                byte[] bytes = Base64.getMimeDecoder().decode(base64Data);
                File uploadDir = new File("uploads");
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                String fileName = "laporan_" + idLaporan + "_" + System.currentTimeMillis() + "_" + index + ".jpg";
                File outFile = new File(uploadDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(bytes);
                    fos.flush();
                }
                return "/api/files/" + fileName;
            }
            return raw.length() <= 500 ? raw : raw.substring(0, 500);
        } catch (Exception e) {
            return null;
        }
    }

    private User resolveUser(String userEmail) {
        if (userEmail != null && !userEmail.isBlank()) {
            Optional<User> opt = userRepository.findByEmail(userEmail);
            if (opt.isPresent()) return opt.get();
        }
        return userRepository.findAll().stream()
                .filter(u -> u.getIdRole() != null && u.getIdRole() == 2L)
                .findFirst()
                .orElseGet(() -> userRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("User tidak ditemukan!")));
    }
}
