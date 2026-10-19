package com.aim.vpn.presentation.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aim.vpn.core.overlay.FloatingIconService
import com.aim.vpn.data.config.ConfigRepository
import com.aim.vpn.domain.AppConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    configRepository: ConfigRepository
) : ViewModel() {

    val appConfig: StateFlow<AppConfig> = configRepository.observeAppConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun startFloatingIcon() {
        if (!canDrawOverlays()) return
        val intent = Intent(context, FloatingIconService::class.java)
        context.startForegroundService(intent)
    }

    fun stopFloatingIcon() {
        context.stopService(Intent(context, FloatingIconService::class.java))
    }
}
