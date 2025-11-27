package com.yx.chatrobot.network

import com.yx.chatrobot.domain.ChatResponse
import com.yx.chatrobot.domain.RequestBody
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit


private const val DOUBAO_BASE_URL =
    "https://ark.cn-beijing.volces.com/api/v3/"

var httpBuilder = OkHttpClient.Builder()
// 设置超时，超过 60s 视为超时
var client = httpBuilder.readTimeout(20, TimeUnit.SECONDS)
    .connectTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()
private val doubaoRetrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(DOUBAO_BASE_URL)
    .client(client)
    .build()





interface DoubaoChatApiService {
    @Headers(
        "Content-Type:application/json"
    )
    @POST("chat/completions")
    fun chatCompletions(
        @retrofit2.http.Header("Authorization") auth: String,
        @Body requestData: RequestBody
    ): Call<ChatResponse>
}

object DoubaoApi {
    val retrofitService: DoubaoChatApiService by lazy {
        doubaoRetrofit.create(DoubaoChatApiService::class.java)
    }
}

interface DoubaoImageApiService {
    @Headers(
        "Content-Type:application/json"
    )
    @POST("images/generations")
    fun generate(
        @retrofit2.http.Header("Authorization") auth: String,
        @Body request: com.yx.chatrobot.domain.ImageGenRequest
    ): Call<com.yx.chatrobot.domain.ImageGenResponse>
}

object DoubaoImageApi {
    val retrofitService: DoubaoImageApiService by lazy {
        doubaoRetrofit.create(DoubaoImageApiService::class.java)
    }
}

interface DoubaoVisionApiService {
    @Headers(
        "Content-Type:application/json"
    )
    @POST("chat/completions")
    fun visionCompletions(
        @retrofit2.http.Header("Authorization") auth: String,
        @Body request: com.yx.chatrobot.domain.VisionRequest
    ): Call<com.yx.chatrobot.domain.ChatResponse>
}

object DoubaoVisionApi {
    val retrofitService: DoubaoVisionApiService by lazy {
        doubaoRetrofit.create(DoubaoVisionApiService::class.java)
    }
}