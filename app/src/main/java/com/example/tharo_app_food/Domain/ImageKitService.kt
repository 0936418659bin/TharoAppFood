package com.example.tharo_app_food.Domain

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
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

    @FormUrlEncoded
    @POST("delete")
    suspend fun deleteImage(
        @Field("fileId") fileId: String
    ): DeleteResponse
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

data class DeleteResponse(
    @SerializedName("success") val success: Boolean
)

sealed class ApiResult<T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error<T>(val message: String, val code: Int) : ApiResult<T>()
}