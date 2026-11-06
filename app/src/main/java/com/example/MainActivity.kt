package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.miner.MinerManager
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MinerScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ReceiveScreen
import com.example.ui.screens.SendScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BetiumTheme
import com.example.viewmodel.WalletViewModel

enum class Screen {
    ONBOARDING,
    DASHBOARD,
    SEND,
    RECEIVE,
    MINER,
    SETTINGS
}

data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val walletViewModel: WalletViewModel = viewModel()
            val userPrefs by walletViewModel.userPreferences.collectAsState()
            val uiState by walletViewModel.uiState.collectAsState()
            val minerState by MinerManager.minerState.collectAsState()

            // Request Notification permission for Android 13+ foreground miner
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* Granted or denied handled gracefully */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            BetiumTheme(
                themeMode = userPrefs.themeMode,
                accentColorHex = userPrefs.accentColorHex,
                isOledDark = userPrefs.isOledDark
            ) {
                var currentScreen by remember {
                    mutableStateOf(if (uiState.wallet != null) Screen.DASHBOARD else Screen.ONBOARDING)
                }
                var sendTokenId by remember { mutableStateOf<String?>(null) }

                // Synchronize if wallet wiped or restored
                LaunchedEffect(uiState.wallet) {
                    if (uiState.wallet == null) {
                        currentScreen = Screen.ONBOARDING
                    } else if (currentScreen == Screen.ONBOARDING) {
                        currentScreen = Screen.DASHBOARD
                    }
                }

                val navItems = listOf(
                    NavItem(Screen.DASHBOARD, "Wallet", Icons.Default.AccountBalanceWallet),
                    NavItem(Screen.SEND, "Send", Icons.AutoMirrored.Filled.Send),
                    NavItem(Screen.RECEIVE, "Receive", Icons.Default.CallReceived),
                    NavItem(Screen.MINER, "Miner", Icons.Default.Construction),
                    NavItem(Screen.SETTINGS, "Settings", Icons.Default.Settings)
                )

                val showBottomBar = currentScreen != Screen.ONBOARDING

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.testTag("main_bottom_nav")
                            ) {
                                navItems.forEach { item ->
                                    val isSelected = currentScreen == item.screen
                                    val isMinerActive = item.screen == Screen.MINER && minerState.isRunning

                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (item.screen == Screen.SEND) {
                                                sendTokenId = null
                                            }
                                            currentScreen = item.screen
                                        },
                                        icon = {
                                            BadgedBox(
                                                badge = {
                                                    if (isMinerActive) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFF10B981))
                                                        )
                                                    }
                                                }
                                            ) {
                                                Icon(item.icon, contentDescription = item.label)
                                            }
                                        },
                                        label = { Text(item.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag("nav_${item.label.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp),
                        label = "screen_transition"
                    ) { targetScreen ->
                        when (targetScreen) {
                            Screen.ONBOARDING -> {
                                OnboardingScreen(
                                    viewModel = walletViewModel,
                                    onWalletReady = { currentScreen = Screen.DASHBOARD }
                                )
                            }
                            Screen.DASHBOARD -> {
                                DashboardScreen(
                                    viewModel = walletViewModel,
                                    onNavigateToSend = { tokenId ->
                                        sendTokenId = tokenId
                                        currentScreen = Screen.SEND
                                    },
                                    onNavigateToReceive = { currentScreen = Screen.RECEIVE },
                                    onNavigateToMiner = { currentScreen = Screen.MINER },
                                    onNavigateToSettings = { currentScreen = Screen.SETTINGS }
                                )
                            }
                            Screen.SEND -> {
                                SendScreen(
                                    viewModel = walletViewModel,
                                    initialTokenId = sendTokenId,
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )
                            }
                            Screen.RECEIVE -> {
                                ReceiveScreen(
                                    viewModel = walletViewModel,
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )
                            }
                            Screen.MINER -> {
                                MinerScreen(
                                    viewModel = walletViewModel,
                                    onBack = { currentScreen = Screen.DASHBOARD }
                                )
                            }
                            Screen.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = walletViewModel,
                                    onBack = { currentScreen = Screen.DASHBOARD },
                                    onWalletWiped = { currentScreen = Screen.ONBOARDING }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
