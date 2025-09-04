package so.engage.android.sdk.handler

import android.content.Context
import com.google.firebase.messaging.RemoteMessage

interface NotificationHandlerInterface {
    fun createNotificationChannel(context: Context)
    suspend fun trackMessageOpened(context: Context, message: String?, messageId: String?)
    suspend fun trackMessageDelivered(context: Context, remoteMessage: RemoteMessage) : Boolean
    fun setOnMessageOpened(handler:  (RemoteMessage) -> Unit)
    fun setOnMessageReceived(handler:  (RemoteMessage) -> Unit)
}
