import os
import pytest
import requests
from dotenv import load_dotenv

# Muat konfigurasi dari PinPoint_testing/.env jika tersedia
load_dotenv(os.path.join(os.path.dirname(__file__), ".env"))

BASE_URL = os.getenv("BASE_URL", "http://localhost:8081").rstrip("/")

ADMIN_EMAIL = os.getenv("ADMIN_EMAIL", "admin123@gmail.com")
ADMIN_PASSWORD = os.getenv("ADMIN_PASSWORD", "")

USER_EMAIL = os.getenv("USER_EMAIL", "salsa@gmail.com")
USER_PHONE = os.getenv("USER_PHONE", "085214736999")
USER_PASSWORD = os.getenv("USER_PASSWORD", "")

GOV_EMAIL = os.getenv("GOV_EMAIL", "zahra@gmail.com")
GOV_PASSWORD = os.getenv("GOV_PASSWORD", "")


@pytest.fixture(scope="session")
def base_url() -> str:
    return BASE_URL


@pytest.fixture(scope="session")
def api_session():
    session = requests.Session()
    session.headers.update({"Content-Type": "application/json"})
    yield session
    session.close()


def _login_and_get_token(session: requests.Session, identifier: str, password: str, role_label: str) -> str:
    if not password or password.startswith("ISI_PASSWORD"):
        pytest.fail(
            f"Password {role_label} belum diisi! Salin .env.example menjadi .env "
            f"di folder PinPoint_testing/ lalu isi kredensial akun database lokal Anda."
        )
    try:
        resp = session.post(
            f"{BASE_URL}/api/auth/login",
            json={"identifier": identifier, "password": password},
            timeout=10,
        )
    except requests.exceptions.ConnectionError:
        pytest.fail(
            f"Tidak dapat terhubung ke server backend di {BASE_URL}. "
            f"Pastikan Spring Boot PinPoint_backend sudah berjalan!"
        )

    assert resp.status_code == 200, (
        f"Login {role_label} ({identifier}) gagal dengan status {resp.status_code}: {resp.text}"
    )
    data = resp.json()
    token = data.get("token")
    assert token, f"Token JWT tidak ditemukan pada response login {role_label}: {data}"
    return token


@pytest.fixture(scope="session")
def admin_token(api_session) -> str:
    return _login_and_get_token(api_session, ADMIN_EMAIL, ADMIN_PASSWORD, "ADMIN")


@pytest.fixture(scope="session")
def user_token(api_session) -> str:
    return _login_and_get_token(api_session, USER_EMAIL, USER_PASSWORD, "USER")


@pytest.fixture(scope="session")
def gov_token(api_session) -> str:
    return _login_and_get_token(api_session, GOV_EMAIL, GOV_PASSWORD, "PEMERINTAH")


def bearer(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}
