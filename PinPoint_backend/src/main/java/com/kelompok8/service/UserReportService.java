package com.kelompok8.service;

import com.kelompok8.model.*;
import com.kelompok8.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

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

    public Laporan createReport(String userEmail, Laporan laporan) {
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User tidak ditemukan!");
        }

        laporan.setIdUser(userOpt.get().getId());
        laporan.setIdStatus(1L); // Asumsi ID 1 adalah "Menunggu Verifikasi"
        return laporanRepository.save(laporan);
    }

    public List<Laporan> getReportsByUser(String userEmail) {
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User tidak ditemukan!");
        }
        return laporanRepository.findByIdUser(userOpt.get().getId());
    }

    public List<Laporan> getAllReportsForMap() {
        // Untuk fitur melihat peta laporan (bisa diakses publik/user)
        return laporanRepository.findAll();
    }

    public Optional<Laporan> getReportDetail(Long id) {
        return laporanRepository.findById(id);
    }

    public List<RiwayatStatus> getStatusHistory(Long idLaporan) {
        return riwayatStatusRepository.findByIdLaporan(idLaporan);
    }

    public Optional<BuktiPerbaikan> getEvidence(Long idLaporan) {
        return buktiPerbaikanRepository.findByIdLaporan(idLaporan);
    }

    public List<Notifikasi> getUserNotifications(String userEmail) {
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User tidak ditemukan!");
        }
        return notifikasiRepository.findByIdUser(userOpt.get().getId());
    }

    public void markNotificationAsRead(Long notifId) {
        Notifikasi notif = notifikasiRepository.findById(notifId)
                .orElseThrow(() -> new RuntimeException("Notifikasi tidak ditemukan"));
        notif.setSudahDibaca(true);
        notifikasiRepository.save(notif);
    }
}