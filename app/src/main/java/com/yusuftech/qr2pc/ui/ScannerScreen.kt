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
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yusuftech.qr2pc.data.AppDatabase
import com.yusuftech.qr2pc.data.FirebaseManager
import com.yusuftech.qr2pc.data.PreferencesManager
import com.yusuftech.qr2pc.data.ScanHistory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import androidx.compose.foundation.gestures.detectTapGestures
import org.json.JSONArray
import org.json.JSONObject

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(modifier: Modifier = Modifier, onMenuClick: () -> Unit = {}) {
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

    val currentPairingId by rememberUpdatedState(pairingId)
    val currentBatchMode by rememberUpdatedState(batchMode)
    val currentAllowDuplicates by rememberUpdatedState(allowDuplicates)
    val currentDataProcessingMode by rememberUpdatedState(dataProcessingMode)
    val currentDataProcessingValue by rememberUpdatedState(dataProcessingValue)
    val currentSplitCharacter by rememberUpdatedState(splitCharacter)
    val currentSoundEnabled by rememberUpdatedState(soundEnabled)
    val currentVibrationEnabled by rememberUpdatedState(vibrationEnabled)

    var scanMode by remember(savedScanMode) { mutableStateOf(savedScanMode) }
    var isCaptured by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var detectedLines by remember { mutableStateOf<List<Text.Line>>(emptyList()) }
    var textBuffer by remember { mutableStateOf("") }
    
    var zoomRatio by remember { mutableFloatStateOf(0f) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    
    val database = remember { AppDatabase.getDatabase(context) }
    val scanHistory by database.scanHistoryDao().getAllHistory().collectAsState(initial = emptyList())
    val firebaseManager = remember { FirebaseManager() }
    
    val linkedDevicesJson by preferencesManager.linkedDevices.collectAsState(initial = "[]")
    val activeDevice = remember(linkedDevicesJson, pairingId) {
        try {
            val arr = JSONArray(linkedDevicesJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("id") == pairingId) return@remember obj.getString("name")
            }
        } catch (e: Exception) {}
        "Default PC"
    }

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
    val sheetState = rememberModalBottomSheetState()

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
            processImageFromGallery(context, it, scanMode, scope, firebaseManager, currentPairingId, database, currentSoundEnabled, currentVibrationEnabled)
        }
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            delay(10000)
            if (isProcessing) isProcessing = false
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
    val imageCapture = remember { ImageCapture.Builder().build() }
    var isFlashOn by remember { mutableStateOf(false) }
    
    val barcodeScanner = remember { 
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        ) 
    }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
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
                                                
                                                if (code.startsWith("QR2PC:PAIR:")) {
                                                    handleScanResult(code, "QR", scope, firebaseManager, currentPairingId, database, currentDataProcessingMode, currentDataProcessingValue, currentSplitCharacter, currentSoundEnabled, currentVibrationEnabled, currentBatchMode, context) {
                                                        isProcessing = it
                                                    }
                                                    return@addOnSuccessListener
                                                }

                                                if (!currentAllowDuplicates && code == lastScannedCode) {
                                                    scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                                        Toast.makeText(context, "Duplicate scan blocked", Toast.LENGTH_SHORT).show()
                                                    }
                                                    return@addOnSuccessListener
                                                }
                                                handleScanResult(code, "QR", scope, firebaseManager, currentPairingId, database, currentDataProcessingMode, currentDataProcessingValue, currentSplitCharacter, currentSoundEnabled, currentVibrationEnabled, currentBatchMode, context) {
                                                    isProcessing = it
                                                    if (!it) lastScannedCode = code
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    } else {
                                        imageProxy.close()
                                    }
                                } else { imageProxy.close() }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.Builder().requireLensFacing(lensFacing).build(), preview, analyzer, imageCapture)
                        camera?.cameraControl?.setLinearZoom(zoomRatio)
                    } catch (e: Exception) { Log.e("ScannerScreen", "Bind failed", e) }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Icons (Top)
        if (!isCaptured) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) { Icon(Icons.Default.Menu, null, tint = Color.White) }
                
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { photoPickerLauncher.launch("image/*") }) { Icon(Icons.Default.Photo, null, tint = Color.White) }
                    IconButton(onClick = { 
                        isFlashOn = !isFlashOn
                        camera?.cameraControl?.enableTorch(isFlashOn)
                    }) { Icon(if(isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff, null, tint = Color.White) }
                    IconButton(onClick = { 
                        lensFacing = if(lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK 
                    }) { Icon(Icons.Default.Cameraswitch, null, tint = Color.White) }
                    IconButton(onClick = { scope.launch { preferencesManager.setBatchMode(!batchMode) } }) {
                        Icon(if(batchMode) Icons.Default.FilterNone else Icons.Default.CropFree, null, tint = if(batchMode) Color.Yellow else Color.White)
                    }
                    IconButton(onClick = { showHistory = true }) { Icon(Icons.Default.History, null, tint = Color.White) }
                }
            }
        }

        // Scan Frame (Only in QR mode)
        if (scanMode == "QR" && !isCaptured) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frameSize = 300.dp.toPx()
                    val left = (size.width - frameSize) / 2
                    val top = (size.height - frameSize) / 2
                    val rect = Rect(left, top, left + frameSize, top + frameSize)
                    drawPath(Path().apply {
                        addRect(Rect(0f, 0f, size.width, size.height))
                        addRoundRect(androidx.compose.ui.geometry.RoundRect(rect, cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())))
                        fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                    }, color = Color.Black.copy(alpha = 0.6f))
                }
                Box(modifier = Modifier.size(300.dp)) {
                    val infiniteTransition = rememberInfiniteTransition(label = "laser")
                    val laserAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                        label = "laserAlpha"
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(Color.Red.copy(alpha = laserAlpha), Offset(10f, size.height / 2), Offset(size.width - 10f, size.height / 2), 3f)
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 12f; val len = 40f; val color = Color(0xFF4285F4)
                        drawLine(color, Offset(0f,0f), Offset(len,0f), stroke)
                        drawLine(color, Offset(0f,0f), Offset(0f,len), stroke)
                        drawLine(color, Offset(size.width,0f), Offset(size.width-len,0f), stroke)
                        drawLine(color, Offset(size.width,0f), Offset(size.width,len), stroke)
                        drawLine(color, Offset(0f,size.height), Offset(len,size.height), stroke)
                        drawLine(color, Offset(0f,size.height), Offset(0f,size.height-len), stroke)
                        drawLine(color, Offset(size.width,size.height), Offset(size.width-len,size.height), stroke)
                        drawLine(color, Offset(size.width,size.height), Offset(size.width,size.height-len), stroke)
                    }
                }
            }
        }

        // Zoom Slider (Bottom)
        if (!isCaptured) {
            Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp, start = 48.dp, end = 48.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ZoomOut, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Slider(
                        value = zoomRatio,
                        onValueChange = { zoomRatio = it; camera?.cameraControl?.setLinearZoom(it) },
                        modifier = Modifier.weight(1f).height(20.dp),
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f))
                    )
                    Icon(Icons.Default.ZoomIn, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Shutter Button (TEXT Mode)
        if (scanMode == "TEXT" && !isCaptured) {
            FloatingActionButton(
                onClick = {
                    isProcessing = true
                    imageCapture.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                            val bitmap = imageProxy.toBitmapCustom()
                            val rotated = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
                            imageProxy.close()
                            textRecognizer.process(InputImage.fromBitmap(rotated, 0)).addOnSuccessListener {
                                capturedBitmap = rotated
                                // Granular detection: Extract lines
                                detectedLines = it.textBlocks.flatMap { b -> b.lines }
                                isCaptured = true
                                isProcessing = false
                                provideFeedback(context, scope, false, vibrationEnabled)
                            }
                        }
                        override fun onError(e: ImageCaptureException) { isProcessing = false }
                    })
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 180.dp),
                shape = CircleShape,
                containerColor = Color.White
            ) { Box(modifier = Modifier.size(60.dp).border(4.dp, Color.Gray, CircleShape)) }
        }

        // Mode Selection (Bottom)
        if (!isCaptured) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .padding(4.dp)
            ) {
                ModeButton("QR/BARCODE", scanMode == "QR") { scanMode = "QR"; isCaptured = false }
                ModeButton("TEXT", scanMode == "TEXT") { scanMode = "TEXT"; isCaptured = false }
            }
        }

        // Lens Selection View with Editor
        if (isCaptured && capturedBitmap != null) {
            BackHandler {
                isCaptured = false
                capturedBitmap = null
                textBuffer = ""
            }
            
            LensOCRView(
                bitmap = capturedBitmap!!,
                lines = detectedLines,
                initialText = textBuffer,
                onTextUpdate = { textBuffer = it },
                onRetake = { isCaptured = false; capturedBitmap = null; textBuffer = "" },
                onSend = {
                    handleScanResult(textBuffer, "TEXT", scope, firebaseManager, currentPairingId, database, "None", "", currentSplitCharacter, currentSoundEnabled, currentVibrationEnabled, false, context) {
                        isProcessing = it
                        if(!it) { isCaptured = false; capturedBitmap = null; textBuffer = "" }
                    }
                }
            )
        }

        // History Bottom Sheet
        if (showHistory) {
            ModalBottomSheet(
                onDismissRequest = { showHistory = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color.White
            ) {
                HistoryList(
                    history = scanHistory,
                    onClearAll = { scope.launch { database.scanHistoryDao().deleteAll() } },
                    onToggleFavorite = { item -> scope.launch { database.scanHistoryDao().update(item.copy(isFavorite = !item.isFavorite)) } },
                    onResend = { item ->
                        scope.launch {
                            handleScanResult(item.content, item.type, scope, firebaseManager, currentPairingId, database, "None", "", currentSplitCharacter, currentSoundEnabled, currentVibrationEnabled, false, context) {
                                if (!it) Toast.makeText(context, "Resent!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        if(isProcessing) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
    }
}

@Composable
fun ModeButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent,
        shape = CircleShape
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

fun handleScanResult(code: String, type: String, scope: kotlinx.coroutines.CoroutineScope, firebaseManager: FirebaseManager, pairingId: String, database: AppDatabase, dataProcessingMode: String, dataProcessingValue: String, splitCharacter: String, soundEnabled: Boolean, vibrationEnabled: Boolean, batchMode: Boolean, context: Context, onProcessingStateChange: (Boolean) -> Unit) {
    if (code.startsWith("QR2PC:PAIR:")) {
        val parts = code.split(":")
        if (parts.size >= 4) {
            val token = parts[2]; val pcName = parts[3]
            scope.launch {
                onProcessingStateChange(true)
                val prefs = PreferencesManager(context)
                val linkedJson = try { prefs.linkedDevices.first() } catch(e: Exception) { "[]" }
                
                var existingId: String? = null
                try {
                    val arr = JSONArray(linkedJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        if (obj.getString("name") == pcName) {
                            existingId = obj.getString("id")
                            break
                        }
                    }
                } catch (e: Exception) {}

                val finalId = existingId ?: ("PC_" + (1000..9999).random())
                
                firebaseManager.linkDevice(token, finalId, pcName) {
                    if (it) {
                        scope.launch {
                            prefs.setPairingId(finalId)
                            if (existingId == null) {
                                prefs.addLinkedDevice(finalId, pcName)
                            }
                            Toast.makeText(context, "Device Linked: $pcName", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Pairing Failed!", Toast.LENGTH_SHORT).show()
                    }
                    onProcessingStateChange(false)
                }
            }
            return
        }
    }
    scope.launch {
        onProcessingStateChange(true)
        
        var processed = code
        try {
            if (type == "QR") {
                when (dataProcessingMode) {
                    "First N" -> processed = code.take(dataProcessingValue.toIntOrNull() ?: code.length)
                    "Last N" -> processed = code.takeLast(dataProcessingValue.toIntOrNull() ?: code.length)
                    "Regex" -> if (dataProcessingValue.isNotEmpty()) processed = Regex(dataProcessingValue).find(code)?.value ?: code
                }
            }
        } catch (e: Exception) {
            Log.e("Scanner", "Processing error", e)
        }

        // 1. Log to database IMMEDIATELY
        try {
            database.scanHistoryDao().insert(ScanHistory(content = processed, type = type))
            Log.d("Scanner", "Saved to local history")
        } catch (e: Exception) {
            Log.e("Scanner", "Database insert failed", e)
        }

        val finalData = when (splitCharacter) {
            "Enter" -> "$processed\n"
            "Tab" -> "$processed\t"
            "Space" -> "$processed "
            else -> processed
        }

        // 2. Try sending to Firebase
        firebaseManager.sendScanResult(pairingId, finalData) { success ->
            scope.launch {
                if (success) {
                    provideFeedback(context, scope, soundEnabled, vibrationEnabled)
                    Toast.makeText(context, "Sent: ${processed.take(20)}...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Logged, but PC send failed!", Toast.LENGTH_SHORT).show()
                }
                delay(if (batchMode) 500 else 2000)
                onProcessingStateChange(false)
            }
        }
    }
}

@Composable
fun LensOCRView(
    bitmap: Bitmap, 
    lines: List<Text.Line>, 
    initialText: String, 
    onTextUpdate: (String) -> Unit, 
    onRetake: () -> Unit, 
    onSend: () -> Unit
) {
    val selectedIndices = remember { mutableStateOf(setOf<Int>()) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).onGloballyPositioned { size = it.size }) {
                androidx.compose.foundation.Image(bitmap.asImageBitmap(), null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                Canvas(modifier = Modifier.fillMaxSize().pointerInput(lines) {
                    detectTapGestures { offset ->
                        lines.forEachIndexed { i, b ->
                            if (scaleRect(b.boundingBox!!, bitmap, size).contains(offset)) {
                                val newSet = selectedIndices.value.toMutableSet()
                                if (newSet.contains(i)) newSet.remove(i) else newSet.add(i)
                                selectedIndices.value = newSet
                                
                                // Reconstruct text from selected lines in order
                                val sorted = newSet.toList().sorted()
                                val combined = sorted.joinToString("\n") { lines[it].text }
                                onTextUpdate(combined)
                            }
                        }
                    }
                }) {
                    lines.forEachIndexed { i, b ->
                        val r = scaleRect(b.boundingBox!!, bitmap, size)
                        val sel = selectedIndices.value.contains(i)
                        drawRect(if (sel) Color.Cyan.copy(0.4f) else Color.White.copy(0.15f), Offset(r.left, r.top), androidx.compose.ui.geometry.Size(r.width, r.height))
                        drawRect(if (sel) Color.Cyan else Color.White.copy(0.6f), Offset(r.left, r.top), androidx.compose.ui.geometry.Size(r.width, r.height), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                    }
                }
            }
            
            // Editor Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 48.dp, top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    OutlinedTextField(
                        value = initialText,
                        onValueChange = onTextUpdate,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
                        placeholder = { Text("Selected text appears here...", color = Color.Gray) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Cyan, unfocusedBorderColor = Color.DarkGray)
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onRetake) { Text("RETAKE", color = Color.White.copy(0.6f)) }
                        Button(onSend, enabled = initialText.isNotEmpty(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))) { 
                            Icon(Icons.AutoMirrored.Filled.Send, null)
                            Spacer(Modifier.width(8.dp))
                            Text("SEND TO PC") 
                        }
                    }
                }
            }
        }
    }
}

private fun scaleRect(rect: android.graphics.Rect, bitmap: Bitmap, container: IntSize): Rect {
    val scale = Math.min(container.width.toFloat() / bitmap.width, container.height.toFloat() / bitmap.height)
    val ox = (container.width - bitmap.width * scale) / 2
    val oy = (container.height - bitmap.height * scale) / 2
    return Rect(rect.left * scale + ox, rect.top * scale + oy, rect.right * scale + ox, rect.bottom * scale + oy)
}

fun rotateBitmap(s: Bitmap, a: Int): Bitmap {
    val m = android.graphics.Matrix(); m.postRotate(a.toFloat())
    return Bitmap.createBitmap(s, 0, 0, s.width, s.height, m, true)
}

fun ImageProxy.toBitmapCustom(): Bitmap {
    val b = planes[0].buffer; val t = ByteArray(b.remaining()); b.get(t)
    return android.graphics.BitmapFactory.decodeByteArray(t, 0, t.size)
}

fun processImageFromGallery(context: Context, uri: Uri, mode: String, scope: kotlinx.coroutines.CoroutineScope, firebaseManager: FirebaseManager, pairingId: String, database: AppDatabase, soundEnabled: Boolean, vibrationEnabled: Boolean) {
    try {
        val bitmap = if (Build.VERSION.SDK_INT < 28) MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        else { val source = ImageDecoder.createSource(context.contentResolver, uri); ImageDecoder.decodeBitmap(source) }
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        if (mode == "QR") {
            BarcodeScanning.getClient().process(inputImage).addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) handleScanResult(barcodes[0].rawValue ?: "", "QR", scope, firebaseManager, pairingId, database, "None", "", "None", soundEnabled, vibrationEnabled, false, context) {}
            }
        } else {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(inputImage).addOnSuccessListener { visionText ->
                if (visionText.text.isNotEmpty()) handleScanResult(visionText.text, "TEXT", scope, firebaseManager, pairingId, database, "None", "", "None", soundEnabled, vibrationEnabled, false, context) {}
            }
        }
    } catch (e: Exception) { Log.e("GalleryScan", "Error", e) }
}

@Composable
fun HistoryList(history: List<ScanHistory>, onClearAll: () -> Unit, onToggleFavorite: (ScanHistory) -> Unit, onResend: (ScanHistory) -> Unit, showTitle: Boolean = true) {
    var favOnly by remember { mutableStateOf(false) }
    val list = if (favOnly) history.filter { it.isFavorite } else history
    Column(modifier = Modifier.fillMaxWidth().padding(if (showTitle) 16.dp else 0.dp)) {
        if (showTitle) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Scan Log", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Row {
                    IconButton(onClick = { favOnly = !favOnly }) { Icon(if (favOnly) Icons.Default.Star else Icons.Default.StarBorder, null, tint = if (favOnly) Color.Yellow else Color.White) }
                    TextButton(onClearAll) { Text("Clear All", color = Color.Red) }
                }
            }
        }
        if (list.isEmpty()) { Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) { Text("No data", color = Color.Gray) } }
        else {
            LazyColumn {
                items(list) { item ->
                    ListItem(
                        headlineContent = { Text(item.content, maxLines = 2, color = Color.White) },
                        supportingContent = { Text(SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault()).format(Date(item.timestamp)), color = Color.Gray) },
                        leadingContent = { Icon(if (item.type == "QR") Icons.Default.QrCode else Icons.Default.TextFields, null, tint = Color.Cyan) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onResend(item) }) { Icon(Icons.AutoMirrored.Filled.Send, "Resend", tint = MaterialTheme.colorScheme.primary) }
                                IconButton(onClick = { onToggleFavorite(item) }) { Icon(if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, null, tint = if (item.isFavorite) Color.Yellow else Color.Gray) }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(color = Color.White.copy(0.1f))
                }
            }
        }
    }
}

fun provideFeedback(c: Context, s: kotlinx.coroutines.CoroutineScope, sound: Boolean, vib: Boolean) {
    if (sound) s.launch { try { val t = ToneGenerator(AudioManager.STREAM_MUSIC, 100); t.startTone(ToneGenerator.TONE_PROP_BEEP, 150); delay(200); t.release() } catch (e: Exception) { } }
    if (vib) try { val v = c.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator; if (v != null && v.hasVibrator()) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)) else v.vibrate(100) } } catch (e: Exception) { }
}
