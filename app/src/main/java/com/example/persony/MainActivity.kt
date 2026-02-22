package com.example.persony

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.DatePicker
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.persony.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    // ... di dalam class MainActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }

        enableEdgeToEdge()
        setContent {
            PersonyTheme {
                MainContainer()
            }
        }
    }

    fun sendBudgetNotification(title: String, message: String) {
        val builder = androidx.core.app.NotificationCompat.Builder(this, "BUDGET_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL) // Tambahkan suara/getar default
            .setAutoCancel(true)

        try {
            val notificationManager = androidx.core.app.NotificationManagerCompat.from(this)
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            // Log izin ditolak
        }
    }

    private fun createNotificationChannel() {
        val name = "Budget Alert"
        val descriptionText = "Notifikasi ketika pengeluaran melebihi budget"
        val importance = android.app.NotificationManager.IMPORTANCE_HIGH
        val channel = android.app.NotificationChannel("BUDGET_CHANNEL", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: android.app.NotificationManager =
            getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun generateFinancialReport(
        transactions: List<Transaction>,
        savings: List<SavingPlan>,
        balance: Long,
        totalIn: Long,
        totalOut: Long
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Ukuran A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
        }

        var y = 40f

        // 1. Header
        canvas.drawText("LAPORAN KEUANGAN PERSONY", 40f, y, titlePaint)
        y += 20f
        paint.textSize = 10f
        canvas.drawText("Dicetak pada: ${SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault()).format(Date())}", 40f, y, paint)
        y += 40f

        paint.style = Paint.Style.STROKE
        canvas.drawRect(40f, y, 555f, y + 80f, paint)
        paint.style = Paint.Style.FILL
        y += 25f
        canvas.drawText("Total Saldo Saat Ini:", 60f, y, paint)
        canvas.drawText("Rp ${formatRupiah(balance)}", 400f, y, titlePaint.apply { textSize = 14f })
        y += 20f
        canvas.drawText("Total Pemasukan:", 60f, y, paint)
        canvas.drawText("Rp ${formatRupiah(totalIn)}", 400f, y, paint.apply { color = android.graphics.Color.GREEN })
        y += 20f
        canvas.drawText("Total Pengeluaran:", 60f, y, paint.apply { color = android.graphics.Color.BLACK })
        canvas.drawText("Rp ${formatRupiah(totalOut)}", 400f, y, paint.apply { color = android.graphics.Color.RED })

        paint.color = android.graphics.Color.BLACK
        y += 60f

        // 3. Tabel Transaksi
        canvas.drawText("RIWAYAT TRANSAKSI", 40f, y, titlePaint.apply { textSize = 12f })
        y += 20f

        // Header Tabel
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Tanggal", 40f, y, paint)
        canvas.drawText("Keterangan", 120f, y, paint)
        canvas.drawText("Nominal", 450f, y, paint)
        canvas.drawLine(40f, y + 5f, 555f, y + 5f, paint)
        y += 25f

        paint.typeface = Typeface.DEFAULT
        transactions.forEach { tx ->
            if (y > 800) return@forEach // Batas halaman sederhana
            canvas.drawText(tx.date, 40f, y, paint)
            canvas.drawText(tx.title, 120f, y, paint)
            val prefix = if (tx.isExpense) "-" else "+"
            canvas.drawText("$prefix Rp ${tx.amount}", 450f, y, paint)
            y += 20f
        }

        pdfDocument.finishPage(page)

        // Simpan ke folder Downloads
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Laporan_Persony_${System.currentTimeMillis()}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this, "PDF berhasil disimpan di folder Downloads", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal simpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isViewingAllTransactions by remember { mutableStateOf(false) }

    // --- STATE LOGIKA ---
    var totalBalance by rememberSaveable { mutableLongStateOf(14570800L) }
    var transactions by remember { mutableStateOf<List<Transaction>>(sampleTransactions) }
    var bills by remember { mutableStateOf<List<Bill>>(sampleBills) }
    var savingPlans by remember { mutableStateOf<List<SavingPlan>>(sampleSavings) }

    var showTransactionDialog by remember { mutableStateOf(false) }
    var showBillDialog by remember { mutableStateOf(false) }
    var selectedBillForMenu by remember { mutableStateOf<Bill?>(null) }
    var editingBill by remember { mutableStateOf<Bill?>(null) }

    val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
    val today = sdf.format(Date())
    val context = LocalContext.current

    // State untuk Budgeting (Default: 0 berarti belum diatur)
    var dailyBudget by rememberSaveable { mutableLongStateOf(0L) }

    // Hitung pengeluaran khusus hari ini
    val dailySpending = transactions
        .filter { it.isExpense && it.date == "Hari ini" }
        .sumOf { it.amount.replace(".", "").toLong() }

    // Fungsi Update Budget
    val onUpdateBudget: (Long) -> Unit = { newBudget ->
        dailyBudget = newBudget
    }

    LaunchedEffect(dailySpending) {
        if (dailyBudget in 1..dailySpending) {
            (context as? MainActivity)?.sendBudgetNotification(
                "Waduh, Boros Nih!",
                "Pengeluaran hari ini sudah Rp ${formatRupiah(dailySpending)}. Melewati limit Rp ${formatRupiah(dailyBudget)}!"
            )
        }
    }

    // Logika Tambah Transaksi
    val onAddNewTransaction: (String, Long, Boolean) -> Unit = { title, amount, isExpense ->
        val newEntry = Transaction(title, "Hari ini", formatRupiah(amount), isExpense)
        transactions = listOf(newEntry) + transactions
        if (isExpense) totalBalance -= amount else totalBalance += amount
        showTransactionDialog = false
    }

    val onAddNewSaving: (String, Long, String, ImageVector) -> Unit = { name, target, location, icon ->
        val newPlan = SavingPlan(
            name = name,
            target = target,
            currentAmount = 0L,
            location = location,
            icon = icon
        )
        savingPlans = savingPlans + newPlan
    }

    // Logika Menabung Dinamis
    val onDepositSaving: (String, Long) -> Unit = { id, amount ->
        if (totalBalance >= amount) {
            val plan = savingPlans.find { it.id == id }
            if (plan != null) {
                savingPlans = savingPlans.map {
                    if (it.id == id) it.copy(currentAmount = it.currentAmount + amount) else it
                }
                totalBalance -= amount
                val newTx = Transaction("Simpan ke ${plan.name}", "Hari ini", formatRupiah(amount), true)
                transactions = listOf(newTx) + transactions
            }
        }
    }

    val onWithdrawSaving: (String, Long) -> Unit = { id, amount ->
        val plan = savingPlans.find { it.id == id }
        if (plan != null && plan.currentAmount >= amount) {
            savingPlans = savingPlans.map {
                if (it.id == id) it.copy(currentAmount = it.currentAmount - amount) else it
            }
            onAddNewTransaction("Tarik dari ${plan.name}", amount, false)
        }
    }

    // Fungsi Edit Tabungan
    val onUpdateSaving: (SavingPlan) -> Unit = { updatedPlan ->
        savingPlans = savingPlans.map { if (it.id == updatedPlan.id) updatedPlan else it }
    }

    // Fungsi Hapus Tabungan
    val onDeleteSaving: (String) -> Unit = { id ->
        savingPlans = savingPlans.filter { it.id != id }
    }

    // Logika Simpan/Update Tagihan
    val onSaveBill: (String, Long, String, String, String?) -> Unit = { title, amount, date, recurrence, id ->
        if (id == null) {
            val newBill = Bill(title = title, price = "Rp ${formatRupiah(amount)}", date = date, recurrence = recurrence)
            bills = bills + newBill
        } else {
            bills = bills.map { if (it.id == id) it.copy(title = title, price = "Rp ${formatRupiah(amount)}", date = date, recurrence = recurrence) else it }
        }
        showBillDialog = false
        editingBill = null
    }

    // Logika Selesaikan Tagihan
    val onCompleteBill: (Bill) -> Unit = { bill ->
        if (bill.recurrence == "Sekali") {
            bills = bills.filter { it.id != bill.id }
        } else {
            val nextDate = calculateNextDate(bill.date, bill.recurrence)
            bills = bills.map { if (it.id == bill.id) it.copy(date = nextDate) else it }
        }
        selectedBillForMenu = null
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
            AnimatedContent(
                targetState = if (isViewingAllTransactions) -1 else selectedTab,
                label = "PageTransition"
            ) { targetState ->
                when (targetState) {
                    -1 -> AllTransactionsScreen(
                        transactions = transactions,
                        onBack = { isViewingAllTransactions = false }
                    )
                    0 -> HomeScreen(
                        balance = totalBalance,
                        transactionList = transactions.take(5),
                        billList = bills.sortedByDescending { it.date == today },
                        onAddTransactionClick = { showTransactionDialog = true },
                        onAddBillClick = { showBillDialog = true },
                        onBillClick = { selectedBillForMenu = it },
                        onSeeAllTransactions = { isViewingAllTransactions = true }
                    )
                    1 -> SavingScreen(
                        savings = savingPlans,
                        totalBalance = totalBalance,
                        onAddSaving = onAddNewSaving,
                        onDeposit = onDepositSaving,
                        onWithdraw = onWithdrawSaving,
                        onUpdate = onUpdateSaving,
                        onDelete = onDeleteSaving,
                        onBack = { selectedTab = 0 }
                    )
                    2 -> ActivityScreen(
                        balance = totalBalance,
                        transactions = transactions,
                        savings = savingPlans,
                        dailyBudget = dailyBudget,
                        dailySpending = dailySpending,
                        onUpdateBudget = onUpdateBudget,
                        onExportReport = {
                            // Hitung totalIn dan totalOut dulu
                            val totalIn = transactions.filter { !it.isExpense }.sumOf { it.amount.replace(".", "").toLong() }
                            val totalOut = transactions.filter { it.isExpense }.sumOf { it.amount.replace(".", "").toLong() }

                            (context as? MainActivity)?.generateFinancialReport(
                                transactions, savingPlans, totalBalance, totalIn, totalOut
                            )
                        },
                        onBack = { selectedTab = 0 }
                    )
                    else -> PlaceholderScreen()
                }
            }
        }
    }

    // Pop-up Menu & Dialogs
    if (selectedBillForMenu != null) {
        ModalBottomSheet(onDismissRequest = { selectedBillForMenu = null }) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(selectedBillForMenu!!.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                ListItem(
                    headlineContent = { Text("Tandai Selesai") },
                    leadingContent = { Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen) },
                    modifier = Modifier.clickable { onCompleteBill(selectedBillForMenu!!) }
                )
                ListItem(
                    headlineContent = { Text("Ubah") },
                    leadingContent = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier.clickable {
                        editingBill = selectedBillForMenu
                        showBillDialog = true
                        selectedBillForMenu = null
                    }
                )
                ListItem(
                    headlineContent = { Text("Hapus", color = ErrorRed) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = ErrorRed) },
                    modifier = Modifier.clickable {
                        bills = bills.filter { it.id != selectedBillForMenu!!.id }
                        selectedBillForMenu = null
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showTransactionDialog) {
        TransactionDialog(onDismiss = { showTransactionDialog = false }, onConfirm = onAddNewTransaction)
    }

    if (showBillDialog) {
        BillDialog(
            existingBill = editingBill,
            onDismiss = { showBillDialog = false; editingBill = null },
            onConfirm = onSaveBill
        )
    }
}

@Composable
fun CustomBottomNavigation(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        shadowElevation = 15.dp
    ) {
        Row(Modifier.fillMaxSize(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
            NavigationIcon(Icons.Default.Home, selectedTab == 0) { onTabSelected(0) }
            NavigationIcon(Icons.Default.AccountBalanceWallet, selectedTab == 1) { onTabSelected(1) }
            Box(Modifier.size(50.dp).background(MainPurple, CircleShape), Alignment.Center) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
            NavigationIcon(Icons.Default.BarChart, selectedTab == 2) { onTabSelected(2) }
            NavigationIcon(Icons.Default.Person, selectedTab == 3) { onTabSelected(3) }
        }
    }
}

@Composable
fun NavigationIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MainPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(28.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(transactions: List<Transaction>, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Semua Transaksi", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(transactions) { transaction ->
                TransactionItem(transaction)
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun HomeScreen(
    balance: Long,
    transactionList: List<Transaction>,
    billList: List<Bill>,
    onAddTransactionClick: () -> Unit,
    onAddBillClick: () -> Unit,
    onBillClick: (Bill) -> Unit,
    onSeeAllTransactions: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { HeaderSection() }
        item { BalanceCard(balance = balance, onAddClick = onAddTransactionClick) }

        item {
            SectionHeader(
                title = "Tagihan Mendatang",
                actionText = "+ Tambah",
                onActionClick = onAddBillClick
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                items(billList) { bill ->
                    PaymentCard(bill = bill, onClick = { onBillClick(bill) })
                }
            }
        }

        item {
            SectionHeader(
                title = "Transaksi Terakhir",
                onActionClick = onSeeAllTransactions
            )
        }
        items(transactionList) { transaction ->
            TransactionItem(transaction)
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun PaymentCard(bill: Bill, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
    val today = sdf.format(Date())
    val isDueDate = bill.date == today

    val bgColor = if (isDueDate) MainPurple else MaterialTheme.colorScheme.surface
    val textColor = if (isDueDate) Color.White else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .width(160.dp)
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(textColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CalendarMonth, null, tint = textColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(bill.title, color = textColor, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(bill.price, color = textColor.copy(alpha = 0.8f), fontSize = 12.sp)
            Text(if(isDueDate) "Hari Ini" else bill.date, color = textColor.copy(alpha = 0.6f), fontSize = 10.sp)
            if (bill.recurrence != "Sekali") {
                Text("🔄 ${bill.recurrence}", color = textColor.copy(alpha = 0.5f), fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(45.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                null,
                tint = if (isSystemInDarkTheme()) LightPurple else DarkBlue
            )
        }
        Spacer(modifier = Modifier.width(15.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(transaction.date, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
        }
        Text(
            text = (if (transaction.isExpense) "-Rp " else "+Rp ") + transaction.amount,
            color = if (transaction.isExpense) ErrorRed else SuccessGreen,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDialog(
    existingBill: Bill?,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, String, String, String?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(existingBill?.title ?: "") }
    var amountStr by remember { mutableStateOf(existingBill?.price?.replace(Regex("[^0-9]"), "") ?: "") }
    var dateStr by remember { mutableStateOf(existingBill?.date ?: "Pilih Tanggal") }
    var recurrence by remember { mutableStateOf(existingBill?.recurrence ?: "Sekali") }
    var expanded by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            dateStr = "$dayOfMonth/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if(existingBill == null) "Tambah Tagihan" else "Ubah Tagihan", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Nama Tagihan") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { if (it.all { c -> c.isDigit() }) amountStr = it },
                    label = { Text("Nominal") },
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedCard(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp)); Text(dateStr)
                    }
                }
                Box {
                    OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Repeat, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp)); Text("Ulangi: $recurrence")
                        }
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("Sekali", "Mingguan", "Bulanan", "Tahunan").forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { recurrence = option; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toLongOrNull() ?: 0L
                    if (title.isNotEmpty() && amount > 0 && dateStr != "Pilih Tanggal") {
                        onConfirm(title, amount, dateStr, recurrence, existingBill?.id)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MainPurple)
            ) { Text("Simpan", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Transaksi", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Transaksi") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountStr = it },
                    label = { Text("Nominal") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("Rp ") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Jenis:")
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        label = { Text("Keluar") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        label = { Text("Masuk") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toLongOrNull() ?: 0L
                    if (title.isNotEmpty() && amount > 0) {
                        onConfirm(title, amount, isExpense)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MainPurple)
            ) { Text("Simpan", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun HeaderSection() {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column {
            Text("Halo,", color = TextSecondary, fontSize = 16.sp)
            Text("Siyam Ahmed!", color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {}, modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)) {
                Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(10.dp))
            IconButton(onClick = {}, modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)) {
                Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun BalanceCard(balance: Long, onAddClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().height(140.dp), shape = RoundedCornerShape(24.dp)) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(LightPurple, SoftLavender))).padding(20.dp)) {
            Column {
                Text("Total Saldo", color = TextSecondary)
                Text("Rp ${formatRupiah(balance)}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DarkBlue)
            }
            FloatingActionButton(onClick = onAddClick, Modifier.align(Alignment.CenterEnd).size(45.dp), containerColor = Color.White, shape = CircleShape) {
                Icon(Icons.Default.Add, null, tint = MainPurple)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, actionText: String = "Lihat semua", onActionClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        TextButton(onClick = onActionClick) { Text(actionText, color = MainPurple, fontSize = 14.sp) }
    }
}

@Composable
fun PlaceholderScreen() { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Halaman Pengembangan", color = MaterialTheme.colorScheme.onBackground) } }

// --- HELPERS & MODELS ---

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

fun formatRupiah(amount: Long): String = NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount)

data class Bill(val id: String = UUID.randomUUID().toString(), val title: String, val price: String, val date: String, val recurrence: String)
data class Transaction(val title: String, val date: String, val amount: String, val isExpense: Boolean)

val sampleBills = listOf(
    Bill(title = "Adobe Premium", price = "Rp 450.000", date = "25/2/2026", recurrence = "Bulanan"),
    Bill(title = "Spotify Family", price = "Rp 86.000", date = "22/2/2026", recurrence = "Bulanan")
)

val sampleTransactions = listOf(
    Transaction("Apple Store", "21 Jan", "2.300.500", true),
    Transaction("Gaji Kantor", "20 Jan", "12.000.000", false),
    Transaction("Indomaret", "19 Jan", "150.000", true)
)
