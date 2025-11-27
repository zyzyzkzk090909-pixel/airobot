package com.yx.chatrobot.domain

data class ImageGenResponse(
    val created: Long? = null,
    val data: List<ImageData> = emptyList()
)

data class ImageData(
    val url: String? = null,
    val b64_json: String? = null
)