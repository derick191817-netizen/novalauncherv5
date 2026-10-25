package dev.nova.core.domain.search

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/**
 * Busca fuzzy para apps, sem dependências externas.
 *
 * Camadas (maior pontuação vence; 0 = sem correspondência):
 * 1. igual (1000) · 2. prefixo (≈900) · 3. prefixo de palavra (800) · 4. iniciais, ex.: "gm" → Google Maps (700)
 * 5. contém (≈600) · 6. subsequência em início de palavra (≈400) · 7. erro de digitação (≤2 edições, ≈250)
 */
object FuzzyMatcher {
    private val DIACRITICS = Regex("\\p{Mn}+")
    private const val MAX_GAPS = 2

    /** Minúsculas, sem acentos e sem espaços nas pontas. "Câmera" → "camera". */
    fun normalize(input: String): String =
        DIACRITICS.replace(Normalizer.normalize(input, Normalizer.Form.NFD), "")
            .lowercase(Locale.ROOT)
            .trim()

    /** Pré-calcula a entrada de busca de um rótulo. */
    fun entryFor(label: String): SearchEntry {
        val normalized = normalize(label)
        val words = normalized.split(' ', '-', '_', '.').filter { it.isNotEmpty() }
        return SearchEntry(normalized, words, words.map { it[0] }.joinToString(""))
    }

    /** Pontua [entry] contra [query] (já normalizada com [normalize]). */
    fun score(query: String, entry: SearchEntry): Int {
        if (query.isEmpty()) return 1
        val n = entry.normalized
        if (n == query) return 1000
        if (n.startsWith(query)) return 900 - (n.length - query.length).coerceAtMost(50)
        for ((i, w) in entry.words.withIndex()) {
            if (w.startsWith(query)) return 800 - i * 20
        }
        if (query.length >= 2 && entry.initials.startsWith(query)) return 700
        val idx = n.indexOf(query)
        if (idx >= 0) return 600 - idx.coerceAtMost(100)
        if (query.length >= 2) {
            val s = subsequenceScore(query, n)
            if (s > 0) return s
        }
        if (query.length >= 4) return typoScore(query, entry.words)
        return 0
    }

    private fun subsequenceScore(q: String, t: String): Int {
        var best = 0
        var start = 0
        while (start < t.length) {
            if (t[start] == q[0]) best = maxOf(best, matchFrom(q, t, start))
            val next = t.indexOf(' ', start)
            if (next < 0) break
            start = next + 1
        }
        return best
    }

    private fun matchFrom(q: String, t: String, start: Int): Int {
        var qi = 0
        var gaps = 0
        var last = -1
        var ti = start
        while (ti < t.length && qi < q.length) {
            if (t[ti] == q[qi]) {
                if (last >= 0 && ti - last > 1) gaps++
                last = ti
                qi++
            }
            ti++
        }
        return if (qi == q.length && gaps <= MAX_GAPS) 400 - gaps * 15 else 0
    }

    private fun typoScore(q: String, words: List<String>): Int {
        val maxDist = if (q.length >= 7) 2 else 1
        for (w in words) {
            for (len in (q.length - maxDist)..(q.length + maxDist)) {
                if (len < 1 || len > w.length) continue
                val d = editDistance(q, w.substring(0, len), maxDist)
                if (d in 1..maxDist) return 300 - d * 50
            }
        }
        return 0
    }

    /** Distância de Damerau-Levenshtein (OSA) com corte antecipado em [max]. */
    internal fun editDistance(a: String, b: String, max: Int): Int {
        if (abs(a.length - b.length) > max) return max + 1
        var prev2 = IntArray(b.length + 1)
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    v = minOf(v, prev2[j - 2] + 1)
                }
                cur[j] = v
                if (v < rowMin) rowMin = v
            }
            if (rowMin > max) return max + 1
            val t = prev2
            prev2 = prev
            prev = cur
            cur = t
        }
        return prev[b.length]
    }
}
