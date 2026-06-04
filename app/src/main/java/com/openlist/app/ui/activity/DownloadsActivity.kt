package com.openlist.app.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.openlist.app.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DownloadsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private val files = mutableListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 直接用代码构建简单布局，避免新增额外 layout XML
        val rootLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "Downloads"
            setBackgroundColor(getColor(R.color.primary))
            setTitleTextColor(getColor(R.color.white))
            setNavigationIcon(R.drawable.ic_arrow_back)
            navigationIconTint = android.content.res.ColorStateList.valueOf(getColor(R.color.white))
            setNavigationOnClickListener { finish() }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(androidx.appcompat.R.dimen.abc_action_bar_default_height_material)
            )
        }

        tvEmpty = TextView(this).apply {
            text = "没有下载的文件"
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setTextColor(getColor(R.color.text_secondary))
            visibility = View.GONE
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@DownloadsActivity)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // 用 FrameLayout 叠放 RecyclerView 和空状态提示
        val frameLayout = android.widget.FrameLayout(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
            addView(recyclerView)
            addView(tvEmpty)
        }

        rootLayout.addView(toolbar)
        rootLayout.addView(frameLayout)
        setContentView(rootLayout)

        loadDownloadedFiles()
    }

    private fun loadDownloadedFiles() {
        files.clear()
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir.exists()) {
            // 只列出文件，不列子目录，按修改时间倒序
            downloadsDir.listFiles()
                ?.filter { it.isFile }
                ?.sortedByDescending { it.lastModified() }
                ?.let { files.addAll(it) }
        }

        if (files.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.adapter = DownloadedFileAdapter(files,
                onOpen = { file -> openFile(file) },
                onDelete = { file -> confirmDelete(file) }
            )
        }
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            val mime = contentResolver.getType(uri) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "打开文件"))
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开文件", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(file: File) {
        MaterialAlertDialogBuilder(this)
            .setTitle("删除文件")
            .setMessage("确定删除 ${file.name}？")
            .setPositiveButton("删除") { _, _ ->
                if (file.delete()) {
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
                    loadDownloadedFiles()
                } else {
                    Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ---- 内部 Adapter ----

    private inner class DownloadedFileAdapter(
        private val items: List<File>,
        private val onOpen: (File) -> Unit,
        private val onDelete: (File) -> Unit
    ) : RecyclerView.Adapter<DownloadedFileAdapter.VH>() {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(android.R.id.text1)
            val tvInfo: TextView = itemView.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            // 使用系统内置的 two_line_list_item 布局
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            view.setPadding(
                resources.getDimensionPixelSize(R.dimen.abc_action_bar_default_padding_start_material),
                16,
                resources.getDimensionPixelSize(R.dimen.abc_action_bar_default_padding_end_material),
                16
            )
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val file = items[position]
            holder.tvName.text = file.name
            holder.tvName.setTextColor(getColor(R.color.text_primary))

            val sizeStr = formatSize(file.length())
            val dateStr = dateFormat.format(Date(file.lastModified()))
            holder.tvInfo.text = "$sizeStr  ·  $dateStr"
            holder.tvInfo.setTextColor(getColor(R.color.text_secondary))

            holder.itemView.setOnClickListener { onOpen(file) }
            holder.itemView.setOnLongClickListener {
                onDelete(file)
                true
            }
        }

        override fun getItemCount() = items.size

        private fun formatSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
                bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024f * 1024f))
                else -> "%.2f GB".format(bytes / (1024f * 1024f * 1024f))
            }
        }
    }
}
