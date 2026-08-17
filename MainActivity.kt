package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.netly.app.NetlyApp
import com.netly.app.util.UrlUtils

class MainActivity : ComponentActivity() {

    private val sharedUrlState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()
        handleIntent(intent)

        setContent {
            NetlyApp(
                sharedUrl = sharedUrlState.value,
                onSharedUrlHandled = { sharedUrlState.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (Intent.ACTION_SEND == intent.action && "text/plain" == intent.type) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            val extractedUrl = UrlUtils.extractYouTubeUrl(sharedText)

            if (extractedUrl != null) {
                sharedUrlState.value = extractedUrl
            } else {
                Toast.makeText(this, "Not a valid YouTube link: $sharedText", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
