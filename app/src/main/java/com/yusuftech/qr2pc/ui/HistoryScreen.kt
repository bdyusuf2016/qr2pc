package com.yusuftech.qr2pc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yusuftech.qr2pc.data.AppDatabase
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val scanHistory by database.scanHistoryDao().getAllHistory().collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize()) {
        if (scanHistory.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        scope.launch { database.scanHistoryDao().deleteAll() }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(com.yusuftech.qr2pc.R.string.history_clear_all))
                }
            }
        }

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
            showTitle = false
        )
    }
}
