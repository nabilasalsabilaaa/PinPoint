import pytest
from conftest import bearer


@pytest.mark.security
def test_admin_users_without_token_blocked(api_session, base_url):
    """NFR-06: Mengakses endpoint daftar pengguna Admin tanpa token JWT harus ditolak (401/403)."""
    resp = api_session.get(f"{base_url}/api/admin/users", timeout=10)
    assert resp.status_code in (401, 403), f"Expected 401/403, got {resp.status_code}"


@pytest.mark.security
def test_admin_stats_without_token_blocked(api_session, base_url):
    """NFR-06: Mengakses statistik dashboard Admin tanpa token JWT harus ditolak (401/403)."""
    resp = api_session.get(f"{base_url}/api/admin/dashboard/stats", timeout=10)
    assert resp.status_code in (401, 403)


@pytest.mark.security
def test_admin_users_with_user_token_forbidden(api_session, base_url, user_token):
    """NFR-07: Akun USER biasa dilarang mengakses endpoint kelola akun ADMIN (403 Forbidden)."""
    resp = api_session.get(
        f"{base_url}/api/admin/users", headers=bearer(user_token), timeout=10
    )
    assert resp.status_code == 403, f"Expected 403, got {resp.status_code}"


@pytest.mark.security
def test_create_category_with_user_token_forbidden(api_session, base_url, user_token):
    """NFR-07: Akun USER biasa dilarang membuat kategori baru (403 Forbidden)."""
    resp = api_session.post(
        f"{base_url}/api/categories",
        headers=bearer(user_token),
        json={"namaKategori": "Ilegal Pytest", "deskripsi": "-", "aktif": True},
        timeout=10,
    )
    assert resp.status_code == 403


@pytest.mark.security
def test_government_reports_without_token_blocked(api_session, base_url):
    """NFR-06: Mengakses endpoint Pemerintah tanpa token JWT harus ditolak (401/403)."""
    resp = api_session.get(f"{base_url}/api/government/reports", timeout=10)
    assert resp.status_code in (401, 403)


@pytest.mark.security
def test_government_reports_with_user_token_forbidden(api_session, base_url, user_token):
    """NFR-07: Akun USER biasa dilarang mengakses endpoint khusus PEMERINTAH (403 Forbidden)."""
    resp = api_session.get(
        f"{base_url}/api/government/reports", headers=bearer(user_token), timeout=10
    )
    assert resp.status_code == 403


@pytest.mark.security
def test_user_create_report_without_token_blocked(api_session, base_url):
    """NFR-06: Membuat laporan tanpa token JWT harus ditolak (401/403)."""
    resp = api_session.post(
        f"{base_url}/api/user/reports",
        json={"judul": "Tanpa Token", "deskripsi": "Harus ditolak"},
        timeout=10,
    )
    assert resp.status_code in (401, 403)
