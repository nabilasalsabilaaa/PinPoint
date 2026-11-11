package com.kelompok8.service;

import com.kelompok8.dto.DisposisiRequest;
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

    public Laporan disposeReport(Long idLaporan, DisposisiRequest request, String adminEmail) {
        if (request == null || request.getIdDinas() == null) {
            throw new RuntimeException("ID dinas wajib diisi");
        }

        Laporan laporan = laporanRepository.findById(idLaporan)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));
        if (laporan.getIdStatus() == null || !laporan.getIdStatus().equals(2L)) {
            throw new RuntimeException("Laporan harus berstatus Terverifikasi sebelum didisposisi");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));
        User dinas = userRepository.findById(request.getIdDinas())
                .orElseThrow(() -> new RuntimeException("Dinas teknis tidak ditemukan"));

        laporan.setIdStatus(4L);
        laporan.setTanggalUpdate(LocalDateTime.now());
        Laporan updated = laporanRepository.save(laporan);

        String catatan = request.getCatatan() == null || request.getCatatan().isBlank()
                ? "Laporan didisposisi ke dinas teknis terkait."
                : request.getCatatan().trim();

        RiwayatStatus riwayat = new RiwayatStatus();
        riwayat.setIdLaporan(idLaporan);
        riwayat.setIdStatus(4L);
        riwayat.setIdPengubah(dinas.getId());
        riwayat.setKeterangan("Didisposisi oleh admin " + admin.getNama() + " ke dinas "
                + dinas.getNama() + ". Catatan: " + catatan);
        riwayatStatusRepository.save(riwayat);

        Notifikasi notifikasi = new Notifikasi();
        notifikasi.setIdUser(dinas.getId());
        notifikasi.setIdLaporan(idLaporan);
        notifikasi.setJudul("Tugas Disposisi Baru");
        notifikasi.setPesan("Laporan '" + laporan.getJudul() + "' didisposisi kepada dinas Anda. Catatan: " + catatan);
        notifikasi.setTipe("INFO");
        notifikasiRepository.save(notifikasi);

        return updated;
    }

    public List<Laporan> getAssignedReports(String emailPemerintah) {
        User dinas = userRepository.findByEmail(emailPemerintah)
                .orElseThrow(() -> new RuntimeException("Akun dinas tidak ditemukan"));

        List<RiwayatStatus> penugasan = riwayatStatusRepository
                .findByIdPengubahAndIdStatus(dinas.getId(), 4L);

        return penugasan.stream()
                .map(RiwayatStatus::getIdLaporan)
                .distinct()
                .map(laporanRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(laporan -> laporan.getIdStatus() != null && laporan.getIdStatus().equals(4L))
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

            if (laporan.getIdStatus() == null || !laporan.getIdStatus().equals(4L)) {
                    throw new RuntimeException("Laporan harus berstatus Diproses sebelum bukti perbaikan diunggah");
            }

            boolean ditugaskan = riwayatStatusRepository.findByIdLaporan(idLaporan).stream()
                            .anyMatch(riwayat -> riwayat.getIdStatus().equals(4L)
                                            && riwayat.getIdPengubah().equals(pemerintah.getId()));
            if (!ditugaskan) {
                    throw new RuntimeException("Laporan tidak ditugaskan kepada akun dinas ini");
            }

            buktiRequest.setIdLaporan(idLaporan);
            buktiRequest.setIdPemerintah(pemerintah.getId());
            buktiRequest.setTanggalUpload(LocalDateTime.now());

            BuktiPerbaikan savedBukti = buktiPerbaikanRepository.save(buktiRequest);

            laporan.setTanggalUpdate(LocalDateTime.now());
            laporanRepository.save(laporan);

            RiwayatStatus riwayat = new RiwayatStatus();
            riwayat.setIdLaporan(idLaporan);
            riwayat.setIdStatus(4L);
            riwayat.setIdPengubah(pemerintah.getId());
            riwayat.setKeterangan("Bukti perbaikan fisik telah diunggah oleh dinas teknis.");
            riwayatStatusRepository.save(riwayat);

            Notifikasi notifikasi = new Notifikasi();
            notifikasi.setIdUser(laporan.getIdUser());
            notifikasi.setIdLaporan(idLaporan);
            notifikasi.setJudul("Bukti Perbaikan Diupload");
            notifikasi.setPesan("Dinas telah mengunggah bukti perbaikan untuk laporan '" + laporan.getJudul() + "'.");
            notifikasi.setTipe("INFO");
            notifikasiRepository.save(notifikasi);

            return savedBukti;
    }
    
    public Laporan completeReport(Long idLaporan, String emailPemerintah) {
        Laporan laporan = laporanRepository.findById(idLaporan)
                .orElseThrow(() -> new RuntimeException("Laporan tidak ditemukan"));

        User pemerintah = userRepository.findByEmail(emailPemerintah)
                .orElseThrow(() -> new RuntimeException("Akun pemerintah tidak ditemukan"));

        if (laporan.getIdStatus() == null || !laporan.getIdStatus().equals(4L)) {
            throw new RuntimeException("Laporan harus berstatus Diproses sebelum diselesaikan");
        }

        boolean ditugaskan = riwayatStatusRepository.findByIdLaporan(idLaporan).stream()
                .anyMatch(riwayat -> riwayat.getIdStatus().equals(4L)
                        && riwayat.getIdPengubah().equals(pemerintah.getId()));
        if (!ditugaskan) {
            throw new RuntimeException("Laporan tidak ditugaskan kepada akun dinas ini");
        }

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