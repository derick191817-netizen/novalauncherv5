package dev.nova.core.data.icons

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

/**
 * Cache de ícones em disco (cacheDir: o sistema pode limpar se faltar espaço).
 * Despejo por LRU aproximado usando o lastModified, verificado a cada [TRIM_EVERY] gravações.
 * Decodifica direto para HARDWARE (sem cópia intermediária no heap).
 */
internal class DiskIconCache(
    private val dir: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
) {
    private val writes = AtomicInteger()

    fun read(key: String): Bitmap? {
        val file = fileFor(key)
        if (!file.exists()) return null
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.HARDWARE }
        val bitmap = BitmapFactory.decodeFile(file.path, options)
        if (bitmap == null) {
            file.delete() // arquivo corrompido
            return null
        }
        file.setLastModified(System.currentTimeMillis()) // marca como usado recentemente
        return bitmap
    }

    fun write(key: String, bitmap: Bitmap) {
        try {
            dir.mkdirs()
            val target = fileFor(key)
            val tmp = File(dir, target.name + ".tmp")
            FileOutputStream(tmp).use { bitmap.compress(FORMAT, QUALITY, it) }
            if (!tmp.renameTo(target)) tmp.delete()
        } catch (_: IOException) {
            // Cache é só otimização: falha de disco não deve quebrar a UI.
        }
        if (writes.incrementAndGet() % TRIM_EVERY == 0) trimIfNeeded()
    }

    @Synchronized
    private fun trimIfNeeded() {
        val files = dir.listFiles() ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) return
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= maxBytes * TRIM_TARGET_PERCENT / 100) break
            val length = file.length()
            if (file.delete()) total -= length
        }
    }

    private fun fileFor(key: String): File {
        val digest = MessageDigest.getInstance("SHA-1").digest(key.toByteArray())
        return File(dir, digest.joinToString("") { "%02x".format(it) })
    }

    private companion object {
        const val DEFAULT_MAX_BYTES = 48L * 1024 * 1024
        const val TRIM_EVERY = 32
        const val TRIM_TARGET_PERCENT = 80
        const val QUALITY = 100

        @Suppress("DEPRECATION")
        val FORMAT: Bitmap.CompressFormat =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                Bitmap.CompressFormat.PNG
            }
    }
}
