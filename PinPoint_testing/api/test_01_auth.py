import pytest
from conftest import (
    ADMIN_EMAIL,
    ADMIN_PASSWORD,
    GOV_EMAIL,
    GOV_PASSWORD,
    USER_EMAIL,
    USER_PASSWORD,
    USER_PHONE,
)


@pytest.mark.auth
def test_login_admin_valid(api_session, base_url, admin_token):
    """FR-02 & FR-04: Login Admin berhasil dan mengembalikan token JWT serta role ADMIN."""
    resp = api_session.post(
        f"{base_url}/api/auth/login",
        json={"identifier": ADMIN_EMAIL, "password": ADMIN_PASSWORD},
        timeout=10,
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body.get("token")
    assert "ADMIN" in str(body.get("role", "")).upper()


@pytest.mark.auth
def test_login_user_valid(api_session, base_url, user_token):
    """FR-02 & FR-04: Login User berhasil dan mengembalikan token JWT serta role USER."""
    resp = api_session.post(
        f"{base_url}/api/auth/login",
        json={"identifier": USER_EMAIL, "password": USER_PASSWORD},
        timeout=10,
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body.get("token")
    assert "USER" in str(body.get("role", "")).upper()


@pytest.mark.auth
def test_login_government_valid(api_session, base_url, gov_token):
    """FR-02 & FR-04: Login Pemerintah berhasil dan mengembalikan token JWT serta role PEMERINTAH."""
    resp = api_session.post(
        f"{base_url}/api/auth/login",
        json={"identifier": GOV_EMAIL, "password": GOV_PASSWORD},
        timeout=10,
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body.get("token")
    role_str = str(body.get("role", "")).upper()
    assert "PEMERINTAH" in role_str or "DINAS" in role_str


@pytest.mark.auth
def test_login_user_with_phone_zero_and_62_formats(api_session, base_url, user_token):
    """FR-02: Login menggunakan nomor HP format 08... maupun 628... harus sama-sama berhasil."""
    phone_zero = USER_PHONE.strip()
    phone_62 = "62" + phone_zero[1:] if phone_zero.startswith("0") else phone_zero

    for variant in (phone_zero, phone_62, f"+{phone_62}"):
        resp = api_session.post(
            f"{base_url}/api/auth/login",
            json={"identifier": variant, "password": USER_PASSWORD},
            timeout=10,
        )
        assert resp.status_code == 200, f"Login gagal untuk format nomor {variant}: {resp.text}"
        assert resp.json().get("token")


@pytest.mark.auth
def test_login_invalid_password_returns_401(api_session, base_url):
    """Negative FR-02: Login dengan kata sandi salah harus ditolak (401 Unauthorized)."""
    resp = api_session.post(
        f"{base_url}/api/auth/login",
        json={"identifier": USER_EMAIL, "password": "PasswordSalah123!"},
        timeout=10,
    )
    assert resp.status_code == 401


@pytest.mark.auth
def test_login_unknown_account_returns_401(api_session, base_url):
    """Negative FR-02: Login dengan akun tidak terdaftar harus ditolak (401 Unauthorized)."""
    resp = api_session.post(
        f"{base_url}/api/auth/login",
        json={"identifier": "tidak.terdaftar.pytest@gmail.com", "password": "Password123"},
        timeout=10,
    )
    assert resp.status_code == 401


@pytest.mark.auth
def test_register_duplicate_email_rejected(api_session, base_url):
    """Negative FR-01: Registrasi dengan email yang sudah terdaftar harus ditolak (400)."""
    resp = api_session.post(
        f"{base_url}/api/auth/register/request-otp",
        json={
            "nama": "Test Duplikat Email",
            "email": USER_EMAIL,
            "noTelepon": "081999888777",
            "password": "Password123",
        },
        timeout=10,
    )
    assert resp.status_code == 400
    assert "sudah terdaftar" in resp.text.lower() or "error" in resp.text.lower()


@pytest.mark.auth
def test_register_duplicate_phone_format_62_vs_0_rejected(api_session, base_url):
    """Negative FR-01: Registrasi dengan nomor HP sama walau beda format (62 vs 0) harus ditolak (400)."""
    phone_62 = "62" + USER_PHONE[1:] if USER_PHONE.startswith("0") else USER_PHONE
    resp = api_session.post(
        f"{base_url}/api/auth/register/request-otp",
        json={
            "nama": "Test Duplikat Nomor 62",
            "email": "akun.baru.pytest.unik@gmail.com",
            "noTelepon": phone_62,
            "password": "Password123",
        },
        timeout=10,
    )
    assert resp.status_code == 400
    assert "nomor telepon" in resp.text.lower() or "sudah terdaftar" in resp.text.lower()


@pytest.mark.auth
def test_forgot_password_unknown_account_rejected(api_session, base_url):
    """Negative FR-03: Reset kata sandi untuk email/nomor tidak terdaftar harus ditolak (400)."""
    resp = api_session.post(
        f"{base_url}/api/auth/check-account",
        json={"identifier": "089999999999", "type": "PHONE"},
        timeout=10,
    )
    assert resp.status_code == 400
