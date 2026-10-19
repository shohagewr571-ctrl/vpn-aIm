package com.aim.vpn.data.files

import com.aim.vpn.domain.FileOpResult
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafeZipExtractor @Inject constructor() {

    private val maxEntries = 500
    private val maxTotalExtracted = 300L * 1024 * 1024
    private val maxSingleEntry = 100L * 1024 * 1024

    fun extract(zipPath: String, destinationDir: File): FileOpResult {
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            return FileOpResult.Failure("Cannot create destination directory")
        }
        val zipFile = File(zipPath)
        if (!zipFile.exists() || zipFile.length() == 0L) {
            return FileOpResult.Failure("ZIP file missing or empty")
        }

        var totalExtracted = 0L
        var entryCount = 0

        try {
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement() as ZipArchiveEntry
                    entryCount++
                    if (entryCount > maxEntries) {
                        return FileOpResult.Failure("Too many entries in archive")
                    }
                    if (entry.isDirectory) continue
                    if (entry.size > maxSingleEntry) {
                        return FileOpResult.Failure("Single entry too large: ${entry.name}")
                    }

                    val name = entry.name
                    if (name.startsWith("/") || name.contains("..") || name.contains("\\..")) {
                        return FileOpResult.Failure("Path traversal detected: $name")
                    }
                    // Reject absolute / drive paths
                    if (name.matches(Regex("^[A-Za-z]:.*")) || name.startsWith("\\\\")) {
                        return FileOpResult.Failure("Absolute path rejected: $name")
                    }

                    val outFile = File(destinationDir, name).canonicalFile
                    if (!outFile.path.startsWith(destinationDir.canonicalPath)) {
                        return FileOpResult.Failure("Path escapes destination: $name")
                    }
                    outFile.parentFile?.mkdirs()

                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(outFile).use { output ->
                            val buffer = ByteArray(8192)
                            var written = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                written += read
                                totalExtracted += read
                                if (totalExtracted > maxTotalExtracted) {
                                    return FileOpResult.Failure("Extracted size limit exceeded")
                                }
                            }
                        }
                    }
                }
            }
            return FileOpResult.Success("Successfully Applied")
        } catch (e: Exception) {
            return FileOpResult.Failure("Extraction failed: ${e.message}")
        }
    }
}
