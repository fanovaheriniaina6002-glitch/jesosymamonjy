package com.example.ui.adapters

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.local.SongEntity
import com.google.android.material.card.MaterialCardView

/**
 * Rule 4: Lightweight & Memoized Card Adapter for Dashboard History ("Hira nampiasaina farany").
 * Uses only pre-indexed properties (id, numberStr, category, title, snippet, isFavorite).
 */
class SongAdapter(
    private val onSongClick: (SongEntity) -> Unit,
    private val onFavoriteToggle: (SongEntity) -> Unit
) : ListAdapter<SongEntity, SongAdapter.SongViewHolder>(DiffCallback) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song_bento, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.card_song_item)
        private val tvNumber: TextView = itemView.findViewById(R.id.tv_song_number)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_song_title)
        private val tvPreview: TextView = itemView.findViewById(R.id.tv_song_preview)
        private val tvCategoryBadge: TextView = itemView.findViewById(R.id.tv_song_category)
        private val tvReadCta: TextView = itemView.findViewById(R.id.tv_song_read_cta)
        private val cardFavBtn: View? = itemView.findViewById(R.id.card_favorite_btn)
        private val btnFavToggle: ImageButton = itemView.findViewById(R.id.btn_favorite_toggle)

        private var boundSong: SongEntity? = null

        @SuppressLint("ClickableViewAccessibility")
        private val gestureDetector = GestureDetector(
            itemView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    boundSong?.let(onSongClick)
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    boundSong?.let(onFavoriteToggle)
                    return true
                }
            }
        )

        init {
            card.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                false
            }
            card.setOnClickListener {
                // Single/Double tap handled by GestureDetector
            }
            val toggleFavListener = View.OnClickListener {
                boundSong?.let(onFavoriteToggle)
            }
            btnFavToggle.setOnClickListener(toggleFavListener)
            cardFavBtn?.setOnClickListener(toggleFavListener)
        }

        fun bind(song: SongEntity) {
            boundSong = song
            val isDark = com.example.ui.theme.ThemeManager.isDarkMode.value
            val cardBg = if (isDark) com.example.ui.theme.ThemeManager.DARK_SURFACE else 0xFFFFFFFF.toInt()
            val cardBorder = if (isDark) com.example.ui.theme.ThemeManager.DARK_BORDER else 0xFFEAE0E2.toInt()
            val titleColor = if (isDark) com.example.ui.theme.ThemeManager.DARK_TEXT_PRIMARY else 0xFF1C1B1F.toInt()
            val previewColor = if (isDark) com.example.ui.theme.ThemeManager.DARK_TEXT_SECONDARY else 0xFF49454F.toInt()
            val dp1 = (1f * itemView.resources.displayMetrics.density).toInt().coerceAtLeast(1)

            com.example.ui.theme.ThemeManager.stabilizeCardForApi23(card, cardBg, cardBorder, dp1)
            tvTitle.setTextColor(titleColor)
            tvPreview.setTextColor(previewColor)
            com.example.ui.theme.ThemeManager.applyThemeToViewTree(itemView, isDark)

            tvNumber.text = song.numberStr
            tvTitle.text = song.title
            tvPreview.text = "“${song.snippet}”"
            tvCategoryBadge.text = song.category
            tvReadCta.setTextColor(if (isDark) 0xFFD4AF37.toInt() else 0xFF781C2E.toInt())
            btnFavToggle.setImageResource(
                if (song.isFavorite) R.drawable.ic_heart else R.drawable.ic_favorite_border
            )
            val favTint = when {
                !isDark -> 0xFF781C2E.toInt()
                song.isFavorite -> 0xFFD4AF37.toInt()
                else -> 0xFFA69497.toInt()
            }
            com.example.ui.theme.ThemeManager.setSolidIconTint(btnFavToggle, favTint)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<SongEntity>() {
            override fun areItemsTheSame(oldItem: SongEntity, newItem: SongEntity): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: SongEntity, newItem: SongEntity): Boolean =
                oldItem.id == newItem.id &&
                    oldItem.title == newItem.title &&
                    oldItem.snippet == newItem.snippet &&
                    oldItem.category == newItem.category &&
                    oldItem.isFavorite == newItem.isFavorite
        }
    }
}
