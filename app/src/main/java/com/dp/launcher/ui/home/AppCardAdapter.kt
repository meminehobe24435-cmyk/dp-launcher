package com.dp.launcher.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dp.launcher.R
import com.dp.launcher.data.model.AppEntry
import com.dp.launcher.ui.DesignSpec
import com.dp.launcher.ui.UiScale
import com.dp.launcher.ui.widget.AppCardView
import com.dp.launcher.ui.widget.ReflectionContainer

/**
 * Home row: the featured apps, one card each, with their reflection.
 *
 * The row is not scrollable in the reference design - it always holds [AppRepository.HOME_ROW_SIZE]
 * cards - so the adapter keeps the item size fixed and the RecyclerView can be told so.
 */
class AppCardAdapter(
    private val ui: UiScale,
    private val onCardClick: (AppEntry) -> Unit,
    private val onCardLongClick: (AppEntry) -> Unit,
) : RecyclerView.Adapter<AppCardAdapter.CardViewHolder>() {

    private val items = mutableListOf<AppEntry>()

    /** Replaces the row content. */
    fun submit(newItems: List<AppEntry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val container = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_card, parent, false) as ReflectionContainer
        container.setReflection(
            reflectionHeightPx = ui.pxInt(DesignSpec.REFLECTION_HEIGHT),
            alpha = DesignSpec.REFLECTION_ALPHA,
        )
        container.layoutParams = RecyclerView.LayoutParams(
            ui.pxInt(DesignSpec.CARD_WIDTH),
            ui.pxInt(DesignSpec.CARD_HEIGHT + DesignSpec.REFLECTION_HEIGHT),
        )
        return CardViewHolder(container)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(items[position])
    }

    /** Focuses the card at [index], or the last card when the index is out of range. */
    fun focusCard(index: Int): Boolean {
        if (items.isEmpty()) return false
        val safeIndex = index.coerceIn(0, items.lastIndex)
        val view = recyclerView?.layoutManager?.findViewByPosition(safeIndex) ?: return false
        return view.findViewById<AppCardView>(R.id.card_plate)?.requestFocus() ?: false
    }

    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

    inner class CardViewHolder(private val container: ReflectionContainer) :
        RecyclerView.ViewHolder(container) {

        private val card: AppCardView = container.findViewById(R.id.card_plate)

        fun bind(app: AppEntry) {
            card.onAppClick = onCardClick
            card.onAppLongClick = onCardLongClick
            card.bind(app, ui)
            container.invalidateReflection()
        }
    }
}
