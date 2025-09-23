package so.engage.android.sdk.network

import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import so.engage.android.sdk.models.AccountModel
import so.engage.android.sdk.models.UserModel
import java.net.URL
import kotlin.math.min
import kotlin.random.Random

class SocketService {
    private val socketUrl = "https://ws.engage.so/webpush"
    private var socket: Socket? = null
    private var socketDisconnected = false
    private var allowChat = true
    private val onAgentsOnline = mutableListOf<(Int) -> Unit>()
    private val onWebpushNotification = mutableListOf<(JSONObject) -> Unit>()
    private var user = UserModel("")
    private var account = AccountModel()

    private fun joinRoom() {
        socket?.let {
            if (account.id != null) {
                it.emit("room", account.id)
                it.emit("room", "${account.id}:${user.id}")
            }
        }
    }

    private fun onSocketConnected() {
        CoroutineScope(Dispatchers.IO).launch {
            joinRoom()
            socketDisconnected = false
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

    fun initSocket(
        conf: Map<String, Any>,
        user: UserModel,
        network: Network
    ) {
        this.user = user
        val url = URL("https://api.engage.so/v1/account")
        val listType = object : TypeToken<AccountModel>() {}
        try {
            CoroutineScope(Dispatchers.IO).launch {
                account = network.get(url, listType)
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
                on("agents_online") { args -> onAgentsOnline.forEach(
                    action = { it(args[0] as Int) })
                }
                on("webpush/notification") { args ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val jsonObject = args[0] as? JSONObject ?: return@launch
                            println("WEBPUSH/NOTIFICATION jsonObject $jsonObject")
                            onWebpushNotification.forEach(
                                action = { it(jsonObject) })
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
    }

    fun onAgentsOnline(handler: (Int) -> Unit): () -> Unit {
        onAgentsOnline.add(handler)
        return { onAgentsOnline.remove(handler) }
    }

    fun onWebpushNotification(handler: (JSONObject) -> Unit): () -> Unit {
        onWebpushNotification.add(handler)
        return { onWebpushNotification.remove(handler) }
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
