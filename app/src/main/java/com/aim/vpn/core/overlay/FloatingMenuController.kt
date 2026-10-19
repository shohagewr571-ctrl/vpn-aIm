package com.aim.vpn.core.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.aim.vpn.R
import com.aim.vpn.data.config.ConfigRepository
import com.aim.vpn.data.download.DownloadEngine
import com.aim.vpn.data.files.FileOperations
import com.aim.vpn.domain.DownloadResult
import com.aim.vpn.domain.DestinationConfig
import com.aim.vpn.domain.DestinationType
import com.aim.vpn.domain.SwitchConfig
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingMenuController(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var menuView: View? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var busy = false

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            OverlayEntryPoint::class.java
        )
    }

    fun toggle() {
        if (menuView != null) {
            dismiss()
        } else {
            show()
        }
    }

    fun dismiss() {
        menuView?.let {
            runCatching { windowManager.removeView(it) }
            menuView = null
        }
    }

    private fun show() {
        val root = LayoutInflater.from(context)
            .inflate(R.layout.floating_menu, null) as FrameLayout

        val switchContainer = root.findViewById<LinearLayout>(R.id.switch_container)
        val seekBar = root.findViewById<SeekBar>(R.id.seek_transparency)
        val seekLabel = root.findViewById<TextView>(R.id.seek_label)
        val closeBtn = root.findViewById<View>(R.id.btn_close)
        val minimizeBtn = root.findViewById<View>(R.id.btn_minimize)

        closeBtn.setOnClickListener {
            dismiss()
            // Normal exit flow: stop service
            context.stopService(
                android.content.Intent(context, FloatingIconService::class.java)
            )
        }
        minimizeBtn.setOnClickListener { dismiss() }

        val seekConfig = entryPoint.configRepository().getDefaultSeekBar()
        seekLabel.text = "${seekConfig.label}: ${seekConfig.initial.toInt()}%"
        seekBar.max = ((seekConfig.max - seekConfig.min) / seekConfig.step).toInt()
        seekBar.progress = ((seekConfig.initial - seekConfig.min) / seekConfig.step).toInt()
        root.alpha = (seekConfig.initial / 100f).coerceIn(0.2f, 1f)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = seekConfig.min + progress * seekConfig.step
                seekLabel.text = "${seekConfig.label}: ${value.toInt()}%"
                root.alpha = (value / 100f).coerceIn(0.2f, 1f)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        scope.launch {
            val switches = withContext(Dispatchers.IO) {
                entryPoint.configRepository().observeSwitches().first()
            }
            switchContainer.removeAllViews()
            switches.forEach { sw ->
                val row = LayoutInflater.from(context)
                    .inflate(R.layout.item_switch_row, switchContainer, false)
                val name = row.findViewById<TextView>(R.id.switch_name)
                val toggle = row.findViewById<Switch>(R.id.switch_toggle)
                name.text = sw.name
                toggle.setOnCheckedChangeListener { _, isChecked ->
                    if (busy) {
                        toggle.isChecked = !isChecked
                        return@setOnCheckedChangeListener
                    }
                    handleSwitch(sw, isChecked, toggle)
                }
                switchContainer.addView(row)
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        menuView = root
        windowManager.addView(root, params)
    }

    private fun handleSwitch(sw: SwitchConfig, on: Boolean, toggle: Switch) {
        val url = if (on) sw.onDownloadUrl else sw.offDownloadUrl
        if (url.isNullOrBlank()) {
            Toast.makeText(context, "No download URL configured", Toast.LENGTH_SHORT).show()
            toggle.isChecked = !on
            return
        }
        busy = true
        Toast.makeText(context, if (on) "Applying ${sw.name}..." else "Reverting ${sw.name}...", Toast.LENGTH_SHORT).show()
        scope.launch {
            try {
                val engine = entryPoint.downloadEngine()
                val files = entryPoint.fileOperations()
                var finalPath: String? = null
                engine.download(url, if (on) sw.expectedSha256On else sw.expectedSha256Off)
                    .collect { result ->
                        when (result) {
                            is DownloadResult.Success -> finalPath = result.filePath
                            is DownloadResult.Failure -> {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Failed: ${result.reason}", Toast.LENGTH_LONG).show()
                                    toggle.isChecked = !on
                                }
                            }
                            is DownloadResult.Cancelled -> {
                                withContext(Dispatchers.Main) {
                                    toggle.isChecked = !on
                                }
                            }
                            else -> {}
                        }
                    }
                val path = finalPath
                if (path != null) {
                    val dest = DestinationConfig(
                        destinationId = sw.destinationId,
                        displayName = sw.destinationId,
                        destinationType = DestinationType.APP_PRIVATE
                    )
                    val op = withContext(Dispatchers.IO) {
                        files.applyArchive(path, dest)
                    }
                    withContext(Dispatchers.Main) {
                        when (op) {
                            is com.aim.vpn.domain.FileOpResult.Success ->
                                Toast.makeText(context, op.message, Toast.LENGTH_SHORT).show()
                            is com.aim.vpn.domain.FileOpResult.Failure -> {
                                Toast.makeText(context, "Failed: ${op.reason}", Toast.LENGTH_LONG).show()
                                toggle.isChecked = !on
                            }
                        }
                    }
                    files.cleanupTemp(path)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    toggle.isChecked = !on
                }
            } finally {
                busy = false
            }
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface OverlayEntryPoint {
    fun configRepository(): ConfigRepository
    fun downloadEngine(): DownloadEngine
    fun fileOperations(): FileOperations
}
