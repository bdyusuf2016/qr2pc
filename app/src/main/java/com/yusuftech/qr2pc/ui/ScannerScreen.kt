package com.yusuftech.qr2pc.ui

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yusuftech.qr2pc.data.AppDatabase
import com.yusuftech.qr2pc.data.FirebaseManager
import com.yusuftech.qr2pc.data.PreferencesManager
import com.yusuftech.qr2pc.data.ScanHistory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    val preferencesManager = remember { PreferencesManager(context) }
    val allowDuplicates by preferencesManager.allowDuplicates.collectAsState(initial = true)
    val soundEnabled by preferencesManager.soundEnabled.collectAsState(initial = true)
    val vibrationEnabled by preferencesManager.vibrationEnabled.collectAsState(initial = true)
    val splitCharacter by preferencesManager.splitCharacter.collectAsState(initial = "Enter")
    val pairingId by preferencesManager.pairingId.collectAsState(initial = "0000")
    val dataProcessingMode by preferencesManager.dataProcessingMode.collectAsState(initial = "None")
    val dataProcessingValue by preferencesManager.dataProcessingValue.collectAsState(initial = "")
    val batchMode by preferencesManager.batchMode.collectAsState(initial = false)
    val savedScanMode by preferencesManager.scanMode.collectAsState(initial = "QR")

    // Use rememberUpdatedState so the Camera Analyzer closure always sees the LATEST values
    val currentPairingId by rememberUpdatedState(pairingId)
    val currentBatchMode by rememberUpdatedState(batchMode)
    val currentAllowDuplicates by rememberUpdatedState(allowDuplicates)
    val currentDataProcessingMode by rememberUpdatedState(dataProcessingMode)
    val currentDataProcessingValue by rememberUpdatedState(dataProcessingValue)
    val currentSplitCharacter by rememberUpdatedState(splitCharacter)
    val currentSoundEnabled by rememberUpdatedState(soundEnabled)
    val currentVibrationEnabled by rememberUpdatedState(vibrationEnabled)

    var scanMode by remember(savedScanMode) { mutableStateOf(savedScanMode) }
    var liveText by remember { mutableStateOf("") }
    var detectedText by remember { mutableStateOf("") }
    var isCaptured by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableFloatStateOf(0f) }
    
    val database = remember { AppDatabase.getDatabase(context) }
    val scanHistory by database.scanHistoryDao().getAllHistory().collectAsState(initial = emptyList())
    val firebaseManager = remember { FirebaseManager() }

    var lastLocalPacketTime by remember { mutableLongStateOf(0L) }
    val serverLastSeen by firebaseManager.getServerLastSeen(pairingId).collectAsState(initial = null)
    
    val pcConnectionStatus by remember {
        derivedStateOf {
            val currentTime = System.currentTimeMillis()
            val isLocalOnline = currentTime - lastLocalPacketTime < 10000L
            val isCloudOnline = serverLastSeen?.let { (currentTime - it) < 20000L } ?: false
            
            when {
                isLocalOnline -> 1
                isCloudOnline -> 2
                else -> 0
            }
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            processImageFromGallery(context, it, scanMode, scope, firebaseManager, pairingId, database, soundEnabled, vibrationEnabled)
        }
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Auto-Discovery (UDP Listener)
    LaunchedEffect(key1 = true) {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val socket = java.net.DatagramSocket(8888)
                socket.broadcast = true
                val buffer = ByteArray(1024)
                
                while (true) {
                    val packet = java.net.DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    
                    try {
                        val json = org.json.JSONObject(message)
                        if (json.getString("type") == "QR2PC_SERVER") {
                            val discoveredPairingId = json.getString("pairingId")
                            val discoveredIp = json.getString("ip")
                            
                            if (pairingId == "0000" || pairingId != discoveredPairingId) {
                                scope.launch {
                                    preferencesManager.setPairingId(discoveredPairingId)
                                    preferencesManager.saveServerIp(discoveredIp)
                                    Toast.makeText(context, "Auto-Paired: $discoveredPairingId", Toast.LENGTH_SHORT).show()
                                }
                            }
                            lastLocalPacketTime = System.currentTimeMillis()
                        }
                    } catch (e: Exception) { }
                }
            } catch (e: Exception) {
                Log.e("Discovery", "UDP Listener failed", e)
            }
        }
    }

    // Safety Timeout: If isProcessing stays true for too long, reset it
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            delay(10000) // 10 seconds timeout
            if (isProcessing) {
                Log.w("ScannerScreen", "Safety Timeout: Resetting isProcessing state")
                isProcessing = false
            }
        }
    }

    if (!hasCameraPermission) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("Request Camera Permission")
            }
        }
        return
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    
    // ML Kit Clients remembered to avoid constant re-instantiation
    val barcodeScanner = remember { 
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        ) 
    }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    
    val sheetState = rememberModalBottomSheetState()

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(android.util.Size(1280, 720))
                        .build()
                        .also { imageAnalysis ->
                            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    
                                    if (scanMode == "QR") {
                                        barcodeScanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                if (isProcessing || barcodes.isEmpty()) return@addOnSuccessListener
                                                
                                                val code = barcodes[0].rawValue ?: return@addOnSuccessListener
                                                Log.d("ScannerScreen", "QR Detected: $code")
                                                
                                                if (!currentAllowDuplicates && code == lastScannedCode) {
                                                    return@addOnSuccessListener
                                                }
                                                
                                                handleScanResult(
                                                    code = code,
                                                    type = "QR",
                                                    scope = scope,
                                                    firebaseManager = firebaseManager,
                                                    pairingId = currentPairingId,
                                                    database = database,
                                                    dataProcessingMode = currentDataProcessingMode,
                                                    dataProcessingValue = currentDataProcessingValue,
                                                    splitCharacter = currentSplitCharacter,
                                                    soundEnabled = currentSoundEnabled,
                                                    vibrationEnabled = currentVibrationEnabled,
                                                    batchMode = currentBatchMode,
                                                    context = context
                                                ) { processing ->
                                                    isProcessing = processing
                                                    if (!processing) {
                                                        lastScannedCode = code
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("ScannerScreen", "Barcode detection failed", e)
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        // Text Mode
                                        textRecognizer.process(image)
                                            .addOnSuccessListener { visionText ->
                                                if (visionText.text.isNotEmpty()) {
                                                    if (!isCaptured) {
                                                        liveText = visionText.text
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("ScannerScreen", "Text detection failed", e)
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            analyzer
                        )
                        // Initialize zoom to current state
                        camera?.cameraControl?.setLinearZoom(zoomRatio)
                    } catch (e: Exception) {
                        Log.e("ScannerScreen", "Use case binding failed", e)
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Scanner Overlay (Hole Design)
        if (scanMode == "QR") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Dimmed Background with clear center
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frameSize = 260.dp.toPx()
                    val left = (size.width - frameSize) / 2
                    val top = (size.height - frameSize) / 2
                    val rect = Rect(left, top, left + frameSize, top + frameSize)
                    
                    // Draw outer dimming
                    drawPath(
                        Path().apply {
                            addRect(Rect(0f, 0f, size.width, size.height))
                            addRoundRect(androidx.compose.ui.geometry.RoundRect(rect, cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())))
                            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                        },
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                }

                Box(
                    modifier = Modifier.size(260.dp)
                ) {
                    // Static Laser Line (Middle)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val midY = size.height / 2
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Red.copy(alpha = 0f), Color.Red, Color.Red.copy(alpha = 0f)),
                                startY = midY - 15,
                                endY = midY + 15
                            ),
                            start = Offset(20f, midY),
                            end = Offset(size.width - 20f, midY),
                            strokeWidth = 3f
                        )
                    }

                    // Four Corners
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 8f
                        val cornerLen = 40f
                        val color = Color.White
                        
                        // Top Left
                        drawLine(color, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth)
                        drawLine(color, Offset(0f, 0f), Offset(0f, cornerLen), strokeWidth)
                        
                        // Top Right
                        drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLen, 0f), strokeWidth)
                        drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLen), strokeWidth)
                        
                        // Bottom Left
                        drawLine(color, Offset(0f, size.height), Offset(cornerLen, size.height), strokeWidth)
                        drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLen), strokeWidth)
                        
                        // Bottom Right
                        drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLen, size.height), strokeWidth)
                        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLen), strokeWidth)
                        
                        // Thin connecting lines
                        val thinWidth = 1f
                        drawRect(color.copy(alpha = 0.3f), Offset(0f, 0f), size, style = androidx.compose.ui.graphics.drawscope.Stroke(thinWidth))
                    }
                }
            }
        }

        // Top Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ID: $pairingId",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        when (pcConnectionStatus) {
                                            1 -> Color.Green
                                            2 -> Color.Yellow
                                            else -> Color.Red
                                        },
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when(pcConnectionStatus) {
                                    1 -> stringResource(com.yusuftech.qr2pc.R.string.status_connected_local)
                                    2 -> stringResource(com.yusuftech.qr2pc.R.string.status_connected_cloud)
                                    else -> stringResource(com.yusuftech.qr2pc.R.string.status_disconnected)
                                },
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Duplicate Toggle
                        Surface(
                            onClick = {
                                scope.launch { preferencesManager.setAllowDuplicates(!allowDuplicates) }
                            },
                            color = if (allowDuplicates) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (allowDuplicates) Icons.Default.RepeatOn else Icons.Default.Repeat,
                                    contentDescription = "Toggle Duplicates",
                                    tint = if (allowDuplicates) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        // Batch Mode Toggle
                        Surface(
                            onClick = {
                                scope.launch { preferencesManager.setBatchMode(!batchMode) }
                            },
                            color = if (batchMode) Color.Yellow.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp).padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (batchMode) Icons.Default.Bolt else Icons.Default.FlashOff,
                                    contentDescription = null,
                                    tint = if (batchMode) Color.Yellow else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (batchMode) stringResource(com.yusuftech.qr2pc.R.string.batch_on) else stringResource(com.yusuftech.qr2pc.R.string.batch_off),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
            
            // Mode Switcher
            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                    .padding(4.dp)
            ) {
                ModeButton(stringResource(com.yusuftech.qr2pc.R.string.mode_qr_barcode), scanMode == "QR") {
                    scanMode = "QR"
                    detectedText = ""
                    liveText = ""
                    isCaptured = false
                    scope.launch { preferencesManager.setScanMode("QR") }
                }
                ModeButton(stringResource(com.yusuftech.qr2pc.R.string.mode_text_ocr), scanMode == "TEXT") {
                    scanMode = "TEXT"
                    scope.launch { preferencesManager.setScanMode("TEXT") }
                }
            }
        }

        // Live Text Preview (Overlay when not captured)
        AnimatedVisibility(
            visible = scanMode == "TEXT" && !isCaptured && liveText.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (liveText.isEmpty()) stringResource(com.yusuftech.qr2pc.R.string.live_text_hint) else (liveText.take(100) + if (liveText.length > 100) "..." else ""),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Capture Button (Shutter)
        if (scanMode == "TEXT" && !isCaptured) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .clickable {
                            if (liveText.isNotEmpty()) {
                                detectedText = liveText
                                isCaptured = true
                                provideFeedback(context, scope, false, vibrationEnabled) // Soft haptic
                            }
                        },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(4.dp, Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }

        // Manual Text Preview (Bottom Overlay - Captured)
        AnimatedVisibility(
            visible = scanMode == "TEXT" && isCaptured,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            isCaptured = false 
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retake", tint = Color.White)
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(com.yusuftech.qr2pc.R.string.captured_text),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = detectedText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            handleScanResult(
                                code = detectedText,
                                type = "TEXT",
                                scope = scope,
                                firebaseManager = firebaseManager,
                                pairingId = currentPairingId,
                                database = database,
                                dataProcessingMode = "None",
                                dataProcessingValue = "",
                                splitCharacter = currentSplitCharacter,
                                soundEnabled = currentSoundEnabled,
                                vibrationEnabled = currentVibrationEnabled,
                                batchMode = false,
                                context = context
                            ) {
                                isProcessing = it
                                if (!it) {
                                    isCaptured = false
                                    detectedText = ""
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send to PC", tint = Color.White)
                    }
                }
            }
        }

        // Zoom Control Slider
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp, start = 48.dp, end = 48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Slider(
                    value = zoomRatio,
                    onValueChange = { 
                        zoomRatio = it
                        camera?.cameraControl?.setLinearZoom(it)
                    },
                    modifier = Modifier.weight(1f).height(32.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // Bottom Actions
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                    )
                )
                .padding(bottom = 32.dp, top = 16.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                        Text(stringResource(com.yusuftech.qr2pc.R.string.btn_gallery), style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FloatingActionButton(
                        onClick = { showHistory = true },
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, contentDescription = "History")
                            Text(stringResource(com.yusuftech.qr2pc.R.string.btn_history), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            val nextState = !isFlashOn
                            camera?.cameraControl?.enableTorch(nextState)
                            isFlashOn = nextState
                        },
                        containerColor = if (isFlashOn) Color.Yellow else Color.White.copy(alpha = 0.2f),
                        contentColor = if (isFlashOn) Color.Black else Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle Flash"
                            )
                            Text(stringResource(com.yusuftech.qr2pc.R.string.btn_flash), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = isProcessing,
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator(color = Color.White)
        }

        if (showHistory) {
            ModalBottomSheet(
                onDismissRequest = { showHistory = false },
                sheetState = sheetState
            ) {
                HistoryList(
                    history = scanHistory,
                    onClearAll = {
                        scope.launch { database.scanHistoryDao().deleteAll() }
                    },
                    onToggleFavorite = { item ->
                        scope.launch {
                            database.scanHistoryDao().update(item.copy(isFavorite = !item.isFavorite))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ModeButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = CircleShape,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

fun handleScanResult(
    code: String,
    type: String,
    scope: kotlinx.coroutines.CoroutineScope,
    firebaseManager: FirebaseManager,
    pairingId: String,
    database: AppDatabase,
    dataProcessingMode: String,
    dataProcessingValue: String,
    splitCharacter: String,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    batchMode: Boolean,
    context: Context,
    onProcessingStateChange: (Boolean) -> Unit
) {
    Log.d("ScannerScreen", "Handling Scan Result: ${code.take(10)} (Type: $type)")
    scope.launch {
        onProcessingStateChange(true)
        
        var processedCode = code
        try {
            if (type == "QR") {
                when (dataProcessingMode) {
                    "First N" -> {
                        val n = dataProcessingValue.toIntOrNull() ?: code.length
                        processedCode = code.take(n)
                    }
                    "Last N" -> {
                        val n = dataProcessingValue.toIntOrNull() ?: code.length
                        processedCode = code.takeLast(n)
                    }
                    "Regex" -> {
                        if (dataProcessingValue.isNotEmpty()) {
                            val regex = Regex(dataProcessingValue)
                            processedCode = regex.find(code)?.value ?: code
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ScannerScreen", "Data processing failed", e)
        }

        val finalData = when (splitCharacter) {
            "Enter" -> "$processedCode\n"
            "Tab" -> "$processedCode\t"
            "Space" -> "$processedCode "
            else -> processedCode
        }

        firebaseManager.sendScanResult(pairingId, finalData) { success ->
            scope.launch {
                if (success) {
                    Log.d("ScannerScreen", "Data sent successfully to Firebase")
                    provideFeedback(context, scope, soundEnabled, vibrationEnabled)
                    Toast.makeText(context, "Sent: ${processedCode.take(15)}...", Toast.LENGTH_SHORT).show()
                    database.scanHistoryDao().insert(ScanHistory(content = processedCode, type = type))
                } else {
                    Log.e("ScannerScreen", "Failed to send data to Firebase")
                    Toast.makeText(context, "Send Failed", Toast.LENGTH_SHORT).show()
                }
                
                // Add a small safety delay to prevent double scans even in batch mode
                if (!batchMode) {
                    delay(2000)
                } else {
                    delay(500)
                }
                onProcessingStateChange(false)
            }
        }
    }
}

fun processImageFromGallery(
    context: Context,
    uri: Uri,
    mode: String,
    scope: kotlinx.coroutines.CoroutineScope,
    firebaseManager: FirebaseManager,
    pairingId: String,
    database: AppDatabase,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean
) {
    try {
        val bitmap = if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        }
        
        val image = InputImage.fromBitmap(bitmap, 0)
        
        if (mode == "QR") {
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val code = barcodes[0].rawValue ?: ""
                        handleScanResult(code, "QR", scope, firebaseManager, pairingId, database, "None", "", "None", soundEnabled, vibrationEnabled, false, context) {}
                    } else {
                        Toast.makeText(context, "No QR code found", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (visionText.text.isNotEmpty()) {
                        handleScanResult(visionText.text, "TEXT", scope, firebaseManager, pairingId, database, "None", "", "None", soundEnabled, vibrationEnabled, false, context) {}
                    } else {
                        Toast.makeText(context, "No text found", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    } catch (e: Exception) {
        Log.e("GalleryScan", "Error processing image", e)
        Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun HistoryList(
    history: List<ScanHistory>,
    onClearAll: () -> Unit,
    onToggleFavorite: (ScanHistory) -> Unit,
    showTitle: Boolean = true
) {
    var filterFavorites by remember { mutableStateOf(false) }
    val filteredHistory = if (filterFavorites) history.filter { it.isFavorite } else history

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(if (showTitle) 16.dp else 0.dp)
    ) {
        if (showTitle) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(com.yusuftech.qr2pc.R.string.history_title), style = MaterialTheme.typography.titleLarge)
                Row {
                    IconButton(onClick = { filterFavorites = !filterFavorites }) {
                        Icon(
                            imageVector = if (filterFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Filter Favorites",
                            tint = if (filterFavorites) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(onClick = onClearAll) {
                        Text(stringResource(com.yusuftech.qr2pc.R.string.history_clear_all))
                    }
                }
            }
        }
        
        if (filteredHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (filterFavorites) stringResource(com.yusuftech.qr2pc.R.string.history_empty) else stringResource(com.yusuftech.qr2pc.R.string.history_empty),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredHistory) { item ->
                    ListItem(
                        leadingContent = {
                            Icon(
                                imageVector = if (item.type == "QR") Icons.Default.QrCode else Icons.Default.TextFields,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        headlineContent = { Text(item.content, maxLines = 2) },
                        supportingContent = {
                            Text(SimpleDateFormat("HH:mm:ss, dd MMM", Locale.getDefault()).format(Date(item.timestamp)))
                        },
                        trailingContent = {
                            IconButton(onClick = { onToggleFavorite(item) }) {
                                Icon(
                                    imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (item.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

fun provideFeedback(context: Context, scope: kotlinx.coroutines.CoroutineScope, soundEnabled: Boolean, vibrationEnabled: Boolean) {
    if (soundEnabled) {
        scope.launch {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                delay(200)
                toneGen.release()
            } catch (e: Exception) {
                Log.e("Feedback", "Tone failed", e)
            }
        }
    }
    if (vibrationEnabled) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            }
        } catch (e: Exception) {
            Log.e("Feedback", "Vibration failed", e)
        }
    }
}
