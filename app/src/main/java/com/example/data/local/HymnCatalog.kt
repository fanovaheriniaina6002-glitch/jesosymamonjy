package com.example.data.local

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * High-Performance 60 FPS In-Memory Pre-Indexed Hymn Engine ("Bible App" Architecture).
 *
 * Implements the 5 strict performance rules:
 * 1. Single-pass pre-indexing of all 830 hymns from assets/hymns.json at startup (0 calculation on scroll).
 * 2. Pagination support (25 items per page on browse, 40 items cap on keyword search + infinite scroll).
 * 3. Dual-speed search engine (0ms exact-first numeric lookup + 120ms debounced title-first keyword lookup).
 * 4. Lightweight card data binding with pre-sliced 65-char snippet & pre-computed lowercase searchIndex.
 * 5. FIFO-10 consultation history without duplicates + Favorites starting empty ([]) by default.
 */
object HymnCatalog {

    const val PAGE_SIZE = 25
    const val KEYWORD_SEARCH_INITIAL_CAP = 40
    const val MAX_HISTORY_SIZE = 10

    private const val PREFS_FAVORITES = "jesosy_mamonjy_hymn_favorites_v5"
    private const val KEY_FAVORITE_IDS = "favorite_ids_csv"

    private const val PREFS_HISTORY = "jesosy_mamonjy_hymn_history_v5"
    private const val KEY_HISTORY_IDS = "history_ids_csv"

    private val STANZA_SPLIT_REGEX = Regex("\\n\\s*\\n")

    @Volatile
    private var isInitialized = false
    private val initLock = Any()

    // Base immutable pre-indexed list of 830 hymns (without favorite flag applied)
    private var baseIndexedHymns: List<SongEntity> = emptyList()
    private var hymnByNumberMap: Map<Int, SongEntity> = emptyMap()

    // Random "HIRA ANIO" chosen once per app launch after loading hymns.json
    private var sessionFeaturedSongNumber: Int? = null

    // Reactive in-memory StateFlows for 0ms tab switching and instant UI updates
    private val _allSongsFlow = MutableStateFlow<List<SongEntity>>(emptyList())
    val allSongsFlow: StateFlow<List<SongEntity>> = _allSongsFlow.asStateFlow()

    private val _favoriteSongsFlow = MutableStateFlow<List<SongEntity>>(emptyList())
    val favoriteSongsFlow: StateFlow<List<SongEntity>> = _favoriteSongsFlow.asStateFlow()

    private val _historySongsFlow = MutableStateFlow<List<SongEntity>>(emptyList())
    val historySongsFlow: StateFlow<List<SongEntity>> = _historySongsFlow.asStateFlow()

    private val _featuredSongFlow = MutableStateFlow<SongEntity?>(null)
    val featuredSongFlow: StateFlow<SongEntity?> = _featuredSongFlow.asStateFlow()

    private var favoriteNumbers: Set<Int> = emptySet()
    private var historyNumbers: List<Int> = emptyList()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isLoadedFlow = MutableStateFlow(false)
    val isLoadedFlow: StateFlow<Boolean> = _isLoadedFlow.asStateFlow()

    /**
     * Non-blocking background initialization on `Dispatchers.IO`.
     * Prevents any Main Thread blocking (ANR) during Activity or Fragment startup.
     */
    fun ensureInitializedAsync(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        ioScope.launch {
            ensureInitialized(appContext)
        }
    }

    suspend fun ensureInitializedIo(context: Context) {
        if (isInitialized) return
        withContext(Dispatchers.IO) {
            ensureInitialized(context.applicationContext)
        }
    }

    /**
     * Rule 1 & 2: Pre-indexes `hymns.json` a single time in memory
     * and selects a 100% random hymn once per launch for "HIRA ANIO".
     */
    fun ensureInitialized(context: Context) {
        if (isInitialized) return
        synchronized(initLock) {
            if (isInitialized) return
            val appContext = context.applicationContext

            favoriteNumbers = loadFavoriteNumbers(appContext)
            historyNumbers = loadHistoryNumbers(appContext)

            val rawJson = loadRawJsonString(appContext)
            val parsed = parseHymnsJson(rawJson)
            if (sessionFeaturedSongNumber == null && parsed.isNotEmpty()) {
                sessionFeaturedSongNumber = parsed.random().number
            }
            baseIndexedHymns = parsed
            hymnByNumberMap = parsed.associateBy { it.number }

            rebuildReactiveFlows()
            isInitialized = true
            _isLoadedFlow.value = true
        }
    }

    private const val IMPORTED_HYMNS_FILE = "imported_hymns.json"

    private fun loadRawJsonString(context: Context): String {
        // Priority 1: User-imported file in app's internal storage if present
        try {
            val importedFile = java.io.File(context.filesDir, IMPORTED_HYMNS_FILE)
            if (importedFile.exists() && importedFile.length() > 0) {
                return importedFile.readText(Charsets.UTF_8)
            }
        } catch (_: Exception) {
        }

        // Priority 2: assets/hymns.json
        try {
            return context.assets.open("hymns.json").use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            }
        } catch (_: Exception) {
        }

        return "[]"
    }

    private fun parseHymnsJson(rawJson: String): List<SongEntity> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty() || trimmed == "[]" || trimmed == "{}") {
            return emptyList()
        }

        val jsonArray = ArrayList<org.json.JSONObject>()

        try {
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i)
                    if (item != null) jsonArray.add(item)
                }
            } else if (trimmed.startsWith("{")) {
                val rootObj = org.json.JSONObject(trimmed)
                // Check if wrapped in array like {"hymns": [...]} or {"data": [...]}
                val candidateKeys = listOf("hymns", "chants", "fihirana", "data", "songs", "items")
                var extractedArray: JSONArray? = null
                for (k in candidateKeys) {
                    if (rootObj.has(k) && rootObj.optJSONArray(k) != null) {
                        extractedArray = rootObj.getJSONArray(k)
                        break
                    }
                }
                if (extractedArray != null) {
                    for (i in 0 until extractedArray.length()) {
                        val item = extractedArray.optJSONObject(i)
                        if (item != null) jsonArray.add(item)
                    }
                } else {
                    // Treat each object property as a hymn
                    val keys = rootObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val item = rootObj.optJSONObject(key)
                        if (item != null) {
                            if (!item.has("numero") && !item.has("number")) {
                                item.put("numero", key)
                            }
                            jsonArray.add(item)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }

        val result = ArrayList<SongEntity>(jsonArray.size)

        for (i in 0 until jsonArray.size) {
            val obj = jsonArray[i]

            // 1. Detect Number
            val numeroRaw = findString(obj, "numero", "number", "laharana", "id", "num", "no")
            val numberInt = numeroRaw.filter { it.isDigit() }.toIntOrNull() ?: (i + 1)
            val numberStr = if (numeroRaw.isNotBlank()) numeroRaw else numberInt.toString()

            // 2. Detect Lyrics / Verses
            val fullLyrics = extractFullLyrics(obj)

            // 3. Detect Title
            var rawTitle = findString(obj, "titre", "title", "lohateny", "name")
            if (rawTitle.isBlank()) {
                // If title is missing, use the first line of lyrics
                rawTitle = computeFirstLineSnippet(fullLyrics).removeSuffix("...")
            }
            if (rawTitle.isBlank()) {
                rawTitle = "HIRA $numberStr"
            }
            val upperTitle = rawTitle.uppercase()
            val lowerTitle = upperTitle.lowercase()

            // 4. Detect Category
            var catRaw = findString(obj, "category", "fihirana", "type", "book", "sokajy")
            val category = when {
                catRaw.contains("vaovao", ignoreCase = true) || catRaw.contains("fanampiny", ignoreCase = true) -> "Fihirana Vaovao"
                catRaw.contains("taloha", ignoreCase = true) || catRaw.contains("lehibe", ignoreCase = true) -> "Fihirana Lehibe"
                numberInt <= 450 -> "Fihirana Lehibe"
                else -> "Fihirana Vaovao"
            }

            // 5. Pre-computed snippet & search index (stanzas are parsed lazily only when opened in HymnReader)
            val snippet = computeFirstLineSnippet(fullLyrics)
            val searchIndex = "$numberStr $lowerTitle ${fullLyrics.lowercase()}"

            result.add(
                SongEntity(
                    number = numberInt,
                    id = numberInt,
                    numberStr = numberStr,
                    title = upperTitle,
                    category = category,
                    snippet = snippet,
                    preview = snippet,
                    fullLyrics = fullLyrics,
                    lyrics = fullLyrics,
                    titleLower = lowerTitle,
                    searchIndex = searchIndex,
                    verse1 = "",
                    chorus = "",
                    verse2 = "",
                    verse3 = "",
                    isFavorite = favoriteNumbers.contains(numberInt),
                    isFeatured = false
                )
            )
        }

        result.sortBy { it.number }
        return result
    }

    private fun findString(obj: org.json.JSONObject, vararg keys: String): String {
        for (k in keys) {
            if (obj.has(k)) {
                val v = obj.opt(k)
                if (v != null && v != org.json.JSONObject.NULL) {
                    val s = v.toString().trim()
                    if (s.isNotEmpty()) return s
                }
            }
        }
        return ""
    }

    private val CHORUS_HEADER_REGEX = Regex(
        "^isan\\s*['’ʼ`]?\\s*andininy\\b(.*)$",
        RegexOption.IGNORE_CASE
    )
    private val STANDALONE_VERSE_HEADER_REGEX = Regex(
        "^(?:andininy\\s*\\d+|\\d+[.):\\-])\\s*:?\\s*$",
        RegexOption.IGNORE_CASE
    )
    private val METADATA_KEYS = setOf(
        "numero", "number", "laharana", "id", "num", "no",
        "titre", "title", "lohateny", "name",
        "category", "fihirana", "type", "book", "sokajy"
    )

    data class HymnSection(
        val isChorus: Boolean,
        val title: String,
        val content: String
    )

    /**
     * Flexible Chorus Header detector (Rule 2):
     * Detects "Isan'andininy", "Isan’andininy", "Isan' andininy 2", "Isan'andininy 4",
     * "Isan'andininy 1 sy 2", "Isan'andininy and 1 sy 2", etc.
     * Uses a 0-allocation prefix guard ("isan") before normalizing apostrophes or running Regex.
     */
    fun parseChorusHeader(rawLine: String): Pair<String, String>? {
        val trimmed = rawLine.trim()
        if (trimmed.length < 12 || !trimmed.startsWith("isan", ignoreCase = true)) return null

        val normalized = trimmed
            .replace('’', '\'')
            .replace('ʼ', '\'')
            .replace('`', '\'')

        val match = if (normalized.startsWith("Isan'andininy", ignoreCase = true)) {
            normalized.substring("Isan'andininy".length)
        } else {
            CHORUS_HEADER_REGEX.find(normalized)?.groupValues?.getOrNull(1)
        } ?: return null

        val remainder = match.trim()
        return if (remainder.contains(':')) {
            val beforeColon = remainder.substringBefore(':').trim()
            val afterColon = remainder.substringAfter(':').trim()
            val label = if (beforeColon.isEmpty()) {
                "ISAN'ANDININY"
            } else {
                "ISAN'ANDININY ${beforeColon.uppercase()}"
            }
            Pair(label, afterColon)
        } else {
            val cleanSuffix = remainder.trimEnd('.', ';', '-')
            val label = if (cleanSuffix.isEmpty()) {
                "ISAN'ANDININY"
            } else {
                "ISAN'ANDININY ${cleanSuffix.uppercase()}"
            }
            Pair(label, "")
        }
    }

    private fun isChorusHeaderLine(line: String): Boolean = parseChorusHeader(line) != null

    private fun isStandaloneVerseHeader(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return false
        val firstChar = trimmed[0]
        if (!firstChar.isDigit() && !trimmed.startsWith("andininy", ignoreCase = true)) return false
        return STANDALONE_VERSE_HEADER_REGEX.matches(trimmed)
    }

    private fun extractFullLyrics(obj: org.json.JSONObject): String {
        val keys = listOf("paroles", "lyrics", "tononkira", "verses", "content", "text", "andininy", "fiverenany")
        for (k in keys) {
            if (obj.has(k)) {
                val v = obj.opt(k) ?: continue
                when (v) {
                    is String -> {
                        val s = v.trim()
                        if (s.isNotEmpty()) return s
                    }
                    is JSONArray -> {
                        val blocks = ArrayList<String>(v.length())
                        for (idx in 0 until v.length()) {
                            val elem = v.opt(idx) ?: continue
                            if (elem is org.json.JSONObject) {
                                val label = findString(elem, "type", "label", "title", "titre", "header")
                                val body = findString(elem, "texte", "text", "paroles", "lyrics", "content")
                                if (body.isNotEmpty()) {
                                    if (isChorusHeaderLine(label) && !isChorusHeaderLine(body.lineSequence().firstOrNull().orEmpty())) {
                                        blocks.add("$label:\n$body")
                                    } else {
                                        blocks.add(body)
                                    }
                                }
                            } else {
                                val line = elem.toString().trim()
                                if (line.isNotEmpty()) blocks.add(line)
                            }
                        }
                        if (blocks.isNotEmpty()) return blocks.joinToString("\n\n")
                    }
                    is org.json.JSONObject -> {
                        val blocks = extractBlocksFromJsonObject(v)
                        if (blocks.isNotEmpty()) return blocks.joinToString("\n\n")
                    }
                }
            }
        }

        // Dynamic fallback if verses/choruses are stored as direct keys on the root hymn JSONObject
        val directBlocks = extractBlocksFromJsonObject(obj, excludeKeys = METADATA_KEYS)
        if (directBlocks.isNotEmpty()) {
            return directBlocks.joinToString("\n\n")
        }

        return ""
    }

    private fun extractBlocksFromJsonObject(
        jsonObj: org.json.JSONObject,
        excludeKeys: Set<String> = emptySet()
    ): List<String> {
        val blocks = ArrayList<String>()
        val it = jsonObj.keys()
        while (it.hasNext()) {
            val subKey = it.next()
            if (excludeKeys.contains(subKey.lowercase())) continue
            val subVal = jsonObj.optString(subKey, "").trim()
            if (subVal.isEmpty()) continue
            val firstLine = subVal.lineSequence().firstOrNull()?.trim().orEmpty()
            if (isChorusHeaderLine(subKey) && !isChorusHeaderLine(firstLine)) {
                blocks.add("$subKey:\n$subVal")
            } else {
                blocks.add(subVal)
            }
        }
        return blocks
    }

    /**
     * Imports a user-selected JSON file content, saves it to internal storage,
     * and re-indexes all reactive flows instantly.
     */
    fun importHymnsFromJsonString(context: Context, jsonContent: String): Boolean {
        synchronized(initLock) {
            val parsed = parseHymnsJson(jsonContent)
            if (parsed.isEmpty()) return false

            try {
                val file = java.io.File(context.applicationContext.filesDir, IMPORTED_HYMNS_FILE)
                file.writeText(jsonContent, Charsets.UTF_8)
            } catch (_: Exception) {
            }

            baseIndexedHymns = parsed
            hymnByNumberMap = parsed.associateBy { it.number }
            rebuildReactiveFlows()
            isInitialized = true
            return true
        }
    }

    /**
     * Computes the first non-blank line of the hymn pre-cut to max 65 chars + "..." once at startup.
     */
    private fun computeFirstLineSnippet(fullLyrics: String): String {
        val firstLine = fullLyrics
            .lineSequence()
            .map { it.trim() }
            .firstOrNull {
                it.isNotEmpty() && !isChorusHeaderLine(it) && !isStandaloneVerseHeader(it)
            }
            .orEmpty()
            .trimEnd('.', ',', ';', ':')

        if (firstLine.isEmpty()) return "..."
        return if (firstLine.length > 65) {
            firstLine.substring(0, 65).trimEnd() + "..."
        } else {
            "$firstLine..."
        }
    }

    /**
     * Rule 1: Dynamic Contextual Snippet Extraction.
     * - Searches in [fullLyrics] (split by `\n`) for the FIRST line containing [rawQuery] (`ignoreCase = true`).
     * - If found in lyrics, returns that exact line (adding "..." at the end if it exceeds 65 characters).
     * - If the query is only in the title (or empty/numeric), returns [defaultSnippet] (the first line by default).
     */
    fun extractContextualSnippet(
        fullLyrics: String,
        defaultSnippet: String,
        rawQuery: String
    ): String {
        val query = rawQuery.trim()
        if (query.isEmpty() || query.all { it.isDigit() } || fullLyrics.isBlank()) {
            return defaultSnippet
        }

        val matchedLine = fullLyrics
            .split('\n')
            .asSequence()
            .map { rawLine ->
                val trimmed = rawLine.trim()
                val chorusParsed = parseChorusHeader(trimmed)
                when {
                    chorusParsed != null -> chorusParsed.second.trim()
                    isStandaloneVerseHeader(trimmed) -> ""
                    else -> trimmed
                }
            }
            .firstOrNull { line ->
                line.isNotEmpty() && line.contains(query, ignoreCase = true)
            } ?: return defaultSnippet

        val cleanLine = matchedLine.trimEnd('.', ',', ';', ':')
        if (cleanLine.length <= 65) {
            return cleanLine
        }

        val matchIdx = cleanLine.indexOf(query, ignoreCase = true)
        return if (matchIdx == -1 || matchIdx + query.length <= 65) {
            cleanLine.substring(0, 65).trimEnd() + "..."
        } else {
            val startIdx = (matchIdx - 18).coerceAtLeast(0)
            val endIdx = (startIdx + 65).coerceAtMost(cleanLine.length)
            cleanLine.substring(startIdx, endIdx).trim() + "..."
        }
    }

    private data class StanzaSplit(
        val verse1: String,
        val chorus: String,
        val verse2: String,
        val verse3: String
    )

    private fun splitLyricsIntoStanzas(fullLyrics: String): StanzaSplit {
        if (fullLyrics.isBlank()) return StanzaSplit("", "", "", "")
        val sections = parseHymnSections(fullLyrics)
        val verses = sections.filter { !it.isChorus }.map { it.content }
        val firstChorus = sections.firstOrNull { it.isChorus }?.content.orEmpty()

        val v1 = verses.getOrElse(0) { fullLyrics }
        val v2 = verses.getOrElse(1) { "" }
        // Rule 1: Never merge verses 4..N into verse3!
        val v3 = verses.getOrElse(2) { "" }
        return StanzaSplit(v1, firstChorus, v2, v3)
    }

    private fun rebuildReactiveFlows() {
        val favSet = favoriteNumbers
        if (sessionFeaturedSongNumber == null && baseIndexedHymns.isNotEmpty()) {
            sessionFeaturedSongNumber = baseIndexedHymns.random().number
        }
        val featuredNum = sessionFeaturedSongNumber
        val updatedAll = baseIndexedHymns.map { song ->
            val fav = favSet.contains(song.number)
            val feat = (song.number == featuredNum)
            if (song.isFavorite == fav && song.isFeatured == feat) {
                song
            } else {
                song.copy(isFavorite = fav, isFeatured = feat)
            }
        }
        val byNum = updatedAll.associateBy { it.number }

        _allSongsFlow.value = updatedAll
        _favoriteSongsFlow.value = updatedAll.filter { it.isFavorite }
        _historySongsFlow.value = historyNumbers.mapNotNull { byNum[it] }
        _featuredSongFlow.value = (if (featuredNum != null) byNum[featuredNum] else null)
            ?: updatedAll.randomOrNull()
    }

    fun getSongByNumber(context: Context, number: Int): SongEntity? {
        ensureInitialized(context)
        val base = hymnByNumberMap[number] ?: return null
        val isFav = favoriteNumbers.contains(number)
        return if (base.isFavorite == isFav) base else base.copy(isFavorite = isFav)
    }

    /**
     * Rule 5: Favorites start empty ([]) by default and update in 0ms on heart click or double-tap.
     */
    fun toggleFavorite(context: Context, songNumber: Int): Boolean {
        ensureInitialized(context)
        val newFavState: Boolean
        synchronized(initLock) {
            val mutable = favoriteNumbers.toMutableSet()
            newFavState = if (mutable.contains(songNumber)) {
                mutable.remove(songNumber)
                false
            } else {
                mutable.add(songNumber)
                true
            }
            favoriteNumbers = mutable
            saveFavoriteNumbers(context.applicationContext, mutable)
            rebuildReactiveFlows()
        }
        return newFavState
    }

    /**
     * Adds a hymn to favorites if not already present (used for double-tap in LyricsReader).
     * Returns true if added, false if it was already in favorites.
     */
    fun addFavorite(context: Context, songNumber: Int): Boolean {
        ensureInitialized(context)
        synchronized(initLock) {
            if (favoriteNumbers.contains(songNumber)) return false
            val mutable = favoriteNumbers.toMutableSet()
            mutable.add(songNumber)
            favoriteNumbers = mutable
            saveFavoriteNumbers(context.applicationContext, mutable)
            rebuildReactiveFlows()
            return true
        }
    }

    /**
     * Removes a hymn from favorites (used for left swipe-to-delete in HiraTiakoFragment).
     */
    fun removeFavorite(context: Context, songNumber: Int) {
        ensureInitialized(context)
        synchronized(initLock) {
            if (!favoriteNumbers.contains(songNumber)) return
            val mutable = favoriteNumbers.toMutableSet()
            mutable.remove(songNumber)
            favoriteNumbers = mutable
            saveFavoriteNumbers(context.applicationContext, mutable)
            rebuildReactiveFlows()
        }
    }

    /**
     * Rule 5: FIFO History (Max 10) without duplicates.
     * Adds the consulted song at the head (index 0) and keeps strictly at most 10 items.
     */
    fun recordSongConsulted(context: Context, songNumber: Int) {
        ensureInitialized(context)
        synchronized(initLock) {
            val updated = ArrayList<Int>(MAX_HISTORY_SIZE)
            updated.add(songNumber)
            for (existing in historyNumbers) {
                if (existing != songNumber && updated.size < MAX_HISTORY_SIZE) {
                    updated.add(existing)
                }
            }
            historyNumbers = updated
            saveHistoryNumbers(context.applicationContext, updated)
            val byNum = _allSongsFlow.value.associateBy { it.number }
            _historySongsFlow.value = updated.mapNotNull { byNum[it] }
        }
    }

    /**
     * Removes a hymn from the consultation history (used for left swipe-to-delete in FandraisanaFragment).
     */
    fun removeSongFromHistory(context: Context, songNumber: Int) {
        ensureInitialized(context)
        synchronized(initLock) {
            if (!historyNumbers.contains(songNumber)) return
            val updated = historyNumbers.filter { it != songNumber }
            historyNumbers = updated
            saveHistoryNumbers(context.applicationContext, updated)
            val byNum = _allSongsFlow.value.associateBy { it.number }
            _historySongsFlow.value = updated.mapNotNull { byNum[it] }
        }
    }

    /**
     * Dynamic & unlimited parser for HymnReaderFragment (Rules 1 & 2):
     * - Dynamically parses ALL verses (ANDININY 1, ANDININY 2, ..., ANDININY N) with no hardcoded limit.
     * - Detects all standard and exceptional choruses ("Isan'andininy", "Isan'andininy 4",
     *   "Isan'andininy 1 sy 2", "Isan'andininy and 1 sy 2", "Isan' andininy 2", etc.) even if
     *   separated by a single newline inside a stanza block.
     */
    fun parseHymnSections(fullLyrics: String): List<HymnSection> {
        if (fullLyrics.isBlank()) return emptyList()

        val rawParagraphs = fullLyrics
            .split(STANZA_SPLIT_REGEX)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val sections = ArrayList<HymnSection>(rawParagraphs.size + 2)
        var verseCounter = 0

        fun flushBlock(isChorus: Boolean, chorusLabel: String, lines: List<String>) {
            val body = lines.joinToString("\n").trim()
            if (body.isEmpty()) return
            if (isChorus) {
                sections.add(
                    HymnSection(
                        isChorus = true,
                        title = chorusLabel.ifBlank { "ISAN'ANDININY" },
                        content = body
                    )
                )
            } else {
                verseCounter++
                sections.add(
                    HymnSection(
                        isChorus = false,
                        title = "ANDININY $verseCounter",
                        content = body
                    )
                )
            }
        }

        for (paragraph in rawParagraphs) {
            val lines = paragraph.lines()
            var currentIsChorus = false
            var currentChorusLabel = ""
            val currentLines = ArrayList<String>(lines.size)

            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue

                val chorusHeader = parseChorusHeader(line)
                if (chorusHeader != null) {
                    if (currentLines.isNotEmpty()) {
                        flushBlock(currentIsChorus, currentChorusLabel, currentLines)
                        currentLines.clear()
                    }
                    currentIsChorus = true
                    currentChorusLabel = chorusHeader.first
                    if (chorusHeader.second.isNotEmpty()) {
                        currentLines.add(chorusHeader.second)
                    }
                } else if (currentLines.isEmpty() && !currentIsChorus && isStandaloneVerseHeader(line)) {
                    // Skip redundant standalone verse header line since badge renders "ANDININY X"
                    continue
                } else {
                    currentLines.add(line)
                }
            }

            if (currentLines.isNotEmpty()) {
                flushBlock(currentIsChorus, currentChorusLabel, currentLines)
            }
        }

        return sections
    }

    /**
     * Kept for backward compatibility: returns all individual verses (1..N) and the primary chorus.
     */
    fun extractAllStanzas(fullLyrics: String): Pair<List<String>, String> {
        val sections = parseHymnSections(fullLyrics)
        val verses = sections.filter { !it.isChorus }.map { it.content }
        val chorus = sections.firstOrNull { it.isChorus }?.content.orEmpty()
        return Pair(verses, chorus)
    }

    fun searchSongs(
        context: Context,
        rawQuery: String,
        categoryFilter: String = "REHETRA"
    ): List<SongEntity> {
        ensureInitialized(context)
        return searchSongs(
            songs = _allSongsFlow.value,
            rawQuery = rawQuery,
            categoryFilter = categoryFilter
        )
    }

    /**
     * Rule 3: Dual-Speed Search Engine (0ms numeric lookup + pre-indexed title/lyrics keyword lookup).
     */
    fun searchSongs(
        songs: List<SongEntity>,
        rawQuery: String,
        categoryFilter: String
    ): List<SongEntity> {
        val filteredByCategory = when (categoryFilter) {
            "LEHIBE", "TALOHA" -> songs.filter { it.category == "Fihirana Lehibe" || it.category == "Fihirana Taloha" }
            "FANAMPINY", "VAOVAO" -> songs.filter { it.category == "Fihirana Vaovao" }
            else -> songs
        }

        val query = rawQuery.trim()
        if (query.isEmpty()) {
            return filteredByCategory
        }

        // 1. Search by Number (Ultra-priority, 0ms): only digits
        if (query.all { it.isDigit() }) {
            val exactMatches = ArrayList<SongEntity>(1)
            val prefixMatches = ArrayList<SongEntity>(32)

            for (song in filteredByCategory) {
                if (song.numberStr == query) {
                    exactMatches.add(song.copy(preview = "$query|${song.snippet}"))
                } else if (song.numberStr.startsWith(query)) {
                    prefixMatches.add(song.copy(preview = "$query|${song.snippet}"))
                }
            }
            return exactMatches + prefixMatches
        }

        // 2. Search by Word or Phrase: Title matches first, then Lyrics (searchIndex) matches,
        // with dynamic contextual snippet extraction (first lyric line matching query, ignoreCase = true)
        val qLower = query.lowercase()
        val titleMatches = ArrayList<SongEntity>(40)
        val lyricsMatches = ArrayList<SongEntity>(40)

        for (song in filteredByCategory) {
            val inTitle = song.titleLower.contains(qLower)
            val inLyrics = if (inTitle) true else song.searchIndex.contains(qLower)
            if (inTitle || inLyrics) {
                val contextualSnippet = extractContextualSnippet(
                    fullLyrics = song.fullLyrics,
                    defaultSnippet = song.snippet,
                    rawQuery = query
                )
                val matchedSong = song.copy(
                    snippet = contextualSnippet,
                    preview = "$qLower|$contextualSnippet"
                )
                if (inTitle) {
                    titleMatches.add(matchedSong)
                } else {
                    lyricsMatches.add(matchedSong)
                }
            }
        }

        return titleMatches + lyricsMatches
    }

    private fun loadFavoriteNumbers(context: Context): Set<Int> {
        val prefs = context.getSharedPreferences(PREFS_FAVORITES, Context.MODE_PRIVATE)
        val csv = prefs.getString(KEY_FAVORITE_IDS, "")?.trim().orEmpty()
        if (csv.isEmpty()) return emptySet() // Starts totally empty ([]) by default!
        return csv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    private fun saveFavoriteNumbers(context: Context, ids: Set<Int>) {
        val prefs = context.getSharedPreferences(PREFS_FAVORITES, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FAVORITE_IDS, ids.joinToString(",")).apply()
    }

    private fun loadHistoryNumbers(context: Context): List<Int> {
        val prefs = context.getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
        val csv = prefs.getString(KEY_HISTORY_IDS, "")?.trim().orEmpty()
        if (csv.isEmpty()) return emptyList()
        return csv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()
            .take(MAX_HISTORY_SIZE)
    }

    private fun saveHistoryNumbers(context: Context, ids: List<Int>) {
        val prefs = context.getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY_IDS, ids.take(MAX_HISTORY_SIZE).joinToString(",")).apply()
    }
}
