package so.engage.android.sdk.views

import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import so.engage.android.sdk.models.MessageModel
import so.engage.android.sdk.models.ThreadModel
import so.engage.android.sdk.models.UserModel
import so.engage.android.sdk.network.Network
import so.engage.android.sdk.network.SocketService
import so.engage.android.sdk.utils.Preference
import so.engage.android.sdk.utils.toFormattedDate
import so.engage.android.sdk.utils.toJson
import java.net.URL
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import kotlin.collections.forEach
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngageWidget(
    userId: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewModel: EngageViewModel =
        viewModel(factory = EngageViewModelFactory(LocalContext.current, userId))

    val threadId = remember(viewModel.threadId.value) {
        viewModel.threadId.value
    }

    DisposableEffect(threadId, viewModel.userId) {
        viewModel.setup()

        onDispose {
            viewModel.dispose()
        }
    }


    // Modal UI
    ModalBottomSheet(
        sheetState = sheetState,
        dragHandle = {},
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

class EngageViewModel(val preference: Preference, val userId: String) : ViewModel() {
    val socketService: SocketService = SocketService()
    val network = Network(preference = preference)
    val messages = mutableStateListOf<MessageModel>()
    val sections = mutableStateListOf<Map<String, Any>>()
    val agentTyping = mutableStateOf(false)
    val typingTimer = ConcurrentHashMap<String, Job>()
    val agentsOnline = mutableIntStateOf(0)
    val threadId = mutableStateOf("")
    val activeMessage = mutableStateOf<String?>(null)
    val cleanUp = mutableStateListOf<() -> Unit>()


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
            network = network,
        )

        CoroutineScope(Dispatchers.IO).launch {
            cleanupOldThreads()
            loadRecentThreads()
        }
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

    fun setup() {
        println("SETUP RUNNING")
        CoroutineScope(Dispatchers.IO).launch {
            messages.clear()
            messages.addAll(loadMessages())
            updateSections()
        }


        val offAgentsOnline = socketService.onAgentsOnline { count ->
            agentsOnline.intValue = count
        }

        val offNewWebpushNotification = socketService.onWebpushNotification { data ->
            when (data["type"]) {
                "chat" -> {
                    if (data["parent_id"] != threadId.value) {
                        threadId.value = data["parent_id"] as String
                    }
                    println("DATA TO STRING $data")
                    val msg = Gson().fromJson(data.toString(), MessageModel::class.java)

                    messages.add(msg)
                    messages.sortBy { it.lastUpdated }
                    updateSections()
                    setActiveMessage(msg.body)
                    persistMessage(msg.parentId, listOf(msg))
                }

                "typing:start" -> CoroutineScope(Dispatchers.IO).launch {
                    delay(5.seconds)
                    agentTyping.value = true
                }

                "typing:stop" -> CoroutineScope(Dispatchers.IO).launch {
                    delay(5.seconds)
                    agentTyping.value = false
                }
            }
        }

        cleanUp.addAll(listOf(offAgentsOnline, offNewWebpushNotification))
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
                    sendMessage(optimistic)
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

    suspend fun loadMessages(): List<MessageModel> {
        if (threadId.value.isEmpty()) return emptyList()

        val messages = preference.loadMessages("chat_threads_${threadId.value}")
        if (messages.isEmpty()) {
            println("No persisted messages")
            val url = URL("https://api.engage.so/v1/messages/chat/${threadId.value}?uid=${userId}")
            val responseType = object : TypeToken<Map<String, Any>>() {}
            return try {
                val data: Map<String, Any> = network.get(url, responseType)
                val chats: List<MessageModel> = data["messages"]?.let {
                    Gson().fromJson(it.toJson, object : TypeToken<List<MessageModel>>() {}.type)
                } ?: emptyList()
                if (chats.isNotEmpty()) {
                    preference.clear("chat_threads_${threadId.value}")
                    persistMessage(threadId.value, chats)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                println("ENGAGE: Failed to load thread: ${e.message}")
                emptyList()
            }
        } else {
            println("Persisted messages found")
        }
        return messages
    }

    suspend fun loadRecentThreads() {
        val url = URL("https://api.engage.so/v1/messages/chat?uid=${userId}")
        val listType = object : TypeToken<List<ThreadModel>>() {}
        try {
            val dataList: List<ThreadModel> = network.get(url, listType)
            if (dataList.isNotEmpty()) {
                threadId.value = ""
                dataList.forEach { thread ->
                    if (thread.status == "open") {
                        threadId.value = thread.id
                        setActiveMessage(thread.excerpt)
                    }
                }
                preference.clear("chat_threads")
                preference.saveThreads("chat_threads", dataList)
            }
        } catch (e: Exception) {
            println("ENGAGE: Failed to load threads: ${e.message}")
        }
    }

    private fun cleanupOldThreads() {
        val threads = preference.loadThreads("chat_threads")
        if (threads.isEmpty()) return

        val thirtyDaysAgo = Date.from(
            ZonedDateTime.now()
                .minusDays(30)
                .toInstant()
        )
        threads.forEach { thread ->
            val isOlderThan30Days = thread.lastUpdated.let {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                sdf.parse(it)?.before(thirtyDaysAgo) ?: false
            }
            if (isOlderThan30Days) {
                preference.clear("chat_threads_${thread.id}")
            }
        }
    }

    private fun persistMessage(threadId: String, messages: List<MessageModel>): List<MessageModel> {
        val oldMessages = preference.loadMessages("chat_threads_$threadId")
        val existingIds = oldMessages.map { it.id }.toSet()
        val filteredNew = messages.filter { !existingIds.contains(it.id) }
        val allMessages = (oldMessages + filteredNew).sortedBy { it.lastUpdated }
        preference.saveMessages("chat_threads_$threadId", allMessages)
        println("Messages persisted")

        return allMessages
    }

    private fun setActiveMessage(msg: String) {
        activeMessage.value = msg
    }

    suspend fun sendMessage(message: MessageModel) {
        val url = URL("https://api.engage.so/v1/messages/chat")

        network.post(
            url,
            body = mapOf(
                "body" to message.body,
                "uid" to message.uid,
                "cid" to message.cid
            ).toJson
        )

    }

    fun dispose() {
        cleanUp.forEach { it() }
        socketService.closeSocket()
        preference.clear("user")
        preference.clear("chat_threads")
    }
}