package com.yusuftech.qr2pc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(com.yusuftech.qr2pc.R.string.help_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        HelpStep(
            icon = Icons.Default.Computer,
            title = stringResource(com.yusuftech.qr2pc.R.string.help_step1_title),
            description = stringResource(com.yusuftech.qr2pc.R.string.help_step1_desc)
        )

        HelpStep(
            icon = Icons.Default.Wifi,
            title = stringResource(com.yusuftech.qr2pc.R.string.help_step2_title),
            description = stringResource(com.yusuftech.qr2pc.R.string.help_step2_desc)
        )

        HelpStep(
            icon = Icons.AutoMirrored.Filled.Login,
            title = stringResource(com.yusuftech.qr2pc.R.string.pairing_id_label),
            description = "Check app settings."
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { uriHandler.openUri("https://github.com/bdyusuf2016/qr2pc-android") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(com.yusuftech.qr2pc.R.string.help_download_btn))
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pro Tip:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Use 'Batch Mode' for rapid scanning of multiple items without delays!",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun HelpStep(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
