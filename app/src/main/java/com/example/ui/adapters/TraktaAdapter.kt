package com.example.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.local.TraktaEntity
import com.google.android.material.card.MaterialCardView

class TraktaAdapter(
    private val onTraktaClick: (TraktaEntity) -> Unit
) : ListAdapter<TraktaEntity, TraktaAdapter.TraktaViewHolder>(DiffCallback) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TraktaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trakta_bento, parent, false)
        return TraktaViewHolder(view, onTraktaClick)
    }

    override fun onBindViewHolder(holder: TraktaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TraktaViewHolder(
        itemView: View,
        private val onTraktaClick: (TraktaEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardRoot: View = itemView.findViewById(R.id.card_trakta_item)
        private val cardCover: MaterialCardView = itemView.findViewById(R.id.card_trakta_cover)
        private val ivCover: ImageView = itemView.findViewById(R.id.iv_trakta_cover)
        private val tvNumber: TextView = itemView.findViewById(R.id.tv_trakta_number)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_trakta_title)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tv_trakta_author)
        private val layoutAuthor: View? = itemView.findViewById(R.id.layout_trakta_author)
        private val btnRead: View = itemView.findViewById(R.id.btn_trakta_read)

        private var currentItem: TraktaEntity? = null

        init {
            val openListener = View.OnClickListener {
                currentItem?.let(onTraktaClick)
            }
            itemView.setOnClickListener(openListener)
            cardRoot.setOnClickListener(openListener)
            cardCover.setOnClickListener(openListener)
            btnRead.setOnClickListener(openListener)
        }

        fun bind(item: TraktaEntity) {
            currentItem = item
            com.example.ui.theme.ThemeManager.applyThemeToViewTree(
                itemView,
                com.example.ui.theme.ThemeManager.isDarkMode.value
            )
            tvNumber.text = item.numberLabel
            tvTitle.text = item.title

            val author = item.author.ifEmpty { com.example.data.local.getTraktaAuthor(item.id) }
            if (author.isNotEmpty()) {
                layoutAuthor?.visibility = View.VISIBLE
                tvAuthor.text = author
            } else {
                layoutAuthor?.visibility = View.GONE
            }

            runCatching {
                cardCover.setCardBackgroundColor(Color.parseColor(item.coverBgHex))
            }

            val fallbackCover = when (item.coverKey) {
                "trakta_1" -> R.drawable.ic_trakta_cover_1
                "trakta_2" -> R.drawable.ic_trakta_cover_2
                "trakta_3" -> R.drawable.ic_trakta_cover_3
                "trakta_4" -> R.drawable.ic_trakta_cover_4
                else -> R.drawable.ic_trakta_cover_1
            }
            com.example.ui.util.AssetImageLoader.loadInto(
                imageView = ivCover,
                assetFileName = item.coverKey,
                reqWidthDp = 160,
                reqHeightDp = 220,
                fallbackResId = fallbackCover
            )
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TraktaEntity>() {
            override fun areItemsTheSame(oldItem: TraktaEntity, newItem: TraktaEntity): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: TraktaEntity, newItem: TraktaEntity): Boolean =
                oldItem == newItem
        }
    }
}
