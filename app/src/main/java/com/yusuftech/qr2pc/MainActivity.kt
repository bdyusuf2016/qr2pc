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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusuftech.qr2pc.data.PreferencesManager
import com.yusuftech.qr2pc.data.AppDatabase
import com.yusuftech.qr2pc.data.FirebaseManager
import com.yusuftech.qr2pc.ui.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { PreferencesManager(context) }
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
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1A1A1A),
                drawerContentColor = Color.White
            ) {
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = stringResource(R.string.version_label),
                    modifier = Modifier.padding(start = 20.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                
                DrawerItem(stringResource(R.string.menu_scanner), Icons.Default.QrCodeScanner, currentScreen == Screen.Scanner) {
                    currentScreen = Screen.Scanner
                    scope.launch { drawerState.close() }
                }
                
                DrawerItem(stringResource(R.string.menu_history), Icons.Default.History, currentScreen == Screen.History) {
                    currentScreen = Screen.History
                    scope.launch { drawerState.close() }
                }

                DrawerItem("Linked PCs", Icons.Default.Devices, currentScreen == Screen.Devices) {
                    currentScreen = Screen.Devices
                    scope.launch { drawerState.close() }
                }

                DrawerItem(stringResource(R.string.menu_settings), Icons.Default.Settings, currentScreen == Screen.Settings) {
                    currentScreen = Screen.Settings
                    scope.launch { drawerState.close() }
                }

                HorizontalDivider(modifier = Modifier.padding(16.dp), color = Color.White.copy(0.1f))

                DrawerItem(stringResource(R.string.menu_about), Icons.Default.Info, currentScreen == Screen.About) {
                    currentScreen = Screen.About
                    scope.launch { drawerState.close() }
                }

                DrawerItem(stringResource(R.string.menu_help), Icons.AutoMirrored.Filled.Help, currentScreen == Screen.Help) {
                    currentScreen = Screen.Help
                    scope.launch { drawerState.close() }
                }

                DrawerItem(stringResource(R.string.menu_share), Icons.Default.Share, false) {
                    scope.launch { drawerState.close() }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Use QR 2 PC by Yusuf for lightning fast data entry: https://github.com/bdyusuf2016/qr2pc-android")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }
                
                Spacer(Modifier.weight(1f))
                
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LanguageButton(label = "EN", isSelected = AppCompatDelegate.getApplicationLocales().toLanguageTags() == "en") {
                        val tags = LocaleListCompat.forLanguageTags("en")
                        if (AppCompatDelegate.getApplicationLocales() != tags) {
                            AppCompatDelegate.setApplicationLocales(tags)
                            scope.launch { prefs.setAppLanguage("en") }
                        }
                    }
                    LanguageButton(label = "BN", isSelected = AppCompatDelegate.getApplicationLocales().toLanguageTags() == "bn") {
                        val tags = LocaleListCompat.forLanguageTags("bn")
                        if (AppCompatDelegate.getApplicationLocales() != tags) {
                            AppCompatDelegate.setApplicationLocales(tags)
                            scope.launch { prefs.setAppLanguage("bn") }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (currentScreen != Screen.Scanner) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = when (currentScreen) {
                                    Screen.Settings -> stringResource(R.string.menu_settings)
                                    Screen.History -> stringResource(R.string.menu_history)
                                    Screen.Help -> stringResource(R.string.menu_help)
                                    Screen.About -> "Md. Yusuf Ali"
                                    Screen.Devices -> "Linked Devices"
                                    else -> ""
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, "Menu")
                            }
                        },
                        actions = {
                            var showMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, "Actions")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Logout Current PC") },
                                    onClick = { 
                                        showMenu = false
                                        scope.launch {
                                            val currentId = prefs.pairingId.first()
                                            val firebase = FirebaseManager()
                                            firebase.unlinkDevice(currentId)
                                            prefs.removeLinkedDevice(currentId)
                                            prefs.setPairingId("0000")
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Log History") },
                                    onClick = { 
                                        showMenu = false
                                        val db = AppDatabase.getDatabase(context)
                                        scope.launch { db.scanHistoryDao().deleteAll() }
                                    }
                                )
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                val contentModifier = if (currentScreen == Screen.Scanner) Modifier.fillMaxSize() else Modifier.fillMaxSize().padding(innerPadding)
                
                Box(modifier = contentModifier) {
                    when (currentScreen) {
                        Screen.Scanner -> ScannerScreen(onMenuClick = { scope.launch { drawerState.open() } })
                        Screen.Settings -> SettingsScreen(onBack = { currentScreen = Screen.Scanner })
                        Screen.History -> HistoryScreen()
                        Screen.Help -> HelpScreen()
                        Screen.About -> AboutScreen()
                        Screen.Devices -> LinkedDevicesScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, null) },
        label = { Text(label, fontWeight = if(selected) FontWeight.Bold else FontWeight.Normal) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedTextColor = Color.White.copy(alpha = 0.8f),
            unselectedIconColor = Color.White.copy(alpha = 0.8f),
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            selectedTextColor = MaterialTheme.colorScheme.primary,
            selectedIconColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun LanguageButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(4.dp),
        border = if(isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

enum class Screen {
    Scanner, Settings, History, Help, About, Devices
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4285F4),
            secondary = Color(0xFFFBBC05),
            background = Color.Black,
            surface = Color(0xFF121212)
        ),
        content = content
    )
}
