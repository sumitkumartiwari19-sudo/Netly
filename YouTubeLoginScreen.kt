package com.netly.app.ui.auth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.netly.app.ui.components.NeumorphicCard
import com.netly.app.ui.theme.NeumorphicTheme

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLoginScreen(
    onBackClick: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    val colors = NeumorphicTheme
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var capturedCookie by remember { mutableStateOf<String?>(null) }
    var hasHandledSuccess by remember { mutableStateOf(false) }

    fun checkAndSaveCookies(url: String?, cookieManager: CookieManager) {
        if (hasHandledSuccess) return

        val ytCookie = cookieManager.getCookie("https://www.youtube.com")
            ?: cookieManager.getCookie("https://m.youtube.com")
            ?: cookieManager.getCookie(url ?: "https://www.youtube.com")

        if (!ytCookie.isNullOrBlank()) {
            capturedCookie = ytCookie
            // Check for key YouTube login session cookies
            val isLoggedIn = ytCookie.contains("LOGIN_INFO") ||
                    ytCookie.contains("SID=") ||
                    ytCookie.contains("SAPISID=") ||
                    ytCookie.contains("HSID=") ||
                    ytCookie.contains("SSID=")

            val isYouTubeHomeOrApp = url != null && (
                    url.contains("youtube.com") && !url.contains("ServiceLogin") && !url.contains("signin")
                    )

            if (isLoggedIn && (isYouTubeHomeOrApp || ytCookie.contains("LOGIN_INFO"))) {
                hasHandledSuccess = true
                Toast.makeText(context, "Signed in successfully", Toast.LENGTH_SHORT).show()
                onLoginSuccess(ytCookie)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // --- NEUMORPHIC TOP BAR ---
        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            cornerRadius = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sign in to YouTube",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Improves download reliability",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }

                IconButton(onClick = { webViewRef?.reload() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Manual confirm button if user is already signed in
                if (!capturedCookie.isNullOrBlank()) {
                    IconButton(
                        onClick = {
                            if (!hasHandledSuccess) {
                                hasHandledSuccess = true
                                Toast.makeText(context, "Signed in successfully", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(capturedCookie!!)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = colors.accent
                        )
                    }
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = colors.accent,
                trackColor = colors.surface
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
        ) {
            AndroidView(
                factory = { ctx ->
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)

                    WebView(ctx).apply {
                        webViewRef = this
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                checkAndSaveCookies(url, cookieManager)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                checkAndSaveCookies(url, cookieManager)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                                if (newProgress >= 100) {
                                    isLoading = false
                                }
                            }
                        }

                        loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fwww.youtube.com%2F")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
