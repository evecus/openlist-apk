package com.openlist.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.openlist.app.data.model.BreadcrumbItem
import com.openlist.app.databinding.ItemBreadcrumbBinding

class BreadcrumbAdapter(
    private val onCrumbClick: (String) -> Unit
) : ListAdapter<BreadcrumbItem, BreadcrumbAdapter.ViewHolder>(BreadcrumbDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBreadcrumbBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemBreadcrumbBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BreadcrumbItem) {
            // Show "/" prefix before every crumb so the bar reads: /Home /123云盘 /9a
            binding.tvCrumb.text = "/${item.name}"
            // Hide the old › separator — the "/" prefix acts as separator now
            binding.tvSeparator.visibility = android.view.View.GONE
            // Every crumb is clickable (including the current one, for easy refresh)
            binding.root.setOnClickListener { onCrumbClick(item.path) }
        }
    }
}

class BreadcrumbDiff : DiffUtil.ItemCallback<BreadcrumbItem>() {
    override fun areItemsTheSame(oldItem: BreadcrumbItem, newItem: BreadcrumbItem) = oldItem.path == newItem.path
    override fun areContentsTheSame(oldItem: BreadcrumbItem, newItem: BreadcrumbItem) = oldItem == newItem
}
