package com.example.persony

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.persony.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userName: String,
    onNameChange: (String) -> Unit,
    onResetData: () -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Avatar Placeholder
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MainPurple.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = MainPurple
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(userName, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text("Persony User Member", color = TextSecondary, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(40.dp))

        // Group Pengaturan
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Edit Nama
                ListItem(
                    headlineContent = { Text("Ubah Nama Panggilan") },
                    leadingContent = { Icon(Icons.Default.Edit, null, tint = MainPurple) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier.clickable { showNameDialog = true }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                // Reset Data
                ListItem(
                    headlineContent = { Text("Reset Semua Data", color = ErrorRed) },
                    leadingContent = { Icon(Icons.Default.DeleteForever, null, tint = ErrorRed) },
                    modifier = Modifier.clickable { showResetDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Text("Version 1.0.0 (Offline Mode)", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(20.dp))
    }

    // Dialog Ubah Nama
    if (showNameDialog) {
        var tempName by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Ubah Nama") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Nama Baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onNameChange(tempName)
                    showNameDialog = false
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Batal") }
            }
        )
    }

    // Dialog Konfirmasi Reset
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Hapus Semua Data?") },
            text = { Text("Tindakan ini akan menghapus seluruh saldo, riwayat transaksi, dan target tabungan secara permanen.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Ya, Reset", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Batal") }
            }
        )
    }
}