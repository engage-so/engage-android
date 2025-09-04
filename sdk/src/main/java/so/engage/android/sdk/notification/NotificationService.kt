package so.engage.android.sdk.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import so.engage.android.sdk.engage.Engage
import so.engage.android.sdk.handler.NotificationHandler
import so.engage.android.sdk.utils.Constants
import so.engage.android.sdk.utils.Preference
import so.engage.android.sdk.utils.toJson

class NotificationService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        NotificationHandler.instance.createNotificationChannel(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        println("NOTIFICATION ${remoteMessage.toJson}")
        val context = this
        // Handle received message if messageId is available
        CoroutineScope(Dispatchers.IO).launch {
            NotificationHandler.instance.trackMessageDelivered(
                context = context,
                remoteMessage = remoteMessage
            )
        }
    }


    override fun onNewToken(token: String) {
        // Handle the updated token
        val hasUsageActivity = Preference(this).getBoolean(Constants.HAS_USAGE_ACTIVITY)
        if (hasUsageActivity) {
            CoroutineScope(Dispatchers.IO).launch {
                Engage.instance.setDeviceToken(token)
            }
        }
    }
}
