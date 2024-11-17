package so.engage.android.sdk.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import so.engage.android.sdk.engage.Engage
import so.engage.android.sdk.handler.NotificationHandler
import so.engage.android.sdk.utils.Constants
import so.engage.android.sdk.utils.Preference

class NotificationService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        println("NOTIFICATION DATA ${remoteMessage.data}")
        // Handle received message if messageId is available
        NotificationHandler.instance.trackMessageDelivered(
            context = this,
            remoteMessage = remoteMessage
        )
    }


    override fun onNewToken(token: String) {
        // Handle the updated token
        println("Token Renewed $token")
        val hasUsageActivity = Preference(this).getBoolean(Constants.HAS_USAGE_ACTIVITY)
        if (hasUsageActivity) {
            Engage.instance.setDeviceToken(token)
        }
    }
}
