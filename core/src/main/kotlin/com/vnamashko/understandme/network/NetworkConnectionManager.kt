package com.vnamashko.understandme.network

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.RequiresPermission
import javax.inject.Inject

interface NetworkConnectionManager {
    fun isInternetAvailable(): Boolean
}

class NetworkConnectionManagerImpl @Inject constructor(context: Context): NetworkConnectionManager {
    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun isInternetAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}