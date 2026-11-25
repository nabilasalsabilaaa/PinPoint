import time
import pytest
from conftest import bearer


@pytest.mark.e2e
def test_full_report_lifecycle_user_admin_government(
    api_session, base_url, user_token, admin_token, gov_token
):
    """
    Pengujian End-to-End (FR-05 s.d. FR-24):
    1. User membuat laporan baru
    2. User melihat laporan di riwayat & memberi dukungan (upvote)
    3. Admin melihat statistik & memverifikasi laporan
    4. Laporan muncul di peta publik
    5. Pemerintah melihat laporan terverifikasi, menandai Sedang Diproses, lalu Selesai + bukti
    6. Laporan yang sudah Selesai terkunci (tidak bisa diedit ulang oleh Pemerintah)
    7. User menerima notifikasi & riwayat status tersimpan
    """
    unique_title = f"[PYTEST] Jalan Berlubang Uji {int(time.time())}"

    # 1. User membuat laporan baru (FR-05 s.d. FR-10)
    create_resp = api_session.post(
        f"{base_url}/api/user/reports",
        headers=bearer(user_token),
        json={
            "idKategori": 1,
            "judul": unique_title,
            "deskripsi": "Pengujian otomatis alur lengkap laporan menggunakan pytest.",
            "alamat": "Jl. Perintis Kemerdekaan, Makassar",
            "latitude": -5.1342,
            "longitude": 119.4889,
            "tingkatKeparahan": "Tinggi",
            "photoUrls": [],
        },
        timeout=15,
    )
    assert create_resp.status_code == 200, f"Gagal membuat laporan: {create_resp.text}"
    report = create_resp.json()
    report_id = report.get("id")
    assert report_id is not None

    # 2. User melihat riwayat laporan sendiri (FR-15)
    my_reports_resp = api_session.get(
        f"{base_url}/api/user/reports", headers=bearer(user_token), timeout=10
    )
    assert my_reports_resp.status_code == 200
    assert any(r.get("id") == report_id for r in my_reports_resp.json())

    # 3. User memberi dukungan (upvote)
    support_resp = api_session.post(
        f"{base_url}/api/user/reports/{report_id}/support",
        headers=bearer(user_token),
        timeout=10,
    )
    assert support_resp.status_code == 200

    # 4. Admin melihat statistik dashboard (FR-19) & memverifikasi laporan (FR-20, FR-21)
    stats_resp = api_session.get(
        f"{base_url}/api/admin/dashboard/stats", headers=bearer(admin_token), timeout=10
    )
    assert stats_resp.status_code == 200
    assert "totalReports" in stats_resp.json()

    verify_resp = api_session.post(
        f"{base_url}/api/admin/reports/{report_id}/verify",
        headers=bearer(admin_token),
        timeout=10,
    )
    assert verify_resp.status_code == 200, f"Gagal verifikasi laporan: {verify_resp.text}"

    # 5. Laporan terverifikasi muncul di peta (FR-11)
    map_resp = api_session.get(f"{base_url}/api/user/reports/map", timeout=10)
    assert map_resp.status_code == 200
    assert any(r.get("id") == report_id for r in map_resp.json())

    # 6. Pemerintah melihat daftar laporan terverifikasi (FR-23)
    gov_list_resp = api_session.get(
        f"{base_url}/api/government/reports", headers=bearer(gov_token), timeout=10
    )
    assert gov_list_resp.status_code == 200
    assert any(r.get("id") == report_id for r in gov_list_resp.json())

    # 7. Pemerintah menandai Sedang Diproses
    process_resp = api_session.post(
        f"{base_url}/api/government/reports/{report_id}/process",
        headers=bearer(gov_token),
        json={"keterangan": "Tim teknis sedang menuju lokasi (pytest)", "photoUrls": []},
        timeout=10,
    )
    assert process_resp.status_code == 200, f"Gagal ubah ke Diproses: {process_resp.text}"

    # 8. Pemerintah menandai Selesai beserta bukti (FR-18, FR-24)
    complete_resp = api_session.post(
        f"{base_url}/api/government/reports/{report_id}/complete",
        headers=bearer(gov_token),
        json={
            "keterangan": "Perbaikan selesai dilakukan dengan baik (pytest).",
            "photoUrls": [],
            "sendNotification": True,
        },
        timeout=15,
    )
    assert complete_resp.status_code == 200, f"Gagal menyelesaikan laporan: {complete_resp.text}"

    # 9. Proteksi Laporan Selesai: Pemerintah tidak boleh mengedit ulang laporan yang sudah SELESAI
    retry_complete_resp = api_session.post(
        f"{base_url}/api/government/reports/{report_id}/complete",
        headers=bearer(gov_token),
        json={"keterangan": "Coba edit ulang laporan selesai", "photoUrls": []},
        timeout=10,
    )
    assert retry_complete_resp.status_code == 400, (
        "Seharusnya laporan yang sudah SELESAI ditolak (400) saat coba diubah kembali!"
    )

    # 10. Cek Riwayat Status (FR-17) & Notifikasi User (FR-16)
    history_resp = api_session.get(
        f"{base_url}/api/user/reports/{report_id}/status-history", timeout=10
    )
    assert history_resp.status_code == 200
    assert len(history_resp.json()) >= 2

    notif_resp = api_session.get(
        f"{base_url}/api/user/notifications", headers=bearer(user_token), timeout=10
    )
    assert notif_resp.status_code == 200
    assert any(n.get("idLaporan") == report_id for n in notif_resp.json())


@pytest.mark.e2e
def test_admin_reject_report_with_reason(api_session, base_url, user_token, admin_token):
    """FR-20 & FR-22: Admin dapat menolak laporan masuk disertai alasan/catatan penolakan."""
    create_resp = api_session.post(
        f"{base_url}/api/user/reports",
        headers=bearer(user_token),
        json={
            "idKategori": 1,
            "judul": f"[PYTEST] Laporan Ditolak {int(time.time())}",
            "deskripsi": "Pengujian skenario penolakan laporan oleh Admin.",
            "alamat": "Jl. AP Pettarani, Makassar",
            "latitude": -5.155,
            "longitude": 119.436,
            "tingkatKeparahan": "Rendah",
            "photoUrls": [],
        },
        timeout=15,
    )
    assert create_resp.status_code == 200
    report_id = create_resp.json().get("id")

    reject_resp = api_session.post(
        f"{base_url}/api/admin/reports/{report_id}/reject",
        headers=bearer(admin_token),
        json={
            "reasonCategory": ["Foto Tidak Jelas / Buram"],
            "rejectionNote": "Pengujian catatan penolakan otomatis dari pytest.",
        },
        timeout=10,
    )
    assert reject_resp.status_code == 200, f"Gagal menolak laporan: {reject_resp.text}"
