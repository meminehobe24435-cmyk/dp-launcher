package com.dp.launcher.ui.drawer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dp.launcher.R
import com.dp.launcher.data.model.AppEntry
import com.dp.launcher.ui.DesignSpec
import com.dp.launcher.ui.UiScale
import com.dp.launcher.ui.widget.AppGridItemView

/**
 * Grid of every installed app, shown by [AppDrawerFragment].
 */
class AllAppsAdapter(
    private val ui: UiScale,
    private val onAppClick: (AppEntry) -> Unit,
) : RecyclerView.Adapter<AllAppsAdapter.AppViewHolder>() {

    private val items = mutableListOf<AppEntry>()

    /** Replaces the whole list. */
    fun submit(newItems: List<AppEntry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_grid, parent, false) as AppGridItemView
        view.layoutParams = RecyclerView.LayoutParams(
            ui.pxInt(DesignSpec.DRAWER_GRID_ITEM_WIDTH),
            ui.pxInt(DesignSpec.DRAWER_GRID_ITEM_HEIGHT),
        )
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class AppViewHolder(private val view: AppGridItemView) : RecyclerView.ViewHolder(view) {

        fun bind(app: AppEntry) {
            view.onAppClick = onAppClick
            view.bind(app, ui)
        }
    }
}
