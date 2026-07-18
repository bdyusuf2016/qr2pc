package com.yusuftech.qr2pc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yusuftech.qr2pc.data.PreferencesManager
import com.yusuftech.qr2pc.ui.AboutScreen
import com.yusuftech.qr2pc.ui.HelpScreen
import com.yusuftech.qr2pc.ui.HistoryScreen
import com.yusuftech.qr2pc.ui.ScannerScreen
import com.yusuftech.qr2pc.ui.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                MainNavigation()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Scanner) }

    BackHandler(enabled = drawerState.isOpen || currentScreen != Screen.Scanner) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            currentScreen = Screen.Scanner
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("QR to PC", style = MaterialTheme.typography.headlineSmall)
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    label = { Text(stringResource(R.string.menu_scanner)) },
                    selected = currentScreen == Screen.Scanner,
                    onClick = {
                        currentScreen = Screen.Scanner
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.menu_settings)) },
                    selected = currentScreen == Screen.Settings,
                    onClick = {
                        currentScreen = Screen.Settings
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text(stringResource(R.string.menu_history)) },
                    selected = currentScreen == Screen.History,
                    onClick = {
                        currentScreen = Screen.History
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) },
                    label = { Text(stringResource(R.string.menu_help)) },
                    selected = currentScreen == Screen.Help,
                    onClick = {
                        currentScreen = Screen.Help
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text(stringResource(R.string.menu_about)) },
                    selected = currentScreen == Screen.About,
                    onClick = {
                        currentScreen = Screen.About
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                HorizontalDivider()
                
                // Language Selection Row
                val prefs = PreferencesManager(androidx.compose.ui.platform.LocalContext.current)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LanguageButton(label = "English", isSelected = AppCompatDelegate.getApplicationLocales().toLanguageTags() == "en") {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")
                        AppCompatDelegate.setApplicationLocales(appLocale)
                        scope.launch { prefs.setAppLanguage("en") }
                    }
                    LanguageButton(label = "বাংলা", isSelected = AppCompatDelegate.getApplicationLocales().toLanguageTags() == "bn") {
                        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("bn")
                        AppCompatDelegate.setApplicationLocales(appLocale)
                        scope.launch { prefs.setAppLanguage("bn") }
                    }
                }

                HorizontalDivider()
                val context = androidx.compose.ui.platform.LocalContext.current
                val shareText = "Scan QR codes and send results instantly to your PC with QR 2 PC by Yusuf! Download now: https://github.com/bdyusuf2016/qr2pc-android"
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Share, contentDescription = null) },
                    label = { Text(stringResource(R.string.menu_share)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                Screen.Scanner -> stringResource(R.string.menu_scanner)
                                Screen.Settings -> stringResource(R.string.menu_settings)
                                Screen.History -> stringResource(R.string.menu_history)
                                Screen.Help -> stringResource(R.string.menu_help)
                                Screen.About -> stringResource(R.string.menu_about)
                            }
                        )
                    },
                    navigationIcon = {
                        if (currentScreen == Screen.Scanner) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                            }
                        } else {
                            IconButton(onClick = { currentScreen = Screen.Scanner }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Scanner")
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            when (currentScreen) {
                Screen.Scanner -> ScannerScreen(modifier = Modifier.padding(innerPadding))
                Screen.Settings -> SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    onBack = { currentScreen = Screen.Scanner }
                )
                Screen.History -> HistoryScreen(modifier = Modifier.padding(innerPadding))
                Screen.Help -> HelpScreen(modifier = Modifier.padding(innerPadding))
                Screen.About -> AboutScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
fun LanguageButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = androidx.compose.foundation.shape.CircleShape,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

enum class Screen {
    Scanner, Settings, History, Help, About
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(), // Using a dark theme by default
        content = content
    )
}
