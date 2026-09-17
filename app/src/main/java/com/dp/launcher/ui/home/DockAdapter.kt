package com.dp.launcher.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dp.launcher.R
import com.dp.launcher.ui.DesignSpec
import com.dp.launcher.ui.UiScale
import com.dp.launcher.ui.widget.DockItemView

/**
 * Bottom bar: the five fixed projector functions.
 *
 * The list is static ([DockItem]), so the adapter only has to apply the design geometry and
 * forward the click to the home screen, which decides what each action really does.
 */
class DockAdapter(
    private val ui: UiScale,
    private val onItemSelected: (DockItem) -> Unit,
) : RecyclerView.Adapter<DockAdapter.DockViewHolder>() {

    private val items = DockItem.entries

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dock, parent, false) as DockItemView
        view.layoutParams = RecyclerView.LayoutParams(
            ui.pxInt(DesignSpec.DOCK_ITEM_WIDTH),
            ui.pxInt(DesignSpec.DOCK_ITEM_HEIGHT),
        )
        return DockViewHolder(view)
    }

    override fun onBindViewHolder(holder: DockViewHolder, position: Int) {
        holder.bind(items[position])
    }

    /** Focuses the dock entry at [index], clamped to the available entries. */
    fun focusItem(index: Int): Boolean {
        val view = recyclerView?.layoutManager?.findViewByPosition(index.coerceIn(0, items.lastIndex))
        return view?.requestFocus() ?: false
    }

    /** Focuses the entry that represents [action]. */
    fun focusAction(action: com.dp.launcher.system.DockAction): Boolean {
        val index = items.indexOfFirst { it.action == action }
        return index >= 0 && focusItem(index)
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

    inner class DockViewHolder(private val view: DockItemView) : RecyclerView.ViewHolder(view) {

        fun bind(item: DockItem) {
            view.onDockAction = onItemSelected
            view.bind(item, ui)
        }
    }
}
