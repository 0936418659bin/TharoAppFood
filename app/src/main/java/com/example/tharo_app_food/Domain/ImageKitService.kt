package com.example.tharo_app_food.Domain

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ImageKitService {
    @POST("api/auth")
    suspend fun getAuthParams(@Body request: AuthRequest): AuthResponse

    @POST("api/upload")
    @Multipart
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("fileName") fileName: RequestBody,
        @Part("folder") folder: RequestBody
    ): UploadResponse
}

data class AuthRequest(val userId: String)
data class AuthResponse(
    val token: String,
    val expire: Long,
    val signature: String
)

data class UploadResponse(
    val url: String,
    val fileId: String,
    val width: Int,
    val height: Int
)

sealed class ApiResult<T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error<T>(val message: String, val code: Int) : ApiResult<T>()
}