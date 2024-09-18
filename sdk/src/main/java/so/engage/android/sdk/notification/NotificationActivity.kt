package so.engage.android.sdk.notification

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import so.engage.android.sdk.handler.NotificationHandler
import so.engage.android.sdk.util.Constants

/**
 * Activity to handle notification click events.
 *
 * This activity is launched when a notification is clicked. It tracks opened
 * metrics, handles the deep link and opens the desired activity in the host app.
 */
class NotificationActivity : Activity() {
    private  val autoTrackingEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(data = intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(data = intent)
    }

    private fun handleIntent(data: Intent?) {
        kotlin.runCatching {
            val extras = data?.extras
            // Ignore event if no data was received in extras
            if (extras == null || extras.isEmpty) return
            // Not an Engage push notification if messageId is null
            val messageId = extras.getString(Constants.MESSAGEID) ?: return

            if (autoTrackingEnabled) {
                println("Tracking Click Activity")
                NotificationHandler.instance.trackMessageOpened(this, messageId)
            }
        }.onFailure { ex ->
            println("Failed to process notification intent: ${ex.message}")
        }
        finish()
    }
}
