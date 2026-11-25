import time
import pytest


@pytest.mark.performance
def test_map_reports_response_under_3_seconds(api_session, base_url):
    """NFR-01: Endpoint peta laporan (/api/user/reports/map) harus merespons kurang dari 3 detik."""
    start = time.perf_counter()
    resp = api_session.get(f"{base_url}/api/user/reports/map", timeout=10)
    elapsed = time.perf_counter() - start

    assert resp.status_code == 200
    assert elapsed < 3.0, f"Waktu muat peta terlalu lambat: {elapsed:.2f} detik (batas < 3.0 detik)"


@pytest.mark.performance
def test_categories_response_under_2_seconds(api_session, base_url):
    """NFR-01: Endpoint daftar kategori harus merespons kurang dari 2 detik."""
    start = time.perf_counter()
    resp = api_session.get(f"{base_url}/api/categories", timeout=10)
    elapsed = time.perf_counter() - start

    assert resp.status_code == 200
    assert elapsed < 2.0, f"Waktu respons kategori terlalu lambat: {elapsed:.2f} detik"
