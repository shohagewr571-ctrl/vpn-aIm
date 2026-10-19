package com.aim.vpn.domain

data class AccessKeyResult(
    val valid: Boolean,
    val message: String = "",
    val keyId: String? = null
)

data class AppConfig(
    val appName: String = "VIP COOL WIRE",
    val logoUrl: String? = null,
    val creditText: String = "CREDIT BY @MODDERSUNNY©",
    val creditLogoUrl: String? = null,
    val floatingIconUrl: String? = null,
    val backgroundColor: Long = 0xFF0A0A0F,
    val accentColor: Long = 0xFFE53935,
    val startLabel: String = "START",
    val exitLabel: String = "EXIT APP",
    val particleAnimationEnabled: Boolean = true,
    val configVersion: Int = 1,
    val minimumSupportedVersion: Int = 1
)

data class SwitchConfig(
    val switchId: String,
    val name: String,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val onDownloadUrl: String? = null,
    val offDownloadUrl: String? = null,
    val destinationId: String = "app_private",
    val expectedSha256On: String? = null,
    val expectedSha256Off: String? = null,
    val version: Int = 1,
    val iconUrl: String? = null,
    val minAppVersion: Int = 1
)

data class DestinationConfig(
    val destinationId: String,
    val displayName: String,
    val destinationType: DestinationType,
    val relativePath: String = "",
    val requiresUserSelection: Boolean = false,
    val enabled: Boolean = true
)

enum class DestinationType {
    APP_PRIVATE,
    APP_EXTERNAL,
    USER_SELECTED,
    SAF
}

data class SeekBarConfig(
    val label: String = "Transparency",
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f,
    val initial: Float = 90f
)

sealed class DownloadResult {
    data class Progress(val bytesDownloaded: Long, val totalBytes: Long?) : DownloadResult()
    data class Success(val filePath: String, val sha256: String?) : DownloadResult()
    data class Failure(val reason: String) : DownloadResult()
    object Cancelled : DownloadResult()
}

sealed class FileOpResult {
    data class Success(val message: String = "Successfully Applied") : FileOpResult()
    data class Failure(val reason: String) : FileOpResult()
}

data class OperationLog(
    val operationId: String,
    val switchId: String,
    val requestedState: Boolean,
    val result: String,
    val errorCode: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val configVersion: Int = 1
)
