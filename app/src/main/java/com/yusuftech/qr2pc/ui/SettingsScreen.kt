package com.yusuftech.qr2pc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusuftech.qr2pc.data.PreferencesManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    
    val serverIp by preferencesManager.serverIp.collectAsState(initial = "")
    val allowDuplicates by preferencesManager.allowDuplicates.collectAsState(initial = true)
    val soundEnabled by preferencesManager.soundEnabled.collectAsState(initial = true)
    val vibrationEnabled by preferencesManager.vibrationEnabled.collectAsState(initial = true)
    val splitCharacter by preferencesManager.splitCharacter.collectAsState(initial = "Enter")
    val pairingId by preferencesManager.pairingId.collectAsState(initial = "0000")
    val dataProcessingMode by preferencesManager.dataProcessingMode.collectAsState(initial = "None")
    val dataProcessingValue by preferencesManager.dataProcessingValue.collectAsState(initial = "")
    val batchMode by preferencesManager.batchMode.collectAsState(initial = false)

    var ipInput by remember(serverIp) { mutableStateOf(serverIp ?: "") }
    var pairingIdInput by remember(pairingId) { mutableStateOf(pairingId) }
    var processingValueInput by remember(dataProcessingValue) { mutableStateOf(dataProcessingValue) }
    var showSplitMenu by remember { mutableStateOf(false) }
    var showProcessingMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Section: Connection
        SettingsSection(
            icon = Icons.Default.WifiTethering,
            title = stringResource(com.yusuftech.qr2pc.R.string.conn_settings)
        ) {
            OutlinedTextField(
                value = pairingIdInput,
                onValueChange = { pairingIdInput = it },
                label = { Text(stringResource(com.yusuftech.qr2pc.R.string.pairing_id_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    scope.launch {
                        preferencesManager.setPairingId(pairingIdInput)
                        onBack()
                    }
                },
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(com.yusuftech.qr2pc.R.string.btn_save))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ipInput,
                onValueChange = { ipInput = it },
                label = { Text(stringResource(com.yusuftech.qr2pc.R.string.server_ip_label)) },
                placeholder = { Text("e.g. 192.168.1.5") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    scope.launch {
                        preferencesManager.saveServerIp(ipInput)
                        onBack()
                    }
                },
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(com.yusuftech.qr2pc.R.string.btn_save))
            }
        }

        // Section: Scan
        SettingsSection(
            icon = Icons.Default.SettingsInputComponent,
            title = stringResource(com.yusuftech.qr2pc.R.string.scan_settings)
        ) {
            SettingsSwitch(
                label = stringResource(com.yusuftech.qr2pc.R.string.setting_duplicates),
                description = stringResource(com.yusuftech.qr2pc.R.string.setting_duplicates_desc),
                checked = allowDuplicates,
                onCheckedChange = { scope.launch { preferencesManager.setAllowDuplicates(it) } }
            )

            SettingsSwitch(
                label = stringResource(com.yusuftech.qr2pc.R.string.setting_sound),
                description = stringResource(com.yusuftech.qr2pc.R.string.setting_sound_desc),
                checked = soundEnabled,
                onCheckedChange = { scope.launch { preferencesManager.setSoundEnabled(it) } }
            )

            SettingsSwitch(
                label = stringResource(com.yusuftech.qr2pc.R.string.setting_vibration),
                description = stringResource(com.yusuftech.qr2pc.R.string.setting_vibration_desc),
                checked = vibrationEnabled,
                onCheckedChange = { scope.launch { preferencesManager.setVibrationEnabled(it) } }
            )

            SettingsSwitch(
                label = stringResource(com.yusuftech.qr2pc.R.string.setting_batch),
                description = stringResource(com.yusuftech.qr2pc.R.string.setting_batch_desc),
                checked = batchMode,
                onCheckedChange = { scope.launch { preferencesManager.setBatchMode(it) } }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box {
                OutlinedTextField(
                    value = splitCharacter,
                    onValueChange = { },
                    label = { Text(stringResource(com.yusuftech.qr2pc.R.string.setting_split)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { showSplitMenu = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                )
                DropdownMenu(
                    expanded = showSplitMenu,
                    onDismissRequest = { showSplitMenu = false },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    listOf("None", "Enter", "Tab", "Space").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                scope.launch { preferencesManager.setSplitCharacter(option) }
                                showSplitMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Section: Data Processing
        SettingsSection(
            icon = Icons.Default.DataArray,
            title = stringResource(com.yusuftech.qr2pc.R.string.data_processing)
        ) {
            Box {
                OutlinedTextField(
                    value = dataProcessingMode,
                    onValueChange = { },
                    label = { Text("Processing Mode") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { showProcessingMenu = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                )
                DropdownMenu(
                    expanded = showProcessingMenu,
                    onDismissRequest = { showProcessingMenu = false },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    listOf("None", "First N", "Last N", "Regex").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                scope.launch { preferencesManager.setDataProcessingMode(option) }
                                showProcessingMenu = false
                            }
                        )
                    }
                }
            }

            if (dataProcessingMode != "None") {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = processingValueInput,
                    onValueChange = { processingValueInput = it },
                    label = {
                        Text(
                            when (dataProcessingMode) {
                                "First N", "Last N" -> "Number of characters (N)"
                                "Regex" -> "Regex Pattern"
                                else -> ""
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = {
                        scope.launch { 
                            preferencesManager.setDataProcessingValue(processingValueInput)
                            onBack()
                        }
                    },
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(com.yusuftech.qr2pc.R.string.btn_save))
                }
            }
        }

        // Security Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Ensure your PC app is running and showing a green status light.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SettingsSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun SettingsSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
