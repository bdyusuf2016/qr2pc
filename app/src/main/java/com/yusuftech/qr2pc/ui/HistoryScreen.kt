package com.yusuftech.qr2pc.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusuftech.qr2pc.data.AppDatabase
import com.yusuftech.qr2pc.data.FirebaseManager
import com.yusuftech.qr2pc.data.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val scanHistory by database.scanHistoryDao().getAllHistory().collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Scans",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (scanHistory.isNotEmpty()) {
                TextButton(
                    onClick = {
                        scope.launch { database.scanHistoryDao().deleteAll() }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(com.yusuftech.qr2pc.R.string.history_clear_all))
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))

        HistoryList(
            history = scanHistory,
            onClearAll = {
                scope.launch { database.scanHistoryDao().deleteAll() }
            },
            onToggleFavorite = { item ->
                scope.launch {
                    database.scanHistoryDao().update(item.copy(isFavorite = !item.isFavorite))
                }
            },
            onResend = { item ->
                scope.launch {
                    val prefs = PreferencesManager(context)
                    val firebase = FirebaseManager()
                    val pairingId = prefs.pairingId.first()
                    val splitChar = prefs.splitCharacter.first()
                    val sound = prefs.soundEnabled.first()
                    val vib = prefs.vibrationEnabled.first()
                    
                    handleScanResult(
                        item.content,
                        item.type,
                        scope,
                        firebase,
                        pairingId,
                        database,
                        "None", // No extra processing on resend
                        "",
                        splitChar,
                        sound,
                        vib,
                        false,
                        context
                    ) { processing ->
                        if (!processing) {
                            Toast.makeText(context, "Resent to PC", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            showTitle = false
        )
    }
}
