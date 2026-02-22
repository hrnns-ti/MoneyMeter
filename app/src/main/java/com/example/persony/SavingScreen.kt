package com.example.persony

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.persony.ui.theme.*
import java.util.UUID

// Model Data
data class SavingPlan(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val target: Long,
    val currentAmount: Long,
    val location: String,
    val icon: ImageVector
)

@Composable
fun SavingScreen(
    savings: List<SavingPlan>,
    totalBalance: Long,
    onAddSaving: (String, Long, String, ImageVector) -> Unit, // Diperbarui agar menerima data
    onDeposit: (String, Long) -> Unit,
    onWithdraw: (String, Long) -> Unit,
    onUpdate: (SavingPlan) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPlanForAction by remember { mutableStateOf<SavingPlan?>(null) }
    var actionType by remember { mutableStateOf("") } // "DEPOSIT", "WITHDRAW", "EDIT"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MainPurple,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, null)
                Text("Tambah Target", modifier = Modifier.padding(start = 8.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    "Tabungan & Target",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }

            // Info Saldo Tersedia
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = MainPurple)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Tersedia untuk ditabung",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                "Rp ${formatRupiah(totalBalance)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            items(savings, key = { it.id }) { plan ->
                SavingItem(
                    plan = plan,
                    onDepositClick = {
                        selectedPlanForAction = plan
                        actionType = "DEPOSIT"
                    },
                    onWithdrawClick = {
                        selectedPlanForAction = plan
                        actionType = "WITHDRAW"
                    },
                    onEditClick = {
                        selectedPlanForAction = plan
                        actionType = "EDIT"
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Dialog Tambah Target Baru (Fitur Baru: Pilih Ikon)
    if (showAddDialog) {
        AddSavingDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, target, location, icon ->
                onAddSaving(name, target, location, icon)
                showAddDialog = false
            }
        )
    }

    // Logic Dialogs lainnya
    selectedPlanForAction?.let { plan ->
        when (actionType) {
            "DEPOSIT", "WITHDRAW" -> {
                SavingActionDialog(
                    isDeposit = actionType == "DEPOSIT",
                    planName = plan.name,
                    onDismiss = { selectedPlanForAction = null },
                    onConfirm = { amount ->
                        if (actionType == "DEPOSIT") onDeposit(plan.id, amount)
                        else onWithdraw(plan.id, amount)
                        selectedPlanForAction = null
                    }
                )
            }
            "EDIT" -> {
                EditSavingDialog(
                    plan = plan,
                    onDismiss = { selectedPlanForAction = null },
                    onDelete = {
                        onDelete(plan.id)
                        selectedPlanForAction = null
                    },
                    onConfirm = { updatedPlan ->
                        onUpdate(updatedPlan)
                        selectedPlanForAction = null
                    }
                )
            }
        }
    }
}

@Composable
fun AddSavingDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long, String, ImageVector) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    // Daftar pilihan ikon
    val iconOptions = listOf(
        Icons.Default.Laptop, Icons.Default.Flight, Icons.Default.Shield,
        Icons.Default.Home, Icons.Default.DirectionsCar, Icons.Default.ShoppingBag,
        Icons.Default.School, Icons.Default.Favorite, Icons.Default.Celebration,
        Icons.Default.Work, Icons.Default.Kitchen, Icons.Default.Headset
    )
    var selectedIcon by remember { mutableStateOf(iconOptions[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Target Tabungan Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Target") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { if (it.all { c -> c.isDigit() }) targetStr = it },
                    label = { Text("Target Nominal") },
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lokasi (Contoh: Bank Jago)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Pilih Ikon:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Geser ke kanan untuk lebih lengkapnya", fontSize = 10.sp)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 5.dp)
                ) {
                    items(iconOptions) { icon ->
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .background(
                                    if (selectedIcon == icon) MainPurple else MainPurple.copy(alpha = 0.1f),
                                    CircleShape
                                )
                                .clickable { selectedIcon = icon },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (selectedIcon == icon) Color.White else MainPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetStr.toLongOrNull() ?: 0L
                    if (name.isNotEmpty() && target > 0) {
                        onConfirm(name, target, location, selectedIcon)
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
fun SavingItem(
    plan: SavingPlan,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val isReached = plan.currentAmount >= plan.target
    val progress = if (plan.target > 0) plan.currentAmount.toFloat() / plan.target.toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .background(MainPurple.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(plan.icon, null, tint = MainPurple)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                plan.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isReached) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            plan.location,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Rp ${formatRupiah(plan.currentAmount)}",
                    fontWeight = FontWeight.Bold,
                    color = if (isReached) SuccessGreen else MainPurple
                )
                Text(
                    "Target: Rp ${formatRupiah(plan.target)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = if (isReached) SuccessGreen else MainPurple,
                trackColor = (if (isReached) SuccessGreen else MainPurple).copy(alpha = 0.15f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onDepositClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MainPurple.copy(alpha = 0.1f),
                        contentColor = MainPurple
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tabung", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onWithdrawClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = plan.currentAmount > 0
                ) {
                    Text("Tarik")
                }
            }
        }
    }
}

@Composable
fun SavingActionDialog(
    isDeposit: Boolean,
    planName: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isDeposit) "Tabung ke $planName" else "Tarik dari $planName", fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = amountStr,
                onValueChange = { if (it.all { c -> c.isDigit() }) amountStr = it },
                label = { Text("Nominal") },
                prefix = { Text("Rp ") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amountStr.toLongOrNull() ?: 0L) },
                colors = ButtonDefaults.buttonColors(containerColor = MainPurple)
            ) { Text("Proses", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun EditSavingDialog(
    plan: SavingPlan,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: (SavingPlan) -> Unit
) {
    var name by remember { mutableStateOf(plan.name) }
    var targetStr by remember { mutableStateOf(plan.target.toString()) }
    var location by remember { mutableStateOf(plan.location) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Detail Tabungan", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Tabungan") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { if (it.all { c -> c.isDigit() }) targetStr = it },
                    label = { Text("Target Nominal") },
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lokasi Penyimpanan") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(plan.copy(
                        name = name,
                        target = targetStr.toLongOrNull() ?: plan.target,
                        location = location
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = MainPurple)
            ) { Text("Simpan", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)) {
                Text("Hapus")
            }
        }
    )
}

val sampleSavings = listOf(
    SavingPlan("1", "Beli MacBook", 25000000L, 5000000L, "Bank Jago", Icons.Default.Laptop),
    SavingPlan("2", "Liburan Jepang", 15000000L, 12000000L, "Tunai/Laci", Icons.Default.Flight),
    SavingPlan("3", "Dana Darurat", 50000000L, 10000000L, "Rekening Utama", Icons.Default.Shield)
)