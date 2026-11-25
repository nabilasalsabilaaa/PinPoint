package com.kelompok8.service;

import com.kelompok8.model.*;
import com.kelompok8.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GovernmentReportServiceTest {

    @Mock
    private LaporanRepository laporanRepository;

    @Mock
    private BuktiPerbaikanRepository buktiPerbaikanRepository;

    @Mock
    private FotoBuktiPerbaikanRepository fotoBuktiPerbaikanRepository;

    @Mock
    private RiwayatStatusRepository riwayatStatusRepository;

    @Mock
    private NotifikasiRepository notifikasiRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminReportService adminReportService;

    @InjectMocks
    private GovernmentReportService governmentReportService;

    @Test
    void updateStatusProcess_throwsWhenReportAlreadyCompleted() {
        Laporan selesai = new Laporan();
        selesai.setId(10L);
        selesai.setIdStatus(5L); // Selesai

        when(laporanRepository.findById(10L)).thenReturn(Optional.of(selesai));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                governmentReportService.updateStatusProcess(10L, "zahra@gmail.com", "Catatan", List.of())
        );

        assertTrue(ex.getMessage().toLowerCase().contains("selesai"));
        verify(laporanRepository, never()).save(any());
    }

    @Test
    void completeReportWithEvidence_throwsWhenReportAlreadyCompleted() {
        Laporan selesai = new Laporan();
        selesai.setId(11L);
        selesai.setIdStatus(5L); // Selesai

        when(laporanRepository.findById(11L)).thenReturn(Optional.of(selesai));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                governmentReportService.completeReportWithEvidence(11L, "zahra@gmail.com", "Edit ulang", List.of(), true)
        );

        assertTrue(ex.getMessage().toLowerCase().contains("selesai"));
        verify(laporanRepository, never()).save(any());
    }

    @Test
    void completeReportWithEvidence_updatesStatusTo5AndSendsNotification() {
        Laporan verified = new Laporan();
        verified.setId(12L);
        verified.setIdUser(15L);
        verified.setIdStatus(2L); // Terverifikasi
        verified.setJudul("Lampu Jalan Mati");

        User govUser = new User();
        govUser.setId(16L);
        govUser.setEmail("zahra@gmail.com");
        govUser.setIdRole(3L);

        when(laporanRepository.findById(12L)).thenReturn(Optional.of(verified));
        when(userRepository.findByEmail("zahra@gmail.com")).thenReturn(Optional.of(govUser));
        when(buktiPerbaikanRepository.findFirstByIdLaporanOrderByIdDesc(12L)).thenReturn(Optional.empty());
        when(buktiPerbaikanRepository.save(any(BuktiPerbaikan.class))).thenAnswer(inv -> {
            BuktiPerbaikan b = inv.getArgument(0);
            b.setId(99L);
            return b;
        });
        when(laporanRepository.save(any(Laporan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(adminReportService.enrichSingleReport(any(Laporan.class))).thenAnswer(inv -> inv.getArgument(0));

        Laporan result = governmentReportService.completeReportWithEvidence(
                12L, "zahra@gmail.com", "Lampu jalan sudah diganti baru", List.of(), true
        );

        assertEquals(5L, result.getIdStatus());
        verify(buktiPerbaikanRepository).save(any(BuktiPerbaikan.class));
        verify(riwayatStatusRepository).save(any(RiwayatStatus.class));
        verify(notifikasiRepository).save(any(Notifikasi.class));
    }
}
