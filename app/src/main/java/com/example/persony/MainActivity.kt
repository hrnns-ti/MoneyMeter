package com.example.persony

import android.Manifest
import android.R
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.persony.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        enableEdgeToEdge()
        setContent {
            PersonyTheme {
                val viewModel: PersonyViewModel = viewModel()
                MainContainer(viewModel)
            }
        }
    }

    // --- FITUR OFFLINE: NOTIFIKASI & PDF ---

    fun sendBudgetNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(this, "BUDGET_CHANNEL")
            .setSmallIcon(R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) { }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "BUDGET_CHANNEL", "Budget Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifikasi limit budget" }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun generateFinancialReport(transactions: List<TransactionEntity>, balance: Long, totalIn: Long, totalOut: Long) {
        val pdfDocument = PdfDocument()
        // Ukuran A4: 595 x 842 unit
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Objek Paint untuk berbagai gaya teks
        val titlePaint = Paint().apply { typeface = Typeface.DEFAULT_BOLD; textSize = 22f; color = android.graphics.Color.BLACK }
        val subtitlePaint = Paint().apply { typeface = Typeface.DEFAULT; textSize = 10f; color = android.graphics.Color.GRAY }
        val headerPaint = Paint().apply { typeface = Typeface.DEFAULT_BOLD; textSize = 12f; color = android.graphics.Color.BLACK }
        val textPaint = Paint().apply { typeface = Typeface.DEFAULT; textSize = 10f; color = android.graphics.Color.BLACK }
        val linePaint = Paint().apply { strokeWidth = 1f; color = android.graphics.Color.LTGRAY }
        val primaryColorPaint = Paint().apply { color = android.graphics.Color.parseColor("#6A5AE0") } // MainPurple

        var y = 60f
        val startX = 40f
        val endX = 555f

        // 1. HEADER (Judul & Tanggal Cetak)
        canvas.drawText("LAPORAN KEUANGAN PERSONY", startX, y, titlePaint)
        y += 15f
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        canvas.drawText("Dicetak pada: ${sdf.format(Date())}", startX, y, subtitlePaint)
        y += 40f

        // 2. SUMMARY BOX (Kotak Ringkasan)
        // Gambar background kotak ungu muda tipis
        val rectPaint = Paint().apply { color = android.graphics.Color.parseColor("#F5F4FF") }
        canvas.drawRoundRect(startX, y, endX, y + 80f, 15f, 15f, rectPaint)

        var summaryY = y + 25f
        // Total Saldo
        canvas.drawText("Total Saldo Saat Ini", startX + 20f, summaryY, textPaint)
        val balanceText = "Rp ${formatRupiah(balance)}"
        canvas.drawText(balanceText, endX - 20f - headerPaint.measureText(balanceText), summaryY, headerPaint)

        summaryY += 25f
        // Pemasukan vs Pengeluaran
        canvas.drawText("Total Pemasukan", startX + 20f, summaryY, textPaint)
        val inText = "+ Rp ${formatRupiah(totalIn)}"
        val greenPaint = Paint().apply { typeface = Typeface.DEFAULT_BOLD; textSize = 11f; color = android.graphics.Color.parseColor("#4CAF50") }
        canvas.drawText(inText, endX - 20f - greenPaint.measureText(inText), summaryY, greenPaint)

        summaryY += 20f
        canvas.drawText("Total Pengeluaran", startX + 20f, summaryY, textPaint)
        val outText = "- Rp ${formatRupiah(totalOut)}"
        val redPaint = Paint().apply { typeface = Typeface.DEFAULT_BOLD; textSize = 11f; color = android.graphics.Color.parseColor("#F44336") }
        canvas.drawText(outText, endX - 20f - redPaint.measureText(outText), summaryY, redPaint)

        y += 120f

        // 3. TRANSACTION TABLE HEADER
        canvas.drawText("RIWAYAT TRANSAKSI", startX, y, headerPaint)
        y += 15f
        canvas.drawLine(startX, y, endX, y, primaryColorPaint.apply { strokeWidth = 2f })
        y += 20f

        // Judul Kolom
        canvas.drawText("TANGGAL", startX, y, headerPaint.apply { textSize = 9f })
        canvas.drawText("KETERANGAN", startX + 80f, y, headerPaint)
        canvas.drawText("NOMINAL", endX - headerPaint.measureText("NOMINAL"), y, headerPaint)
        y += 10f
        canvas.drawLine(startX, y, endX, y, linePaint)
        y += 25f

        // 4. LIST TRANSAKSI (Tabel Baris)
        transactions.forEach { tx ->
            if (y > 800) return@forEach // Cegah overflow halaman

            // Tanggal
            canvas.drawText(tx.date, startX, y, textPaint)

            // Keterangan
            val titleDisplay = if (tx.title.length > 30) tx.title.take(27) + "..." else tx.title
            canvas.drawText(titleDisplay, startX + 80f, y, textPaint)

            // Nominal
            val amountDisplay = (if (tx.isExpense) "-" else "+") + " Rp ${tx.amount}"
            val amountColor = if (tx.isExpense) redPaint.apply { textSize = 10f } else greenPaint.apply { textSize = 10f }
            canvas.drawText(amountDisplay, endX - amountColor.measureText(amountDisplay), y, amountColor)

            y += 15f
            canvas.drawLine(startX, y, endX, y, linePaint.apply { alpha = 50 }) // Garis pemisah tipis
            y += 20f
        }

        // 5. FOOTER
        canvas.drawText("Laporan ini dibuat secara otomatis oleh aplikasi Persony.", startX, 810f, subtitlePaint)

        pdfDocument.finishPage(page)

        // Simpan file
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Laporan_Persony_${System.currentTimeMillis()}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this, "PDF berhasil disimpan di folder Download", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal simpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(viewModel: PersonyViewModel) {
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val bills by viewModel.bills.collectAsState(initial = emptyList())
    val savings by viewModel.savings.collectAsState(initial = emptyList())
    val prefs by viewModel.userPrefs.collectAsState(initial = UserPrefs())

    var selectedTab by remember { mutableIntStateOf(0) }
    var isViewingAllTransactions by remember { mutableStateOf(false) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var showBillDialog by remember { mutableStateOf(false) }
    var selectedBillForMenu by remember { mutableStateOf<BillEntity?>(null) }
    var editingBill by remember { mutableStateOf<BillEntity?>(null) }

    val context = LocalContext.current
    val dailySpending = transactions
        .filter { it.isExpense && it.date == "Hari ini" }
        .sumOf { it.amount.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L }

    LaunchedEffect(dailySpending, prefs.dailyBudget) {
        if (prefs.dailyBudget in 1..dailySpending) {
            (context as? MainActivity)?.sendBudgetNotification("Limit Tercapai!", "Pengeluaran hari ini Rp ${formatRupiah(dailySpending)}")
        }
    }

    Scaffold(
        bottomBar = {
            if (!isViewingAllTransactions) {
                CustomBottomNavigation(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AnimatedContent(targetState = if (isViewingAllTransactions) -1 else selectedTab, label = "") { state ->
                when (state) {
                    -1 -> AllTransactionsScreen(transactions, { isViewingAllTransactions = false }, { viewModel.deleteTransaction(it) })
                    0 -> HomeScreen(prefs.userName, prefs.totalBalance, transactions.take(5), bills, { showTransactionDialog = true }, { showBillDialog = true }, { selectedBillForMenu = it }, { isViewingAllTransactions = true }, { viewModel.deleteTransaction(it) })
                    1 -> SavingScreen(savings.map { it.toSavingPlan() }, prefs.totalBalance, { n, t, l, i -> viewModel.addSaving(n, t, l, i) }, { id, amt -> viewModel.depositSaving(id, amt) }, { id, amt -> viewModel.withdrawSaving(id, amt) }, { viewModel.updateSaving(it.toEntity()) }, { viewModel.deleteSaving(it) }, { selectedTab = 0 })
                    2 -> ActivityScreen(prefs.totalBalance, transactions, savings.map { it.toSavingPlan() }, prefs.dailyBudget, dailySpending, { viewModel.updateBudget(it) }, {
                        val totalIn = transactions.filter { !it.isExpense }.sumOf { it.amount.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L }
                        val totalOut = transactions.filter { it.isExpense }.sumOf { it.amount.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L }
                        (context as? MainActivity)?.generateFinancialReport(transactions, prefs.totalBalance, totalIn, totalOut)
                    }, { viewModel.deleteTransaction(it) }, { selectedTab = 0 })
                    3 -> ProfileScreen(prefs.userName, { viewModel.updateName(it) }, { viewModel.resetData() })
                    else -> PlaceholderScreen()
                }
            }
        }
    }

    if (selectedBillForMenu != null) {
        ModalBottomSheet(onDismissRequest = { selectedBillForMenu = null }) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(selectedBillForMenu!!.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // TOMBOL TANDAI SELESAI (BAYAR)
                ListItem(
                    headlineContent = { Text("Tandai Selesai / Bayar") },
                    leadingContent = { Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen) },
                    modifier = Modifier.clickable {
                        val bill = selectedBillForMenu!!
                        // Jika pengulangan 'Sekali', hapus. Jika rutin, majukan tanggalnya.
                        if (bill.recurrence == "Sekali") {
                            viewModel.deleteBill(bill.id)
                        } else {
                            val nextDate = calculateNextDate(bill.date, bill.recurrence)
                            viewModel.saveBill(bill.title, bill.price.replace(Regex("[^0-9]"), "").toLong(), nextDate, bill.recurrence, bill.id)
                        }
                        selectedBillForMenu = null
                    }
                )

                // TOMBOL EDIT
                ListItem(
                    headlineContent = { Text("Ubah Tagihan") },
                    leadingContent = { Icon(Icons.Default.Edit, null, tint = MainPurple) },
                    modifier = Modifier.clickable {
                        editingBill = selectedBillForMenu
                        showBillDialog = true
                        selectedBillForMenu = null
                    }
                )

                // TOMBOL HAPUS
                ListItem(
                    headlineContent = { Text("Hapus Permanen", color = ErrorRed) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = ErrorRed) },
                    modifier = Modifier.clickable {
                        viewModel.deleteBill(selectedBillForMenu!!.id)
                        selectedBillForMenu = null
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // --- DIALOG TRANSAKSI ---
    if (showTransactionDialog) {
        TransactionDialog(
            onDismiss = { showTransactionDialog = false },
            onConfirm = { t, a, e ->
                // 1. Simpan ke Database melalui ViewModel
                viewModel.addTransaction(t, a, e)
                // 2. LANGSUNG TUTUP DIALOG
                showTransactionDialog = false
            }
        )
    }

// --- DIALOG TAGIHAN ---
    if (showBillDialog) {
        BillDialogEntity(
            existing = editingBill,
            onDismiss = {
                showBillDialog = false
                editingBill = null
            },
            onConfirm = { t, a, d, r, id ->
                // 1. Simpan ke Database melalui ViewModel
                viewModel.saveBill(t, a, d, r, id)
                // 2. LANGSUNG TUTUP DIALOG
                showBillDialog = false
                editingBill = null
            },
        )
    }
}

// --- SHARED UI COMPONENTS ---

@Composable
fun HomeScreen(userName: String, balance: Long, transactionList: List<TransactionEntity>, billList: List<BillEntity>, onAddTx: () -> Unit, onAddBill: () -> Unit, onBillClick: (BillEntity) -> Unit, onSeeAll: () -> Unit, onDeleteTx: (TransactionEntity) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { HeaderSection(userName) }
        item { BalanceCard(balance, onAddTx) }
        item { SectionHeader("Tagihan Mendatang", "+ Tambah", onAddBill) }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(15.dp)) { items(billList) { PaymentCardEntity(it) { onBillClick(it) } } } }
        item { SectionHeader("Transaksi Terakhir", "Lihat semua", onSeeAll) }
        items(transactionList) { TransactionItemEntity(it, { onDeleteTx(it) }) }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun TransactionItemEntity(transaction: TransactionEntity, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(45.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AccountBalanceWallet, null, tint = if (isSystemInDarkTheme()) LightPurple else DarkBlue)
        }
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) {
            Text(transaction.title, fontWeight = FontWeight.Bold)
            Text(transaction.date, color = MaterialTheme.colorScheme.onBackground.copy(0.5f), fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = (if (transaction.isExpense) "-Rp " else "+Rp ") + transaction.amount, color = if (transaction.isExpense) ErrorRed else SuccessGreen, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = ErrorRed.copy(0.3f), modifier = Modifier.size(16.dp)) }
        }
    }
    if (showConfirm) {
        AlertDialog(onDismissRequest = { showConfirm = false }, title = { Text("Hapus?") }, text = { Text("Saldo akan dikembalikan secara otomatis.") },
            confirmButton = { TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Hapus", color = ErrorRed) } },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Batal") } })
    }
}

@Composable
fun CustomBottomNavigation(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().height(80.dp), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), shadowElevation = 15.dp) {
        Row(Modifier.fillMaxSize(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
            NavigationIcon(Icons.Default.Home, selectedTab == 0) { onTabSelected(0) }
            NavigationIcon(Icons.Default.AccountBalanceWallet, selectedTab == 1) { onTabSelected(1) }
            NavigationIcon(Icons.Default.BarChart, selectedTab == 2) { onTabSelected(2) }
            NavigationIcon(Icons.Default.Person, selectedTab == 3) { onTabSelected(3) }
        }
    }
}

@Composable
fun NavigationIcon(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, null, tint = if (isSelected) MainPurple else MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(28.dp)) }
}

@Composable
fun BalanceCard(balance: Long, onAddClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().height(140.dp), shape = RoundedCornerShape(24.dp)) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(LightPurple, SoftLavender))).padding(20.dp)) {
            Column {
                Text("Total Saldo", color = TextSecondary)
                Text("Rp ${formatRupiah(balance)}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DarkBlue)
            }
            FloatingActionButton(onClick = onAddClick, Modifier.align(Alignment.CenterEnd).size(45.dp), containerColor = Color.White, shape = CircleShape) { Icon(Icons.Default.Add, null, tint = MainPurple) }
        }
    }
}

@Composable
fun HeaderSection(name: String) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column { Text("Halo,", color = TextSecondary, fontSize = 16.sp); Text(name, color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun SectionHeader(title: String, actionText: String = "Lihat semua", onActionClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp); TextButton(onClick = onActionClick) { Text(actionText, color = MainPurple, fontSize = 14.sp) }
    }
}

@Composable
fun PaymentCardEntity(bill: BillEntity, onClick: () -> Unit) {
    val today = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date())
    val isDueDate = bill.date == today

    // Styling dinamis
    val bgColor = if (isDueDate) MainPurple else MaterialTheme.colorScheme.surface
    val textColor = if (isDueDate) Color.White else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isDueDate) MainPurple else MainPurple.copy(alpha = 0.2f)
    val iconBgColor = if (isDueDate) Color.White.copy(alpha = 0.2f) else MainPurple.copy(alpha = 0.1f)
    val iconTint = if (isDueDate) Color.White else MainPurple

    Card(
        modifier = Modifier
            .width(165.dp) // Sedikit lebih lebar
            .height(185.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        // Tambahkan Border Ungu Tipis
        border = androidx.compose.foundation.BorderStroke(
            width = if (isDueDate) 4.dp else 2.dp,
            color = borderColor
        ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        // Perbaikan Shadow agar lebih soft dan premium
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDueDate) 10.dp else 4.dp,
            pressedElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = bill.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                color = textColor
            )

            Text(
                text = bill.price,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDueDate) Icons.Default.Warning else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (isDueDate) "HARI INI" else bill.date,
                    fontSize = 11.sp,
                    fontWeight = if (isDueDate) FontWeight.ExtraBold else FontWeight.Normal,
                    color = textColor.copy(alpha = 0.7f),
                    letterSpacing = if (isDueDate) 0.5.sp else 0.sp
                )
            }

            if (bill.recurrence != "Sekali") {
                Text(
                    text = "🔄 ${bill.recurrence}",
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDialog(onDismiss: () -> Unit, onConfirm: (String, Long, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }; var amt by remember { mutableStateOf("") }; var isExp by remember { mutableStateOf(true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Tambah Transaksi") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(amt, { if (it.all { c -> c.isDigit() }) amt = it }, label = { Text("Nominal") }, prefix = { Text("Rp ") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = isExp, onClick = { isExp = true }, label = { Text("Keluar") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = !isExp, onClick = { isExp = false }, label = { Text("Masuk") })
            }
        }
    }, confirmButton = { Button(onClick = { val a = amt.toLongOrNull() ?: 0L; if (title.isNotEmpty() && a > 0) onConfirm(title, a, isExp) }) { Text("Simpan") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDialogEntity(existing: BillEntity?, onDismiss: () -> Unit, onConfirm: (String, Long, String, String, String?) -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var amt by remember { mutableStateOf(existing?.price?.replace(Regex("[^0-9]"), "") ?: "") }
    var date by remember { mutableStateOf(existing?.date ?: "Pilih Tanggal") }
    var rec by remember { mutableStateOf(existing?.recurrence ?: "Sekali") }
    var expanded by remember { mutableStateOf(false) } // Untuk Dropdown pengulangan

    val cal = Calendar.getInstance()
    val dpd = DatePickerDialog(ctx, { _, y, m, d -> date = "$d/${m + 1}/$y" }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Tambah Tagihan" else "Edit Tagihan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Nama Tagihan") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amt, { if (it.all { c -> c.isDigit() }) amt = it }, label = { Text("Nominal") }, prefix = { Text("Rp ") }, modifier = Modifier.fillMaxWidth())

                // Pilih Tanggal
                OutlinedCard(onClick = { dpd.show() }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp)) { Icon(Icons.Default.Event, null); Spacer(Modifier.width(12.dp)); Text(date) }
                }

                // Pilih Pengulangan
                Box {
                    OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Repeat, null)
                            Spacer(Modifier.width(12.dp))
                            Text("Ulangi: $rec")
                        }
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("Sekali", "Mingguan", "Bulanan", "Tahunan").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { rec = option; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val a = amt.toLongOrNull() ?: 0L
                if (title.isNotEmpty() && a > 0 && date != "Pilih Tanggal") onConfirm(title, a, date, rec, existing?.id)
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(txs: List<TransactionEntity>, onBack: () -> Unit, onDelete: (TransactionEntity) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Semua Transaksi", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } })
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(txs) { TransactionItemEntity(it, { onDelete(it) }) } }
    }
}

fun calculateNextDate(currentDate: String, recurrence: String): String {
    val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
    val date = try { sdf.parse(currentDate) } catch (e: Exception) { null } ?: return currentDate
    val cal = Calendar.getInstance()
    cal.time = date
    when (recurrence) {
        "Mingguan" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
        "Bulanan" -> cal.add(Calendar.MONTH, 1)
        "Tahunan" -> cal.add(Calendar.YEAR, 1)
    }
    return sdf.format(cal.time)
}

@Composable
fun PlaceholderScreen() { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Coming Soon") } }

// --- UTILS & MAPPERS ---
fun formatRupiah(amount: Long): String = NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount)
fun SavingEntity.toSavingPlan() = SavingPlan(id, name, target, currentAmount, location, iconName)
fun SavingPlan.toEntity() = SavingEntity(id, name, target, currentAmount, location, iconName)
