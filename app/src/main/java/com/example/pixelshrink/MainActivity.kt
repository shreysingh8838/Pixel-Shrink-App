package com.example.pixelshrink

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                PixelShrinkScreen()
            }
        }
    }
}

@Composable
fun PixelShrinkScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var originalSize by remember {
        mutableLongStateOf(0L)
    }

    var compressedBytes by remember {
        mutableStateOf<ByteArray?>(null)
    }

    var compressedBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var isProcessing by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                errorMessage = null
                originalBitmap = null
                compressedBitmap = null
                compressedBytes = null

                try {
                    val result = withContext(Dispatchers.IO) {
                        val bytes = context.contentResolver
                            .openInputStream(uri)?.use {
                                it.readBytes()
                            } ?: error("Cannot open image")

                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }

                        BitmapFactory.decodeByteArray(
                            bytes, 0, bytes.size, options
                        )

                        val sample = calculateSampleSize(
                            options.outWidth,
                            options.outHeight,
                            2048
                        )

                        val decodeOptions =
                            BitmapFactory.Options().apply {
                                inSampleSize = sample
                            }

                        val bitmap = BitmapFactory.decodeByteArray(
                            bytes, 0, bytes.size, decodeOptions
                        ) ?: error("Cannot decode image")

                        Pair(bitmap, bytes.size.toLong())
                    }

                    originalBitmap = result.first
                    originalSize = result.second

                } catch (e: Exception) {
                    errorMessage = e.message ?: "Image loading failed"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "PixelShrink",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(8.dp))

        Text("Compress & Resize Your Photos")

        Spacer(Modifier.height(24.dp))

        Button(
            enabled = !isProcessing,
            onClick = {
                imagePicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts
                            .PickVisualMedia.ImageOnly
                    )
                )
            }
        ) {
            Text("Select Image")
        }

        if (isProcessing) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error
            )
        }

        originalBitmap?.let { bitmap ->
            Spacer(Modifier.height(24.dp))

            Text("Original Image")

            Spacer(Modifier.height(8.dp))

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Original image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Original Size: ${formatSize(originalSize)}"
            )

            Text(
                "Preview Dimensions: " +
                        "${bitmap.width} x ${bitmap.height}"
            )

            Spacer(Modifier.height(20.dp))

            Button(
                enabled = !isProcessing,
                onClick = {
                    scope.launch {
                        isProcessing = true
                        errorMessage = null
                        compressedBitmap = null
                        compressedBytes = null

                        try {
                            val bytes = withContext(
                                Dispatchers.Default
                            ) {
                                ByteArrayOutputStream().use {
                                        stream ->
                                    bitmap.compress(
                                        Bitmap.CompressFormat.JPEG,
                                        60,
                                        stream
                                    )
                                    stream.toByteArray()
                                }
                            }

                            compressedBytes = bytes

                            compressedBitmap = withContext(
                                Dispatchers.Default
                            ) {
                                BitmapFactory.decodeByteArray(
                                    bytes, 0, bytes.size
                                )
                            }
                        } catch (e: Exception) {
                            errorMessage =
                                e.message ?: "Compression failed"
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            ) {
                Text("Compress Image")
            }
        }

        compressedBytes?.let { bytes ->
            Spacer(Modifier.height(24.dp))

            Text(
                "Compressed Image",
                style = MaterialTheme.typography.titleMedium
            )

            compressedBitmap?.let { bitmap ->
                Spacer(Modifier.height(8.dp))

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Compressed image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Compressed Size: ${formatSize(bytes.size.toLong())}"
            )

            if (originalSize > 0) {
                val savedPercent =
                    (1.0 - bytes.size.toDouble() /
                            originalSize.toDouble()) * 100

                Text(
                    if (savedPercent >= 0) {
                        "Storage Saved: %.1f%%".format(savedPercent)
                    } else {
                        "Output is %.1f%% larger".format(-savedPercent)
                    }
                )
            }
        }
    }
}

fun calculateSampleSize(
    width: Int,
    height: Int,
    maxDimension: Int
): Int {
    var sample = 1

    while (
        width / sample > maxDimension ||
        height / sample > maxDimension
    ) {
        sample *= 2
    }

    return sample
}

fun formatSize(bytes: Long): String {
    return if (bytes >= 1024 * 1024) {
        "%.2f MB".format(bytes / (1024.0 * 1024.0))
    } else {
        "%.1f KB".format(bytes / 1024.0)
    }
}