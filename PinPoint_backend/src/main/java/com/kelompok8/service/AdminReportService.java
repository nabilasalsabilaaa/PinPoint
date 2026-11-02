package com.kelompok8.service;

import com.kelompok8.model.Laporan;
import com.kelompok8.model.RiwayatStatus;
import com.kelompok8.model.Notifikasi;
import com.kelompok8.model.User;
import com.kelompok8.repository.LaporanRepository;
import com.kelompok8.repository.RiwayatStatusRepository;
import com.kelompok8.repository.NotifikasiRepository;
import com.kelompok8.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminReportService {

    @Autowired
    private LaporanRepository laporanRepository;

    @Autowired
    private RiwayatStatusRepository riwayatStatusRepository;

    @Autowired
    private NotifikasiRepository notifikasiRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Laporan> getAllReports() {
        return laporanRepository.findAll();
    }

    public Laporan approveReport(Long idLaporan, String adminEmail) {
        Laporan laporan = laporanRepository.findById(idLaporan)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

        // Update status laporan menjadi disetujui / terverifikasi (Asumsi ID Status 2 = Terverifikasi)
        laporan.setIdStatus(2L);
        laporan.setTanggalUpdate(LocalDateTime.now());
        Laporan updated = laporanRepository.save(laporan);

        // Catat riwayat status
        RiwayatStatus riwayat = new RiwayatStatus();
        riwayat.setIdLaporan(idLaporan);
        riwayat.setIdStatus(2L);
        riwayat.setIdPengubah(admin.getId());
        riwayat.setKeterangan("Laporan disetujui dan diverifikasi oleh admin.");
        riwayatStatusRepository.save(riwayat);

        // Kirim notifikasi ke user pemilik laporan
        Notifikasi notifikasi = new Notifikasi();
        notifikasi.setIdUser(laporan.getIdUser());
        notifikasi.setIdLaporan(idLaporan);
        notifikasi.setJudul("Laporan Diverifikasi");
        notifikasi.setPesan("Laporan Anda dengan judul '" + laporan.getJudul() + "' telah diverifikasi oleh admin dan diteruskan ke pihak pemerintah.");
        notifikasi.setTipe("INFO");
        notifikasiRepository.save(notifikasi);

        return updated;
    }

    public Laporan rejectReport(Long idLaporan, String adminEmail, String catatanPenolakan) {
        Laporan laporan = laporanRepository.findById(idLaporan)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

        // Update status laporan menjadi ditolak (Asumsi ID Status 3 = Ditolak)
        laporan.setIdStatus(3L);
        laporan.setTanggalUpdate(LocalDateTime.now());
        Laporan updated = laporanRepository.save(laporan);

        // Catat riwayat status beserta alasan penolakan
        RiwayatStatus riwayat = new RiwayatStatus();
        riwayat.setIdLaporan(idLaporan);
        riwayat.setIdStatus(3L);
        riwayat.setIdPengubah(admin.getId());
        riwayat.setKeterangan("Laporan ditolak. Catatan: " + catatanPenolakan);
        riwayatStatusRepository.save(riwayat);

        // Kirim notifikasi penolakan ke user pemilik laporan
        Notifikasi notifikasi = new Notifikasi();
        notifikasi.setIdUser(laporan.getIdUser());
        notifikasi.setIdLaporan(idLaporan);
        notifikasi.setJudul("Laporan Ditolak");
        notifikasi.setPesan("Laporan Anda dengan judul '" + laporan.getJudul() + "' ditolak. Alasan: " + catatanPenolakan);
        notifikasi.setTipe("WARNING");
        notifikasiRepository.save(notifikasi);

        return updated;
    }
}