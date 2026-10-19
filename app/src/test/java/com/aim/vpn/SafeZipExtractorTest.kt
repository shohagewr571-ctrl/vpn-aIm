package com.aim.vpn

import com.aim.vpn.data.files.SafeZipExtractor
import com.aim.vpn.domain.FileOpResult
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SafeZipExtractorTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var extractor: SafeZipExtractor
    private lateinit var dest: File

    @Before
    fun setup() {
        extractor = SafeZipExtractor()
        dest = tmp.newFolder("dest")
    }

    private fun makeZip(entries: Map<String, ByteArray>): File {
        val zip = tmp.newFile("test.zip")
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            entries.forEach { (name, bytes) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return zip
    }

    @Test
    fun validZipExtracts() {
        val zip = makeZip(mapOf("hello.txt" to "world".toByteArray()))
        val result = extractor.extract(zip.absolutePath, dest)
        assertTrue(result is FileOpResult.Success)
        assertTrue(File(dest, "hello.txt").exists())
    }

    @Test
    fun pathTraversalRejected() {
        val zip = makeZip(mapOf("../evil.txt" to "x".toByteArray()))
        val result = extractor.extract(zip.absolutePath, dest)
        assertTrue(result is FileOpResult.Failure)
    }

    @Test
    fun absolutePathRejected() {
        val zip = makeZip(mapOf("/tmp/evil.txt" to "x".toByteArray()))
        val result = extractor.extract(zip.absolutePath, dest)
        assertTrue(result is FileOpResult.Failure)
    }
}
