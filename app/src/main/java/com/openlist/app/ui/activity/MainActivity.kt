package com.openlist.app.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.openlist.app.R
import com.openlist.app.databinding.ActivityMainBinding
import com.openlist.app.data.model.FileItem
import com.openlist.app.data.model.SearchItem
import com.openlist.app.ui.adapter.BreadcrumbAdapter
import com.openlist.app.ui.adapter.FileListAdapter
import com.openlist.app.ui.viewmodel.FileListViewModel
import com.openlist.app.utils.DownloadHelper
import com.openlist.app.utils.isTablet

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: FileListViewModel by viewModels()

    private lateinit var fileAdapter: FileListAdapter
    private lateinit var breadcrumbAdapter: BreadcrumbAdapter

    private var isGridMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDrawer()
        setupRecyclerViews()
        setupSwipeRefresh()
        observeViewModel()
        setupBackPress()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (isTablet()) {
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
        }
    }

    private fun setupDrawer() {
        if (isTablet()) {
            // On tablet landscape: lock drawer open permanently as a side nav panel
            binding.drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
            binding.drawerLayout?.setScrimColor(android.graphics.Color.TRANSPARENT)
        }
        binding.navView?.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    viewModel.loadFiles("/")
                    if (!isTablet()) binding.drawerLayout?.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_servers -> {
                    startActivity(Intent(this, SetupActivity::class.java))
                    true
                }
                R.id.nav_downloads -> {
                    // TODO: Show downloads screen
                    Toast.makeText(this, "Downloads coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerViews() {
        // File list
        fileAdapter = FileListAdapter(
            onItemClick = { item -> onFileItemClick(item) },
            onItemLongClick = { item -> showFileOptions(item) },
            onDownloadClick = { item -> downloadFile(item) }
        )

        binding.rvFiles.apply {
            adapter = fileAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            setHasFixedSize(true)
        }

        // Breadcrumb
        breadcrumbAdapter = BreadcrumbAdapter { path ->
            viewModel.loadFiles(path)
        }
        binding.rvBreadcrumbs.apply {
            adapter = breadcrumbAdapter
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary, R.color.secondary)
    }

    private fun observeViewModel() {
        viewModel.files.observe(this) { files ->
            fileAdapter.submitList(files)
            binding.tvEmptyState.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(this) { loading ->
            if (!loading) binding.swipeRefresh.isRefreshing = false
            binding.progressBar.visibility = if (loading && fileAdapter.itemCount == 0) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.breadcrumbs.observe(this) { crumbs ->
            breadcrumbAdapter.submitList(crumbs)
            binding.rvBreadcrumbs.scrollToPosition(crumbs.size - 1)
        }

        viewModel.currentPath.observe(this) { path ->
            supportActionBar?.subtitle = path
        }

        viewModel.serverName.observe(this) { name ->
            supportActionBar?.title = name.ifEmpty { "OpenList" }
        }

        viewModel.searchResults.observe(this) { results ->
            if (results != null) {
                val fileItems = results.map { searchItem ->
                    FileItem(
                        name = searchItem.name,
                        size = searchItem.size,
                        isDir = searchItem.isDir,
                        type = searchItem.type
                    )
                }
                fileAdapter.submitList(fileItems)
                binding.tvEmptyState.visibility = if (fileItems.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun onFileItemClick(item: FileItem) {
        // If search result, navigate by building path
        val searchResults = viewModel.searchResults.value
        if (item.isDir) {
            val currentPath = viewModel.currentPath.value ?: "/"
            val newPath = if (currentPath == "/") "/${item.name}" else "$currentPath/${item.name}"
            viewModel.clearSearch()
            viewModel.loadFiles(newPath)
        } else {
            openFile(item)
        }
    }

    private fun openFile(item: FileItem) {
        val currentPath = viewModel.currentPath.value ?: "/"
        val filePath = if (currentPath == "/") "/${item.name}" else "$currentPath/${item.name}"

        when {
            item.isVideo -> {
                val url = viewModel.getDownloadUrl(item)
                val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                    putExtra(VideoPlayerActivity.EXTRA_URL, url)
                    putExtra(VideoPlayerActivity.EXTRA_TITLE, item.name)
                }
                startActivity(intent)
            }
            item.isAudio -> {
                val url = viewModel.getDownloadUrl(item)
                val intent = Intent(this, AudioPlayerActivity::class.java).apply {
                    putExtra(AudioPlayerActivity.EXTRA_URL, url)
                    putExtra(AudioPlayerActivity.EXTRA_TITLE, item.name)
                }
                startActivity(intent)
            }
            item.isImage -> {
                val url = viewModel.getDownloadUrl(item)
                val intent = Intent(this, ImageViewerActivity::class.java).apply {
                    putExtra(ImageViewerActivity.EXTRA_URL, url)
                    putExtra(ImageViewerActivity.EXTRA_TITLE, item.name)
                }
                startActivity(intent)
            }
            else -> showFileOptions(item)
        }
    }

    private fun showFileOptions(item: FileItem) {
        val options = arrayOf(
            getString(R.string.action_download),
            getString(R.string.action_share_link),
            getString(R.string.action_copy_link)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> downloadFile(item)
                    1 -> shareFile(item)
                    2 -> copyLink(item)
                }
            }
            .show()
    }

    private fun downloadFile(item: FileItem) {
        val url = viewModel.getDownloadUrl(item)
        DownloadHelper.startDownload(this, url, item.name)
        Toast.makeText(this, getString(R.string.download_started, item.name), Toast.LENGTH_SHORT).show()
    }

    private fun shareFile(item: FileItem) {
        val url = viewModel.getDownloadUrl(item)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            putExtra(Intent.EXTRA_SUBJECT, item.name)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_via)))
    }

    private fun copyLink(item: FileItem) {
        val url = viewModel.getDownloadUrl(item)
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("link", url))
        Toast.makeText(this, getString(R.string.link_copied), Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.search(it) }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) viewModel.clearSearch()
                return false
            }
        })
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem) = true
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.clearSearch()
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (isTablet()) {
                    binding.drawerLayout?.let {
                        if (it.isDrawerOpen(GravityCompat.START)) it.closeDrawer(GravityCompat.START)
                        else it.openDrawer(GravityCompat.START)
                    }
                } else {
                    binding.drawerLayout?.openDrawer(GravityCompat.START)
                }
                true
            }
            R.id.action_toggle_view -> {
                toggleViewMode()
                true
            }
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            R.id.action_refresh -> {
                viewModel.refresh()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleViewMode() {
        isGridMode = !isGridMode
        binding.rvFiles.layoutManager = if (isGridMode) {
            val cols = if (isTablet()) 4 else 2
            GridLayoutManager(this, cols)
        } else {
            LinearLayoutManager(this)
        }
        fileAdapter.setGridMode(isGridMode)
    }

    private fun showSortDialog() {
        val options = arrayOf("Name ↑", "Name ↓", "Size ↑", "Size ↓", "Date ↑", "Date ↓")
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sort_by)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.setSortBy("name", false)
                    1 -> viewModel.setSortBy("name", true)
                    2 -> viewModel.setSortBy("size", false)
                    3 -> viewModel.setSortBy("size", true)
                    4 -> viewModel.setSortBy("modified", false)
                    5 -> viewModel.setSortBy("modified", true)
                }
            }
            .show()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
                    binding.drawerLayout?.closeDrawer(GravityCompat.START)
                    return
                }
                if (viewModel.searchResults.value != null) {
                    viewModel.clearSearch()
                    return
                }
                if (!viewModel.navigateUp()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
}
