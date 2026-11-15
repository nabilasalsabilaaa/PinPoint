package com.kelompok8.service;

import com.kelompok8.dto.DisposisiRequest;
import com.kelompok8.model.*;
import com.kelompok8.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GovernmentReportService {

    @Autowired
    private LaporanRepository laporanRepository;

    @Autowired
    private BuktiPerbaikanRepository buktiPerbaikanRepository;

    @Autowired
    private FotoBuktiPerbaikanRepository fotoBuktiPerbaikanRepository;

    @Autowired
    private RiwayatStatusRepository riwayatStatusRepository;

    @Autowired
    private NotifikasiRepository notifikasiRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminReportService adminReportService;

    public List<Laporan> getGovernmentReports() {
        return adminReportService.getAllReports().stream()
                .filter(r -> r.getIdStatus() != null && (r.getIdStatus() == 2L || r.getIdStatus() == 4L || r.getIdStatus() == 5L))
                .collect(Collectors.toList());
    }

    public List<Laporan> getVerifiedReports() {
        return adminReportService.getAllReports().stream()
                .filter(laporan -> laporan.getIdStatus() != null && (laporan.getIdStatus().equals(2L) || laporan.getIdStatus().equals(4L)))
                .collect(Collectors.toList());
    }

    public Laporan disposeReport(Long idLaporan, DisposisiRequest request, String adminEmail) {
        if (request == null || request.getIdDinas() == null) {
            throw new RuntimeException("ID dinas wajib diisi");
        }

        Laporan laporan = laporanRepository.findById(idLaporan)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));

        User admin = resolveUserOrFallback(adminEmail, 1L);
        User dinas = userRepository.findById(request.getIdDinas())
                .orElseGet(() -> resolveUserOrFallback(null, 3L));

        laporan.setIdStatus(4L);
        laporan.setTanggalUpdate(LocalDateTime.now());
        Laporan updated = laporanRepository.save(laporan);

        String catatan = request.getCatatan() == null || request.getCatatan().isBlank()
                ? "Laporan didisposisi ke dinas teknis terkait."
                : request.getCatatan().trim();

        if (dinas != null) {
            RiwayatStatus riwayat = new RiwayatStatus();
            riwayat.setIdLaporan(idLaporan);
            riwayat.setIdStatus(4L);
            riwayat.setIdPengubah(dinas.getId());
            riwayat.setKeterangan("Didisposisi oleh admin " + (admin != null ? admin.getNama() : "Admin") + " ke dinas "
                    + dinas.getNama() + ". Catatan: " + catatan);
            riwayatStatusRepository.save(riwayat);

            Notifikasi notifikasi = new Notifikasi();
            notifikasi.setIdUser(dinas.getId());
            notifikasi.setIdLaporan(idLaporan);
            notifikasi.setJudul("Tugas Disposisi Baru");
            notifikasi.setPesan("Laporan '" + laporan.getJudul() + "' didisposisi kepada dinas Anda. Catatan: " + catatan);
            notifikasi.setTipe("INFO");
            notifikasiRepository.save(notifikasi);
        }

        return adminReportService.enrichSingleReport(updated);
    }

    public List<Laporan> getAssignedReports(String emailPemerintah) {
        return getVerifiedReports();
    }

    public Laporan updateStatusProcess(Long idLaporan, String emailPemerintah, String draftNotes, List<String> photoItems) {
        Laporan laporan = laporanRepository.findById(idLaporan)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));

        User pemerintah = resolveUserOrFallback(emailPemerintah, 3L);

        // ID Status 4 = Diproses / Dikerjakan
        laporan.setIdStatus(4L);
        laporan.setTanggalUpdate(LocalDateTime.now());
        Laporan updated = laporanRepository.save(laporan);

        if (draftNotes != null && !draftNotes.isBlank() || (photoItems != null && !photoItems.isEmpty())) {
            saveEvidenceData(idLaporan, pemerintah, draftNotes, photoItems);
        }

        if (pemerintah != null) {
            RiwayatStatus riwayat = new RiwayatStatus();
            riwayat.setIdLaporan(idLaporan);
            riwayat.setIdStatus(4L);
            riwayat.setIdPengubah(pemerintah.getId());
            riwayat.setKeterangan("Pihak pemerintah sedang memproses perbaikan di lapangan.");
            riwayatStatusRepository.save(riwayat);
        }

        Notifikasi notifikasi = new Notifikasi();
        notifikasi.setIdUser(laporan.getIdUser());
        notifikasi.setIdLaporan(idLaporan);
        notifikasi.setJudul("Laporan Sedang Diproses");
        notifikasi.setPesan("Laporan '" + laporan.getJudul() + "' sedang ditindaklanjuti oleh tim pemerintah di lapangan.");
        notifikasi.setTipe("INFO");
        notifikasiRepository.save(notifikasi);

        return adminReportService.enrichSingleReport(updated);
    }

    public Laporan updateStatusProcess(Long idLaporan, String emailPemerintah) {
        return updateStatusProcess(idLaporan, emailPemerintah, null, null);
    }

    public BuktiPerbaikan uploadEvidence(Long idLaporan, String emailPemerintah, BuktiPerbaikan buktiRequest) {
        User pemerintah = resolveUserOrFallback(emailPemerintah, 3L);
        Laporan laporan = laporanRepository.findById(idLaporan)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));

        List<String> photos = new ArrayList<>();
        if (buktiRequest.getPathFile() != null && !buktiRequest.getPathFile().isBlank()) {
            photos.add(buktiRequest.getPathFile());
        }
        BuktiPerbaikan savedBukti = saveEvidenceData(idLaporan, pemerintah, buktiRequest.getKeterangan(), photos);

        laporan.setTanggalUpdate(LocalDateTime.now());
        laporanRepository.save(laporan);

        if (pemerintah != null) {
            RiwayatStatus riwayat = new RiwayatStatus();
            riwayat.setIdLaporan(idLaporan);
            riwayat.setIdStatus(laporan.getIdStatus() != null ? laporan.getIdStatus() : 4L);
            riwayat.setIdPengubah(pemerintah.getId());
            riwayat.setKeterangan("Bukti perbaikan fisik telah diunggah oleh dinas teknis.");
            riwayatStatusRepository.save(riwayat);
        }

        Notifikasi notifikasi = new Notifikasi();
        notifikasi.setIdUser(laporan.getIdUser());
        notifikasi.setIdLaporan(idLaporan);
        notifikasi.setJudul("Bukti Perbaikan Diunggah");
        notifikasi.setPesan("Dinas telah mengunggah bukti perbaikan untuk laporan '" + laporan.getJudul() + "'.");
        notifikasi.setTipe("INFO");
        notifikasiRepository.save(notifikasi);

        return savedBukti;
    }

    public Laporan completeReportWithEvidence(
            Long idLaporan,
            String emailPemerintah,
            String keterangan,
            List<String> photoItems,
            boolean sendNotification
    ) {
        Laporan laporan = laporanRepository.findById(idLaporan)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));

        User pemerintah = resolveUserOrFallback(emailPemerintah, 3L);

        saveEvidenceData(idLaporan, pemerintah, keterangan, photoItems);

        // ID Status 5 = Selesai Resmi
        laporan.setIdStatus(5L);
        laporan.setTanggalUpdate(LocalDateTime.now());
        Laporan updated = laporanRepository.save(laporan);

        String ketText = (keterangan != null && !keterangan.isBlank())
                ? keterangan.trim()
                : "Laporan perbaikan diselesaikan secara resmi oleh pihak pemerintah.";

        if (pemerintah != null) {
            RiwayatStatus riwayat = new RiwayatStatus();
            riwayat.setIdLaporan(idLaporan);
            riwayat.setIdStatus(5L);
            riwayat.setIdPengubah(pemerintah.getId());
            String riwayatKet = "Selesai diperbaiki: " + ketText;
            if (riwayatKet.length() > 250) {
                riwayatKet = riwayatKet.substring(0, 250);
            }
            riwayat.setKeterangan(riwayatKet);
            riwayatStatusRepository.save(riwayat);
        }

        if (sendNotification && laporan.getIdUser() != null) {
            Notifikasi notifikasi = new Notifikasi();
            notifikasi.setIdUser(laporan.getIdUser());
            notifikasi.setIdLaporan(idLaporan);
            notifikasi.setJudul("Laporan Selesai Diperbaiki");
            notifikasi.setPesan("Laporan Anda '" + laporan.getJudul() + "' telah selesai diperbaiki oleh Pemerintah. Keterangan: " + ketText);
            notifikasi.setTipe("SUCCESS");
            notifikasiRepository.save(notifikasi);
        }

        return adminReportService.enrichSingleReport(updated);
    }

    public Laporan completeReport(Long idLaporan, String emailPemerintah) {
        return completeReportWithEvidence(idLaporan, emailPemerintah, null, null, true);
    }

    private BuktiPerbaikan saveEvidenceData(
            Long idLaporan,
            User pemerintah,
            String keterangan,
            List<String> photoItems
    ) {
        Long govId = pemerintah != null && pemerintah.getId() != null ? pemerintah.getId() : 16L;
        List<String> storedPaths = new ArrayList<>();

        if (photoItems != null) {
            int idx = 1;
            for (String item : photoItems) {
                if (item == null || item.isBlank()) continue;
                String storedPath = storePhotoItem(idLaporan, idx++, item.trim());
                if (storedPath != null) {
                    storedPaths.add(storedPath);
                }
            }
        }

        BuktiPerbaikan bukti = buktiPerbaikanRepository.findFirstByIdLaporanOrderByIdDesc(idLaporan)
                .orElseGet(BuktiPerbaikan::new);
        bukti.setIdLaporan(idLaporan);
        bukti.setIdPemerintah(govId);
        if (keterangan != null && !keterangan.isBlank()) {
            bukti.setKeterangan(keterangan.trim());
        } else if (bukti.getKeterangan() == null) {
            bukti.setKeterangan("Perbaikan fasilitas umum telah diselesaikan.");
        }
        if (!storedPaths.isEmpty()) {
            String firstPath = storedPaths.get(0);
            bukti.setPathFile(firstPath);
            bukti.setNamaFile(firstPath.substring(firstPath.lastIndexOf('/') + 1));
        }
        bukti.setTanggalUpload(LocalDateTime.now());
        BuktiPerbaikan savedBukti = buktiPerbaikanRepository.save(bukti);

        if (!storedPaths.isEmpty() && savedBukti.getId() != null) {
            List<FotoBuktiPerbaikan> existing = fotoBuktiPerbaikanRepository.findByIdBuktiOrderByIdAsc(savedBukti.getId());
            Set<String> existingPaths = existing.stream()
                    .map(FotoBuktiPerbaikan::getPathFile)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (String path : storedPaths) {
                if (!existingPaths.contains(path)) {
                    FotoBuktiPerbaikan fbp = new FotoBuktiPerbaikan();
                    fbp.setIdBukti(savedBukti.getId());
                    fbp.setPathFile(path);
                    fbp.setNamaFile(path.substring(path.lastIndexOf('/') + 1));
                    fbp.setTipeFile("image/jpeg");
                    fbp.setUkuranFile(0);
                    fbp.setTanggalUpload(LocalDateTime.now());
                    fotoBuktiPerbaikanRepository.save(fbp);
                }
            }
        }

        return savedBukti;
    }

    private String storePhotoItem(Long idLaporan, int index, String raw) {
        try {
            if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("/api/files/")) {
                return raw.length() <= 500 ? raw : raw.substring(0, 500);
            }
            String base64Data = raw;
            if (raw.contains(",")) {
                base64Data = raw.substring(raw.indexOf(',') + 1);
            }
            // Cek apakah string merupakan Base64 image
            if (base64Data.length() > 120 && !raw.startsWith("file:") && !raw.startsWith("content:")) {
                byte[] bytes = Base64.getMimeDecoder().decode(base64Data);
                File uploadDir = new File("uploads");
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                String fileName = "bukti_" + idLaporan + "_" + System.currentTimeMillis() + "_" + index + ".jpg";
                File outFile = new File(uploadDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(bytes);
                    fos.flush();
                }
                return "/api/files/" + fileName;
            }
            return raw.length() <= 500 ? raw : raw.substring(0, 500);
        } catch (Exception e) {
            return raw.length() <= 500 ? raw : raw.substring(0, 500);
        }
    }

    private User resolveUserOrFallback(String email, Long fallbackRole) {
        if (email != null && !email.isBlank()) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) return byEmail.get();
        }
        return userRepository.findAll().stream()
                .filter(u -> u.getIdRole() != null && u.getIdRole().equals(fallbackRole))
                .findFirst()
                .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
    }
}
