# PinPoint Testing Hub

Folder terpusat untuk seluruh pengujian aplikasi **PinPoint** (API, Unit, UI Espresso, Functional Appium, Performance, dan Compatibility).

## Struktur Folder
- `api/` — **API Testing (`pytest` + `requests`)**: Autentikasi, Normalisasi No HP, CRUD Kategori, Alur E2E Laporan, Security/RBAC, & Latency.
- `functional_appium/` — *(Tahap selanjutnya)* Functional & E2E Testing menggunakan `pytest` + `Appium-Python-Client`.
- `performance/` — *(Tahap selanjutnya)* Dokumentasi hasil **Android Profiler** (CPU, RAM, Network).
- `compatibility/` — *(Tahap selanjutnya)* Skrip CLI **Firebase Test Lab** (`gcloud`).
- `reports/` — Seluruh hasil laporan HTML otomatis terkumpul di sini.
- `run_tests.ps1` — Script terpusat untuk menjalankan semua suite testing.

---

## Cara Menjalankan API Testing (Tahap 1)

### 1. Prasyarat
- Pastikan **Python 3.10+** sudah terinstal dan terdaftar di PATH Windows (centang *"Add Python to PATH"* saat instalasi).
- Pastikan **Backend Spring Boot (`PinPoint_backend`)** sudah berjalan di `http://localhost:8081` (restart terlebih dahulu setelah perubahan `SecurityConfig.java`).

### 2. Install Dependency Python
Buka PowerShell di folder `PinPoint_testing/`:
```powershell
pip install -r requirements.txt
```

### 3. Konfigurasi Akun Testing (`.env`)
Salin `.env.example` menjadi `.env`, lalu isi password akun **Admin**, **User**, dan **Pemerintah** yang terdaftar di database PostgreSQL lokal Anda:
```powershell
Copy-Item .env.example .env
```

### 4. Jalankan API Testing & Hasilkan Laporan HTML
```powershell
.\run_tests.ps1 -Suite api
```
Atau jalankan `pytest` langsung per modul:
```powershell
pytest api/test_01_auth.py -v
pytest api/test_04_security_rbac.py -v
```
Hasil laporan visual akan otomatis tersimpan di `PinPoint_testing/reports/api_test_report.html` — buka file tersebut di browser untuk melihat ringkasan *Passed/Failed* yang siap dilampirkan ke laporan/presentasi.
