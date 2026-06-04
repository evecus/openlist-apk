package com.openlist.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.openlist.app.R
import com.openlist.app.data.model.FileItem
import com.openlist.app.databinding.ItemFileListBinding
import com.openlist.app.databinding.ItemFileGridBinding

class FileListAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Unit,
    private val onDownloadClick: (FileItem) -> Unit
) : ListAdapter<FileItem, RecyclerView.ViewHolder>(FileDiffCallback()) {

    private var isGridMode = false

    companion object {
        private const val VIEW_TYPE_LIST = 0
        private const val VIEW_TYPE_GRID = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (isGridMode) VIEW_TYPE_GRID else VIEW_TYPE_LIST

    fun setGridMode(grid: Boolean) {
        isGridMode = grid
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_GRID) {
            val binding = ItemFileGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            GridViewHolder(binding)
        } else {
            val binding = ItemFileListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ListViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ListViewHolder -> holder.bind(item)
            is GridViewHolder -> holder.bind(item)
        }
    }

    inner class ListViewHolder(private val binding: ItemFileListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FileItem) {
            binding.tvName.text = item.name
            binding.tvSize.text = if (item.isDir) "" else item.formattedSize
            binding.tvModified.text = item.modified.take(10)

            // Set icon
            binding.ivIcon.setImageResource(getFileIcon(item))

            // Load thumbnail for images
            if (item.isImage && item.thumb.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(item.thumb)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(getFileIcon(item))
                    .into(binding.ivIcon)
            }

            binding.btnDownload.visibility = if (item.isDir) View.GONE else View.VISIBLE
            binding.btnDownload.setOnClickListener { onDownloadClick(item) }

            // Media badge
            binding.ivMediaBadge.visibility = when {
                item.isVideo -> { binding.ivMediaBadge.setImageResource(R.drawable.ic_video_badge); View.VISIBLE }
                item.isAudio -> { binding.ivMediaBadge.setImageResource(R.drawable.ic_audio_badge); View.VISIBLE }
                else -> View.GONE
            }

            binding.root.setOnClickListener { onItemClick(item) }
            binding.root.setOnLongClickListener { onItemLongClick(item); true }
        }
    }

    inner class GridViewHolder(private val binding: ItemFileGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FileItem) {
            binding.tvName.text = item.name
            binding.tvSize.text = if (item.isDir) "" else item.formattedSize

            binding.ivIcon.setImageResource(getFileIcon(item))

            if ((item.isImage || item.isVideo) && item.thumb.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(item.thumb)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .placeholder(getFileIcon(item))
                    .into(binding.ivIcon)
            }

            binding.root.setOnClickListener { onItemClick(item) }
            binding.root.setOnLongClickListener { onItemLongClick(item); true }
        }
    }

    private fun getFileIcon(item: FileItem): Int = when {
        item.isDir -> R.drawable.ic_folder
        item.isVideo -> R.drawable.ic_file_video
        item.isAudio -> R.drawable.ic_file_audio
        item.isImage -> R.drawable.ic_file_image
        item.isPdf -> R.drawable.ic_file_pdf
        item.isText -> R.drawable.ic_file_text
        else -> R.drawable.ic_file_generic
    }
}

class FileDiffCallback : DiffUtil.ItemCallback<FileItem>() {
    override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem) = oldItem.name == newItem.name
    override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem) = oldItem == newItem
}
