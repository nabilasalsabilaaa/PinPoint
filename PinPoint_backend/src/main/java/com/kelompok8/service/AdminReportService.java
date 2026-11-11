package com.kelompok8.service;

import com.kelompok8.model.Kategori;
import com.kelompok8.model.Laporan;
import com.kelompok8.model.RiwayatStatus;
import com.kelompok8.model.Notifikasi;
import com.kelompok8.model.User;
import com.kelompok8.repository.CategoryRepository;
import com.kelompok8.repository.LaporanRepository;
import com.kelompok8.repository.RiwayatStatusRepository;
import com.kelompok8.repository.NotifikasiRepository;
import com.kelompok8.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminReportService {

    @Autowired
    private LaporanRepository laporanRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RiwayatStatusRepository riwayatStatusRepository;

    @Autowired
    private NotifikasiRepository notifikasiRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Laporan> getAllReports() {
        List<Laporan> reports = laporanRepository.findAll();
        Map<Long, String> categoryMap = categoryRepository.findAll().stream()
                .filter(k -> k.getId() != null && k.getNamaKategori() != null)
                .collect(Collectors.toMap(Kategori::getId, Kategori::getNamaKategori, (a, b) -> a));
        Map<Long, User> userMap = userRepository.findAll().stream()
                .filter(u -> u.getId() != null)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        for (Laporan r : reports) {
            if (r.getIdKategori() != null) {
                r.setCategoryName(categoryMap.getOrDefault(r.getIdKategori(), "Kategori #" + r.getIdKategori()));
            }
            if (r.getIdUser() != null && userMap.containsKey(r.getIdUser())) {
                User pelapor = userMap.get(r.getIdUser());
                r.setReporterName(pelapor.getNama());
                r.setNoTelepon(pelapor.getNoTelepon());
            } else {
                r.setReporterName("Warga Pelapor (ID #" + (r.getIdUser() != null ? r.getIdUser() : "-") + ")");
                r.setNoTelepon("0812-4211-0000");
            }
        }
        reports.sort(Comparator.comparing(Laporan::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        return reports;
    }

    public Laporan approveReport(Long idLaporan, String adminEmail) {
        Laporan laporan = laporanRepository.findById(idLaporan)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));

        // Update status laporan menjadi disetujui / terverifikasi (ID Status 2 = Terverifikasi)
        laporan.setIdStatus(2L);
        laporan.setTanggalUpdate(LocalDateTime.now());
        Laporan updated = laporanRepository.save(laporan);

        try {
            User admin = (adminEmail != null ? userRepository.findByEmail(adminEmail) : java.util.Optional.<User>empty())
                    .orElseGet(() -> userRepository.findAll().stream()
                            .filter(u -> u.getIdRole() != null && u.getIdRole() == 1L)
                            .findFirst()
                            .orElse(null));

            if (admin != null) {
                RiwayatStatus riwayat = new RiwayatStatus();
                riwayat.setIdLaporan(idLaporan);
                riwayat.setIdStatus(2L);
                riwayat.setIdPengubah(admin.getId());
                riwayat.setKeterangan("Laporan disetujui dan diverifikasi oleh admin.");
                riwayatStatusRepository.save(riwayat);
            }

            Notifikasi notifikasi = new Notifikasi();
            notifikasi.setIdUser(laporan.getIdUser());
            notifikasi.setIdLaporan(idLaporan);
            notifikasi.setJudul("Laporan Diverifikasi");
            notifikasi.setPesan("Laporan Anda dengan judul '" + laporan.getJudul() + "' telah diverifikasi oleh admin dan diteruskan ke pihak pemerintah.");
            notifikasi.setTipe("INFO");
            notifikasiRepository.save(notifikasi);
        } catch (Exception ignored) {}

        return enrichSingleReport(updated);
    }

    public Laporan rejectReport(Long idLaporan, String adminEmail, String catatanPenolakan) {
        Laporan laporan = laporanRepository.findById(idLaporan)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));

        // Update status laporan menjadi ditolak (ID Status 3 = Ditolak)
        laporan.setIdStatus(3L);
        laporan.setTanggalUpdate(LocalDateTime.now());
        Laporan updated = laporanRepository.save(laporan);

        try {
            User admin = (adminEmail != null ? userRepository.findByEmail(adminEmail) : java.util.Optional.<User>empty())
                    .orElseGet(() -> userRepository.findAll().stream()
                            .filter(u -> u.getIdRole() != null && u.getIdRole() == 1L)
                            .findFirst()
                            .orElse(null));

            if (admin != null) {
                RiwayatStatus riwayat = new RiwayatStatus();
                riwayat.setIdLaporan(idLaporan);
                riwayat.setIdStatus(3L);
                riwayat.setIdPengubah(admin.getId());
                String ket = "Laporan ditolak. " + catatanPenolakan;
                if (ket.length() > 250) {
                    ket = ket.substring(0, 250);
                }
                riwayat.setKeterangan(ket);
                riwayatStatusRepository.save(riwayat);
            }

            Notifikasi notifikasi = new Notifikasi();
            notifikasi.setIdUser(laporan.getIdUser());
            notifikasi.setIdLaporan(idLaporan);
            notifikasi.setJudul("Laporan Ditolak");
            notifikasi.setPesan("Laporan Anda dengan judul '" + laporan.getJudul() + "' ditolak. " + catatanPenolakan);
            notifikasi.setTipe("WARNING");
            notifikasiRepository.save(notifikasi);
        } catch (Exception ignored) {}

        return enrichSingleReport(updated);
    }

    private Laporan enrichSingleReport(Laporan r) {
        if (r.getIdKategori() != null) {
            categoryRepository.findById(r.getIdKategori())
                    .ifPresent(k -> r.setCategoryName(k.getNamaKategori()));
        }
        if (r.getIdUser() != null) {
            userRepository.findById(r.getIdUser()).ifPresentOrElse(u -> {
                r.setReporterName(u.getNama());
                r.setNoTelepon(u.getNoTelepon());
            }, () -> {
                r.setReporterName("Warga Pelapor (ID #" + r.getIdUser() + ")");
                r.setNoTelepon("0812-4211-0000");
            });
        }
        return r;
    }
}