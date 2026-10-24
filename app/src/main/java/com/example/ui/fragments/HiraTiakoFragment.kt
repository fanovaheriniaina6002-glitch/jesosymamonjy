package com.example.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.HymnCatalog
import com.example.data.local.SongEntity
import com.example.data.repository.SongRepository
import com.example.ui.adapters.HiraTiakoSongAdapter
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HiraTiakoFragment : Fragment() {

    private lateinit var repository: SongRepository
    private lateinit var favoriteAdapter: HiraTiakoSongAdapter

    private var allFavoriteSongs: List<SongEntity> = emptyList()
    private var currentCategoryFilter: String = "REHETRA" // "REHETRA", "LEHIBE", "FANAMPINY"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_hira_tiako, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext()
        HymnCatalog.ensureInitializedAsync(ctx)
        val dao = AppDatabase.getInstance(ctx).songDao()
        repository = SongRepository(dao, ctx.applicationContext)

        val tvHeaderTitle = view.findViewById<TextView>(R.id.tv_hira_tiako_header_title)
        val tvHeaderSubtitle = view.findViewById<TextView>(R.id.tv_hira_tiako_subtitle)

        val chipAll = view.findViewById<MaterialCardView>(R.id.chip_fav_all)
        val chipLehibe = view.findViewById<MaterialCardView>(R.id.chip_fav_lehibe)
        val chipFanampiny = view.findViewById<MaterialCardView>(R.id.chip_fav_fanampiny)

        val iconChipAll = view.findViewById<ImageView>(R.id.icon_fav_chip_all)
        val tvChipAll = view.findViewById<TextView>(R.id.tv_fav_chip_all)
        val iconChipLehibe = view.findViewById<ImageView>(R.id.icon_fav_chip_lehibe)
        val tvChipLehibe = view.findViewById<TextView>(R.id.tv_fav_chip_lehibe)
        val iconChipFanampiny = view.findViewById<ImageView>(R.id.icon_fav_chip_fanampiny)
        val tvChipFanampiny = view.findViewById<TextView>(R.id.tv_fav_chip_fanampiny)

        val tvCount = view.findViewById<TextView>(R.id.tv_hira_tiako_count)
        val cardEmptyFavorites = view.findViewById<MaterialCardView>(R.id.card_favorites_empty_state)
        val tvEmpty = view.findViewById<TextView>(R.id.tv_empty_favorites)
        val rvFavorites = view.findViewById<RecyclerView>(R.id.rv_favorite_songs)

        cardEmptyFavorites?.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(MainActivity.TAB_HIKAROKA)
        }

        favoriteAdapter = HiraTiakoSongAdapter(
            onSongClick = { song ->
                HymnCatalog.recordSongConsulted(requireContext(), song.number)
                (activity as? MainActivity)?.openHymnReader(song.number)
            },
            onRemoveFavorite = { song ->
                HymnCatalog.removeFavorite(requireContext(), song.number)
            }
        )

        rvFavorites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoriteAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }

        com.example.ui.util.SwipeToDeleteHelper.attachLeftSwipeToDelete(
            context = requireContext(),
            recyclerView = rvFavorites,
            onSwipedAtPosition = { position ->
                val song = favoriteAdapter.currentList.getOrNull(position) ?: return@attachLeftSwipeToDelete
                HymnCatalog.removeFavorite(requireContext(), song.number)
            }
        )

        fun updateFilterChipsUI() {
            val isDark = com.example.ui.theme.ThemeManager.isDarkMode.value
            val activeBg = if (isDark) 0xFF800020.toInt() else 0xFF5A1827.toInt()
            val inactiveBg = if (isDark) 0xFF1E1214.toInt() else 0xFFFFFFFF.toInt()
            val white = 0xFFFFFFFF.toInt()
            val inactiveColor = if (isDark) 0xFFA69497.toInt() else 0xFF564243.toInt()

            fun styleChip(
                card: MaterialCardView,
                icon: ImageView,
                tv: TextView,
                isActive: Boolean
            ) {
                com.example.ui.theme.ThemeManager.stabilizeCardForApi23(
                    card,
                    if (isActive) activeBg else inactiveBg,
                    0,
                    0
                )
                com.example.ui.theme.ThemeManager.setSolidIconTint(icon, if (isActive) white else inactiveColor)
                tv.setTextColor(if (isActive) white else inactiveColor)
            }

            styleChip(chipAll, iconChipAll, tvChipAll, currentCategoryFilter == "REHETRA")
            styleChip(chipLehibe, iconChipLehibe, tvChipLehibe, currentCategoryFilter == "LEHIBE")
            styleChip(chipFanampiny, iconChipFanampiny, tvChipFanampiny, currentCategoryFilter == "FANAMPINY")
        }
        updateFilterChipsUI()

        fun applyFilterAndRender() {
            val filtered = when (currentCategoryFilter) {
                "LEHIBE" -> allFavoriteSongs.filter { it.category == "Fihirana Lehibe" || it.category == "Fihirana Taloha" }
                "FANAMPINY" -> allFavoriteSongs.filter { it.category == "Fihirana Vaovao" }
                else -> allFavoriteSongs
            }
            val isEmpty = filtered.isEmpty()
            favoriteAdapter.submitList(filtered)
            tvCount.text = "${filtered.size} hira voatahiry"
            cardEmptyFavorites?.visibility = if (isEmpty) View.VISIBLE else View.GONE
            tvEmpty?.visibility = if (isEmpty) View.VISIBLE else View.GONE
            rvFavorites.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        chipAll.setOnClickListener {
            currentCategoryFilter = "REHETRA"
            updateFilterChipsUI()
            applyFilterAndRender()
        }
        chipLehibe.setOnClickListener {
            currentCategoryFilter = "LEHIBE"
            updateFilterChipsUI()
            applyFilterAndRender()
        }
        chipFanampiny.setOnClickListener {
            currentCategoryFilter = "FANAMPINY"
            updateFilterChipsUI()
            applyFilterAndRender()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    repository.favoriteSongs.collectLatest { favorites ->
                        allFavoriteSongs = favorites
                        applyFilterAndRender()
                    }
                }
                launch {
                    com.example.ui.theme.ThemeManager.isDarkMode.collectLatest { isDark ->
                        applyThemeSync(isDark)
                    }
                }
            }
        }
        applyThemeSync(com.example.ui.theme.ThemeManager.isDarkMode.value)
    }

    fun applyThemeSync(isDark: Boolean = com.example.ui.theme.ThemeManager.isDarkMode.value) {
        val root = view ?: return
        com.example.ui.theme.ThemeManager.applyThemeToViewTree(root, isDark)
        root.findViewById<TextView>(R.id.tv_hira_tiako_header_title)?.setTextColor(
            if (isDark) 0xFFF5E6E8.toInt() else 0xFF781C2E.toInt()
        )
        root.findViewById<TextView>(R.id.tv_hira_tiako_subtitle)?.setTextColor(
            if (isDark) 0xFFA69497.toInt() else 0xFF564243.toInt()
        )

        val activeBg = if (isDark) 0xFF800020.toInt() else 0xFF5A1827.toInt()
        val inactiveBg = if (isDark) 0xFF1E1214.toInt() else 0xFFFFFFFF.toInt()
        val white = 0xFFFFFFFF.toInt()
        val inactiveColor = if (isDark) 0xFFA69497.toInt() else 0xFF564243.toInt()

        fun styleChip(cardId: Int, iconId: Int, tvId: Int, isActive: Boolean) {
            root.findViewById<MaterialCardView>(cardId)?.let { card ->
                com.example.ui.theme.ThemeManager.stabilizeCardForApi23(
                    card,
                    if (isActive) activeBg else inactiveBg,
                    0,
                    0
                )
            }
            com.example.ui.theme.ThemeManager.setSolidIconTint(
                root.findViewById(iconId),
                if (isActive) white else inactiveColor
            )
            root.findViewById<TextView>(tvId)?.setTextColor(if (isActive) white else inactiveColor)
        }
        styleChip(R.id.chip_fav_all, R.id.icon_fav_chip_all, R.id.tv_fav_chip_all, currentCategoryFilter == "REHETRA")
        styleChip(R.id.chip_fav_lehibe, R.id.icon_fav_chip_lehibe, R.id.tv_fav_chip_lehibe, currentCategoryFilter == "LEHIBE")
        styleChip(R.id.chip_fav_fanampiny, R.id.icon_fav_chip_fanampiny, R.id.tv_fav_chip_fanampiny, currentCategoryFilter == "FANAMPINY")
    }
}
