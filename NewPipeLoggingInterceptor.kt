package com.netly.app.data.remote

import android.content.Context
import android.util.Log
import com.netly.app.util.NetworkStatusTracker
import okhttp3.Interceptor
import okhttp3.Response

class NewPipeLoggingInterceptor(private val context: Context? = null) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method
        val userAgent = request.header("User-Agent") ?: "None"

        Log.d(TAG, "===> [OUTGOING REQUEST] $method $url")
        Log.d(TAG, "     User-Agent: $userAgent")

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            Log.e(TAG, "<=== [REQUEST FAILED] $method $url -> Exception: ${e.message}", e)
            throw e
        }

        val code = response.code
        val message = response.message
        val contentType = response.header("Content-Type") ?: "unknown"

        Log.d(TAG, "<=== [INCOMING RESPONSE] $code $message for $url (Type: $contentType)")

        // Diagnostic checks for 403, 429, or other error codes
        if (code == 403) {
            Log.w(TAG, "403 Forbidden response received for $url")
            NetworkStatusTracker.setThrottled(true)
        } else if (code == 429) {
            val retryAfter = response.header("Retry-After") ?: "unspecified"
            Log.w(TAG, "429 Too Many Requests for $url. Retry-After: $retryAfter")
            NetworkStatusTracker.setThrottled(true)
        } else if (code in 200..299) {
            NetworkStatusTracker.setThrottled(false)
        }

        return response
    }

    companion object {
        private const val TAG = "NewPipeNetwork"
    }
}

