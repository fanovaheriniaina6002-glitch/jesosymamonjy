package com.example.ui.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.ui.theme.ThemeManager

/**
 * 60 FPS Left Swipe-to-Delete (EndToStart) helper for Bento song cards in
 * HiraTiakoFragment and FandraisanaFragment ("Hira vao novakiana").
 */
object SwipeToDeleteHelper {

    fun attachLeftSwipeToDelete(
        context: Context,
        recyclerView: RecyclerView,
        canSwipePosition: (Int) -> Boolean = { true },
        onSwipedAtPosition: (Int) -> Unit
    ) {
        val density = context.resources.displayMetrics.density
        val cornerRadiusPx = 24f * density
        val iconMarginPx = (22f * density).toInt()
        val iconSizePx = (24f * density).toInt()
        val bottomMarginPx = 12f * density

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val bgRect = RectF()
        val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)?.mutate()

        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val pos = viewHolder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION || !canSwipePosition(pos)) return 0
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSwipedAtPosition(position)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                if (dX < 0f) {
                    val isDark = ThemeManager.isDarkMode.value
                    bgPaint.color = if (isDark) 0xFF8B1E32.toInt() else 0xFF781C2E.toInt()

                    val top = itemView.top.toFloat()
                    val bottom = (itemView.bottom.toFloat() - bottomMarginPx).coerceAtLeast(top + 24f)
                    val left = (itemView.right.toFloat() + dX - cornerRadiusPx)
                        .coerceAtLeast(itemView.left.toFloat())
                    val right = itemView.right.toFloat()

                    bgRect.set(left, top, right, bottom)
                    c.drawRoundRect(bgRect, cornerRadiusPx, cornerRadiusPx, bgPaint)

                    deleteIcon?.let { icon ->
                        icon.setTint(if (isDark) 0xFFF5E6E8.toInt() else 0xFFFFFFFF.toInt())
                        val itemHeight = (bottom - top).toInt()
                        val iconTop = top.toInt() + (itemHeight - iconSizePx) / 2
                        val iconBottom = iconTop + iconSizePx
                        val iconRight = itemView.right - iconMarginPx
                        val iconLeft = iconRight - iconSizePx
                        if (dX < -(iconMarginPx + iconSizePx / 2)) {
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            icon.draw(c)
                        }
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }
}
