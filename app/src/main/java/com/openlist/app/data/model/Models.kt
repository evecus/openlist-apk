package com.openlist.app.data.model

import com.google.gson.annotations.SerializedName

// Base API response wrapper
data class ApiResponse<T>(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("message") val message: String = "",
    @SerializedName("data") val data: T? = null
) {
    val isSuccess: Boolean get() = code == 200
}

// Auth models
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("otp_code") val otpCode: String = ""
)

data class LoginResponse(
    @SerializedName("token") val token: String
)

// File/Directory listing
data class FsListRequest(
    @SerializedName("path") val path: String,
    @SerializedName("password") val password: String = "",
    @SerializedName("page") val page: Int = 1,
    @SerializedName("per_page") val perPage: Int = 50,
    @SerializedName("refresh") val refresh: Boolean = false
)

data class FsListResponse(
    @SerializedName("content") val content: List<FileItem>?,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("readme") val readme: String = "",
    @SerializedName("header") val header: String = "",
    @SerializedName("write") val write: Boolean = false,
    @SerializedName("provider") val provider: String = ""
)

data class FileItem(
    @SerializedName("name") val name: String,
    @SerializedName("size") val size: Long = 0,
    @SerializedName("is_dir") val isDir: Boolean = false,
    @SerializedName("modified") val modified: String = "",
    @SerializedName("created") val created: String = "",
    @SerializedName("sign") val sign: String = "",
    @SerializedName("thumb") val thumb: String = "",
    @SerializedName("type") val type: Int = 0,
    @SerializedName("hashinfo") val hashInfo: String = "",
    @SerializedName("hash_info") val hashInfoMap: Map<String, String>? = null
) {
    // OpenList file types
    // 0 = unknown, 1 = folder, 2 = video, 3 = audio, 4 = text, 5 = image, 6 = office, 7 = pdf, 8 = code
    val isVideo: Boolean get() = type == 2 || (!isDir && name.hasVideoExtension())
    val isAudio: Boolean get() = type == 3 || (!isDir && name.hasAudioExtension())
    val isImage: Boolean get() = type == 5 || (!isDir && name.hasImageExtension())
    val isText: Boolean get() = type == 4
    val isPdf: Boolean get() = type == 7 || name.endsWith(".pdf", ignoreCase = true)
    
    val formattedSize: String get() = formatFileSize(size)
    
    private fun String.hasVideoExtension(): Boolean {
        val videoExts = setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp", "ts", "rmvb", "rm")
        return substringAfterLast('.', "").lowercase() in videoExts
    }
    
    private fun String.hasAudioExtension(): Boolean {
        val audioExts = setOf("mp3", "flac", "aac", "wav", "ogg", "m4a", "wma", "opus", "ape", "alac")
        return substringAfterLast('.', "").lowercase() in audioExts
    }
    
    private fun String.hasImageExtension(): Boolean {
        val imageExts = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "tiff")
        return substringAfterLast('.', "").lowercase() in imageExts
    }
    
    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return "%.1f %s".format(value, units[minOf(digitGroups, units.size - 1)])
    }
}

// File detail/get response
data class FsGetRequest(
    @SerializedName("path") val path: String,
    @SerializedName("password") val password: String = ""
)

data class FsGetResponse(
    @SerializedName("name") val name: String = "",
    @SerializedName("size") val size: Long = 0,
    @SerializedName("is_dir") val isDir: Boolean = false,
    @SerializedName("modified") val modified: String = "",
    @SerializedName("created") val created: String = "",
    @SerializedName("sign") val sign: String = "",
    @SerializedName("thumb") val thumb: String = "",
    @SerializedName("type") val type: Int = 0,
    @SerializedName("raw_url") val rawUrl: String = "",
    @SerializedName("readme") val readme: String = "",
    @SerializedName("header") val header: String = "",
    @SerializedName("provider") val provider: String = "",
    @SerializedName("related") val related: List<FileItem>? = null
)

// Search
data class SearchRequest(
    @SerializedName("parent") val parent: String = "/",
    @SerializedName("keywords") val keywords: String,
    @SerializedName("scope") val scope: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("per_page") val perPage: Int = 50,
    @SerializedName("password") val password: String = ""
)

data class SearchResponse(
    @SerializedName("content") val content: List<SearchItem>?,
    @SerializedName("total") val total: Int = 0
)

data class SearchItem(
    @SerializedName("parent") val parent: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("is_dir") val isDir: Boolean = false,
    @SerializedName("size") val size: Long = 0,
    @SerializedName("type") val type: Int = 0
)

// Server config
data class StoredServer(
    val id: Long = 0,
    val name: String,
    val url: String,
    val token: String = "",
    val username: String = "",
    val isActive: Boolean = false,
    val lastUsed: Long = System.currentTimeMillis()
)

// Download task
data class DownloadTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val url: String,
    val destPath: String,
    val totalBytes: Long = 0,
    var downloadedBytes: Long = 0,
    var status: DownloadStatus = DownloadStatus.PENDING
)

enum class DownloadStatus {
    PENDING, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
}

// Breadcrumb
data class BreadcrumbItem(
    val name: String,
    val path: String
)
