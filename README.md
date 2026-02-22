# Persony — Personal Finance Assistant (Offline)

Persony adalah aplikasi Android untuk mengelola keuangan pribadi secara **offline** (tanpa cloud). Aplikasi dibangun menggunakan **Jetpack Compose + Material 3**, menerapkan **MVVM**, dan menyimpan data lokal memakai **Room Database**.

---

## Fitur (Sesuai Implementasi)

### 1) Home (Dashboard)
- Menampilkan sapaan user, **saldo**, ringkasan, tagihan mendatang, dan transaksi terakhir.
- Aksi utama:
  - Tambah transaksi (Masuk/Keluar)
  - Tambah tagihan (Bills)
  - Lihat semua transaksi
  - Hapus transaksi (saldo otomatis disesuaikan)

### 2) Transaksi (CRUD + Auto Revert)
- **Create**: tambah transaksi baru (Masuk/Keluar)
- **Read**: tampil di Home, All Transactions, dan Activity/Analytics
- **Delete**: hapus transaksi dan **saldo otomatis di-revert**
- Semua transaksi baru disimpan dengan `date = "Hari ini"` (dipakai untuk perhitungan budget harian).

> Jika transaksi yang dihapus adalah hasil fitur tabungan (judul “Simpan ke …” / “Tarik dari …”), jumlah tabungan **ikut di-revert** agar konsisten.

### 3) Bills (Tagihan) + Recurrence
- **Tambah / Edit / Hapus** tagihan
- Tanggal via Date Picker
- Recurrence: **Sekali, Mingguan, Bulanan, Tahunan**
- Aksi “Tandai Selesai”:
  - Sekali → tagihan dihapus
  - Berulang → tanggal maju otomatis ke periode berikutnya

### 4) Saving (Tabungan & Target)
- Buat target tabungan berisi:
  - nama target
  - target nominal
  - lokasi
  - ikon (disimpan sebagai `iconName`)
- Aksi:
  - **Deposit (Tabung)**: memindahkan dana dari saldo utama ke target tabungan + mencatat transaksi “Simpan ke …”
  - **Withdraw (Tarik)**: memindahkan dana dari target tabungan ke saldo utama + mencatat transaksi “Tarik dari …”
  - Edit target
  - Delete target

### 5) Activity / Analytics
- Pie chart ringkas: pemasukan vs pengeluaran vs tabungan
- “Rincian Dana” + detail transaksi (opsional)
- Alat produktif:
  - Budgeting harian
  - Export PDF report

### 6) Budget Harian + Notifikasi
- Set limit budget harian
- Pengeluaran hari ini dihitung dari transaksi pengeluaran dengan `date == "Hari ini"`
- Jika melewati limit → aplikasi mengirim notifikasi (channel: `BUDGET_CHANNEL`)
- Android 13+ akan meminta izin `POST_NOTIFICATIONS`

### 7) Export PDF
- Export laporan PDF ke folder **Downloads**:
  - saldo saat ini
  - total pemasukan & pengeluaran
  - tabel ringkas transaksi
  - total tabungan (ringkas)

### 8) Profile
- Ubah nama panggilan
- Reset semua data (transaksi, bills, savings, prefs)

---

## Navigasi
Bottom Navigation:
1. Home
2. Saving
3. Activity
4. Profile

---

## Arsitektur & Data Flow

### MVVM + Room (Offline)
- **View (Compose screens)**: menampilkan data dan mengirim event user
- **ViewModel (`PersonyViewModel`)**: memproses logika bisnis + mengubah data
- **Model (Room)**:
  - `TransactionEntity` (transactions)
  - `BillEntity` (bills)
  - `SavingEntity` (savings)
  - `UserPrefs` (user_prefs)

### Observasi Data
UI mengamati `Flow` dari Room (collectAsState) sehingga ketika data berubah, UI akan otomatis recompose.

---

## Struktur File (Utama)
- `MainActivity.kt`  
  Entry point + `MainContainer()` (navigation) + komponen Home/Transaction/Bills UI + util `formatRupiah()`.
- `ActivityScreen.kt`  
  Analytics + budgeting + export PDF trigger.
- `SavingScreen.kt`  
  Target tabungan + deposit/withdraw + edit.
- `ProfileScreen.kt`  
  Nama user + reset data.
- `PersonyData.kt`  
  Room Entities, DAO, dan Database.
- `PersonyViewModel.kt`  
  Semua business logic (CRUD + perhitungan saldo/tabungan).
- `ui/theme/*`  
  Warna, typography, dan theme (Light/Dark).

---

## Cara Menjalankan
1. Buka project di Android Studio
2. Sync Gradle (pastikan dependency Room sudah ada)
3. Run ke emulator/device

### Permission
- Android 13+: `POST_NOTIFICATIONS`

---

## Troubleshooting Cepat
- **Notifikasi budget tidak muncul**
  - Pastikan limit budget harian > 0
  - Pastikan ada transaksi pengeluaran dengan `date == "Hari ini"`
  - Pastikan izin notifikasi aktif (Android 13+)
- **PDF tidak ada di Downloads**
  - Lihat Toast error (scoped storage di beberapa versi Android bisa berpengaruh)
