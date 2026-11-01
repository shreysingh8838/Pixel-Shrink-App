package com.example.pixelshrink

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory

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

    var selectedImageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
    }

    val bitmap = remember(selectedImageUri) {
        selectedImageUri?.let { uri ->
            runCatching {
                context.contentResolver
                    .openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
            }.getOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "PixelShrink",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Compress & Resize Your Photos",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
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

        Spacer(modifier = Modifier.height(24.dp))

        bitmap?.let { image ->

            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "Selected photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Width: ${image.width} px")
            Text("Height: ${image.height} px")
        }
    }
}