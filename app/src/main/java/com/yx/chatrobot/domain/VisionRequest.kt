package com.yx.chatrobot.domain

data class VisionImageUrl(
    val url: String
)

data class VisionContent(
    val type: String,
    val image_url: VisionImageUrl? = null,
    val text: String? = null,
    val image_base64: String? = null
)

data class VisionMessage(
    val role: String = "user",
    val content: List<VisionContent>
)

data class VisionRequest(
    val model: String,
    val messages: List<VisionMessage>
)