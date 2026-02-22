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
    onExportReport: () -> Unit,
    onDeleteTransaction: (Transaction) -> Unit, // Tambahkan parameter ini
    onBack: () -> Unit
) {
    var selectedTimeRange by remember { mutableStateOf("Bulan") }
    var showFullDetails by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    val timeRanges = listOf("Hari", "Minggu", "Bulan", "Tahun")
    val isOverBudget = dailyBudget > 0 && dailySpending >= dailyBudget

    // Hitung Statistik Nyata
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

        // Kartu Budgeting
        if (dailyBudget > 0) {
            item {
                BudgetStatusCard(
                    dailyBudget = dailyBudget,
                    dailySpending = dailySpending,
                    isOverBudget = isOverBudget
                )
            }
        }

        // Filter Waktu
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

        // Kartu Pie Chart
        item {
            SavingsChartCard(
                balance = balance,
                income = totalIncome,
                expense = totalExpense,
                savings = totalSaved,
                range = selectedTimeRange
            )
        }

        // Section Rincian Dana
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

        // PERBAIKAN: Melewatkan onDelete ke TransactionItem
        if (showFullDetails) {
            items(transactions) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onDelete = { onDeleteTransaction(transaction) }
                )
            }
        }

        // Alat Produktif Offline
        item { SectionHeader(title = "Alat Produktif", onActionClick = {}) }
        item {
            ProductiveGrid(
                onBudgetClick = { showBudgetDialog = true },
                onExportClick = onExportReport
            )
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

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

@Composable
fun ProductiveGrid(
    onBudgetClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val tools = listOf(
        ToolItem("Budgeting", Icons.Default.Timer, ErrorRed, "Limit Harian"),
        ToolItem("Laporan", Icons.Default.PictureAsPdf, Color(0xFFE64A19), "Ekspor PDF")
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
                            onClick = {
                                when (tool.title) {
                                    "Budgeting" -> onBudgetClick()
                                    "Laporan" -> onExportClick()
                                }
                            }
                        )
                    }
                }
                if (rowItems.size < 3) {
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun SavingsChartCard(balance: Long, income: Long, expense: Long, savings: Long, range: String) {
    val total = (income + expense + savings).toFloat()

    val incomePercent = if (total > 0) (income / total * 100).toInt() else 0
    val expensePercent = if (total > 0) (expense / total * 100).toInt() else 0
    val savingsPercent = if (total > 0) (savings / total * 100).toInt() else 0

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

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
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

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ChartLegendDetail("Pemasukan", "$incomePercent%", SuccessGreen)
                ChartLegendDetail("Pengeluaran", "$expensePercent%", ErrorRed)
                ChartLegendDetail("Tabungan", "$savingsPercent%", MainPurple)
            }
        }
    }
}

@Composable
fun ChartLegendDetail(label: String, percent: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, color = DarkBlue.copy(alpha = 0.7f))
        }
        Text(percent, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkBlue)
    }
}

@Composable
fun BudgetStatusCard(dailyBudget: Long, dailySpending: Long, isOverBudget: Boolean) {
    val progress = if (dailyBudget > 0) (dailySpending.toFloat() / dailyBudget.toFloat()).coerceIn(0f, 1f) else 0f
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
                Icon(Icons.Default.Shield, null, tint = statusColor)
                Spacer(Modifier.width(8.dp))
                Text("Status Budget Harian", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Terpakai: Rp ${formatRupiah(dailySpending)}", fontSize = 12.sp)
                Text("Limit: Rp ${formatRupiah(dailyBudget)}", fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp))) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.2f)
                )
            }
            if (isOverBudget) {
                Text("Melebihi budget harian!", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            } else {
                Text("Sisa budget: Rp ${formatRupiah(if(remaining < 0) 0 else remaining)}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun InfoSummaryRow(income: Long, expense: Long, savings: Long) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoMiniCard(Modifier.weight(1f), "Pemasukan", income, SuccessGreen)
        InfoMiniCard(Modifier.weight(1f), "Pengeluaran", expense, ErrorRed)
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
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 10.sp, color = TextSecondary)
            Text(formatRupiah(amount), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
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
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(40.dp).background(tool.color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(tool.icon, null, tint = tool.color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(tool.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
            Text(tool.sub, fontSize = 9.sp, color = TextSecondary, textAlign = TextAlign.Center)
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
            Button(onClick = { onConfirm(budgetStr.toLongOrNull() ?: 0L) }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun ActivityHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Text("Aktifitas & Analisis", fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

data class ToolItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color, val sub: String)