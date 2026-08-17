package com.netly.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetworkStatusState(val label: String, val description: String) {
    ONLINE("Online", "Connected & Optimal"),
    THROTTLED("Throttled", "Rate Limited / 403 Throttled"),
    OFFLINE("Offline", "No Internet Connection")
}

object NetworkStatusTracker {

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isThrottled = MutableStateFlow(false)
    val isThrottled: StateFlow<Boolean> = _isThrottled.asStateFlow()

    private val _networkState = MutableStateFlow(NetworkStatusState.ONLINE)
    val networkState: StateFlow<NetworkStatusState> = _networkState.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val appContext = context.applicationContext
        val connectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        val updateState = {
            val online = NetworkUtils.isOnline(appContext)
            _isOnline.value = online
            recalculateState()
        }

        updateState()

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    recalculateState()
                }

                override fun onLost(network: Network) {
                    _isOnline.value = NetworkUtils.isOnline(appContext)
                    recalculateState()
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    _isOnline.value = hasInternet
                    recalculateState()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setThrottled(throttled: Boolean) {
        _isThrottled.value = throttled
        recalculateState()
    }

    private fun recalculateState() {
        _networkState.value = when {
            !_isOnline.value -> NetworkStatusState.OFFLINE
            _isThrottled.value -> NetworkStatusState.THROTTLED
            else -> NetworkStatusState.ONLINE
        }
    }
}
