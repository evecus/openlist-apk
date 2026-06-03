package com.openlist.app.data.api

import com.openlist.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface OpenListApiService {

    // Auth
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    // File system
    @POST("api/fs/list")
    suspend fun listFiles(
        @Header("Authorization") token: String,
        @Body request: FsListRequest
    ): Response<ApiResponse<FsListResponse>>

    @POST("api/fs/get")
    suspend fun getFile(
        @Header("Authorization") token: String,
        @Body request: FsGetRequest
    ): Response<ApiResponse<FsGetResponse>>

    @POST("api/fs/search")
    suspend fun searchFiles(
        @Header("Authorization") token: String,
        @Body request: SearchRequest
    ): Response<ApiResponse<SearchResponse>>

    // Public settings
    @GET("api/public/settings")
    suspend fun getPublicSettings(): Response<ApiResponse<Map<String, String>>>

    // User info
    @GET("api/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<ApiResponse<UserInfo>>
}

data class UserInfo(
    val id: Int = 0,
    val username: String = "",
    val password: String = "",
    val base_path: String = "/",
    val role: Int = 0,
    val disabled: Boolean = false,
    val permission: Long = 0,
    val sso_id: String = ""
)
