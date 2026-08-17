package com.netly.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.netly.app.ui.theme.NeumorphicTheme

/**
 * Reusable 1:1 Adsterra Native Banner component for Netly.
 *
 * Implements:
 * - Exact Adsterra 1:1 format container (#container-e70c40cd36e6d8b925c382ac16d78361).
 * - Native Neumorphic Card container styled with soft shadows & matching theme background.
 * - 1:1 Aspect ratio constraint.
 * - Isolated Android WebView with JavaScript enabled solely for the ad script.
 * - Safe external link handling (opens ad clicks in system browser).
 * - Automatic and silent collapse if the ad fails to load or encounters fatal network errors.
 * - Proper lifecycle teardown preventing memory leaks and duplicate creation during recomposition.
 */
@Composable
fun Native1x1Ad(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    showSponsoredLabel: Boolean = true
) {
    val colors = NeumorphicTheme
    val context = LocalContext.current
    var isAdVisible by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // HTML payload for Adsterra 1:1 Native Banner
    val adHtml = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                    -webkit-tap-highlight-color: transparent;
                }
                html, body {
                    width: 100%;
                    height: 100%;
                    background-color: transparent;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    overflow: hidden;
                }
                #container-e70c40cd36e6d8b925c382ac16d78361 {
                    width: 100%;
                    height: 100%;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                }
                iframe {
                    max-width: 100% !important;
                    max-height: 100% !important;
                    border: none !important;
                }
            </style>
        </head>
        <body>
            <div id="container-e70c40cd36e6d8b925c382ac16d78361"></div>
            <script async="async" data-cfasync="false" src="https://pl30842212.effectivecpmnetwork.com/e70c40cd36e6d8b925c382ac16d78361/invoke.js"></script>
        </body>
        </html>
        """.trimIndent()
    }

    // Clean up WebView on disposal
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { webView ->
                try {
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.clearHistory()
                    webView.removeAllViews()
                    webView.destroy()
                } catch (_: Exception) {}
            }
            webViewRef = null
        }
    }

    AnimatedVisibility(
        visible = isAdVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("adsterra_1x1_native_container"),
            contentAlignment = Alignment.Center
        ) {
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
                cornerRadius = cornerRadius,
                shadowOffset = 5.dp,
                blurRadius = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showSponsoredLabel) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sponsored",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Ad Info",
                                tint = colors.textSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // 1:1 Aspect Ratio Box with inner rounded clipping
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(cornerRadius - 6.dp))
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                createNative1x1WebView(
                                    context = ctx,
                                    htmlData = adHtml,
                                    onFatalError = {
                                        // Silently collapse on loading error
                                        isAdVisible = false
                                    }
                                ).also { webViewRef = it }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility alias for Native1x1Ad.
 */
@Composable
fun NativeAdsterraBanner(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    showSponsoredLabel: Boolean = true
) {
    Native1x1Ad(
        modifier = modifier,
        cornerRadius = cornerRadius,
        showSponsoredLabel = showSponsoredLabel
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun createNative1x1WebView(
    context: Context,
    htmlData: String,
    onFatalError: () -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccess = false
            allowContentAccess = false
        }

        webChromeClient = WebChromeClient()

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val urlString = uri.toString()

                // Allow initial or internal about:blank loads
                if (urlString.startsWith("about:", ignoreCase = true) ||
                    urlString.startsWith("data:", ignoreCase = true)
                ) {
                    return false
                }

                // Launch external browser for ad clicks
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                } catch (e: Exception) {
                    false
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                // If the main frame fails to load, collapse the ad
                if (request?.isForMainFrame == true) {
                    onFatalError()
                }
            }
        }

        loadDataWithBaseURL(
            "https://pl30842212.effectivecpmnetwork.com/",
            htmlData,
            "text/html",
            "UTF-8",
            null
        )
    }
}
