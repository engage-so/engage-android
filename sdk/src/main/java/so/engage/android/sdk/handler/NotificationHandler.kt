package so.engage.android.sdk.handler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService.NOTIFICATION_SERVICE
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import so.engage.android.sdk.network.Endpoint
import so.engage.android.sdk.network.Network
import so.engage.android.sdk.notification.NotificationActivity
import so.engage.android.sdk.utils.Constants
import so.engage.android.sdk.utils.Preference
import so.engage.android.sdk.utils.getColorOrNull
import so.engage.android.sdk.utils.getDrawableByName
import so.engage.android.sdk.utils.getMetaDataResource
import so.engage.android.sdk.utils.getMetaDataString
import so.engage.android.sdk.utils.toColorOrNull
import so.engage.android.sdk.utils.toJson
import so.engage.android.sdk.utils.toRemoteMessage
import java.net.URL
import kotlin.math.abs

typealias MessageHandler = (RemoteMessage) -> Unit

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

    private val channelId = "engage_so_default_channel"

    override fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Engage.so default channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override suspend fun trackMessageOpened(context: Context, message: String?, messageId: String?) {
        val preference = Preference(context)
        val network = Network(preference)

        val data: HashMap<String, Any> = HashMap()
        data["event"] = "opened"

        val remoteMessage = message?.toRemoteMessage
        val id = messageId ?: remoteMessage?.data?.get(Constants.MESSAGEID)

        if (id != null) {
            network.post(Endpoint.trackNotification(id), data.toJson)
        }

        // Launch the host app
        val defaultHostAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        context.startActivity(defaultHostAppIntent)

        if (remoteMessage != null) {
            onMessageOpened?.invoke(remoteMessage)
        }
    }

    override suspend fun trackMessageDelivered(context: Context, remoteMessage: RemoteMessage) : Boolean {
        val preference = Preference(context)
        val network = Network(preference)

        val messageId = remoteMessage.data[Constants.MESSAGEID] ?: return false

        val data: HashMap<String, Any> = HashMap()
        data["event"] = "delivered"

        network.post(Endpoint.trackNotification(messageId), data.toJson)
        onMessageReceived?.invoke(remoteMessage)

        return showNotification(context, remoteMessage)
    }

    override fun setOnMessageOpened(handler: (RemoteMessage) -> Unit) {
        onMessageOpened = handler
    }

    override fun setOnMessageReceived(handler: (RemoteMessage) -> Unit) {
        onMessageReceived = handler
    }

    private fun showNotification(context: Context, remoteMessage: RemoteMessage) : Boolean {
        val applicationName = context.applicationInfo.loadLabel(context.packageManager).toString()
        val requestCode = abs(System.currentTimeMillis().toInt())

        val applicationInfo = try {
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
        } catch (ex: Exception) {
            println("Package not found ${ex.message}")
            null
        }
        val appMetaData = applicationInfo?.metaData

        @DrawableRes
        val smallIcon: Int =
            remoteMessage.notification?.icon?.let { iconName -> context.getDrawableByName(iconName) }
                ?: appMetaData?.getMetaDataResource(name = Constants.FCM_METADATA_DEFAULT_NOTIFICATION_ICON)
                ?: context.applicationInfo.icon

        @ColorInt
        val tintColor: Int? =
            remoteMessage.notification?.color?.toColorOrNull()
                ?: appMetaData?.getMetaDataResource(name = Constants.FCM_METADATA_DEFAULT_NOTIFICATION_COLOR)
                    ?.let { id -> context.getColorOrNull(id) }
                ?: appMetaData?.getMetaDataString(name = Constants.FCM_METADATA_DEFAULT_NOTIFICATION_COLOR)
                    ?.toColorOrNull()
        // set title and body
        val title = remoteMessage.data[Constants.TITLE_KEY] ?: remoteMessage.notification?.title ?: ""
        val body = remoteMessage.data[Constants.BODY_KEY] ?: remoteMessage.notification?.body ?: ""

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setTicker(applicationName)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        tintColor?.let { color -> notificationBuilder.setColor(color) }
        try {
            // check for image in data and notification payload to cater for both simple and rich push
            // data only payload (foreground and background)
            // notification + data payload (foreground)
            val notificationImage = remoteMessage.data[Constants.IMAGE_KEY] ?: remoteMessage.notification?.imageUrl?.toString()
            if (notificationImage != null) {
                addImage(notificationImage, notificationBuilder, body)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        createNotificationChannel(context)

        // set pending intent
        val notifyIntent = Intent(context, NotificationActivity::class.java)
        notifyIntent.putExtra(Constants.ENGAGE_INTENT_EXTRA, remoteMessage.toJson)
        // In Android M, you must specify the mutability of each PendingIntent
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        notificationBuilder.setContentIntent(
            PendingIntent.getActivity(context, requestCode, notifyIntent, flags)
        )
        val notification = notificationBuilder.build()
        notificationManager.notify(requestCode, notification)

        return true
    }

    private fun addImage(
        imageUrl: String,
        builder: NotificationCompat.Builder,
        body: String
    ) = runBlocking {
        val style = NotificationCompat.BigPictureStyle()
            .setSummaryText(body)
        val url = URL(imageUrl)
        withContext(Dispatchers.IO) {
            try {
                val input = url.openStream()
                BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                null
            }
        }?.let { bitmap ->
            style.bigPicture(bitmap)
            builder.setLargeIcon(bitmap)
            builder.setStyle(style)
        }
    }
}
