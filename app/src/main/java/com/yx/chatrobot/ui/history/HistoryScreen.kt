package com.yx.chatrobot.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.produceState
import androidx.compose.material.IconButton
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yx.chatrobot.MainViewModel
import com.yx.chatrobot.ui.AppViewModelProvider
import com.yx.chatrobot.data.entity.Conversation
import com.yx.chatrobot.data.toMessageUiState
import com.yx.chatrobot.data.dateToStrFriendly
import android.os.Build
import androidx.annotation.RequiresApi

@Composable
fun HistoryScreen(
    viewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        val sessions: List<Conversation> by viewModel.conversations.collectAsState()
        val triedCreate = remember { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(sessions.isEmpty()) {
            if (sessions.isEmpty() && !triedCreate.value) {
                triedCreate.value = true
                viewModel.startNewSession()
            }
        }
        if (sessions.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "暂无会话")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.startNewSession() }) { Text("新建对话") }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items(sessions) { conv ->
                Card(modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clickable { viewModel.openConversation(conv.id) }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = conv.title)
                        val ts = conv.createdTime
                        val date = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) dateToStrFriendly(ts) else ts.toString()
                        val snippet by produceState(initialValue = "") { value = viewModel.getLastMessageSnippet(conv.id) }
                        Text(text = date)
                        Text(text = if (snippet.isNotEmpty()) snippet else "暂无消息", modifier = Modifier.padding(top = 4.dp))
                        val preview by produceState(initialValue = emptyList<com.yx.chatrobot.data.entity.Message>()) {
                            value = viewModel.getPreviewMessages(conv.id, 3)
                        }
                        preview.forEach { msg ->
                            Text(text = "${if (msg.isSelf) "用户" else "AI"}: ${msg.content}", modifier = Modifier.padding(top = 2.dp))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            val showRename = remember { mutableStateOf(false) }
                            val titleInput = remember { mutableStateOf(conv.title) }
                            IconButton(onClick = { showRename.value = true }) { Icon(Icons.Default.Edit, contentDescription = null) }
                            IconButton(onClick = { viewModel.deleteConversation(conv.id) }) { Icon(Icons.Default.Delete, contentDescription = null) }
                            if (showRename.value) {
                                AlertDialog(onDismissRequest = { showRename.value = false }, title = { Text("重命名会话") }, text = {
                                    TextField(value = titleInput.value, onValueChange = { titleInput.value = it })
                                }, confirmButton = {
                                    IconButton(onClick = {
                                        viewModel.renameConversation(conv.id, titleInput.value)
                                        showRename.value = false
                                    }) { Icon(Icons.Default.Edit, contentDescription = null) }
                                }, dismissButton = {
                                    IconButton(onClick = { showRename.value = false }) { Icon(Icons.Default.Delete, contentDescription = null) }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
}