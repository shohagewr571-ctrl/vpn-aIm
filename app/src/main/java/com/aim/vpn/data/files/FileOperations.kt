package com.aim.vpn.data.files

import android.content.Context
import com.aim.vpn.domain.DestinationConfig
import com.aim.vpn.domain.DestinationType
import com.aim.vpn.domain.FileOpResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileOperations @Inject constructor(
    @ApplicationContext private val context: Context,
    private val zipExtractor: SafeZipExtractor
) {
    fun resolveDestination(config: DestinationConfig): File? {
        return when (config.destinationType) {
            DestinationType.APP_PRIVATE -> {
                File(context.filesDir, config.relativePath.ifBlank { "downloads" }).also { it.mkdirs() }
            }
            DestinationType.APP_EXTERNAL -> {
                context.getExternalFilesDir(null)?.let { base ->
                    File(base, config.relativePath.ifBlank { "downloads" }).also { it.mkdirs() }
                }
            }
            DestinationType.USER_SELECTED, DestinationType.SAF -> {
                // Caller must supply SAF URI-selected path; we only support private for default
                File(context.filesDir, "user_selected").also { it.mkdirs() }
            }
        }
    }

    fun applyArchive(
        zipPath: String,
        destination: DestinationConfig
    ): FileOpResult {
        val destDir = resolveDestination(destination)
            ?: return FileOpResult.Failure("Destination not available")
        return zipExtractor.extract(zipPath, destDir)
    }

    fun cleanupTemp(path: String) {
        runCatching { File(path).delete() }
    }
}
