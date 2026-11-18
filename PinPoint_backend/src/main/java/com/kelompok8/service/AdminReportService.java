package com.kelompok8.service;

import com.kelompok8.model.*;
import com.kelompok8.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
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

    @Autowired
    private FotoLaporanRepository fotoLaporanRepository;

    @Autowired
    private BuktiPerbaikanRepository buktiPerbaikanRepository;

    @Autowired
    private FotoBuktiPerbaikanRepository fotoBuktiPerbaikanRepository;

    public List<Laporan> getAllReports() {
        List<Laporan> reports = laporanRepository.findAll();
        Map<Long, String> categoryMap = categoryRepository.findAll().stream()
                .filter(k -> k.getId() != null && k.getNamaKategori() != null)
                .collect(Collectors.toMap(Kategori::getId, Kategori::getNamaKategori, (a, b) -> a));
        Map<Long, User> userMap = userRepository.findAll().stream()
                .filter(u -> u.getId() != null)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        for (Laporan r : reports) {
            enrichReportData(r, categoryMap, userMap);
        }
        reports.sort(Comparator.comparing(Laporan::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        return reports;
    }

    public void enrichReportData(Laporan r, Map<Long, String> categoryMap, Map<Long, User> userMap) {
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

        if (r.getId() != null) {
            List<String> reportPhotos = new ArrayList<>();
            List<FotoLaporan> fotoList = fotoLaporanRepository.findByIdLaporanOrderByIdAsc(r.getId());
            for (FotoLaporan fl : fotoList) {
                if (fl.getPathFile() != null && !fl.getPathFile().isBlank()) {
                    reportPhotos.add(fl.getPathFile());
                }
            }
            if (!reportPhotos.isEmpty()) {
                r.setFotoProfil(reportPhotos.get(0));
            }
            r.setPhotoUrls(reportPhotos);

            List<BuktiPerbaikan> buktiList = buktiPerbaikanRepository.findByIdLaporanOrderByIdDesc(r.getId());
            if (!buktiList.isEmpty()) {
                BuktiPerbaikan latestBukti = buktiList.get(0);
                r.setEvidenceNotes(latestBukti.getKeterangan());
                if (latestBukti.getIdPemerintah() != null && userMap.containsKey(latestBukti.getIdPemerintah())) {
                    r.setCompletedBy(userMap.get(latestBukti.getIdPemerintah()).getNama());
                } else {
                    r.setCompletedBy("Tim Dinas Pemerintah");
                }

                List<String> evPhotos = new ArrayList<>();
                for (BuktiPerbaikan bp : buktiList) {
                    if (bp.getId() != null) {
                        List<FotoBuktiPerbaikan> fbpList = fotoBuktiPerbaikanRepository.findByIdBuktiOrderByIdAsc(bp.getId());
                        for (FotoBuktiPerbaikan fbp : fbpList) {
                            if (fbp.getPathFile() != null && !fbp.getPathFile().isBlank() && !evPhotos.contains(fbp.getPathFile())) {
                                evPhotos.add(fbp.getPathFile());
                            }
                        }
                    }
                    if (bp.getPathFile() != null && !bp.getPathFile().isBlank() && !evPhotos.contains(bp.getPathFile())) {
                        evPhotos.add(bp.getPathFile());
                    }
                }
                r.setEvidencePhotoUrls(evPhotos);
            }
        }
    }

    public Laporan approveReport(Long idLaporan, String adminEmail) {
        Laporan laporan = laporanRepository.findById(idLaporan)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));

        // Update status laporan menjadi disetujui / terverifikasi (ID Status 2 = Terverifikasi)
        laporan.setIdStatus(2L);
        laporan.setTanggalUpdate(LocalDateTime.now());
        Laporan updated = laporanRepository.save(laporan);

        try {
            User admin = (adminEmail != null ? userRepository.findByEmail(adminEmail) : Optional.<User>empty())
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
            User admin = (adminEmail != null ? userRepository.findByEmail(adminEmail) : Optional.<User>empty())
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

    public Laporan enrichSingleReport(Laporan r) {
        Map<Long, String> categoryMap = categoryRepository.findAll().stream()
                .filter(k -> k.getId() != null && k.getNamaKategori() != null)
                .collect(Collectors.toMap(Kategori::getId, Kategori::getNamaKategori, (a, b) -> a));
        Map<Long, User> userMap = userRepository.findAll().stream()
                .filter(u -> u.getId() != null)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        enrichReportData(r, categoryMap, userMap);
        return r;
    }
}
