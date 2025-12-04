package com.yx.chatrobot

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yx.chatrobot.data.*
import com.yx.chatrobot.data.entity.Message
import com.yx.chatrobot.data.entity.User
import com.yx.chatrobot.data.repository.ConfigRepository
import com.yx.chatrobot.data.repository.MessageRepository
import com.yx.chatrobot.data.repository.UserRepository
import com.yx.chatrobot.domain.ChatResponse
import com.yx.chatrobot.domain.RequestBody
import com.yx.chatrobot.network.DoubaoApi
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import kotlinx.coroutines.flow.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

data class ChatUiState(val chatList: List<Message> = listOf())
private const val TIMEOUT_MILLIS = 5_000L

class MainViewModel(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val configRepository: ConfigRepository,
    private val conversationRepository: com.yx.chatrobot.data.repository.ConversationRepository
) : ViewModel() {
    private val userId: Int = checkNotNull(savedStateHandle["userId"])
    private lateinit var restaurantsCall: Call<ChatResponse>
    var configUiState by mutableStateOf(ConfigUiState())
        private set
    var isLoading by mutableStateOf(false)

    private val currentSessionIdFlow = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            ensureConfig()
            ensureSession()
            if (com.yx.chatrobot.BuildConfig.DOUBAO_API_KEY.isNotEmpty()) {
                configUiState = configUiState.copy(model = "doubao-1-5-thinking-pro-250415")
            }
        }
    }

    val userState: StateFlow<UserUiState> =
        userRepository.getUserById(userId)
            .filterNotNull()
            .map {
                it.toUserUiState()
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = UserUiState()
            )
    val chatListState: StateFlow<ChatUiState> =
        currentSessionIdFlow.flatMapLatest { sid ->
            messageRepository.getMessagesStreamBySessionId(sid)
        }
            .filterNotNull()
            .map { ChatUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = ChatUiState()
            )

    val conversations: StateFlow<List<com.yx.chatrobot.data.entity.Conversation>> =
        conversationRepository.getConversationsByUser(userId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val listState = LazyListState(0)// 记录聊天界面信息列表的位置状态


    fun getAiReply(content: String) {
        updateMessageUiState(content, true) // 将用户的输入进行记录
        isLoading = true
        if (shouldGenerateImage(content)) {
            generateImage(content)
            return
        }
        // 不再插入占位消息，改为列表尾部统一显示 Typing 指示

        val lastImage = chatListState.value.chatList.lastOrNull { it.imageUri != null }
        val imageUri = lastImage?.imageUri
        val isHttp = imageUri?.startsWith("http") == true
        val isBase64 = imageUri?.startsWith("base64:") == true
        val isContent = imageUri?.startsWith("content://") == true
        if (imageUri != null && (isHttp || isBase64)) {
            val visionReq = com.yx.chatrobot.domain.VisionRequest(
                model = "doubao-seed-1-6-vision-250815",
                messages = listOf(
                    com.yx.chatrobot.domain.VisionMessage(
                        role = "user",
                        content = listOf(
                            when {
                                isHttp -> com.yx.chatrobot.domain.VisionContent(
                                    type = "image_url",
                                    image_url = com.yx.chatrobot.domain.VisionImageUrl(url = imageUri)
                                )
                                isBase64 -> com.yx.chatrobot.domain.VisionContent(
                                    type = "input_image",
                                    image_base64 = imageUri.removePrefix("base64:")
                                )
                                else -> com.yx.chatrobot.domain.VisionContent(
                                    type = "input_image",
                                    image_base64 = imageUri.removePrefix("base64:")
                                )
                            },
                            com.yx.chatrobot.domain.VisionContent(
                                type = "text",
                                text = content
                            )
                        )
                    )
                )
            )
            val call = com.yx.chatrobot.network.DoubaoVisionApi.retrofitService.visionCompletions(
                "Bearer ${com.yx.chatrobot.BuildConfig.DOUBAO_API_KEY}", visionReq
            )
            call.enqueue(object : Callback<com.yx.chatrobot.domain.ChatResponse> {
                override fun onResponse(
                    call: Call<com.yx.chatrobot.domain.ChatResponse>,
                    response: Response<com.yx.chatrobot.domain.ChatResponse>
                ) {
                    if (!response.isSuccessful) {
                        val msg = response.errorBody()?.string() ?: "请求失败"
                        updateMessageUiState(msg, false, status = "failed")
                        isLoading = false
                        return
                    }
                    val reply = response.body()?.choices?.firstOrNull()?.message?.content ?: response.body()?.choices?.firstOrNull()?.text ?: ""
                    updateMessageUiState(reply.trim(), false)
                    isLoading = false
                    generateConversationTitle(currentSessionIdFlow.value)
                }

                override fun onFailure(
                    call: Call<com.yx.chatrobot.domain.ChatResponse>,
                    t: Throwable
                ) {
                    updateMessageUiState("请求失败，请检查网络或API密钥配置", false, status = "failed")
                    isLoading = false
                }
            })
            return
        }
        val conf = runBlocking { configRepository.getConfigByUserId(userId) }
        configUiState = conf.toConfigUiState()
        val history = runBlocking { messageRepository.getAllMessagesBySessionIdOnce(currentSessionIdFlow.value) }
        val lastN = history.takeLast(10).map {
            if (it.isSelf) com.yx.chatrobot.domain.ChatMessage(role = "user", content = it.content)
            else com.yx.chatrobot.domain.ChatMessage(role = "assistant", content = it.content)
        }
        val requestBody = configUiState.toRequestBody(content).copy(
            prompt = "",
            messages = listOf(com.yx.chatrobot.domain.ChatMessage(role = "system", content = configUiState.systemPrompt)) + lastN + listOf(
                com.yx.chatrobot.domain.ChatMessage(role = "user", content = content)
            )
        )
        val auth = "Bearer ${com.yx.chatrobot.BuildConfig.DOUBAO_API_KEY}"
        restaurantsCall = DoubaoApi.retrofitService.chatCompletions(auth, requestBody)
        restaurantsCall.enqueue(
            object : Callback<ChatResponse> {
                override fun onResponse(
                    call: Call<ChatResponse>,
                    response: Response<ChatResponse>
                ) {
                    if (!response.isSuccessful) {
                        val msg = response.errorBody()?.string() ?: "请求失败"
                        updateMessageUiState(msg, false, status = "failed")
                        isLoading = false
                        return
                    }
                    response.body()?.choices?.get(0)?.let {
                        viewModelScope.launch {
                            val realTimeConfig =
                                async { configRepository.getConfigByUserId(userId) }.await()
                            if (realTimeConfig.id != 0) {
                                configUiState = configUiState.copy(robotName = realTimeConfig.robotName)
                            }
                            val reply = it.message?.content ?: (it.text ?: "")
                            updateMessageUiState(reply.trim(), false)
                            generateConversationTitle(currentSessionIdFlow.value)
                            isLoading = false
                        }

                    }
                }

                override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                    t.printStackTrace()
                    Log.e("MYTEST", "获取信息失败: ${t.message}")
                    updateMessageUiState("请求失败，请检查网络或API密钥配置", false, status = "failed")
                    isLoading = false
                }
            })
    }

    suspend fun getLastMessageSnippet(sessionId: Int): String {
        val messages = messageRepository.getAllMessagesBySessionIdOnce(sessionId)
        val firstUser = messages.firstOrNull { it.isSelf }?.content ?: ""
        val lastAssistant = messages.lastOrNull { !it.isSelf }?.content ?: ""
        fun trunc(s: String, n: Int) = if (s.length > n) s.take(n) + "…" else s
        val summary = listOf(trunc(firstUser, 20), trunc(lastAssistant, 40)).filter { it.isNotEmpty() }.joinToString("  ·  ")
        return summary
    }

    fun retryAssistantMessage(messageId: Int) {
        viewModelScope.launch {
            messageRepository.updateStatus(messageId, "loading")
            // 使用最近一条用户消息作为重试输入
            val lastUser = chatListState.value.chatList.lastOrNull { it.isSelf }
            if (lastUser != null) {
                getAiReply(lastUser.content)
            } else {
                messageRepository.updateContentAndStatus(messageId, "无可用的重试内容", "failed")
            }
        }
    }

    private fun generateConversationTitle(sessionId: Int) {
        viewModelScope.launch {
            val msgs = messageRepository.getAllMessagesBySessionIdOnce(sessionId)
            val recent = msgs.takeLast(6).joinToString("\n") { (if (it.isSelf) "用户:" else "AI:") + it.content }
            val req = com.yx.chatrobot.domain.RequestBody(
                model = configUiState.model,
                prompt = "请用不超过12字为下面对话生成中文标题，不要标点:\n" + recent,
                temperature = 0.3,
                max_tokens = 30
            )
            val call = com.yx.chatrobot.network.DoubaoApi.retrofitService.chatCompletions(
                "Bearer ${com.yx.chatrobot.BuildConfig.DOUBAO_API_KEY}", req
            )
            try {
                val resp = call.execute()
                val title = resp.body()?.choices?.firstOrNull()?.message?.content ?: resp.body()?.choices?.firstOrNull()?.text ?: "新对话"
                conversationRepository.updateTitle(sessionId, title.trim())
            } catch (_: Exception) {}
        }
    }

    fun renameConversation(sessionId: Int, title: String) {
        viewModelScope.launch { conversationRepository.updateTitle(sessionId, title) }
    }

    fun deleteConversation(sessionId: Int) {
        viewModelScope.launch {
            messageRepository.deleteBySessionId(sessionId)
            conversationRepository.delete(sessionId)
        }
    }

    suspend fun getPreviewMessages(sessionId: Int, count: Int): List<Message> {
        val all = messageRepository.getAllMessagesBySessionIdOnce(sessionId)
        return all.takeLast(count)
    }

    private fun shouldGenerateImage(text: String): Boolean {
        val t = text.lowercase()
        val keywords = listOf("生成图片", "图片生成", "帮我生成", "画一张", "帮我画", "生成一张", "generate image", "draw")
        return keywords.any { t.contains(it) }
    }

    fun generateImage(prompt: String) {
        isLoading = true
        val req = com.yx.chatrobot.domain.ImageGenRequest(
            model = "doubao-seedream-4-0-250828",
            prompt = prompt,
            sequential_image_generation = "disabled",
            response_format = "b64_json",
            size = "2K",
            stream = false,
            watermark = true
        )
        val call = com.yx.chatrobot.network.DoubaoImageApi.retrofitService.generate(
            "Bearer ${com.yx.chatrobot.BuildConfig.DOUBAO_API_KEY}", req
        )
        call.enqueue(object : Callback<com.yx.chatrobot.domain.ImageGenResponse> {
            override fun onResponse(
                call: Call<com.yx.chatrobot.domain.ImageGenResponse>,
                response: Response<com.yx.chatrobot.domain.ImageGenResponse>
            ) {
                if (!response.isSuccessful) {
                    updateMessageUiState("图片生成失败(${response.code()})", false)
                    isLoading = false
                    return
                }
                val data = response.body()?.data?.firstOrNull()
                val b64 = data?.b64_json
                val url = data?.url
                val content = "[生成图片]"
                var finalUri: String? = null
                if (b64 != null) {
                    try {
                        val payload = org.json.JSONObject()
                        payload.put("base64", b64)
                        val req = Request.Builder().url(backendBaseUrl + "/uploadImage").post(
                            payload.toString().toRequestBody("application/json".toMediaType())
                        ).build()
                        val resp = com.yx.chatrobot.network.client.newCall(req).execute()
                        val bodyStr = resp.body?.string() ?: "{}"
                        resp.close()
                        val json = org.json.JSONObject(bodyStr)
                        if (json.optBoolean("ok")) {
                            finalUri = backendBaseUrl + json.optString("path")
                        }
                    } catch (_: Exception) { finalUri = null }
                }
                val msg = MessageUiState(
                    name = configUiState.robotName,
                    time = Date().time / 1000,
                    content = content,
                    isSelf = false,
                    status = "sent",
                    imageUri = finalUri ?: (url ?: (if (b64 != null) "base64:" + b64 else null))
                )
                viewModelScope.launch {
                    val sid = requireSessionId()
                    messageRepository.insertMessage(msg.toMessage(userState.value.id).copy(sessionId = sid))
                    val payload = org.json.JSONObject()
                    payload.put("name", msg.name)
                    payload.put("time", msg.time)
                    payload.put("content", msg.content)
                    payload.put("user_id", userState.value.id)
                    payload.put("is_self", false)
                    payload.put("session_id", sid)
                    payload.put("image_uri", msg.imageUri)
                    payload.put("status", msg.status)
                    try {
                        val url = backendBaseUrl + "/messages"
                        val req = Request.Builder().url(url).post(payload.toString().toRequestBody("application/json".toMediaType())).build()
                        com.yx.chatrobot.network.client.newCall(req).enqueue(object: okhttp3.Callback {
                            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
                            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) { response.close() }
                        })
                    } catch (_: Exception) {}
                }
                isLoading = false
            }

            override fun onFailure(
                call: Call<com.yx.chatrobot.domain.ImageGenResponse>,
                t: Throwable
            ) {
                updateMessageUiState("图片生成失败: ${t.message}", false)
                isLoading = false
            }
        })
    }


fun updateMessageUiState(result: String, isSelf: Boolean, imageUri: String? = null, status: String = "sent") {
    val tmp = MessageUiState(
        name = if (isSelf) userState.value.name else configUiState.robotName,
        time = Date().time / 1000,
        content = result,
        isSelf = isSelf,
        status = status,
    )
    viewModelScope.launch {
        val sid = requireSessionId()
        messageRepository.insertMessage(
            tmp.toMessage(userState.value.id).copy(sessionId = sid, imageUri = imageUri)
        )
        val payload = org.json.JSONObject()
        payload.put("name", tmp.name)
        payload.put("time", tmp.time)
        payload.put("content", tmp.content)
        payload.put("user_id", userState.value.id)
        payload.put("is_self", isSelf)
        payload.put("session_id", sid)
        payload.put("image_uri", imageUri)
        payload.put("status", status)
        try {
            val url = backendBaseUrl + "/messages"
            val req = Request.Builder().url(url).post(payload.toString().toRequestBody("application/json".toMediaType())).build()
            com.yx.chatrobot.network.client.newCall(req).enqueue(object: okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) { response.close() }
            })
        } catch (_: Exception) {}
        delay(100)
        listState.scrollToItem(chatListState.value.chatList.size - 1)
    }
}

    

    private suspend fun ensureSession() {
        val latest = conversationRepository.getLatestConversation(userId)
        if (latest != null) {
            currentSessionIdFlow.value = latest.id
        } else {
            val sid = conversationRepository.insert(
                com.yx.chatrobot.data.entity.Conversation(
                    title = "新对话",
                    userId = userId
                )
            )
            try {
                val payload = org.json.JSONObject().apply {
                    put("user_id", userId)
                    put("title", "新对话")
                }
                val req = Request.Builder().url(backendBaseUrl + "/conversations").post(
                    payload.toString().toRequestBody("application/json".toMediaType())
                ).build()
                com.yx.chatrobot.network.client.newCall(req).enqueue(object: okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) { response.close() }
                })
            } catch (_: Exception) {}
            currentSessionIdFlow.value = sid
        }
    }

    private suspend fun ensureConfig() {
        val cid = configRepository.getConfigIdByUserId(userId)
        if (cid == 0) {
            configRepository.insert(
                com.yx.chatrobot.data.entity.Config(userId = userId)
            )
        }
        val conf = configRepository.getConfigByUserId(userId)
        configUiState = conf.toConfigUiState()
        if (configUiState.backendUrl.isNotBlank()) {
            backendBaseUrl = configUiState.backendUrl
        }
    }

    fun startNewSession(title: String = "新对话") {
        viewModelScope.launch {
            val sid = conversationRepository.insert(
                com.yx.chatrobot.data.entity.Conversation(
                    title = title,
                    userId = userId
                )
            )
            try {
                val payload = org.json.JSONObject().apply {
                    put("user_id", userId)
                    put("title", title)
                }
                val req = Request.Builder().url(backendBaseUrl + "/conversations").post(
                    payload.toString().toRequestBody("application/json".toMediaType())
                ).build()
                com.yx.chatrobot.network.client.newCall(req).enqueue(object: okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) { response.close() }
                })
            } catch (_: Exception) {}
            currentSessionIdFlow.value = sid
        }
    }

    fun openConversation(id: Int) {
        viewModelScope.launch {
            currentSessionIdFlow.value = id
        }
    }

    private suspend fun requireSessionId(): Int {
        if (currentSessionIdFlow.value <= 0) {
            ensureSession()
        }
        return currentSessionIdFlow.value
    }
    var backendBaseUrl = com.yx.chatrobot.BuildConfig.BACKEND_BASE_URL
 
    fun getVisionReply(imageUri: String, prompt: String, resolver: android.content.ContentResolver) {
        updateMessageUiState(prompt, true)
        isLoading = true
        val isHttp = imageUri.startsWith("http")
        val isBase64 = imageUri.startsWith("base64:")
        val content = when {
            isHttp -> com.yx.chatrobot.domain.VisionContent(
                type = "image_url",
                image_url = com.yx.chatrobot.domain.VisionImageUrl(url = imageUri)
            )
            isBase64 -> com.yx.chatrobot.domain.VisionContent(
                type = "input_image",
                image_base64 = imageUri.removePrefix("base64:")
            )
            else -> {
                val (urlStr, b64) = try {
                    val uri = android.net.Uri.parse(imageUri)
                    val bytes = resolver.openInputStream(uri)?.readBytes() ?: ByteArray(0)
                    val enc = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    val payload = org.json.JSONObject().put("base64", enc)
                    val req = Request.Builder().url(backendBaseUrl + "/uploadImage").post(payload.toString().toRequestBody("application/json".toMediaType())).build()
                    val resp = com.yx.chatrobot.network.client.newCall(req).execute()
                    val bodyStr = resp.body?.string() ?: "{}"; resp.close()
                    val json = org.json.JSONObject(bodyStr)
                    val path = if (json.optBoolean("ok")) json.optString("path") else ""
                    val full = if (path.isNotEmpty()) backendBaseUrl + path else ""
                    full to enc
                } catch (_: Exception) { "" to "" }
                if (urlStr.isNotEmpty()) com.yx.chatrobot.domain.VisionContent(
                    type = "image_url",
                    image_url = com.yx.chatrobot.domain.VisionImageUrl(url = urlStr)
                ) else com.yx.chatrobot.domain.VisionContent(
                    type = "input_image",
                    image_base64 = b64
                )
            }
        }
        val visionReq = com.yx.chatrobot.domain.VisionRequest(
            model = "doubao-seed-1-6-vision-250815",
            messages = listOf(
                com.yx.chatrobot.domain.VisionMessage(
                    role = "user",
                    content = listOf(
                        content,
                        com.yx.chatrobot.domain.VisionContent(type = "text", text = prompt)
                    )
                )
            )
        )
        val call = com.yx.chatrobot.network.DoubaoVisionApi.retrofitService.visionCompletions(
            "Bearer ${com.yx.chatrobot.BuildConfig.DOUBAO_API_KEY}", visionReq
        )
        call.enqueue(object : Callback<com.yx.chatrobot.domain.ChatResponse> {
            override fun onResponse(
                call: Call<com.yx.chatrobot.domain.ChatResponse>,
                response: Response<com.yx.chatrobot.domain.ChatResponse>
            ) {
                if (!response.isSuccessful) {
                    val msg = response.errorBody()?.string() ?: "请求失败"
                    updateMessageUiState(msg, false, status = "failed")
                    isLoading = false
                    return
                }
                val reply = response.body()?.choices?.firstOrNull()?.message?.content ?: response.body()?.choices?.firstOrNull()?.text ?: ""
                updateMessageUiState(reply.trim(), false)
                isLoading = false
                generateConversationTitle(currentSessionIdFlow.value)
            }

            override fun onFailure(
                call: Call<com.yx.chatrobot.domain.ChatResponse>,
                t: Throwable
            ) {
                updateMessageUiState("请求失败，请检查网络或API密钥配置", false, status = "failed")
                isLoading = false
            }
        })
    }
}
