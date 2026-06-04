package com.openlist.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.openlist.app.OpenListApp
import com.openlist.app.data.model.BreadcrumbItem
import com.openlist.app.data.model.FileItem
import com.openlist.app.data.model.SearchItem
import com.openlist.app.data.repository.OpenListRepository
import com.openlist.app.data.repository.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FileListViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as OpenListApp
    private val prefs = app.preferences

    private var repository: OpenListRepository? = null
    private var serverUrl: String = ""

    private val _files = MutableLiveData<List<FileItem>>()
    val files: LiveData<List<FileItem>> = _files

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Signals MainActivity to go back to SetupActivity (e.g. missing config, auth error)
    private val _navigateToSetup = MutableLiveData<Boolean>(false)
    val navigateToSetup: LiveData<Boolean> = _navigateToSetup

    private val _currentPath = MutableLiveData<String>("/")
    val currentPath: LiveData<String> = _currentPath

    private val _breadcrumbs = MutableLiveData<List<BreadcrumbItem>>()
    val breadcrumbs: LiveData<List<BreadcrumbItem>> = _breadcrumbs

    private val _searchResults = MutableLiveData<List<SearchItem>?>(null)
    val searchResults: LiveData<List<SearchItem>?> = _searchResults

    private val _isSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> = _isSearching

    private val _serverName = MutableLiveData<String>()
    val serverName: LiveData<String> = _serverName

    private var currentPage = 1
    private var hasMorePages = false
    private var totalItems = 0
    private val pageSize = 50

    private var sortBy = "name"
    private var sortDesc = false

    init {
        viewModelScope.launch {
            val url = prefs.activeServerUrl.first()
            val token = prefs.activeServerToken.first()
            val name = prefs.activeServerName.first()

            // Guard: if URL is empty the session is corrupt — send user back to setup
            if (url.isBlank()) {
                prefs.clearActiveServer()
                _navigateToSetup.value = true
                return@launch
            }

            serverUrl = url
            _serverName.value = name.ifEmpty { "OpenList" }
            repository = OpenListRepository(serverUrl, token)
            sortBy = prefs.sortBy.first()
            sortDesc = prefs.sortDesc.first()
            loadFiles("/")
        }
    }

    fun loadFiles(path: String, refresh: Boolean = false) {
        currentPage = 1
        _currentPath.value = path
        updateBreadcrumbs(path)
        fetchFiles(path, refresh = refresh)
    }

    fun loadMore() {
        if (!hasMorePages || _isLoading.value == true) return
        val path = _currentPath.value ?: "/"
        fetchFiles(path, page = ++currentPage, append = true)
    }

    fun refresh() {
        val path = _currentPath.value ?: "/"
        loadFiles(path, refresh = true)
    }

    private fun fetchFiles(path: String, page: Int = 1, append: Boolean = false, refresh: Boolean = false) {
        val repo = repository ?: return
        viewModelScope.launch {
            if (!append) _isLoading.value = true
            _error.value = null
            when (val result = repo.listFiles(path, page, pageSize, refresh)) {
                is Result.Success -> {
                    val response = result.data
                    totalItems = response.total
                    hasMorePages = (page * pageSize) < totalItems
                    val items = response.content ?: emptyList()
                    val sorted = sortItems(items)
                    if (append) {
                        _files.value = (_files.value ?: emptyList()) + sorted
                    } else {
                        _files.value = sorted
                    }
                }
                is Result.Error -> {
                    if (!append) _files.value = emptyList()
                    // 401 means token expired/invalid — kick back to setup
                    if (result.code == 401) {
                        prefs.clearActiveServer()
                        _navigateToSetup.value = true
                    } else {
                        _error.value = result.message
                    }
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun navigateUp(): Boolean {
        val path = _currentPath.value ?: "/"
        if (path == "/") return false
        val parentPath = path.substringBeforeLast("/").ifEmpty { "/" }
        loadFiles(parentPath)
        return true
    }

    fun search(query: String) {
        if (query.isBlank()) {
            clearSearch()
            return
        }
        val repo = repository ?: return
        viewModelScope.launch {
            _isSearching.value = true
            when (val result = repo.searchFiles(query)) {
                is Result.Success -> _searchResults.value = result.data.content ?: emptyList()
                is Result.Error -> {
                    _error.value = result.message
                    _searchResults.value = emptyList()
                }
                else -> {}
            }
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchResults.value = null
        _isSearching.value = false
    }

    fun setSortBy(by: String, desc: Boolean) {
        sortBy = by
        sortDesc = desc
        viewModelScope.launch { prefs.setSortBy(by, desc) }
        _files.value = _files.value?.let { sortItems(it) }
    }

    private fun sortItems(items: List<FileItem>): List<FileItem> {
        val (dirs, files) = items.partition { it.isDir }
        val sortedDirs = when (sortBy) {
            "name" -> if (sortDesc) dirs.sortedByDescending { it.name.lowercase() } else dirs.sortedBy { it.name.lowercase() }
            "size" -> if (sortDesc) dirs.sortedByDescending { it.size } else dirs.sortedBy { it.size }
            "modified" -> if (sortDesc) dirs.sortedByDescending { it.modified } else dirs.sortedBy { it.modified }
            else -> dirs.sortedBy { it.name.lowercase() }
        }
        val sortedFiles = when (sortBy) {
            "name" -> if (sortDesc) files.sortedByDescending { it.name.lowercase() } else files.sortedBy { it.name.lowercase() }
            "size" -> if (sortDesc) files.sortedByDescending { it.size } else files.sortedBy { it.size }
            "modified" -> if (sortDesc) files.sortedByDescending { it.modified } else files.sortedBy { it.modified }
            else -> files.sortedBy { it.name.lowercase() }
        }
        return sortedDirs + sortedFiles
    }

    private fun updateBreadcrumbs(path: String) {
        val crumbs = mutableListOf(BreadcrumbItem("Home", "/"))
        if (path != "/") {
            val parts = path.trim('/').split("/")
            var accumulated = ""
            parts.forEach { part ->
                accumulated += "/$part"
                crumbs.add(BreadcrumbItem(part, accumulated))
            }
        }
        _breadcrumbs.value = crumbs
    }

    fun getDownloadUrl(item: FileItem): String {
        val repo = repository ?: return ""
        val path = "${_currentPath.value?.trimEnd('/')}/${item.name}"
        return repo.buildAuthDownloadUrl(path, item.sign)
    }

    fun getFileUrl(path: String, sign: String): String {
        val repo = repository ?: return ""
        return repo.buildAuthDownloadUrl(path, sign)
    }

    fun getServerUrl(): String = serverUrl

    fun getToken(): String {
        var token = ""
        viewModelScope.launch {
            token = prefs.activeServerToken.first()
        }
        return token
    }
}
