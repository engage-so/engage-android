package so.engage.android.sdk.network

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import so.engage.android.sdk.models.AccountModel
import so.engage.android.sdk.models.MessageModel
import so.engage.android.sdk.models.ThreadModel
import so.engage.android.sdk.models.UserModel
import so.engage.android.sdk.utils.Preference
import so.engage.android.sdk.utils.toJson
import java.net.URL
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.min
import kotlin.random.Random

class SocketService(
    private val preference: Preference,
) {
    private val socketUrl = "https://ws.engage.so/webpush"
    private var socket: Socket? = null
    private var socketDisconnected = false
    private var allowChat = true
    private val onMessageHandlers = mutableListOf<(MessageModel) -> Unit>()
    private val onTypingHandlers = mutableListOf<(Boolean, Any?) -> Unit>()
    private var user = UserModel("")
    private var account = AccountModel()
    private val activeMessageListeners = mutableListOf<(String) -> Unit>()
    private var activeMessage: String? = null

    private var network = Network(preference = preference)

    private fun joinRoom() {
        socket?.let {
            if (account.id != null) {
                it.emit("room", account.id)
                it.emit("room", "${account.id}:${user.id}")
            }
        }
    }

    val openThreadId = mutableStateOf("")
    val onlineAgents = mutableIntStateOf(0)


    fun getActiveMessage(): String? = activeMessage

    suspend fun loadMessages(threadId: String = openThreadId.value): List<MessageModel> {
        if (threadId.isEmpty()) return emptyList()

        val messages = preference.loadMessages("chat_threads_$threadId")
        if (messages.isEmpty()) {
            println("No persisted messages")
            val url = URL("https://api.engage.so/v1/messages/chat/$threadId?uid=${user.id}")
            val responseType = object : TypeToken<Map<String, Any>>() {}
            return try {
                val data: Map<String, Any> = network.get(url, responseType)
                val chats: List<MessageModel> = data["messages"]?.let {
                    Gson().fromJson(it.toJson, object : TypeToken<List<MessageModel>>() {}.type)
                } ?: emptyList()
                if (chats.isNotEmpty()) {
                    preference.clear("chat_threads_$threadId")
                    persistMessage(threadId, chats)
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

    suspend fun loadRecentThreads(): List<ThreadModel> {
        val url = URL("https://api.engage.so/v1/messages/chat?uid=${user.id}")
        val listType = object : TypeToken<List<ThreadModel>>() {}
        return try {
            val dataList: List<ThreadModel> = network.get(url, listType)
            if (dataList.isNotEmpty()) {
                openThreadId.value = ""
                dataList.forEach { thread ->
                    if (thread.status == "open") {
                        openThreadId.value = thread.id
                        setActiveMessage(thread.excerpt)
                    }
                }
                preference.clear("chat_threads")
                preference.saveThreads("chat_threads", dataList)
            }
            dataList
        } catch (e: Exception) {
            println("ENGAGE: Failed to load threads: ${e.message}")
            emptyList()
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
        activeMessage = msg
        activeMessageListeners.forEach { it(msg) }
    }

    private fun onSocketConnected() {
        CoroutineScope(Dispatchers.IO).launch {
            joinRoom()
            socketDisconnected = false
            cleanupOldThreads()
        }
    }

    private fun onSocketDisconnected() {
        if (!allowChat) return
        socketDisconnected = true
        var delay = 2000L
        val maxDelay = 30000L
        val jitterFactor = 0.1
        var attempts = 0
        val maxAttempts = 10

        fun reconnect() {
            if (!socketDisconnected || attempts >= maxAttempts) return
            attempts++
            try {
                socket?.connect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val jitter = (delay * jitterFactor * Random.nextDouble()).toLong()
            delay = min(delay * 3 / 2 + jitter, maxDelay)
            CoroutineScope(Dispatchers.IO).launch {
                delay(delay)
                reconnect()
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            delay(delay)
            reconnect()
        }
    }

    private fun onAgentsOnline(count: Int) {
        onlineAgents.intValue = count
    }

    private fun onNewNotification(data: JSONObject) {
        when (data["type"]) {
            "chat" -> {
                if (data["parent_id"] != openThreadId.value) {
                    openThreadId.value = data["parent_id"] as String
                }
                println("DATA TO STRING $data")
                val msg = Gson().fromJson(data.toString(), MessageModel::class.java)

                onMessageHandlers.forEach { it(msg) }
                setActiveMessage(msg.body)
                persistMessage(msg.parentId, listOf(msg))
            }

            "typing" -> onTypingHandlers.forEach { it(true, data) }
            "typing:stop" -> onTypingHandlers.forEach { it(false, data) }
        }
    }

    fun initSocket(
        conf: Map<String, Any>,
        user: UserModel,
    ) {
        this.user = user
        val url = URL("https://api.engage.so/v1/account")
        val listType = object : TypeToken<AccountModel>() {}
        try {
            openThreadId.value = ""
            CoroutineScope(Dispatchers.IO).launch {
                account = network.get(url, listType)
                loadRecentThreads()
            }

            if (conf["no_chat"] == true || (conf["ignore_anonymous"] == true && !user.identified)) {
                allowChat = false
                socket?.disconnect()
                return
            }

            allowChat = true
            socket = IO.socket(socketUrl).apply {
                on(Socket.EVENT_CONNECT) { onSocketConnected() }
                on(Socket.EVENT_DISCONNECT) { onSocketDisconnected() }
                on("agents_online") { args -> onAgentsOnline(args[0] as Int) }
                on("webpush/notification") { args ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val jsonObject = args[0] as? JSONObject ?: return@launch
                            println("WEBPUSH/NOTIFICATION jsonObject $jsonObject")
                            onNewNotification(jsonObject)
                        } catch (e: Exception) {
                            // Handle error (e.g., log or notify user)
                            println("Error processing notification: ${e.message}")
                        }
                    }
                }
                connect()
            }
        } catch (e: Exception) {
            println("ENGAGE: Failed to load threads: ${e.message}")
        }
    }

    fun closeSocket() {
        socket?.disconnect()
        preference.clear("user")
        preference.clear("chat_threads")
    }

    fun getSocket(): Socket =
        socket ?: throw IllegalStateException("Socket not initialized. Call initSocket() first.")

    suspend fun sendMessage(message: MessageModel) {
        val url = URL("https://api.engage.so/v1/messages/chat")
        try {
            network.post(
                url,
                body = mapOf(
                    "body" to message.body,
                    "uid" to message.uid,
                    "cid" to message.cid
                ).toJson
            )
        } catch (e: Exception) {
            println("ENGAGE: Failed to send message: ${e.message}")
        }
    }

    fun onMessage(handler: (MessageModel) -> Unit): () -> Unit {
        onMessageHandlers.add(handler)
        return { onMessageHandlers.remove(handler) }
    }

    fun onTyping(handler: (Boolean, Any?) -> Unit): () -> Unit {
        onTypingHandlers.add(handler)
        return { onTypingHandlers.remove(handler) }
    }

    fun emitTyping(threadId: String, isTyping: Boolean) {
        socket?.emit(
            if (isTyping) "typing:start" else "typing:stop",
            mapOf(
                "parent_id" to threadId,
                "user_id" to user.id,
                "org_id" to account.id
            )
        )
    }
}
