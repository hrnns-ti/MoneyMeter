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
import androidx.compose.runtime.Composable
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
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ActivityHeader(onBack) }

        item { SavingsChartCard(balance) }

        item { SectionHeader(title = "Fitur Offline", onActionClick = {}) }

        item { QuickMenuRow() }

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
        Text("Analitik & Aktivitas", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        IconButton(
            onClick = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
        ) {
            Icon(Icons.Outlined.Assignment, contentDescription = null)
        }
    }
}

@Composable
fun SavingsChartCard(balance: Long) {
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
                    Text("Total Saldo", color = TextSecondary, fontSize = 14.sp)
                    Text("Rp ${formatRupiah(balance)}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = DarkBlue)
                }
                Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.5f)) {
                    Text("Bulan Ini", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Chart dengan Warna Beragam
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    // Background Ring
                    drawArc(Color.White.copy(alpha = 0.3f), 0f, 360f, false, style = Stroke(30f))

                    // Segmen 1: Kebutuhan (Ungu Utama)
                    drawArc(MainPurple, -90f, 140f, false, style = Stroke(35f, cap = StrokeCap.Round))

                    // Segmen 2: Tabungan (Biru Tua)
                    drawArc(DarkBlue, 55f, 100f, false, style = Stroke(35f, cap = StrokeCap.Round))

                    // Segmen 3: Hiburan (Oranye/Kuning - ErrorRed sebagai aksen)
                    drawArc(ErrorRed, 160f, 60f, false, style = Stroke(35f, cap = StrokeCap.Round))

                    // Segmen 4: Lain-lain (Hijau)
                    drawArc(SuccessGreen, 225f, 40f, false, style = Stroke(35f, cap = StrokeCap.Round))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Stabil", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = DarkBlue)
                    Text("Kesehatan Finansial", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend Chart
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChartLegendItem("Kebutuhan", MainPurple)
                ChartLegendItem("Tabungan", DarkBlue)
                ChartLegendItem("Lainnya", SuccessGreen)
            }
        }
    }
}

@Composable
fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = DarkBlue)
    }
}

@Composable
fun QuickMenuRow() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        contentPadding = PaddingValues(bottom = 10.dp)
    ) {
        item { QuickMenuCard("Catat Hutang", Icons.Default.Book, Color(0xFFE3F2FD), Color(0xFF1976D2)) }
        item { QuickMenuCard("Target Simpanan", Icons.Default.AdsClick, Color(0xFFF1F8E9), SuccessGreen) }
        item { QuickMenuCard("Ekspor Laporan", Icons.Default.PictureAsPdf, Color(0xFFFFF3E0), Color(0xFFE64A19)) }
        item { QuickMenuCard("Kalkulasi Pajak", Icons.Default.Calculate, Color(0xFFF3E5F5), MainPurple) }
    }
}

@Composable
fun QuickMenuCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bgColor: Color, iconColor: Color) {
    Card(
        modifier = Modifier.width(130.dp).height(140.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier.size(45.dp).background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(15.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp)
            Text("Offline", fontSize = 10.sp, color = TextSecondary)
        }
    }
}