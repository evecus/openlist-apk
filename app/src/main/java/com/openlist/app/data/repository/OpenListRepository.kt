package com.openlist.app.data.repository

import com.openlist.app.data.api.OpenListApiService
import com.openlist.app.data.api.RetrofitClient
import com.openlist.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int = -1) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class OpenListRepository(private val baseUrl: String, private val token: String) {

    private val api: OpenListApiService by lazy {
        RetrofitClient.getService(baseUrl)
    }

    private val authHeader: String get() = token

    suspend fun login(username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess) {
                    Result.Success(body.data?.token ?: "")
                } else {
                    Result.Error(body?.message ?: "Login failed", body?.code ?: -1)
                }
            } else {
                Result.Error("HTTP ${response.code()}: ${response.message()}", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun listFiles(path: String, page: Int = 1, perPage: Int = 50, refresh: Boolean = false): Result<FsListResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.listFiles(
                authHeader,
                FsListRequest(path = path, page = page, perPage = perPage, refresh = refresh)
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess) {
                    Result.Success(body.data ?: FsListResponse(null))
                } else {
                    Result.Error(body?.message ?: "Failed to list files", body?.code ?: -1)
                }
            } else {
                Result.Error("HTTP ${response.code()}: ${response.message()}", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getFile(path: String): Result<FsGetResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getFile(authHeader, FsGetRequest(path = path))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess) {
                    Result.Success(body.data ?: FsGetResponse())
                } else {
                    Result.Error(body?.message ?: "Failed to get file", body?.code ?: -1)
                }
            } else {
                Result.Error("HTTP ${response.code()}: ${response.message()}", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun searchFiles(keywords: String, parent: String = "/", page: Int = 1): Result<SearchResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchFiles(authHeader, SearchRequest(parent = parent, keywords = keywords, page = page))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess) {
                    Result.Success(body.data ?: SearchResponse(null))
                } else {
                    Result.Error(body?.message ?: "Search failed", body?.code ?: -1)
                }
            } else {
                Result.Error("HTTP ${response.code()}: ${response.message()}", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getPublicSettings(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPublicSettings()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess) {
                    Result.Success(body.data ?: emptyMap())
                } else {
                    Result.Error(body?.message ?: "Failed to get settings")
                }
            } else {
                Result.Error("HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    // Build the raw download URL for a file
    fun buildDownloadUrl(path: String, sign: String = ""): String {
        val base = RetrofitClient.normalizeUrl(baseUrl)
        val encodedPath = path.split("/").joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8") }
        return if (sign.isNotEmpty()) {
            "${base}d${encodedPath}?sign=$sign"
        } else {
            "${base}d${encodedPath}"
        }
    }

    fun buildAuthDownloadUrl(path: String, sign: String = ""): String {
        val base = buildDownloadUrl(path, sign)
        return if (token.isNotEmpty()) {
            val sep = if (base.contains("?")) "&" else "?"
            "${base}${sep}token=$token"
        } else base
    }
}
