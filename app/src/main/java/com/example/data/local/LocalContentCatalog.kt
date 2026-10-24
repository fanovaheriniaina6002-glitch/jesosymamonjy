package com.example.data.local

import android.content.Context
import com.example.ui.util.AssetImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LocalContentCatalog {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val traktaLock = Any()

    @Volatile
    private var cachedTraktas: List<TraktaEntity>? = null

    private val _traktaEntitiesFlow = MutableStateFlow<List<TraktaEntity>>(emptyList())
    val traktaEntitiesFlow: StateFlow<List<TraktaEntity>> = _traktaEntitiesFlow.asStateFlow()

    @Volatile
    private var cachedArchivePhotos: List<ArchivePhotoEntity>? = null

    private data class TraktaSpec(
        val id: Int,
        val numberLabel: String,
        val title: String,
        val txtFile: String,
        val coverFile: String,
        val coverBgHex: String
    )

    private data class RakitsarySpec(
        val id: Int,
        val title: String,
        val subtitle: String,
        val description: String,
        val category: String,
        val fileName: String
    )

    private val traktaSpecs = listOf(
        TraktaSpec(
            id = 1,
            numberLabel = "N° 01",
            title = "Afaka Miaina Tsy Manota ve ny Kristiana ?",
            txtFile = "afaka_miaina_tsy_manota_ve_ny_kristiana.txt",
            coverFile = "afaka_miaina_tsy_manota_ve_ny_kristiana.jpg",
            coverBgHex = "#351019"
        ),
        TraktaSpec(
            id = 2,
            numberLabel = "N° 02",
            title = "Fiaran-dalamby Mankany An-danitra",
            txtFile = "fiarandalamby_makany_andanitra.txt",
            coverFile = "fiarandalamby_makany_andanitra.jpg",
            coverBgHex = "#2C1810"
        ),
        TraktaSpec(
            id = 3,
            numberLabel = "N° 03",
            title = "Ny Finoana Marina Handraisana Fanasitranana",
            txtFile = "finoana_marina_nohandraisana_fanasitranana.txt",
            coverFile = "finoana_marina_nohandraisana_fanasitranana.jpeg",
            coverBgHex = "#1A1A24"
        ),
        TraktaSpec(
            id = 4,
            numberLabel = "N° 04",
            title = "Lalana Roa",
            txtFile = "lalana_roa.txt",
            coverFile = "lalana_roa.jpg",
            coverBgHex = "#28181A"
        ),
        TraktaSpec(
            id = 5,
            numberLabel = "N° 05",
            title = "Mety ho Anao ny Famonjena",
            txtFile = "mety_ho_anao_ny_famonjena.txt",
            coverFile = "mety_ho_anao_ny_famonjena.jpeg",
            coverBgHex = "#351019"
        ),
        TraktaSpec(
            id = 6,
            numberLabel = "N° 06",
            title = "Mety ho Sitrana Ianao",
            txtFile = "mety_ho_sitrana_ianao(m.a_daoud).txt",
            coverFile = "mety_ho_sitrana_ianao_ma_daoud.jpeg",
            coverBgHex = "#2C1810"
        ),
        TraktaSpec(
            id = 7,
            numberLabel = "N° 07",
            title = "Miverena any Betela",
            txtFile = "miverena_any_betela.txt",
            coverFile = "miverena_any_betela.jpg",
            coverBgHex = "#1A1A24"
        ),
        TraktaSpec(
            id = 8,
            numberLabel = "N° 08",
            title = "Ny Fanasitranan'Andriamanitra Anao",
            txtFile = "ny_fanasitrananan_andriamanitra_anao.txt",
            coverFile = "ny_fanasitrananan_andriamanitra_anao.jpg",
            coverBgHex = "#28181A"
        ),
        TraktaSpec(
            id = 9,
            numberLabel = "N° 09",
            title = "Ny Hery Mahadion'ny Ràn'i Jesosy",
            txtFile = "ny_hery_mahadion_ny_ran_i_jesosy.txt",
            coverFile = "ny_hery_mahadion_ny_ran_i_jesosy.jpg",
            coverBgHex = "#351019"
        ),
        TraktaSpec(
            id = 10,
            numberLabel = "N° 10",
            title = "Paska Iray Tsy Hay Hadinoina Tany Martinika",
            txtFile = "paska_iray_tsy_hay_hadinoina_tany_martinika.txt",
            coverFile = "paska_iray_tsy_hay_hadinoina_tany_martinika.jpeg",
            coverBgHex = "#2C1810"
        ),
        TraktaSpec(
            id = 11,
            numberLabel = "N° 11",
            title = "Soa Ihany",
            txtFile = "soa_ihany.txt",
            coverFile = "soa_ihany.jpg",
            coverBgHex = "#1A1A24"
        ),
        TraktaSpec(
            id = 12,
            numberLabel = "N° 12",
            title = "Tafasokatra ve ny Vavahadin'ny Helo ?",
            txtFile = "tafasokatra_ve_ny_vavahadin_ny_helo.txt",
            coverFile = "tafasokatra_ve_ny_vavahadin_ny_helo.webp",
            coverBgHex = "#28181A"
        ),
        TraktaSpec(
            id = 13,
            numberLabel = "N° 13",
            title = "Tena Misy ny Lanitra",
            txtFile = "tena_misy_ny_lanitra.txt",
            coverFile = "tena_misy_ny_lanitra.webp",
            coverBgHex = "#351019"
        )
    )

    private val rakitsaryImageById = mapOf(
        1 to "mpitandrina_amin_ny_fiangonana_jesosy_mamonjy.webp",
        2 to "mpitandrina_amin_ny_fiangonana_jesosy_mamonjy1.webp",
        3 to "hady_fototra_fiangonana_jesosy_mamonjy_atanety_be_bord.jpg",
        4 to "fitoriana_filazan_tsara_ambatondrazaka3.jpg",
        5 to "fitoriana_filazan_tsara_ambatondrazaka2.jpg",
        6 to "fitoriana_filazan_tsara_ambatondrazaka.jpg",
        7 to "evangelisation_tamatave.jpg",
        8 to "vatomandry_maintinandry.jpg",
        9 to "vakinankaratra_behenjy.jpg",
        10 to "toamasina_mangarano.jpg",
        11 to "talata_apano_vohibato_fianarantsoa.jpg",
        12 to "sandravola_atsimondrano.jpg",
        13 to "morafeno_ankazobe.jpg",
        14 to "manjakandriana_ankazondandy.jpg",
        15 to "mangarivotra_andriba_maevatanana.jpg",
        16 to "mangaika_ampasy_fort_dauphin.jpg",
        17 to "maibahoaka_ivato.png",
        18 to "maibahoaka_ambohidratrimo.jpg",
        19 to "mahanoro_menagisa.jpg",
        20 to "maevatanana.jpg",
        21 to "ivory_avaratra_fianarantsoa.jpg",
        22 to "fjm_carion_manjakandriana.jpg",
        23 to "fjm_andandihazo_alakamisy_fenoarivo.jpg",
        24 to "fjm_ambositra.jpg",
        25 to "fjm_ambatomaro.jpg",
        26 to "fianarantsoa_ambahisamotra.jpg",
        27 to "bongolava_ambohimarina.jpg",
        28 to "behenjy.jpg",
        29 to "avarajozoro_ambohitratrimo.jpg",
        30 to "avaradrano_ankadinandriana.jpg",
        31 to "avaradrano_ambohimangakely.jpg",
        32 to "atsimondrano_ampitatafika.jpg",
        33 to "atsimondrano_ambatofotsy_gara.jpg",
        34 to "anjomakely_betoho_manjakandriana.jpg",
        35 to "anjanakazo_tsiroanomandidy_bongolava.jpg",
        36 to "andranovelona_antanetibe_mahazaza_ambohidratrimo.jpg",
        37 to "ambohitsararay_manjakandriana.jpg",
        38 to "ambohipo_foibe.jpg",
        39 to "ambohimamory_ambatofotsy_gara.jpg",
        40 to "ambohidratrimo_atsahamavory.jpg",
        41 to "ambatohazaza_mahavelona_ankazobe.png",
        42 to "ambatoazaza_ankazobe.jpg",
        43 to "evangelision_toamasina_15_16_aout_2025.webp",
        44 to "fitokanampiangonana_toamasina_17_aout_2025.webp"
    )

    private val defaultRakitsarySpecs = listOf(
        RakitsarySpec(1, "Mpitandrina", "Mpitandrina ao amin'ny Fiangonana", "Ireo mpitandrina ao amin'ny Fiangonana Jesosy Mamonjy M.A DAOUD", "Mpitandrina", rakitsaryImageById.getValue(1)),
        RakitsarySpec(2, "Mpitandrina", "Mpitandrina ao amin'ny Fiangonana", "Ireo mpitandrina ao amin'ny Fiangonana Jesosy Mamonjy M.A DAOUD", "Mpitandrina", rakitsaryImageById.getValue(2)),
        RakitsarySpec(3, "Hady Fototra", "Fiangonana Jesosy Mamonjy Atanety Be Nord", "Hady fototra fiangonana Jesosy Mamonjy M.A Daoud Atanety be Nord", "Fotoana", rakitsaryImageById.getValue(3)),
        RakitsarySpec(4, "Fitoriana Filazan-tsara", "Ambatondrazaka", "Fitoriana Filazantsara ataon'ireo rahalahy sy anabavy any Ambatondrazaka", "Fotoana", rakitsaryImageById.getValue(4)),
        RakitsarySpec(5, "Fitoriana Filazan-tsara", "Ambatondrazaka", "Fitoriana Filazantsara ataon'ireo rahalahy sy anabavy any Ambatondrazaka", "Fotoana", rakitsaryImageById.getValue(5)),
        RakitsarySpec(6, "Fitoriana Filazan-tsara", "Ambatondrazaka", "Fitoriana Filazantsara ataon'ireo rahalahy sy anabavy any Ambatondrazaka", "Fotoana", rakitsaryImageById.getValue(6)),
        RakitsarySpec(7, "Evangélisation", "Tamatave", "Evangelision notanterahina tany Toamasina ny ", "Fotoana", rakitsaryImageById.getValue(7)),
        RakitsarySpec(8, "Jesosy Mamonjy M.A Daoud", "Vatomandry Maintinandry", "Fiangonana Jesosy Mamonjy M.A any Vatomandry Maintinandry", "Fiangonana", rakitsaryImageById.getValue(8)),
        RakitsarySpec(9, "Jesosy Mamonjy M.A Daoud", "Vakinankaratra Behenjy", "Fiangonana Jesosy Mamonjy M.A any Vakinankaratra Behenjy", "Fiangonana", rakitsaryImageById.getValue(9)),
        RakitsarySpec(10, "Jesosy Mamonjy M.A Daoud", "Toamasina Mangarano", "Fiangonana Jesosy Mamonjy M.A any Toamasina Mangarano", "Fiangonana", rakitsaryImageById.getValue(10)),
        RakitsarySpec(11, "Jesosy Mamonjy M.A Daoud", "Talata Ampano Vohibato Fianarantsoa", "Fiangonana Jesosy Mamonjy M.A any Talata Ampano Vohibato Fianarantsoa", "Fiangonana", rakitsaryImageById.getValue(11)),
        RakitsarySpec(12, "Jesosy Mamonjy M.A Daoud", "Sandravola Atsimondrano", "Fiangonana Jesosy Mamonjy M.A any Sandravola Atsimondrano", "Fiangonana", rakitsaryImageById.getValue(12)),
        RakitsarySpec(13, "Jesosy Mamonjy M.A Daoud", "Morafeno Ankazobe", "Fiangonana Jesosy Mamonjy M.A any Morafeno Ankazobe", "Fiangonana", rakitsaryImageById.getValue(13)),
        RakitsarySpec(14, "Jesosy Mamonjy M.A Daoud", "Manjakandriana Ankazondandy", "Fiangonana Jesosy Mamonjy M.A any Manjakandriana Ankazondandy", "Fiangonana", rakitsaryImageById.getValue(14)),
        RakitsarySpec(15, "Jesosy Mamonjy M.A Daoud", "Mangarivotra Andriba Maevatanana", "Fiangonana Jesosy Mamonjy M.A any Mangarivotra Andriba Maevatanana", "Fiangonana", rakitsaryImageById.getValue(15)),
        RakitsarySpec(16, "Jesosy Mamonjy M.A Daoud", "Mangaika Ampasy Fort-Dauphin", "Fiangonana Jesosy Mamonjy M.A any Mangaika Ampasy Fort-Dauphin", "Fiangonana", rakitsaryImageById.getValue(16)),
        RakitsarySpec(17, "Jesosy Mamonjy M.A Daoud", "Maibahoaka Ivato", "Fiangonana Jesosy Mamonjy M.A any Maibahoaka Ivato", "Fiangonana", rakitsaryImageById.getValue(17)),
        RakitsarySpec(18, "Jesosy Mamonjy M.A Daoud", "Maibahoaka Ambohidratrimo", "Fiangonana Jesosy Mamonjy M.A any Maibahoaka Ambohidratrimo", "Fiangonana", rakitsaryImageById.getValue(18)),
        RakitsarySpec(19, "Jesosy Mamonjy M.A Daoud", "Mahanoro Menagisa", "Fiangonana Jesosy Mamonjy M.A any Mahanoro Menagisa", "Fiangonana", rakitsaryImageById.getValue(19)),
        RakitsarySpec(20, "Jesosy Mamonjy M.A Daoud", "Maevatanana", "Fiangonana Jesosy Mamonjy M.A any Maevatanana", "Fiangonana", rakitsaryImageById.getValue(20)),
        RakitsarySpec(21, "Jesosy Mamonjy M.A Daoud", "Ivory Avaratra Fianarantsoa", "Fiangonana Jesosy Mamonjy M.A any Ivory Avaratra Fianarantsoa", "Fiangonana", rakitsaryImageById.getValue(21)),
        RakitsarySpec(22, "Jesosy Mamonjy M.A Daoud", "FJM Carion Manjakandriana", "Fiangonana Jesosy Mamonjy M.A any FJM Carion Manjakandriana", "Fiangonana", rakitsaryImageById.getValue(22)),
        RakitsarySpec(23, "Jesosy Mamonjy M.A Daoud", "FJM Andandihazo Alakamisy Fenoarivo", "Fiangonana Jesosy Mamonjy M.A any FJM Andandihazo Alakamisy Fenoarivo", "Fiangonana", rakitsaryImageById.getValue(23)),
        RakitsarySpec(24, "Jesosy Mamonjy M.A Daoud", "FJM Ambositra", "Fiangonana Jesosy Mamonjy M.A any FJM Ambositra", "Fiangonana", rakitsaryImageById.getValue(24)),
        RakitsarySpec(25, "Jesosy Mamonjy M.A Daoud", "FJM Ambatomaro", "Fiangonana Jesosy Mamonjy M.A any FJM Ambatomaro", "Fiangonana", rakitsaryImageById.getValue(25)),
        RakitsarySpec(26, "Jesosy Mamonjy M.A Daoud", "Fianarantsoa Ambahisamotra", "Fiangonana Jesosy Mamonjy M.A any Fianarantsoa Ambahisamotra", "Fiangonana", rakitsaryImageById.getValue(26)),
        RakitsarySpec(27, "Jesosy Mamonjy M.A Daoud", "Bongolava Ambohimarina", "Fiangonana Jesosy Mamonjy M.A any Bongolava Ambohimarina", "Fiangonana", rakitsaryImageById.getValue(27)),
        RakitsarySpec(28, "Jesosy Mamonjy M.A Daoud", "Behenjy", "Fiangonana Jesosy Mamonjy M.A any Behenjy", "Fiangonana", rakitsaryImageById.getValue(28)),
        RakitsarySpec(29, "Jesosy Mamonjy M.A Daoud", "Avarajozoro Ambohidratrimo", "Fiangonana Jesosy Mamonjy M.A any Avarajozoro Ambohidratrimo", "Fiangonana", rakitsaryImageById.getValue(29)),
        RakitsarySpec(30, "Jesosy Mamonjy M.A Daoud", "Avaradrano Ankadinandriana", "Fiangonana Jesosy Mamonjy M.A any Avaradrano Ankadinandriana", "Fiangonana", rakitsaryImageById.getValue(30)),
        RakitsarySpec(31, "Jesosy Mamonjy M.A Daoud", "Avaradrano Ambohimangakely", "Fiangonana Jesosy Mamonjy M.A any Avaradrano Ambohimangakely", "Fiangonana", rakitsaryImageById.getValue(31)),
        RakitsarySpec(32, "Jesosy Mamonjy M.A Daoud", "Atsimondrano Ampitatafika", "Fiangonana Jesosy Mamonjy M.A any Atsimondrano Ampitatafika", "Fiangonana", rakitsaryImageById.getValue(32)),
        RakitsarySpec(33, "Jesosy Mamonjy M.A Daoud", "Atsimondrano Ambatofotsy-Gara", "Fiangonana Jesosy Mamonjy M.A any Atsimondrano Ambatofotsy-Gara", "Fiangonana", rakitsaryImageById.getValue(33)),
        RakitsarySpec(34, "Jesosy Mamonjy M.A Daoud", "Anjomakely Betoho Manjakandriana", "Fiangonana Jesosy Mamonjy M.A any Anjomakely Betoho Manjakandriana", "Fiangonana", rakitsaryImageById.getValue(34)),
        RakitsarySpec(35, "Jesosy Mamonjy M.A Daoud", "Anjanakazo Tsiroanomandidy Bongolava", "Fiangonana Jesosy Mamonjy M.A any Anjanakazo Tsiroanomandidy Bongolava", "Fiangonana", rakitsaryImageById.getValue(35)),
        RakitsarySpec(36, "Jesosy Mamonjy M.A Daoud", "Andranovelona Antanetibe Mahazaza Ambohidratrimo", "Fiangonana Jesosy Mamonjy M.A any Andranovelona Antanetibe Mahazaza Ambohidratrimo", "Fiangonana", rakitsaryImageById.getValue(36)),
        RakitsarySpec(37, "Jesosy Mamonjy M.A Daoud", "Ambohitsararay Manjakandriana", "Fiangonana Jesosy Mamonjy M.A any Ambohitsararay Manjakandriana", "Fiangonana", rakitsaryImageById.getValue(37)),
        RakitsarySpec(38, "Jesosy Mamonjy M.A Daoud", "Ambohipo Foibe", "Fiangonana Jesosy Mamonjy M.A any Ambohipo Foibe", "Fiangonana", rakitsaryImageById.getValue(38)),
        RakitsarySpec(39, "Jesosy Mamonjy M.A Daoud", "Ambohimamory Ambatofotsy-Gara", "Fiangonana Jesosy Mamonjy M.A any Ambohimamory Ambatofotsy-Gara", "Fiangonana", rakitsaryImageById.getValue(39)),
        RakitsarySpec(40, "Jesosy Mamonjy M.A Daoud", "Ambohidratrimo Atsahamavory", "Fiangonana Jesosy Mamonjy M.A any Ambohidratrimo Atsahamavory", "Fiangonana", rakitsaryImageById.getValue(40)),
        RakitsarySpec(41, "Jesosy Mamonjy M.A Daoud", "Ambatohazaza Mahavelona Ankazobe", "Fiangonana Jesosy Mamonjy M.A any Ambatohazaza Mahavelona Ankazobe", "Fiangonana", rakitsaryImageById.getValue(41)),
        RakitsarySpec(42, "Jesosy Mamonjy M.A Daoud", "Ambatoazaza Ankazobe", "Fiangonana Jesosy Mamonjy M.A any Ambatoazaza Ankazobe", "Fiangonana", rakitsaryImageById.getValue(42)),
        RakitsarySpec(43, "Évangélisation", "Toamasina", "Évangélisation notanterahin'ireo rahalahy sy anabavy tany Toamasina ny 15-16 Août 2025", "Fotoana", rakitsaryImageById.getValue(43)),
        RakitsarySpec(44, "Fitokanana Fiangonana", "Toamasina", "Fitokanana ny Fiangonana Jesosy Mamonjy M.A DAOUD Toamasina ny 17 Août 2025", "Fotoana", rakitsaryImageById.getValue(44))
    )

    fun getCachedTraktasOrEmpty(): List<TraktaEntity> = cachedTraktas ?: emptyList()

    fun ensureTraktasLoadedAsync(context: Context) {
        if (cachedTraktas != null) return
        val appContext = context.applicationContext
        ioScope.launch {
            loadTraktaEntities(appContext)
        }
    }

    suspend fun loadTraktaEntitiesIo(context: Context): List<TraktaEntity> =
        withContext(Dispatchers.IO) {
            loadTraktaEntities(context.applicationContext)
        }

    fun loadTraktaEntities(context: Context): List<TraktaEntity> {
        cachedTraktas?.let { return it }
        synchronized(traktaLock) {
            cachedTraktas?.let { return it }
            val appContext = context.applicationContext
            val loaded = traktaSpecs.map { spec ->
                val rawText = AssetImageLoader.readAssetText(appContext, spec.txtFile)
                    .lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n\n")
                TraktaEntity(
                    id = spec.id,
                    numberLabel = spec.numberLabel,
                    title = spec.title,
                    author = getTraktaAuthor(spec.id),
                    coverKey = spec.coverFile,
                    coverBgHex = spec.coverBgHex,
                    content = rawText,
                    isOffline = true
                )
            }
            cachedTraktas = loaded
            _traktaEntitiesFlow.value = loaded
            return loaded
        }
    }

    fun loadArchivePhotoEntities(context: Context): List<ArchivePhotoEntity> {
        cachedArchivePhotos?.let { return it }
        val raw = AssetImageLoader.readAssetText(context, "tahirintsary_id_description.txt")
        val itemRegex = Regex(
            """id\s*[=:]\s*(\d+)\s*,?\s*title\s*[=:]\s*"([^"]*)"\s*,?\s*subtitle\s*[=:]\s*"([^"]*)"\s*,?\s*description\s*[=:]\s*"([^"]*)"\s*,?\s*category\s*[=:]\s*"([^"]*)"""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )

        val parsedById = if (raw.isNotBlank()) {
            itemRegex.findAll(raw).mapNotNull { match ->
                val id = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                val title = match.groupValues[2].trim()
                val subtitle = match.groupValues[3].trim()
                val description = match.groupValues[4].trim()
                val rawCategory = match.groupValues[5].trim()

                val normalizedCategory = when {
                    rawCategory.equals("fotoana", ignoreCase = true) -> "Fotoana"
                    rawCategory.equals("mpitandrina", ignoreCase = true) -> "Mpitandrina"
                    rawCategory.equals("fiangonana", ignoreCase = true) -> "Fiangonana"
                    else -> rawCategory.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }

                val fileName = rakitsaryImageById[id] ?: ""

                id to ArchivePhotoEntity(
                    id = id,
                    title = title,
                    subtitle = subtitle,
                    year = normalizedCategory,
                    location = subtitle,
                    category = normalizedCategory,
                    description = description,
                    drawableKey = fileName,
                    isFeatured = (id == 1)
                )
            }.toMap()
        } else {
            emptyMap()
        }

        return defaultRakitsarySpecs.map { spec ->
            parsedById[spec.id] ?: ArchivePhotoEntity(
                id = spec.id,
                title = spec.title,
                subtitle = spec.subtitle,
                year = spec.category,
                location = spec.subtitle,
                category = spec.category,
                description = spec.description,
                drawableKey = spec.fileName,
                isFeatured = (spec.id == 1)
            )
        }.sortedBy { it.id }.also { cachedArchivePhotos = it }
    }
}
