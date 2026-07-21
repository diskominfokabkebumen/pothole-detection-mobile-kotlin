# ASPAL — Aplikasi Sistem Pendeteksi Jalan Berlubang

**Nama:** Rizky Fajar Triwibowo
**NIM:** L0324032
**Program Studi:** S1 Informatika
**Universitas:** Universitas Sebelas Maret (UNS)

---

## Tentang Aplikasi

ASPAL adalah aplikasi Android yang dirancang untuk membantu mendeteksi lubang jalan secara otomatis menggunakan kamera smartphone dan model kecerdasan buatan (AI). Aplikasi ini memanfaatkan **YOLOv8** yang telah dilatih khusus untuk mengenali lubang jalan, dikombinasikan dengan pelacakan lokasi GPS untuk mencatat titik lokasi dan jarak tempuh selama proses pemantauan berlangsung.

Ide dasarnya sederhana: pengendara cukup menempelkan HP di dashboard motor/mobil, menyalakan mode pemantauan, lalu berkendara seperti biasa. Di latar belakang, aplikasi akan otomatis menganalisis rekaman kamera secara real-time, mencatat lokasi tiap lubang yang ditemukan, dan menyimpannya secara permanen untuk dilihat kembali kapan saja lewat halaman Peta atau Laporan.

## Fitur Utama

- **Deteksi Lubang Real-Time** — Menggunakan model AI YOLOv8 (TensorFlow Lite) yang berjalan langsung di perangkat (on-device), tanpa perlu koneksi internet saat proses deteksi.
- **Pelacakan Jarak Otomatis** — Memanfaatkan GPS (Fused Location Provider) untuk menghitung jarak tempuh selama sesi pemantauan, dan diakumulasikan secara permanen lintas sesi.
- **Mode Landscape Khusus** — Layar deteksi dikunci ke mode horizontal agar nyaman digunakan saat HP diposisikan di dashboard kendaraan.
- **Anti-Duplikat Deteksi** — Mencegah satu lubang yang sama terhitung berkali-kali saat kendaraan melintasinya secara perlahan, menggunakan kombinasi IoU (overlap kotak deteksi) dan jarak titik tengah.
- **Foto & Lokasi Otomatis** — Setiap lubang yang terdeteksi otomatis difoto (dengan kotak penanda) dan dicatat koordinat GPS-nya.
- **Halaman Beranda Interaktif** — Menampilkan statistik nyata (total laporan, total jarak tempuh, rata-rata tingkat keyakinan AI, laporan bulan ini) beserta cuplikan laporan terbaru.
- **Peta Interaktif** — Menampilkan seluruh titik lubang yang tersimpan di atas peta OpenStreetMap (via CARTO basemap), lengkap dengan kartu ringkasan, daftar lubang terbaru yang bisa digeser, dan tombol untuk melompat ke lokasi pengguna saat ini.
- **Halaman Laporan (Riwayat)** — Menampilkan seluruh riwayat lubang yang pernah tersimpan, dengan alamat asli hasil *reverse geocoding*, dan opsi hapus data yang salah deteksi.
- **Export Laporan ke PDF** — Pengguna dapat memilih beberapa lubang di halaman Laporan lalu membuatnya menjadi satu berkas PDF (berisi foto, tingkat keyakinan, alamat, dan waktu deteksi) yang bisa dibuka atau dibagikan langsung ke aplikasi lain (WhatsApp, Google Drive, dsb).
- **Profil Pengguna** — Nama dan email dapat diedit dan tersimpan secara permanen di perangkat, disertai ringkasan statistik pribadi.

## Alur Kerja Sistem

1. Pengguna membuka aplikasi dan menekan tombol **"Laporkan Jalan Rusak"** di halaman Beranda.
2. Aplikasi meminta izin akses **Kamera** dan **Lokasi (GPS)**.
3. Setelah diizinkan, aplikasi membuka layar pemindaian penuh (landscape) — `DetectionActivity`.
4. Kamera merekam kondisi jalan secara kontinu dan mengumpankan tiap frame ke model AI untuk dianalisis.
5. Jika ditemukan objek dengan tingkat keyakinan (*confidence*) di atas ambang batas dan bukan duplikat dari deteksi sebelumnya, lubang tersebut otomatis difoto dan dicatat lokasinya.
6. Secara paralel, GPS terus melacak pergerakan kendaraan dan mengakumulasikan jarak tempuh sesi tersebut.
7. Saat pengguna menekan tombol **"Berhenti"**, sesi pemantauan berakhir, jarak tempuh ditambahkan ke total permanen, dan ringkasan hasil scan ditampilkan — pengguna dapat memilih mana saja temuan yang ingin disimpan.
8. Data yang disimpan dapat dilihat kembali kapan saja lewat halaman **Peta** (posisi di atas peta) maupun **Laporan** (daftar riwayat), dan sebagian atau seluruhnya dapat diekspor menjadi **PDF** untuk dibagikan sebagai bukti/dokumentasi.

## Teknologi yang Digunakan

| Komponen | Teknologi |
|---|---|
| Bahasa Pemrograman | Kotlin |
| Model AI | YOLOv8 (di-training via Google Colab) |
| Runtime Inferensi AI | TensorFlow Lite + TensorFlow Lite Support |
| Modul Kamera | CameraX |
| Layanan Lokasi | Google Play Services — Fused Location Provider |
| Peta | osmdroid (basemap CARTO, gratis tanpa API key) |
| Reverse Geocoding | Geocoder bawaan Android |
| Daftar & Kartu Data | RecyclerView |
| Export Laporan | PdfDocument bawaan Android (native, tanpa library tambahan) |
| Berbagi Berkas | FileProvider |
| Penyimpanan Lokal | JSON file (data lubang) & SharedPreferences (statistik & profil) |
| Build System | Gradle (Kotlin DSL) |
| IDE | Android Studio |

## Struktur Proyek

```
app/src/main/
├── AndroidManifest.xml
├── assets/
│   └── yolov8_pothole.tflite          # Model AI hasil training
├── java/com/example/hallohalloapp/
│   ├── MainActivity.kt                # Navigasi utama (bottom nav)
│   │
│   ├── model/                         # Data class murni
│   │   ├── PotholeRecord.kt
│   │   └── Detection.kt
│   │
│   ├── adapter/                       # RecyclerView.Adapter
│   │   ├── PotholeReportAdapter.kt
│   │   ├── PotholeMiniReportAdapter.kt
│   │   ├── PotholeMapMiniAdapter.kt
│   │   └── PotholeResultAdapter.kt
│   │
│   ├── ai/                            # Logika AI & pemrosesan deteksi
│   │   ├── PotholeDetector.kt
│   │   ├── PotholeTracker.kt
│   │   └── BoxOverlayView.kt
│   │
│   ├── convertpdf/                    # Pembuatan laporan PDF
│   │   └── PdfReportGenerator.kt
│   │
│   ├── detection/                     # Alur scanning & Activity terkait
│   │   ├── DetectionActivity.kt
│   │   ├── ScanResultActivity.kt
│   │   ├── MapDetailActivity.kt
│   │   ├── PotholeDetailDialogHelper.kt
│   │   ├── MapTileConfig.kt
│   │   └── storage/                   # Penyimpanan data permanen
│   │       ├── PotholeStorage.kt
│   │       ├── AppStatsStorage.kt
│   │       └── UserProfileStorage.kt
│   │
│   └── ui/                            # 4 halaman bottom nav
│       ├── HomeFragment.kt
│       ├── MapFragment.kt
│       ├── ReportFragment.kt
│       └── ProfileFragment.kt
│
└── res/
    ├── layout/                        # Berkas desain antarmuka (XML)
    ├── drawable/                      # Ikon & background
    └── xml/
        └── file_paths.xml             # Konfigurasi FileProvider (share PDF)
```

## Model AI

Model deteksi lubang jalan dilatih menggunakan arsitektur **YOLOv8** melalui **Google Colab**, kemudian diekspor ke format **TensorFlow Lite (.tflite)** agar dapat berjalan secara efisien langsung di perangkat Android tanpa memerlukan koneksi server. Preprocessing gambar disesuaikan secara manual ke format input NCHW `[1,3,640,640]` sesuai kebutuhan model.

## Dokumentasi & Demo

🔗 Progres Minggu 1
    https://drive.google.com/file/d/1tNSdu2QZ71B-mpSkL5Moj_5Z4yoIxerU/view?usp=sharing

🔗 Progres Minggu 2
    https://drive.google.com/file/d/1vwM7H9qCxhcHkiqXqRIFvxELJfwm1Ulz/view?usp=sharing



<p align="center">Dibuat oleh Rizky Fajar Triwibowo</p>
