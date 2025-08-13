package so.engage.android.sdk.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
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
        // Handle received message if messageId is available
        NotificationHandler.instance.trackMessageDelivered(
            context = this,
            remoteMessage = remoteMessage
        )
    }


    override fun onNewToken(token: String) {
        // Handle the updated token
        val hasUsageActivity = Preference(this).getBoolean(Constants.HAS_USAGE_ACTIVITY)
        if (hasUsageActivity) {
            Engage.instance.setDeviceToken(token)
        }
    }
}
