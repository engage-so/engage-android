package so.engage.android.sdk.handler

import android.content.Context

interface NotificationHandlerInterface {
    fun trackMessageOpened(context: Context, id: String)
    fun trackMessageDelivered(context: Context, id: String)
    fun setOnMessageOpened(handler:  (Map<String, Any>) -> Unit)
    fun setOnMessageReceived(handler:  (Map<String, Any>) -> Unit)
}
