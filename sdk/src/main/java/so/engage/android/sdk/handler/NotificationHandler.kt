package so.engage.android.sdk.handler

import android.content.Context
import so.engage.android.sdk.network.Endpoint
import so.engage.android.sdk.network.Network
import so.engage.android.sdk.util.Preference
import so.engage.android.sdk.util.toJson

typealias MessageHandler = (Map<String, Any>) -> Unit

class NotificationHandler private constructor(): NotificationHandlerInterface {
    companion object {
        private var _instance: NotificationHandler? = null

        val instance: NotificationHandler
            get() {
                return  _instance ?: NotificationHandler().also { _instance = it }
            }

        private var onMessageOpened: MessageHandler? = null
        private var onMessageReceived: MessageHandler? = null
    }

    override fun trackMessageOpened(context: Context, id: String) {
        val preference = Preference(context)
        val network = Network(preference)

        val data: HashMap<String, Any> = HashMap()
        data["event"] = "opened"

        network.post(Endpoint.trackNotification(id), data.toJson)
        onMessageOpened?.invoke(data)
    }

    override fun trackMessageDelivered(context: Context, id: String) {
        val preference = Preference(context)
        val network = Network(preference)

        val data: HashMap<String, Any> = HashMap()
        data["event"] = "delivered"

        network.post(Endpoint.trackNotification(id), data.toJson)
        onMessageReceived?.invoke(data)
    }

    override fun setOnMessageOpened(handler: (Map<String, Any>) -> Unit) {
        onMessageOpened = handler
    }

    override fun setOnMessageReceived(handler: (Map<String, Any>) -> Unit) {
        onMessageReceived = handler
    }
}
