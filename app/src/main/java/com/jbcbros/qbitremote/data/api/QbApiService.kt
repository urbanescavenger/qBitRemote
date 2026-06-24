package com.jbcbros.qbitremote.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface QbApiService {

    @FormUrlEncoded
    @POST("/api/v2/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<ResponseBody>

    @GET("/api/v2/torrents/info")
    suspend fun getTorrents(
        @Query("sort") sort: String = "added_on",
        @Query("reverse") reverse: Boolean = true,
        @Query("filter") filter: String? = null
    ): Response<ResponseBody>

    @GET("/api/v2/transfer/info")
    suspend fun getTransferInfo(): Response<ResponseBody>

    @GET("/api/v2/sync/maindata")
    suspend fun getMainData(): Response<ResponseBody>

    @GET("/api/v2/torrents/categories")
    suspend fun getCategories(): Response<ResponseBody>

    @GET("/api/v2/torrents/tags")
    suspend fun getTags(): Response<ResponseBody>

    @GET("/api/v2/torrents/files")
    suspend fun getTorrentFiles(@Query("hash") hash: String): Response<ResponseBody>

    @GET("/api/v2/torrents/trackers")
    suspend fun getTorrentTrackers(@Query("hash") hash: String): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/api/v2/torrents/add")
    suspend fun addTorrentByUrl(
        @Field("urls") urls: String,
        @Field("category") category: String? = null,
        @Field("tags") tags: String? = null
    ): Response<ResponseBody>

    @Multipart
    @POST("/api/v2/torrents/add")
    suspend fun addTorrentFile(
        @Part torrents: MultipartBody.Part,
        @Part("category") category: RequestBody? = null,
        @Part("tags") tags: RequestBody? = null
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/api/v2/torrents/stop")
    suspend fun stopTorrent(@Field("hashes") hashes: String): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/api/v2/torrents/start")
    suspend fun startTorrent(@Field("hashes") hashes: String): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/api/v2/torrents/recheck")
    suspend fun recheckTorrent(@Field("hashes") hashes: String): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/api/v2/torrents/delete")
    suspend fun deleteTorrent(
        @Field("hashes") hashes: String,
        @Field("deleteFiles") deleteFiles: String = "true"
    ): Response<ResponseBody>
}
