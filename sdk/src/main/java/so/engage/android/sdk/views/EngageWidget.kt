package so.engage.android.sdk.views

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.*
import so.engage.android.sdk.models.MessageModel
import so.engage.android.sdk.models.UserModel
import so.engage.android.sdk.network.SocketService
import so.engage.android.sdk.utils.Preference
import so.engage.android.sdk.utils.toFormattedDate
import so.engage.android.sdk.utils.toJson
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngageWidget(
    userId: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewModel: EngageViewModel = viewModel(factory = EngageViewModelFactory(LocalContext.current, userId))


    // Modal UI
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            scope.launch {
                sheetState.hide()
                onDismiss.invoke()
            }
        },
    ) {
        val navigator = rememberNavController()
        NavHost(navController = navigator, startDestination = "index") {
            composable("index") {
                HomeView(navigator = navigator, viewModel = viewModel)
            }
            composable("chat") {
                ChatView(viewModel = viewModel)
            }
        }

    }

}

@Composable
fun HomeView(navigator: NavController, viewModel: EngageViewModel) {
    val scope = rememberCoroutineScope()

    val threadId = remember(viewModel.socketService.openThreadId.value) {
        viewModel.socketService.openThreadId.value
    }

    DisposableEffect(threadId, viewModel.userId) {
        val jobs = mutableStateListOf<() -> Unit>()
        scope.launch {
            jobs.addAll(viewModel.setup())
        }


        onDispose {
            jobs.forEach { it.invoke() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .fillMaxWidth()
            .background(Color(0xFFF8F9FA))
    ) {
        ElevatedButton(onClick = {
            navigator.navigate("chat")
        }) {
            Text("Send a message")
        }
    }
}

// Client ID for optimistic updates
private val clientId = UUID.randomUUID().toString()

fun getCurrentTime(): String {
    return ZonedDateTime.now()
        .format(DateTimeFormatter.ISO_INSTANT)
}

// ViewModel factory to pass context and userId
class EngageViewModelFactory(
    private val context: Context,
    private val userId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EngageViewModel::class.java)) {
            val preference = Preference(context)
            return EngageViewModel(preference, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class EngageViewModel(val preference: Preference, val userId: String): ViewModel() {
    val socketService: SocketService = SocketService(preference = preference)
    val messages = mutableStateListOf<MessageModel>()
    val sections = mutableStateListOf<Map<String, Any>>()
    var agentTyping = mutableStateOf(false)
    val typingTimer = ConcurrentHashMap<String, Job>()
    val agentsOnline = mutableIntStateOf(0)
    val threadId = mutableStateOf("")


    init {
        println("SOCKET SERVICE SET")
        val conf = mapOf(
            "no_chat" to false,
            "ignore_anonymous" to false,
            // Not needed for now
            // autotrack: {
            //   pageviews: false,
            //   buttons: false,
            //   forms: false
            // }
        )
        val user = UserModel(
            id = userId,
            identified = false
        )

        socketService.initSocket(
            conf = conf,
            user = user,
        )
        agentsOnline.intValue = socketService.onlineAgents.intValue
        threadId.value = socketService.openThreadId.value
    }

    fun updateSections() {
        sections.addAll(
            elements = if (messages.isEmpty()) emptyList()
            else {
                val groups = messages.groupBy { msg ->
                    msg.lastUpdated.toFormattedDate
                }
                groups.entries.sortedBy { it.key }.map { (date, messages) ->
                    mapOf("title" to date, "data" to messages)
                }
            }
        )

    }

    fun setup(): List<() -> Unit> {
        println("SETUP RUNNING")
        CoroutineScope(Dispatchers.IO).launch {
            messages.clear()
            messages.addAll(socketService.loadMessages())
            updateSections()
        }


        val offMsg = socketService.onMessage { msg ->
            println("NEW MESSAGE RECEIVED ${msg.toJson}")
            if (msg.parentId == threadId.value && msg.uid == userId) {
                messages.add(msg)
                messages.sortBy { it.lastUpdated }
                updateSections()
            }
        }

        val offTyping = socketService.onTyping { isTyping, data ->
            println("TYPING $data")
            if ((data as? Map<*, *>)?.get("parent_id") == threadId) {
                agentTyping.value = isTyping
                if (isTyping) {
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(5.seconds)
                        agentTyping.value = false
                    }
                }
            }
        }

        return listOf(offMsg, offTyping)
    }

    fun handleSend(input: MutableState<String>) {
        if (input.value.trim().isNotEmpty()) {
            val tempId = "temp-${UUID.randomUUID()}"
            val optimistic = MessageModel(
                id = tempId,
                messageId = "",
                body = input.value,
                parentId = threadId.value,
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
            updateSections()
            input.value = ""
            CoroutineScope(Dispatchers.IO).launch {
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
                    messages.sortBy { it.lastUpdated }
                    updateSections()
                }
            }
        }
    }

    fun handleTypingChange() {
        if (threadId.value.isNotEmpty()) {
            if (typingTimer["typing"] == null) {
                socketService.emitTyping(threadId.value, true)
                typingTimer["typing"] = CoroutineScope(Dispatchers.IO).launch {
                    delay(4.seconds)
                    socketService.emitTyping(threadId.value, false)
                    typingTimer.remove("typing")
                }
            } else {
                typingTimer["typing"]?.cancel()
                typingTimer["typing"] = CoroutineScope(Dispatchers.IO).launch {
                    delay(4.seconds)
                    socketService.emitTyping(threadId.value, false)
                    typingTimer.remove("typing")
                }
            }
        }
    }
}