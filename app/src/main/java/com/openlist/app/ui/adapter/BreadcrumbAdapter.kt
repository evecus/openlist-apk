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
        val item = getItem(position)
        val isLast = position == itemCount - 1
        holder.bind(item, isLast)
    }

    inner class ViewHolder(private val binding: ItemBreadcrumbBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BreadcrumbItem, isLast: Boolean) {
            binding.tvCrumb.text = item.name
            binding.tvCrumb.alpha = if (isLast) 1.0f else 0.6f
            binding.tvSeparator.visibility = if (isLast) android.view.View.GONE else android.view.View.VISIBLE
            binding.root.setOnClickListener {
                if (!isLast) onCrumbClick(item.path)
            }
        }
    }
}

class BreadcrumbDiff : DiffUtil.ItemCallback<BreadcrumbItem>() {
    override fun areItemsTheSame(oldItem: BreadcrumbItem, newItem: BreadcrumbItem) = oldItem.path == newItem.path
    override fun areContentsTheSame(oldItem: BreadcrumbItem, newItem: BreadcrumbItem) = oldItem == newItem
}
