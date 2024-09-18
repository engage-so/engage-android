package so.engage.android.sdk.handler

import android.content.Context
import so.engage.android.sdk.network.Endpoint
import so.engage.android.sdk.network.Network
import so.engage.android.sdk.util.Preference
import so.engage.android.sdk.util.toJson

class NotificationHandler private constructor(): NotificationHandlerInterface {
    companion object {
        private var _instance: NotificationHandler? = null

        val instance: NotificationHandler
            get() {
                return  _instance ?: NotificationHandler().also { _instance = it }
            }
    }

    override fun trackMessageOpened(context: Context, id: String) {
        val preference = Preference(context)
        val network = Network(preference)

        val data: HashMap<String, Any> = HashMap()
        data["event"] = "opened"

        network.post(Endpoint.trackNotification(id), data.toJson)
    }

    override fun trackMessageDelivered(context: Context, id: String) {
        val preference = Preference(context)
        val network = Network(preference)

        val data: HashMap<String, Any> = HashMap()
        data["event"] = "delivered"

        network.post(Endpoint.trackNotification(id), data.toJson)
    }
}
