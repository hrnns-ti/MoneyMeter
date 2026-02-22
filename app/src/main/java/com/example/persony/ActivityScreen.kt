package com.example.persony

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.persony.ui.theme.*

@Composable
fun ActivityScreen(
    balance: Long,
    transactions: List<Transaction>,
    savings: List<SavingPlan>,
    dailyBudget: Long,
    dailySpending: Long,
    onUpdateBudget: (Long) -> Unit,
    onBack: () -> Unit
) {
    var selectedTimeRange by remember { mutableStateOf("Bulan") }
    var showFullDetails by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    val timeRanges = listOf("Hari", "Minggu", "Bulan", "Tahun")

    // --- LOGIKA BUDGETING DIPERBAIKI ---
    // Peringatan muncul jika budget telah diatur ( > 0) dan pengeluaran >= budget
    val isOverBudget = dailyBudget > 0 && dailySpending >= dailyBudget

    val totalIncome = transactions.filter { !it.isExpense }.sumOf { it.amount.replace(".", "").toLong() }
    val totalExpense = transactions.filter { it.isExpense }.sumOf { it.amount.replace(".", "").toLong() }
    val totalSaved = savings.sumOf { it.currentAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ActivityHeader(onBack) }

        // --- KARTU STATUS BUDGET AKTIF ---
        // Akan selalu muncul jika budget sudah di-set
        if (dailyBudget > 0) {
            item {
                BudgetStatusCard(
                    dailyBudget = dailyBudget,
                    dailySpending = dailySpending,
                    isOverBudget = isOverBudget
                )
            }
        }

        // Filter Rentang Waktu
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(timeRanges) { range ->
                    FilterChip(
                        selected = selectedTimeRange == range,
                        onClick = { selectedTimeRange = range },
                        label = { Text(range) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Chart Card
        item {
            SavingsChartCard(
                balance = balance,
                income = totalIncome,
                expense = totalExpense,
                savings = totalSaved,
                range = selectedTimeRange
            )
        }

        // Rincian Dana Section
        item {
            SectionHeader(
                title = "Rincian Dana",
                actionText = if (showFullDetails) "Sembunyikan" else "Selengkapnya",
                onActionClick = { showFullDetails = !showFullDetails }
            )
        }

        item {
            InfoSummaryRow(income = totalIncome, expense = totalExpense, savings = totalSaved)
        }

        // Fitur Selengkapnya (Aktivitas Detail)
        if (showFullDetails) {
            items(transactions) { transaction ->
                TransactionItem(transaction)
            }
        }

        // Alat Produktif
        item { SectionHeader(title = "Alat Produktif", onActionClick = {}) }
        item {
            ProductiveGrid(onBudgetClick = { showBudgetDialog = true })
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Dialog Input Budget
    if (showBudgetDialog) {
        BudgetDialog(
            currentBudget = dailyBudget,
            onDismiss = { showBudgetDialog = false },
            onConfirm = {
                onUpdateBudget(it)
                showBudgetDialog = false
            }
        )
    }
}

// --- KARTU STATUS BUDGET ---
@Composable
fun BudgetStatusCard(dailyBudget: Long, dailySpending: Long, isOverBudget: Boolean) {
    val progress = (dailySpending.toFloat() / dailyBudget.toFloat()).coerceIn(0f, 1f)
    val remaining = dailyBudget - dailySpending
    val statusColor = if (isOverBudget) ErrorRed else if (progress > 0.8f) Color(0xFFFFA000) else SuccessGreen

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = "Budget Status",
                    tint = statusColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Status Budget Harian",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Terpakai: Rp ${formatRupiah(dailySpending)}", fontSize = 12.sp)
                Text("Limit: Rp ${formatRupiah(dailyBudget)}", fontSize = 12.sp, color = TextSecondary)
            }

            Spacer(Modifier.height(8.dp))

            // Progress bar untuk budget
            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp))) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.2f)
                )
            }

            Spacer(Modifier.height(8.dp))

            if (isOverBudget) {
                Text(
                    "Anda telah melebihi budget harian!",
                    color = ErrorRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    "Sisa budget hari ini: Rp ${formatRupiah(remaining)}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}


@Composable
fun ProductiveGrid(onBudgetClick: () -> Unit) {
    val tools = listOf(
        ToolItem("Budgeting", Icons.Default.Timer, ErrorRed, "Limit Harian"),
        ToolItem("Laporan", Icons.Default.PictureAsPdf, Color(0xFFE64A19), "Ekspor PDF"),
        ToolItem("Kalender", Icons.Default.DateRange, Color(0xFF1976D2), "Jadwal Gaji"),
        // ToolItem("Catatan", Icons.Default.EditNote, MainPurple, "Rencana Pos"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tools.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { tool ->
                    Box(modifier = Modifier.weight(1f)) {
                        ProductiveSquareCard(
                            tool = tool,
                            onClick = { if (tool.title == "Budgeting") onBudgetClick() }
                        )
                    }
                }
                if (rowItems.size < 3) {
                    repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun BudgetDialog(currentBudget: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var budgetStr by remember { mutableStateOf(if(currentBudget > 0) currentBudget.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur Limit Harian", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Bantu kontrol pengeluaran harianmu agar tidak boros.", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = budgetStr,
                    onValueChange = { if (it.all { c -> c.isDigit() }) budgetStr = it },
                    label = { Text("Nominal Limit") },
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(budgetStr.toLongOrNull() ?: 0L) },
                colors = ButtonDefaults.buttonColors(containerColor = MainPurple)
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun ProductiveSquareCard(tool: ToolItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(tool.color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon, null, tint = tool.color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(tool.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
            Text(tool.sub, fontSize = 9.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SavingsChartCard(balance: Long, income: Long, expense: Long, savings: Long, range: String) {
    val total = (income + expense + savings).toFloat()
    val incomeSweep = if (total > 0) (income / total) * 360f else 0f
    val expenseSweep = if (total > 0) (expense / total) * 360f else 0f
    val savingsSweep = if (total > 0) (savings / total) * 360f else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(LightPurple, SoftLavender)))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Efisiensi Dana ($range)", color = DarkBlue.copy(alpha = 0.6f), fontSize = 14.sp)
                    Text("Rp ${formatRupiah(balance)}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = DarkBlue)
                }
                Icon(Icons.Default.TrendingUp, null, tint = SuccessGreen)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    drawArc(Color.White.copy(alpha = 0.3f), 0f, 360f, false, style = Stroke(30f))
                    drawArc(SuccessGreen, -90f, incomeSweep, false, style = Stroke(35f, cap = StrokeCap.Round))
                    drawArc(ErrorRed, -90f + incomeSweep, expenseSweep, false, style = Stroke(35f, cap = StrokeCap.Round))
                    drawArc(MainPurple, -90f + incomeSweep + expenseSweep, savingsSweep, false, style = Stroke(35f, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val status = if (expense > income) "Boros" else "Stabil"
                    Text(status, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = if(status == "Stabil") DarkBlue else ErrorRed)
                    Text("Kondisi Dompet", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ChartLegendItem("Masuk", SuccessGreen)
                ChartLegendItem("Keluar", ErrorRed)
                ChartLegendItem("Simpan", MainPurple)
            }
        }
    }
}

@Composable
fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = DarkBlue, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun InfoSummaryRow(income: Long, expense: Long, savings: Long) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoMiniCard(Modifier.weight(1f), "Masuk", income, SuccessGreen)
        InfoMiniCard(Modifier.weight(1f), "Keluar", expense, ErrorRed)
        InfoMiniCard(Modifier.weight(1f), "Tabungan", savings, MainPurple)
    }
}

@Composable
fun InfoMiniCard(modifier: Modifier, title: String, amount: Long, color: Color) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape))
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 10.sp, color = TextSecondary)
            Text(formatRupiah(amount), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun ActivityHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Text("Analitik Keuangan", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        IconButton(
            onClick = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
        ) {
            Icon(Icons.Outlined.Assignment, contentDescription = null)
        }
    }
}

data class ToolItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color, val sub: String)
