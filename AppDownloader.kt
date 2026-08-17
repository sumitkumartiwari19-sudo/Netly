package com.netly.app.data.remote

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

class AppDownloader private constructor(
    private val client: OkHttpClient
) : Downloader() {

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val builder = okhttp3.Request.Builder().url(url)

        var hasUserAgent = false
        var hasCookieHeader = false

        headers.forEach { (headerName, headerValueList) ->
            if (headerName.equals("User-Agent", ignoreCase = true)) {
                hasUserAgent = true
            }
            if (headerName.equals("Cookie", ignoreCase = true)) {
                hasCookieHeader = true
            }
            headerValueList.forEach { value ->
                builder.addHeader(headerName, value)
            }
        }

        if (!hasUserAgent) {
            builder.addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            )
        }

        val activeCookie = sessionCookie
        if (!hasCookieHeader && !activeCookie.isNullOrBlank()) {
            builder.addHeader("Cookie", activeCookie)
        }

        val body = if (dataToSend != null) {
            dataToSend.toRequestBody()
        } else if (httpMethod.equals("POST", ignoreCase = true) || httpMethod.equals("PUT", ignoreCase = true)) {
            ByteArray(0).toRequestBody()
        } else null

        builder.method(httpMethod, body)

        val response = client.newCall(builder.build()).execute()
        val responseBody = response.body?.string() ?: ""

        val responseHeaders = mutableMapOf<String, List<String>>()
        response.headers.names().forEach { name ->
            responseHeaders[name] = response.headers(name)
        }

        return Response(
            response.code,
            response.message,
            responseHeaders,
            responseBody,
            response.request.url.toString()
        )
    }

    companion object {
        @Volatile
        private var instance: AppDownloader? = null
        var appContext: android.content.Context? = null
        var sessionCookie: String? = null

        fun setCookie(cookie: String?) {
            sessionCookie = cookie
        }

        fun init(client: OkHttpClient, context: android.content.Context? = null): AppDownloader {
            if (context != null) {
                appContext = context.applicationContext
            }
            return instance ?: synchronized(this) {
                instance ?: run {
                    val downloader = AppDownloader(client)
                    val defaultLocale = Locale.getDefault()
                    NewPipe.init(
                        downloader,
                        Localization.fromLocale(defaultLocale),
                        ContentCountry(defaultLocale.country)
                    )
                    instance = downloader
                    downloader
                }
            }
        }
    }
}

