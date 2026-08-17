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
 * Reusable 320x50 Adsterra Banner component for Netly.
 *
 * Requirements satisfied:
 * - Exact ad size: 320x50 px (centered horizontally, never stretched to full width).
 * - Soft Neumorphic container matching the app's theme and background.
 * - Isolated Android WebView with JavaScript enabled solely for the ad script.
 * - Safe external link navigation (opens ad clicks in default system browser).
 * - Automatic and silent collapse if the ad fails to load or if offline.
 * - Proper lifecycle teardown preventing memory leaks and duplicate creation during recomposition.
 */
@Composable
fun Banner320x50Ad(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    showSponsoredLabel: Boolean = true
) {
    val colors = NeumorphicTheme
    val context = LocalContext.current
    var isAdVisible by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val bannerHtml = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=320, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                    -webkit-tap-highlight-color: transparent;
                }
                html, body {
                    width: 320px;
                    height: 50px;
                    background-color: transparent;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    overflow: hidden;
                    margin: 0 auto;
                }
                iframe {
                    width: 320px !important;
                    height: 50px !important;
                    border: none !important;
                    overflow: hidden !important;
                }
            </style>
        </head>
        <body>
            <script>
              atOptions = {
                'key' : '783d2d5fe4a2c6caab0b88641bfe4dd6',
                'format' : 'iframe',
                'height' : 50,
                'width' : 320,
                'params' : {}
              };
            </script>
            <script src="https://www.highperformanceformat.com/783d2d5fe4a2c6caab0b88641bfe4dd6/invoke.js"></script>
        </body>
        </html>
        """.trimIndent()
    }

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
                .testTag("adsterra_320x50_banner_container"),
            contentAlignment = Alignment.Center
        ) {
            NeumorphicCard(
                modifier = Modifier
                    .widthIn(max = 352.dp),
                cornerRadius = cornerRadius,
                shadowOffset = 4.dp,
                blurRadius = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showSponsoredLabel) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp, start = 2.dp, end = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sponsored",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Ad Info",
                                tint = colors.textSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    // 320x50 fixed size viewport
                    Box(
                        modifier = Modifier
                            .size(width = 320.dp, height = 50.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            modifier = Modifier.size(width = 320.dp, height = 50.dp),
                            factory = { ctx ->
                                createBannerWebView(
                                    context = ctx,
                                    htmlData = bannerHtml,
                                    onFatalError = {
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

@SuppressLint("SetJavaScriptEnabled")
private fun createBannerWebView(
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

                if (urlString.startsWith("about:", ignoreCase = true) ||
                    urlString.startsWith("data:", ignoreCase = true)
                ) {
                    return false
                }

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
                if (request?.isForMainFrame == true) {
                    onFatalError()
                }
            }
        }

        loadDataWithBaseURL(
            "https://www.highperformanceformat.com/",
            htmlData,
            "text/html",
            "UTF-8",
            null
        )
    }
}
