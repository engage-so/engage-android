package so.engage.android.sdk.handler

import android.content.Context
import com.google.firebase.messaging.RemoteMessage

interface NotificationHandlerInterface {
    fun trackMessageOpened(context: Context, id: String)
    fun trackMessageDelivered(context: Context, remoteMessage: RemoteMessage) : Boolean
    fun setOnMessageOpened(handler:  (RemoteMessage) -> Unit)
    fun setOnMessageReceived(handler:  (RemoteMessage) -> Unit)
}
