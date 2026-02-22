package com.example.persony

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.persony.ui.theme.*

@Composable
fun ActivityScreen(
    balance: Long,
    transactions: List<Transaction>,
    savings: List<SavingPlan>, // Sekarang menerima data tabungan
    onBack: () -> Unit
) {
    // State untuk filter waktu
    var selectedTimeRange by remember { mutableStateOf("Bulan") }
    val timeRanges = listOf("Hari", "Minggu", "Bulan", "Tahun")

    // Menghitung Statistik dari data nyata
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

        // Filter Rentang Waktu
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
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

        item {
            SavingsChartCard(
                balance = balance,
                income = totalIncome,
                expense = totalExpense,
                savings = totalSaved,
                range = selectedTimeRange
            )
        }

        item { SectionHeader(title = "Rincian Dana", onActionClick = {}) }

        item {
            InfoSummaryRow(income = totalIncome, expense = totalExpense, savings = totalSaved)
        }

        item { SectionHeader(title = "Riwayat Aktivitas", onActionClick = {}) }

        items(transactions) { transaction ->
            TransactionItem(transaction)
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun ActivityHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
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

@Composable
fun SavingsChartCard(balance: Long, income: Long, expense: Long, savings: Long, range: String) {
    val total = (income + expense + savings).toFloat()

    // Proporsi Chart
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
                    // Background Ring
                    drawArc(Color.White.copy(alpha = 0.3f), 0f, 360f, false, style = Stroke(30f))

                    // Pemasukan (Hijau)
                    drawArc(SuccessGreen, -90f, incomeSweep, false, style = Stroke(35f, cap = StrokeCap.Round))

                    // Pengeluaran (Merah)
                    drawArc(ErrorRed, -90f + incomeSweep, expenseSweep, false, style = Stroke(35f, cap = StrokeCap.Round))

                    // Tabungan (Ungu)
                    drawArc(MainPurple, -90f + incomeSweep + expenseSweep, savingsSweep, false, style = Stroke(35f, cap = StrokeCap.Round))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val status = if (expense > income) "Warning" else "Sehat"
                    Text(status, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = if(status == "Sehat") DarkBlue else ErrorRed)
                    Text("Kondisi Dompet", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Legend Chart - Menggunakan fungsi yang tadi error
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ChartLegendItem("Masuk", SuccessGreen)
                ChartLegendItem("Keluar", ErrorRed)
                ChartLegendItem("Simpan", MainPurple)
            }
        }
    }
}

// FUNGSI INI HARUS ADA DI DALAM FILE YANG SAMA ATAU PUBLIC
@Composable
fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
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
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 10.sp, color = TextSecondary)
            Text(formatRupiah(amount), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}