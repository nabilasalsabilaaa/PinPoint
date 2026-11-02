package com.kelompok8.service;

import com.kelompok8.model.*;
import com.kelompok8.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernmentReportService {

@Autowired
private LaporanRepository laporanRepository;

@Autowired
private BuktiPerbaikanRepository buktiPerbaikanRepository;

@Autowired
private RiwayatStatusRepository riwayatStatusRepository;

@Autowired
private NotifikasiRepository notifikasiRepository;

@Autowired
private UserRepository userRepository;

        public List<Laporan> getVerifiedReports() {
        // Asumsi ID Status 2 adalah "Terverifikasi" (siap ditindaklanjuti oleh pemerintah)
        return laporanRepository.findAll().stream()
                .filter(laporan -> laporan.getIdStatus() != null && laporan.getIdStatus().equals(2L))
                .collect(Collectors.toList());
    }

    public Laporan updateStatusProcess(Long idLaporan, String emailPemerintah) {
        Laporan laporan = laporanRepository.findById(idLaporan)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));

        User pemerintah = userRepository.findByEmail(emailPemerintah)
                .orElseThrow(() -> new RuntimeException("Akun pemerintah tidak ditemukan"));

        // Asumsi ID Status 4 adalah "Diproses / Dikerjakan"
        laporan.setIdStatus(4L);
        laporan.setTanggalUpdate(LocalDateTime.now());
        Laporan updated = laporanRepository.save(laporan);

        // Catat riwayat status
        RiwayatStatus riwayat = new RiwayatStatus();
        riwayat.setIdLaporan(idLaporan);
        riwayat.setIdStatus(4L);
        riwayat.setIdPengubah(pemerintah.getId());
        riwayat.setKeterangan("Pihak pemerintah sedang memproses/menangani perbaikan di lapangan.");
        riwayatStatusRepository.save(riwayat);

        // Kirim notifikasi ke user
        Notifikasi notifikasi = new Notifikasi();
        notifikasi.setIdUser(laporan.getIdUser());
        notifikasi.setIdLaporan(idLaporan);
        notifikasi.setJudul("Laporan Sedang Diproses");
        notifikasi.setPesan("Laporan '" + laporan.getJudul() + "' sedang ditindaklanjuti oleh tim pemerintah di lapangan.");
        notifikasi.setTipe("INFO");
        notifikasiRepository.save(notifikasi);

        return updated;
    }

    public BuktiPerbaikan uploadEvidence(Long idLaporan, String emailPemerintah, BuktiPerbaikan buktiRequest) {
            User pemerintah = userRepository.findByEmail(emailPemerintah)
                            .orElseThrow(() -> new RuntimeException("Akun pemerintah tidak ditemukan"));

            Laporan laporan = laporanRepository.findById(idLaporan)
                            .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));

            buktiRequest.setIdLaporan(idLaporan);
            buktiRequest.setIdPemerintah(pemerintah.getId());
            buktiRequest.setTanggalUpload(LocalDateTime.now());

            BuktiPerbaikan savedBukti = buktiPerbaikanRepository.save(buktiRequest);

            // Otomatis ubah status laporan menjadi Selesai (Asumsi ID Status 5 = Selesai)
            laporan.setIdStatus(5L);
            laporan.setTanggalUpdate(LocalDateTime.now());
            laporanRepository.save(laporan);

            // Catat riwayat status selesai
            RiwayatStatus riwayat = new RiwayatStatus();
            riwayat.setIdLaporan(idLaporan);
            riwayat.setIdStatus(5L);
            riwayat.setIdPengubah(pemerintah.getId());
            riwayat.setKeterangan("Perbaikan selesai dilakukan dan bukti telah diunggah.");
            riwayatStatusRepository.save(riwayat);

            // Kirim notifikasi ke user
            Notifikasi notifikasi = new Notifikasi();
            notifikasi.setIdUser(laporan.getIdUser());
            notifikasi.setIdLaporan(idLaporan);
            notifikasi.setJudul("Perbaikan Selesai");
            notifikasi.setPesan("Laporan '" + laporan.getJudul()
                            + "' telah selesai diperbaiki. Anda dapat melihat dokumentasi bukti perbaikan.");
            notifikasi.setTipe("SUCCESS");
            notifikasiRepository.save(notifikasi);

            return savedBukti;
    }
    
    public Laporan completeReport(Long idLaporan, String emailPemerintah) {
        Laporan laporan = laporanRepository.findById(idLaporan)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));

        User pemerintah = userRepository.findByEmail(emailPemerintah)
                .orElseThrow(() -> new RuntimeException("Akun pemerintah tidak ditemukan"));

        // Asumsi ID Status 5 adalah "Selesai Resmi"
        laporan.setIdStatus(5L);
        laporan.setTanggalUpdate(LocalDateTime.now());
        Laporan updated = laporanRepository.save(laporan);

        // Catat riwayat status
        RiwayatStatus riwayat = new RiwayatStatus();
        riwayat.setIdLaporan(idLaporan);
        riwayat.setIdStatus(5L);
        riwayat.setIdPengubah(pemerintah.getId());
        riwayat.setKeterangan("Laporan perbaikan diselesaikan secara resmi oleh pihak pemerintah.");
        riwayatStatusRepository.save(riwayat);

        // Kirim notifikasi ke user
        Notifikasi notifikasi = new Notifikasi();
        notifikasi.setIdUser(laporan.getIdUser());
        notifikasi.setIdLaporan(idLaporan);
        notifikasi.setJudul("Laporan Selesai");
        notifikasi.setPesan("Laporan '" + laporan.getJudul() + "' telah dinyatakan selesai secara resmi.");
        notifikasi.setTipe("SUCCESS");
        notifikasiRepository.save(notifikasi);

        return updated;
}
}