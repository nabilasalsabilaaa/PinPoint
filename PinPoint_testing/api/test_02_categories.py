import time
import pytest
from conftest import bearer


@pytest.mark.categories
def test_get_categories_public(api_session, base_url):
    """FR-07: Daftar kategori kerusakan dapat diakses publik dan tidak kosong."""
    resp = api_session.get(f"{base_url}/api/categories", timeout=10)
    assert resp.status_code == 200
    data = resp.json()
    assert isinstance(data, list)
    assert len(data) > 0, "Daftar kategori di database kosong!"


@pytest.mark.categories
def test_admin_crud_category_lifecycle(api_session, base_url, admin_token):
    """UC-09 & NFR-17: Admin dapat Create, Read, Update, dan Delete kategori kerusakan."""
    headers = bearer(admin_token)
    unique_name = f"Kategori Pytest {int(time.time())}"
    created_id = None

    try:
        # 1. CREATE
        create_resp = api_session.post(
            f"{base_url}/api/categories",
            headers=headers,
            json={
                "namaKategori": unique_name,
                "deskripsi": "Kategori sementara untuk pengujian API pytest",
                "ikon": "category",
                "aktif": True,
            },
            timeout=10,
        )
        assert create_resp.status_code in (200, 201), f"Gagal membuat kategori: {create_resp.text}"
        created = create_resp.json()
        created_id = created.get("id")
        assert created_id is not None

        # 2. READ
        list_resp = api_session.get(f"{base_url}/api/categories", timeout=10)
        assert list_resp.status_code == 200
        assert any(c.get("id") == created_id for c in list_resp.json())

        # 3. UPDATE
        updated_name = f"{unique_name} (Edited)"
        update_resp = api_session.put(
            f"{base_url}/api/categories/{created_id}",
            headers=headers,
            json={
                "namaKategori": updated_name,
                "deskripsi": "Deskripsi diperbarui oleh pytest",
                "ikon": "category",
                "aktif": True,
            },
            timeout=10,
        )
        assert update_resp.status_code == 200, f"Gagal update kategori: {update_resp.text}"
        assert update_resp.json().get("namaKategori") == updated_name

    finally:
        # 4. DELETE (Cleanup agar database tetap bersih)
        if created_id is not None:
            del_resp = api_session.delete(
                f"{base_url}/api/categories/{created_id}",
                headers=headers,
                timeout=10,
            )
            assert del_resp.status_code in (200, 204), f"Gagal menghapus kategori cleanup: {del_resp.text}"
