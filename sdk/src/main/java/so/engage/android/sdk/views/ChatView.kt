package so.engage.android.sdk.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import so.engage.android.sdk.models.MessageModel
import so.engage.android.sdk.models.UserModel
import so.engage.android.sdk.network.SocketService
import so.engage.android.sdk.utils.toFormattedDate
import so.engage.android.sdk.utils.toJson
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatView(
    socketService: SocketService,
    userId: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val messages = remember { mutableStateListOf<MessageModel>() }
    var input by remember { mutableStateOf("") }
    var agentTyping by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val typingTimer = remember { ConcurrentHashMap<String, Job>() }
    val agentsOnline = socketService.getOnlineAgents()
    val threadId = socketService.openThreadId.value

    // Load messages and set up socket listeners
    DisposableEffect(threadId, userId) {
        scope.launch {
            messages.clear()
            messages.addAll(socketService.loadMessages())
            isLoading = false
        }

        val offMsg = socketService.onMessage { msg ->
            println("NEW MESSAGE RECEIVED ${msg.toJson}")
            if (msg.parentId == threadId && msg.uid == userId) {
                messages.add(msg)
                messages.sortBy { it.lastUpdated }
            }
        }

        val offTyping = socketService.onTyping { isTyping, data ->
            println("TYPING $data")
            if ((data as? Map<*, *>)?.get("parent_id") == threadId) {
                agentTyping = isTyping
                if (isTyping) {
                    scope.launch {
                        delay(5.seconds)
                        agentTyping = false
                    }
                }
            }
        }

        // Cleanup socket listeners when composable is disposed
        onDispose {
            offMsg()
            offTyping()
        }
    }

    // Group messages by date
    val sections = remember(messages.size) {
        if (messages.isEmpty()) emptyList()
        else {
            val groups = messages.groupBy { msg ->
                msg.lastUpdated.toFormattedDate
            }
            groups.entries.sortedBy { it.key }.map { (date, messages) ->
                mapOf("title" to date, "data" to messages)
            }
        }
    }

    // Modal UI
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            scope.launch {
                sheetState.hide()
                onDismiss.invoke()
            }
        },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
        ) {
            // Offline message
            if (agentsOnline < 1) {
                Text(
                    text = "We are currently offline. Send us a message and we will respond soon.",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp,
                    color = Color(0xFF374151)
                )
            }

            // Message List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                sections.forEach { section ->
                    item {
                        SectionHeader(title = section["title"] as String)
                    }
                    items(section["data"] as List<MessageModel>) { message ->
                        MessageBubble(message = message, userId = userId)
                    }
                }
            }

            // Typing indicator
            if (agentTyping) {
                Text(
                    text = "Typing...",
                    modifier = Modifier.padding(start = 12.dp),
                    fontSize = 14.sp,
                    color = Color(0xFF777777)
                )
            }

            // Input Area
            InputArea(
                input = input,
                onInputChange = { text ->
                    input = text
                    if (threadId.isNotEmpty()) {
                        if (typingTimer["typing"] == null) {
                            socketService.emitTyping(threadId, true)
                            typingTimer["typing"] = scope.launch {
                                delay(4.seconds)
                                socketService.emitTyping(threadId, false)
                                typingTimer.remove("typing")
                            }
                        } else {
                            typingTimer["typing"]?.cancel()
                            typingTimer["typing"] = scope.launch {
                                delay(4.seconds)
                                socketService.emitTyping(threadId, false)
                                typingTimer.remove("typing")
                            }
                        }
                    }
                },
                onSend = {
                    if (input.trim().isNotEmpty()) {
                        val tempId = "temp-${UUID.randomUUID()}"
                        val optimistic = MessageModel(
                            id = tempId,
                            messageId = "",
                            body = input,
                            parentId = threadId,
                            uid = userId,
                            user = "",
                            from = UserModel(id = userId),
                            cid = clientId,
                            outbound = true,
                            read = false,
                            date = getCurrentTime(),
                            lastUpdated = getCurrentTime(),
                        )
                        messages.add(optimistic)
                        messages.sortBy { it.lastUpdated }
                        input = ""
                        scope.launch {
                            try {
                                socketService.sendMessage(optimistic)
                            } catch (e: Exception) {
                                println(e.toString())
                                // Replace replaceAll with map for API 21 compatibility
                                val updatedMessages = messages.map { msg ->
                                    if (msg.id == tempId) msg.copy(status = "failed") else msg
                                }
                                messages.clear()
                                messages.addAll(updatedMessages)
                            }
                        }
                    }
                }
            )
        }
    }

}

@Composable
fun SectionHeader(title: String) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp)
            .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            modifier = Modifier.padding(6.dp)
        )
    }
}

@Composable
fun MessageBubble(message: MessageModel, userId: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures {
                    println(message.toString())
                }
            }
            .wrapContentWidth(if (message.outbound) Alignment.End else Alignment.Start)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (message.outbound) Color.Green else Color.Cyan,
                    RoundedCornerShape(8.dp)
                )
                .padding(10.dp)
        ) {
            HtmlTextView(html = message.body)
            Text(
                text = message.lastUpdated.toFormattedDate,
                fontSize = 12.sp,
                color = Color(0xFF777777),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun InputArea(input: String, onInputChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .border(1.dp, Color(0xFFE5E7EB)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text("Type a message...", color = Color(0xFF9CA3AF)) },
            modifier = Modifier
                .weight(1f)
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        IconButton(onClick = {}) { Text("😊") }
        IconButton(onClick = {}) { Text("📎") }
        Button(
            onClick = onSend,
            modifier = Modifier
                .padding(start = 8.dp)
                .background(Color(0xFF0B93F6), RoundedCornerShape(6.dp))
        ) {
            Text("Send", color = Color.White)
        }
    }
}

// Client ID for optimistic updates
private val clientId = UUID.randomUUID().toString()

fun getCurrentTime(): String {
    return ZonedDateTime.now()
        .format(DateTimeFormatter.ISO_INSTANT)
}