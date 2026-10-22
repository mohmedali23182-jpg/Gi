package com.dawahayah.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

data class NetworkStatus(
    val isConnected: Boolean,
    val isUnmeteredWifi: Boolean
)

class NetworkConnectivityObserver(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun currentStatus(): NetworkStatus {
        val activeNetwork = connectivityManager.activeNetwork ?: return NetworkStatus(
            isConnected = false,
            isUnmeteredWifi = false
        )
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return NetworkStatus(
            isConnected = false,
            isUnmeteredWifi = false
        )
        val connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val wifiOrUnmetered = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        return NetworkStatus(isConnected = connected, isUnmeteredWifi = wifiOrUnmetered)
    }

    val statusFlow: Flow<NetworkStatus> = callbackFlow {
        trySend(currentStatus())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(currentStatus())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(currentStatus())
            }

            override fun onLost(network: Network) {
                trySend(currentStatus())
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
