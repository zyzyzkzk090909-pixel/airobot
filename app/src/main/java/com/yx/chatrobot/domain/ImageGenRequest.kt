package com.yx.chatrobot.domain

data class ImageGenRequest(
    val model: String,
    val prompt: String,
    val sequential_image_generation: String? = null,
    val response_format: String = "b64_json",
    val size: String? = null,
    val stream: Boolean = false,
    val watermark: Boolean = true
)