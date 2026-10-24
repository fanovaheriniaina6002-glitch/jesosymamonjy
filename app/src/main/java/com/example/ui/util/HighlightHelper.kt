package com.example.ui.util

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.util.LruCache
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

/**
 * Rule 2 & 3: Case-Insensitive Highlighting Composable with 60 FPS Memoization.
 * - Finds ALL occurrences of [query] in [text] with `ignoreCase = true`.
 * - Applies a `SpanStyle` with `Color.Yellow.copy(alpha = 0.3f)` background and `FontWeight.ExtraBold`.
 * - Preserves the exact original casing of [text].
 */
@Composable
fun HighlightedText(
    text: String,
    query: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val annotatedText = remember(text, query) {
        HighlightHelper.buildHighlightedAnnotatedString(
            text = text,
            query = query
        )
    }

    Text(
        text = annotatedText,
        style = style,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow
    )
}

object HighlightHelper {

    /** ARGB equivalent of `Color.Yellow.copy(alpha = 0.3f)` (30% alpha yellow). */
    const val COLOR_HIGHLIGHT_YELLOW_ARGB: Int = 0x4DFFEB3B

    private val spannableCache = object : LruCache<String, CharSequence>(512) {}

    /**
     * Builds a Compose [AnnotatedString] highlighting ALL occurrences of [query] inside [text]
     * with `ignoreCase = true`, `Color.Yellow.copy(alpha = 0.3f)` background, and `FontWeight.ExtraBold`,
     * while strictly preserving the original character casing of [text].
     */
    fun buildHighlightedAnnotatedString(
        text: String,
        query: String,
        highlightBackground: Color = Color.Yellow.copy(alpha = 0.3f)
    ): AnnotatedString {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty() || text.isEmpty()) {
            return AnnotatedString(text)
        }

        val firstMatch = text.indexOf(cleanQuery, startIndex = 0, ignoreCase = true)
        if (firstMatch == -1) {
            return AnnotatedString(text)
        }

        val highlightSpanStyle = SpanStyle(
            background = highlightBackground,
            fontWeight = FontWeight.ExtraBold
        )

        return buildAnnotatedString {
            var currentIndex = 0
            val queryLength = cleanQuery.length
            while (currentIndex < text.length) {
                val matchIndex = text.indexOf(cleanQuery, startIndex = currentIndex, ignoreCase = true)
                if (matchIndex == -1) {
                    append(text.substring(currentIndex))
                    break
                }
                if (matchIndex > currentIndex) {
                    append(text.substring(currentIndex, matchIndex))
                }
                withStyle(style = highlightSpanStyle) {
                    // Keep exact original casing from `text`
                    append(text.substring(matchIndex, matchIndex + queryLength))
                }
                currentIndex = matchIndex + queryLength
            }
        }
    }

    /**
     * Case-insensitive Spannable highlighter for Android TextViews (`HikarokaSongAdapter` & `HymnReaderFragment`).
     * - Finds ALL occurrences of [query] in [fullText] with `ignoreCase = true`.
     * - Applies `BackgroundColorSpan(COLOR_HIGHLIGHT_YELLOW_ARGB)` + `StyleSpan(Typeface.BOLD)`.
     * - Preserves original casing of [fullText] and caches results in an LRU cache for 60 FPS scrolling.
     */
    fun highlightText(
        fullText: CharSequence,
        query: String?,
        isVisible: Boolean = true
    ): CharSequence {
        if (!isVisible || query.isNullOrBlank() || fullText.isEmpty()) {
            return fullText
        }
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return fullText
        }

        val textStr = fullText.toString()
        val cacheKey = "${cleanQuery.lowercase()}\u0000$textStr"
        synchronized(spannableCache) {
            spannableCache.get(cacheKey)?.let { return it }
        }

        val firstIdx = textStr.indexOf(cleanQuery, startIndex = 0, ignoreCase = true)
        if (firstIdx == -1) {
            return fullText
        }

        val spannable = SpannableStringBuilder(textStr)
        var start = 0
        val queryLen = cleanQuery.length
        while (start < textStr.length) {
            val idx = textStr.indexOf(cleanQuery, startIndex = start, ignoreCase = true)
            if (idx == -1) break
            val end = idx + queryLen
            spannable.setSpan(
                BackgroundColorSpan(COLOR_HIGHLIGHT_YELLOW_ARGB),
                idx,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                idx,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            start = end
        }

        synchronized(spannableCache) {
            spannableCache.put(cacheKey, spannable)
        }
        return spannable
    }
}
