package so.engage.android.sdk.handler

import android.content.Context

interface NotificationHandlerInterface {
    fun trackMessageOpened(context: Context, id: String)
    fun trackMessageDelivered(context: Context, id: String)
}
