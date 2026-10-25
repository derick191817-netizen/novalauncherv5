package dev.nova.core.data.icons

import android.graphics.Bitmap
import android.util.LruCache

/**
 * LRU em memória limitado por BYTES (não por quantidade).
 * Guarda bitmaps HARDWARE: ficam na memória gráfica (fora do heap Java) e o RenderThread os desenha sem
 * upload de textura por frame — menos GC e menos RAM no heap.
 */
internal class MemoryIconCache(private val baseBytes: Int) {
    private val lru = object : LruCache<String, Bitmap>(baseBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount.coerceAtLeast(1)
    }

    fun get(key: String): Bitmap? = lru.get(key)

    /** Ajusta o orçamento (ex.: 0.5 no modo Economia); o excedente é despejado na hora. */
    fun scale(factor: Float) {
        lru.resize((baseBytes * factor).toInt().coerceAtLeast(1))
    }

    fun put(key: String, bitmap: Bitmap) {
        lru.put(key, bitmap)
    }

    /** Reage a `onTrimMemory`: metade em UI_HIDDEN (20+), tudo em níveis críticos (60+). */
    fun trim(level: Int) {
        when {
            level >= LEVEL_CRITICAL -> lru.evictAll()
            level >= LEVEL_UI_HIDDEN -> lru.trimToSize(lru.maxSize() / 2)
        }
    }

    private companion object {
        const val LEVEL_UI_HIDDEN = 20
        const val LEVEL_CRITICAL = 60
    }
}
