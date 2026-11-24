package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.SaniGlassCard
import com.example.ui.theme.LocalSaniTheme
import com.example.ui.viewmodel.SaniViewModel
import java.util.concurrent.Executors

@Composable
fun CameraVisionScreen(
    viewModel: SaniViewModel
) {
    val theme = LocalSaniTheme.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val visionResult by viewModel.visionResult.collectAsStateWithLifecycle()
    val visionAnalyzing by viewModel.visionAnalyzing.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var visionPrompt by remember { mutableStateOf("What am I looking at? Explain clearly.") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = 96.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "SANI Live Vision",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.primaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "CameraX + Gemini Multimodal AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.glowCyan
                    )
                }
                if (hasCameraPermission) {
                    IconButton(onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.FlipCameraAndroid,
                            contentDescription = "Switch camera",
                            tint = theme.primaryText
                        )
                    }
                }
            }

            if (!hasCameraPermission) {
                SaniGlassCard(
                    glowColor = theme.glowCyan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.primaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Grant camera access so SANI can understand objects, read text, solve math problems, and answer visual questions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.secondaryText
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.primaryAccent,
                            contentColor = theme.background
                        ),
                        modifier = Modifier.testTag("grant_camera_button")
                    ) {
                        Text("Allow Camera Access", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // CameraX Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(theme.secondaryBackground)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                        },
                        update = { previewView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val selector = CameraSelector.Builder()
                                        .requireLensFacing(lensFacing)
                                        .build()
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        selector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (_: Exception) {
                                }
                            }, ContextCompat.getMainExecutor(context))
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (visionAnalyzing) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(theme.background.copy(alpha = 0.55f))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = theme.glowCyan)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "SANI is analyzing...",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = theme.primaryText
                                )
                            }
                        }
                    }

                    // Capture button overlay
                    Surface(
                        shape = CircleShape,
                        color = theme.primaryAccent,
                        border = BorderStroke(3.dp, theme.glowCyan),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .size(68.dp)
                            .clip(CircleShape)
                            .testTag("camera_capture_button")
                            .clickable(enabled = !visionAnalyzing) {
                                imageCapture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bmp = imageProxyToScaledBitmap(image)
                                            image.close()
                                            if (bmp != null) {
                                                viewModel.analyzeCameraImage(bmp, visionPrompt)
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            viewModel.showToast("Capture failed: ${exception.message}")
                                        }
                                    }
                                )
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Capture and Ask SANI",
                                tint = theme.background,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // Quick vision presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(
                        "What is this?",
                        "Read & translate text",
                        "Solve this problem",
                        "Identify ingredients"
                    ).forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = theme.secondaryBackground,
                            border = BorderStroke(1.dp, theme.primaryAccent.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { visionPrompt = preset }
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.primaryText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Vision question input
                SaniGlassCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = visionPrompt,
                        onValueChange = { visionPrompt = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = theme.primaryText),
                        cursorBrush = SolidColor(theme.primaryAccent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!visionResult.isNullOrBlank()) {
                    SaniGlassCard(
                        glowColor = theme.glowCyan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "SANI Vision Answer",
                                style = MaterialTheme.typography.labelLarge,
                                color = theme.glowCyan
                            )
                            Row {
                                IconButton(
                                    onClick = { viewModel.speakText(visionResult!!) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VolumeUp,
                                        contentDescription = "Speak answer",
                                        tint = theme.secondaryAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.clearVisionResult() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Dismiss",
                                        tint = theme.secondaryText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = visionResult!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.primaryText,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}

private fun imageProxyToScaledBitmap(image: ImageProxy): Bitmap? {
    return try {
        val bitmap = image.toBitmap()
        val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val maxDim = 1024
        if (rotated.width <= maxDim && rotated.height <= maxDim) rotated
        else {
            val scale = minOf(maxDim.toFloat() / rotated.width, maxDim.toFloat() / rotated.height)
            Bitmap.createScaledBitmap(
                rotated,
                (rotated.width * scale).toInt(),
                (rotated.height * scale).toInt(),
                true
            )
        }
    } catch (_: Exception) {
        null
    }
}
