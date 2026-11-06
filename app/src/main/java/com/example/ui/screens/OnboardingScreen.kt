package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.WalletViewModel

@Composable
fun OnboardingScreen(
    viewModel: WalletViewModel,
    onWalletReady: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var isRestoreMode by remember { mutableStateOf(false) }
    var restoreTab by remember { mutableIntStateOf(0) } // 0: Mnemonic, 1: Private Key

    // New Wallet Backup Dialog State
    var showBackupDialog by remember { mutableStateOf(false) }
    var generatedWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var backupConfirmed by remember { mutableStateOf(false) }

    // Restore Fields
    var mnemonicInput by remember { mutableStateOf("") }
    var privateKeyInput by remember { mutableStateOf("") }
    var walletNameInput by remember { mutableStateOf("My Betium Wallet") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("onboarding_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Futuristic Hero Logo
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
                    .border(2.dp, primaryColor.copy(alpha = 0.6f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "B",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryColor,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "BETIUM",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Decentralized Wallet & Light Node Miner",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            if (!isRestoreMode) {
                // Initial Choice Cards
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_wallet_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(primaryColor.copy(alpha = 0.5f), primaryColor.copy(alpha = 0.1f))
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = primaryColor)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Create New Wallet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Generate secp256k1 keys & 12-word seed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                val (_, words) = viewModel.createNewWallet("Betium Main Wallet")
                                generatedWords = words
                                backupConfirmed = false
                                showBackupDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_create_new_wallet"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text(
                                text = "Create Wallet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restore_wallet_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = primaryColor)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Restore Wallet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Import using seed phrase or private key",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedButton(
                            onClick = { isRestoreMode = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_go_to_restore"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Import Existing Wallet",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = primaryColor
                            )
                        }
                    }
                }
            } else {
                // Restore Mode
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restore_form_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        TabRow(
                            selectedTabIndex = restoreTab,
                            containerColor = Color.Transparent,
                            contentColor = primaryColor,
                            indicator = { tabPositions ->
                                SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[restoreTab]),
                                    color = primaryColor
                                )
                            }
                        ) {
                            Tab(
                                selected = restoreTab == 0,
                                onClick = { restoreTab = 0 },
                                text = { Text("12-Word Seed", fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = restoreTab == 1,
                                onClick = { restoreTab = 1 },
                                text = { Text("Private Key (Hex)", fontWeight = FontWeight.Bold) }
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (restoreTab == 0) {
                            Text(
                                text = "Enter your 12-word recovery phrase separated by spaces:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = mnemonicInput,
                                onValueChange = { mnemonicInput = it; errorMessage = null },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_mnemonic"),
                                minLines = 3,
                                maxLines = 4,
                                placeholder = { Text("abandon ability able about above absent absorb abstract absurd abuse access accident") },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        clipboard.getText()?.text?.let { mnemonicInput = it }
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Paste")
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            Text(
                                text = "Enter 64-character secp256k1 hex private key:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = privateKeyInput,
                                onValueChange = { privateKeyInput = it; errorMessage = null },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_private_key"),
                                minLines = 2,
                                maxLines = 3,
                                placeholder = { Text("e.g. 88b02c68889cd9bdff3ae6f4e47ff123886781d1a1dd76f0bb...") },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        clipboard.getText()?.text?.let { privateKeyInput = it }
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Paste")
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (restoreTab == 0) {
                                    val words = mnemonicInput.trim().split("\\s+".toRegex())
                                    val result = viewModel.restoreFromMnemonic(words)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Wallet restored successfully!", Toast.LENGTH_SHORT).show()
                                        onWalletReady()
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to restore wallet"
                                    }
                                } else {
                                    val cleanHex = privateKeyInput.trim()
                                    val result = viewModel.restoreFromPrivateKey(cleanHex)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Wallet imported successfully!", Toast.LENGTH_SHORT).show()
                                        onWalletReady()
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Invalid private key"
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_submit_restore"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("Confirm & Restore Wallet", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { isRestoreMode = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Back")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Security Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = primaryColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Hardware-backed Android Keystore Encryption",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Backup Mnemonic Dialog
    if (showBackupDialog && generatedWords.isNotEmpty()) {
        Dialog(onDismissRequest = { /* Force acknowledge */ }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backup_mnemonic_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = primaryColor)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Backup Recovery Phrase",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Write down these 12 words in order and store them in a secure location. Anyone with these words can access your funds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 12 Words Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(generatedWords) { index, word ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}. $word",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(generatedWords.joinToString(" ")))
                            Toast.makeText(context, "Mnemonic copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy All Words", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { backupConfirmed = !backupConfirmed }
                    ) {
                        Checkbox(
                            checked = backupConfirmed,
                            onCheckedChange = { backupConfirmed = it },
                            colors = CheckboxDefaults.colors(checkedColor = primaryColor),
                            modifier = Modifier.testTag("cb_backup_confirmed")
                        )
                        Text(
                            text = "I have saved my 12 recovery words securely offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            showBackupDialog = false
                            onWalletReady()
                        },
                        enabled = backupConfirmed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_finish_backup"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Access Betium Wallet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
