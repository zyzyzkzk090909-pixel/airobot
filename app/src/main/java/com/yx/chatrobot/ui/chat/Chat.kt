package com.yx.chatrobot

import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import android.util.Base64
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yx.chatrobot.data.MessageUiState
import com.yx.chatrobot.data.toMessageUiState
import com.yx.chatrobot.ui.AppViewModelProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ChatScreen(
    viewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {

    val listState = viewModel.listState
    Surface() {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                ChatDisplay(
                    viewModel,
                    modifier = Modifier.weight(1f),
                    listState = listState
                )
                UserInput(viewModel, listState)
            }
        }
    }
}

@Composable
fun ChatDisplay(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    listState: LazyListState
) {
    val chatUiState by viewModel.chatListState.collectAsState()

    Surface(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colors.background),
            state = listState
        ) {
            items(chatUiState.chatList) { item ->
                MessageItem(messageUiState = item.toMessageUiState(), viewModel = viewModel)
            }
            if (viewModel.isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        TypingIndicator()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UserInput(
    viewModel: MainViewModel,
    listState: LazyListState
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.Bottom)
            ) {
                val pickImageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        viewModel.updateMessageUiState("[图片]", true, uri.toString())
                    }
                }
                val fullWidthModifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                val textFieldValue = remember { mutableStateOf(TextFieldValue("")) }
                OutlinedTextField(
                    modifier = fullWidthModifier,
                    value = textFieldValue.value,
                    label = {
                        if (textFieldValue.value.text.isEmpty()) {
                            Text("输入您想问的")
                        } else {
                            Text("点击回车等待回复")
                        }
                    },
                    placeholder = { Text("请输入内容") },
                    onValueChange = { newValue ->
                        textFieldValue.value = newValue
                    },
                    isError = textFieldValue.value.text.isEmpty(),
                    trailingIcon = {
                        if (textFieldValue.value.text.isEmpty()) {
                            Row {
                                IconButton(onClick = { pickImageLauncher.launch("image/*") }) {
                                    Icon(Icons.Default.Image, contentDescription = null)
                                }
                                IconButton(onClick = { }) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            }
                        } else {
                            Row {
                                IconButton(onClick = {
                                    viewModel.generateImage(textFieldValue.value.text)
                                }) { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
                                IconButton(onClick = {
                                    if (!textFieldValue.value.text.isEmpty()) {
                                        val lastImage = viewModel.chatListState.value.chatList.lastOrNull { it.imageUri != null }
                                        val uri = lastImage?.imageUri
                                        if (uri != null) {
                                            viewModel.getVisionReply(uri, textFieldValue.value.text, context.contentResolver)
                                        } else {
                                            viewModel.getAiReply(textFieldValue.value.text)
                                        }
                                        textFieldValue.value = TextFieldValue("")
                                        keyboard?.hide()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "输入内容不能为空!",
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    }
                                }) {
                                    Icon(Icons.Default.Send, contentDescription = null)
                                }
                            }
                        }
                    },

                    )
            }
        }
    }
}


@SuppressLint("SimpleDateFormat")
@Composable
fun MessageItem(messageUiState: MessageUiState, viewModel: MainViewModel? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 8.dp),
        horizontalArrangement = if (messageUiState.isSelf) Arrangement.End else Arrangement.Start
    ) {
        val ctx = LocalContext.current
        if (!messageUiState.isSelf) {
            var bmpLeft by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
            LaunchedEffect(Unit) {
                try {
                    val stream = ctx.assets.open("生成 App 头像.png")
                    bmpLeft = android.graphics.BitmapFactory.decodeStream(stream)
                    stream.close()
                } catch (_: Exception) { bmpLeft = null }
            }
            if (bmpLeft != null) {
                androidx.compose.foundation.Image(
                    bitmap = bmpLeft!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colors.onSurface, CircleShape)
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.robot_avatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colors.secondaryVariant, CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row {
                Text(
                    text = messageUiState.name,
                    color = MaterialTheme.colors.onSurface,
                    style = MaterialTheme.typography.subtitle2
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = messageUiState.dateStr,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.25.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                elevation = 3.dp,
                color = if (messageUiState.isSelf) MaterialTheme.colors.secondary else MaterialTheme.colors.surface,
                modifier = Modifier
                    .animateContentSize()
                    .padding(1.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = messageUiState.content,
                        modifier = Modifier.padding(all = 4.dp),
                        style = MaterialTheme.typography.body1
                    )
                }
                if (messageUiState.imageUri != null) {
                    val context = LocalContext.current
                    val uriHandler = LocalUriHandler.current
                    var bmp by remember(messageUiState.imageUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                    LaunchedEffect(messageUiState.imageUri) {
                        try {
                            val uriStr = messageUiState.imageUri
                            if (uriStr!!.startsWith("base64:")) {
                                val pure = uriStr.removePrefix("base64:")
                                val bytes = Base64.decode(pure, Base64.DEFAULT)
                                bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } else if (uriStr.startsWith("http")) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val req = okhttp3.Request.Builder().url(uriStr).build()
                                    val resp = com.yx.chatrobot.network.client.newCall(req).execute()
                                    val stream = resp.body?.byteStream()
                                    bmp = android.graphics.BitmapFactory.decodeStream(stream)
                                    resp.close()
                                }
                            } else {
                                val uri = android.net.Uri.parse(uriStr)
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val input = context.contentResolver.openInputStream(uri)
                                    bmp = android.graphics.BitmapFactory.decodeStream(input)
                                }
                            }
                        } catch (_: Exception) {
                            bmp = null
                        }
                    }
                    val bitmapLocal = bmp
                    if (bitmapLocal != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmapLocal.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .padding(4.dp)
                        )
                    } else if (messageUiState.imageUri!!.startsWith("http")) {
                        Text(
                            text = "点击查看生成图片",
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable { uriHandler.openUri(messageUiState.imageUri!!) }
                        )
                    }
                }
            }
        }

        if (!messageUiState.isSelf && messageUiState.status == "failed") {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        viewModel?.retryAssistantMessage(messageUiState.id)
                    }
            )
        }

        if (messageUiState.isSelf) {
            Spacer(modifier = Modifier.width(8.dp))
            var bmpRight by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
            LaunchedEffect(Unit) {
                try {
                    val stream = ctx.assets.open("生成 App 头像.png")
                    bmpRight = android.graphics.BitmapFactory.decodeStream(stream)
                    stream.close()
                } catch (_: Exception) { bmpRight = null }
            }
            if (bmpRight != null) {
                androidx.compose.foundation.Image(
                    bitmap = bmpRight!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colors.onSurface, CircleShape)
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.user_avatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colors.onSurface, CircleShape)
                )
            }
        }
    }


}
@Composable
fun TypingIndicator() {
    val transition = rememberInfiniteTransition()
    val s1 = transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
    )
    val s2 = transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = LinearEasing), repeatMode = RepeatMode.Reverse)
    )
    Row(modifier = Modifier.height(24.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size((12 * s1.value).dp).clip(CircleShape).background(MaterialTheme.colors.primary))
        Box(modifier = Modifier.size((12 * s2.value).dp).clip(CircleShape).background(MaterialTheme.colors.secondary))
    }
}

