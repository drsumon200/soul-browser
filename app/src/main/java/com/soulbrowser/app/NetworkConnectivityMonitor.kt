package com.soulbrowser.app
import android.content.Context
import android.net.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
data class NetworkStatus(val isOnline:Boolean,val isMetered:Boolean)
object NetworkConnectivityMonitor { fun observe(context:Context):Flow<NetworkStatus> = callbackFlow { val cm=context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager; fun snap():NetworkStatus{val c=cm.activeNetwork?.let(cm::getNetworkCapabilities);return NetworkStatus(c?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true&&c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),cm.isActiveNetworkMetered)};trySend(snap());val cb=object:ConnectivityManager.NetworkCallback(){override fun onAvailable(network:Network){trySend(snap())};override fun onLost(network:Network){trySend(snap())};override fun onCapabilitiesChanged(network:Network,caps:NetworkCapabilities){trySend(snap())}};cm.registerDefaultNetworkCallback(cb);awaitClose{runCatching{cm.unregisterNetworkCallback(cb)}} }.distinctUntilChanged() }