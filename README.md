#  ASPAL — Aplikasi Sistem Pendeteksi Jalan Berlubang

**Nama:** Rizky Fajar Triwibowo
**NIM:** L0324032
**Program Studi:** S1 Informatika
**Universitas:** Universitas Sebelas Maret (UNS)

---

## Tentang Aplikasi

ASPAL adalah aplikasi Android yang dirancang untuk membantu mendeteksi lubang jalan secara otomatis menggunakan kamera smartphone dan model kecerdasan buatan (AI). Aplikasi ini memanfaatkan **YOLOv8** yang telah dilatih khusus untuk mengenali lubang jalan, dikombinasikan dengan pelacakan lokasi GPS untuk mencatat jarak tempuh selama proses pemantauan berlangsung.

Ide dasarnya sederhana: pengendara cukup menempelkan HP di dashboard motor/mobil, menyalakan mode pemantauan, lalu berkendara seperti biasa. Di latar belakang, aplikasi akan otomatis menganalisis rekaman kamera secara real-time dan menghitung jumlah lubang jalan yang terdeteksi sepanjang perjalanan.

##  Fitur Utama

- **Deteksi Lubang Real-Time** — Menggunakan model AI YOLOv8 (TensorFlow Lite) yang berjalan langsung di perangkat (on-device), tanpa perlu koneksi internet.
- **Pelacakan Jarak Otomatis** — Memanfaatkan GPS (Fused Location Provider) untuk menghitung jarak tempuh selama sesi pemantauan berlangsung.
- **Mode Landscape Khusus** — Layar deteksi dikunci ke mode horizontal agar nyaman digunakan saat HP diposisikan di dashboard kendaraan.
- **Sistem Cooldown Cerdas** — Mencegah satu lubang yang sama terhitung berkali-kali saat kendaraan melintasinya secara perlahan.
- **Ringkasan Laporan** — Menampilkan total jarak tempuh dan jumlah lubang yang ditemukan setelah sesi pemantauan selesai.

##  Alur Kerja Sistem

1. Pengguna membuka aplikasi dan menekan tombol **"Laporkan Jalan Rusak"** di halaman Beranda.
2. Aplikasi meminta izin akses **Kamera** dan **Lokasi (GPS)**.
3. Setelah diizinkan, aplikasi membuka layar pemindaian penuh (landscape) — `DetectionActivity`.
4. Kamera merekam kondisi jalan secara kontinu, memotong video menjadi frame gambar setiap interval tertentu, lalu mengumpankannya ke model AI.
5. Model AI menganalisis setiap frame; jika ditemukan objek dengan tingkat keyakinan (*confidence*) di atas ambang batas yang ditentukan, counter lubang jalan bertambah.
6. Secara paralel, GPS terus melacak pergerakan kendaraan dan mengakumulasikan jarak tempuh.
7. Saat pengguna menekan tombol **"Berhenti"**, sesi pemantauan berakhir dan ringkasan hasil ditampilkan.

##  Teknologi yang Digunakan

| Komponen | Teknologi |
|---|---|
| Bahasa Pemrograman | Kotlin |
| Model AI | YOLOv8 (di-training via Google Colab) |
| Runtime Inferensi AI | TensorFlow Lite |
| Modul Kamera | CameraX |
| Layanan Lokasi | Google Play Services — Fused Location Provider |
| Build System | Gradle (Kotlin DSL) |
| IDE | Android Studio |

## 📂 Struktur Proyek

```
app/src/main/
├── AndroidManifest.xml
├── assets/
│   └── yolov8_pothole.tflite      # Model AI hasil training
├── java/com/example/hallohalloapp/
│   ├── MainActivity.kt            # Navigasi utama (Beranda, Profil, Laporan)
│   ├── HomeFragment.kt            # Halaman beranda & tombol mulai deteksi
│   ├── DetectionActivity.kt       # Halaman inti: kamera + GPS + AI
│   ├── PotholeDetector.kt         # Kelas pemroses inferensi model AI
│   ├── ReportFragment.kt          # Tampilan ringkasan laporan
│   ├── ProfileFragment.kt
│   └── MapFragment.kt
└── res/
    └── layout/                    # Berkas desain antarmuka (XML)
```

## 🧪 Model AI

Model deteksi lubang jalan dilatih menggunakan arsitektur **YOLOv8** melalui **Google Colab**, kemudian diekspor ke format **TensorFlow Lite (.tflite)** agar dapat berjalan secara efisien langsung di perangkat Android tanpa memerlukan koneksi server.


## 📸 Dokumentasi & Demo

🔗 Progres Minggu 1
    https://drive.google.com/file/d/1tNSdu2QZ71B-mpSkL5Moj_5Z4yoIxerU/view?usp=sharing



---

<p align="center">Dibuat dengan 💻 oleh Rizky Fajar Triwibowo — S1 Informatika, Universitas Sebelas Maret</p>
