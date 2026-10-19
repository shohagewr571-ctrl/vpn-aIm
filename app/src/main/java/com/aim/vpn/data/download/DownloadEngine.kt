package com.aim.vpn.data.download

import android.content.Context
import com.aim.vpn.domain.DownloadResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val allowedHosts = setOf(
        "github.com",
        "raw.githubusercontent.com",
        "objects.githubusercontent.com",
        "mediafire.com",
        "www.mediafire.com",
        "download.mediafire.com"
    )

    private val cancelFlags = mutableMapOf<String, AtomicBoolean>()

    fun download(
        url: String,
        expectedSha256: String? = null,
        maxBytes: Long = 200L * 1024 * 1024
    ): Flow<DownloadResult> = flow {
        val normalized = url.trim()
        if (!normalized.startsWith("https://")) {
            emit(DownloadResult.Failure("Only HTTPS URLs are allowed"))
            return@flow
        }
        val host = runCatching { java.net.URI(normalized).host?.lowercase() }.getOrNull()
        if (host == null || allowedHosts.none { host == it || host.endsWith(".$it") }) {
            emit(DownloadResult.Failure("Host not in allowed provider list: $host"))
            return@flow
        }

        val opId = System.currentTimeMillis().toString()
        val cancel = AtomicBoolean(false)
        cancelFlags[opId] = cancel

        val tmpDir = File(context.cacheDir, "downloads").apply { mkdirs() }
        val tmpFile = File(tmpDir, "dl_${opId}.tmp")

        try {
            val request = Request.Builder().url(normalized).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadResult.Failure("HTTP ${response.code}"))
                    return@flow
                }
                val body = response.body ?: run {
                    emit(DownloadResult.Failure("Empty response body"))
                    return@flow
                }
                val total = body.contentLength().takeIf { it > 0 }
                if (total != null && total > maxBytes) {
                    emit(DownloadResult.Failure("File exceeds size limit"))
                    return@flow
                }

                var downloaded = 0L
                val digest = MessageDigest.getInstance("SHA-256")
                FileOutputStream(tmpFile).use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            if (cancel.get()) {
                                emit(DownloadResult.Cancelled)
                                tmpFile.delete()
                                return@flow
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloaded += read
                            if (downloaded > maxBytes) {
                                emit(DownloadResult.Failure("File exceeds size limit"))
                                tmpFile.delete()
                                return@flow
                            }
                            emit(DownloadResult.Progress(downloaded, total))
                        }
                    }
                }

                val sha = digest.digest().joinToString("") { "%02x".format(it) }
                if (expectedSha256 != null && !expectedSha256.equals(sha, ignoreCase = true)) {
                    tmpFile.delete()
                    emit(DownloadResult.Failure("SHA-256 mismatch"))
                    return@flow
                }

                val finalFile = File(tmpDir, "dl_${opId}.bin")
                if (!tmpFile.renameTo(finalFile)) {
                    tmpFile.copyTo(finalFile, overwrite = true)
                    tmpFile.delete()
                }
                emit(DownloadResult.Success(finalFile.absolutePath, sha))
            }
        } catch (e: Exception) {
            tmpFile.delete()
            emit(DownloadResult.Failure(e.message ?: "Download failed"))
        } finally {
            cancelFlags.remove(opId)
        }
    }.flowOn(Dispatchers.IO)

    fun cancelAll() {
        cancelFlags.values.forEach { it.set(true) }
    }
}
