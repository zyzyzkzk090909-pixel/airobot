package com.yx.chatrobot.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.yx.chatrobot.data.entity.Config
import com.yx.chatrobot.domain.RequestBody

data class ConfigUiState(
    val robotName: String = "AI",
    var model: String = "doubao-1-5-thinking-pro-250415",
    val maxTokens: Int = 60,
    val temperature: Double = 0.5,
    val topP: Double = 1.0,
    val frequency_penalty: Double = 0.0,
    val presence_penalty: Double = 0.6,
    val systemPrompt: String = "你是人工智能助手."
)


fun Config.toConfigUiState() = ConfigUiState(
    robotName = robotName,
    model = model,
    maxTokens = maxTokens,
    temperature = temperature,
    topP = topP,
    presence_penalty = presence_penalty,
    frequency_penalty = frequency_penalty,
    systemPrompt = systemPrompt
)

fun ConfigUiState.toConfig(id: Int = 0, userId: Int) = Config(
    id = id,
    robotName = robotName,
    model = model,
    maxTokens = maxTokens,
    temperature = temperature,
    topP = topP,
    userId = userId,
    frequency_penalty = frequency_penalty,
    presence_penalty = presence_penalty,
    systemPrompt = systemPrompt
)

fun ConfigUiState.toRequestBody(prompt: String) = RequestBody(
    model = model,
    max_tokens = maxTokens,
    prompt = prompt,
    temperature = temperature,
    top_p = topP,
    frequency_penalty = frequency_penalty,
    presence_penalty = presence_penalty
)


val modelList = listOf(
    Pair("doubao-1-5-thinking-pro-250415", Icons.Filled.Settings),
    Pair("doubao-seedream-4-0-250828", Icons.Filled.Settings),
    Pair("doubao-seed-1-6-vision-250815", Icons.Filled.Settings)
)

val fontSizeList = listOf(
    Pair("小", Icons.Filled.Settings),
    Pair("中", Icons.Filled.Settings),
    Pair("大", Icons.Filled.Settings)
)



val configItem = mapOf("model" to modelList, "fontSize" to fontSizeList)