package so.engage.android.sdk.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import so.engage.android.sdk.engage.Engage
import so.engage.android.sdk.handler.NotificationHandler
import so.engage.android.sdk.util.Constants
import so.engage.android.sdk.util.getColorOrNull
import so.engage.android.sdk.util.getDrawableByName
import so.engage.android.sdk.util.getMetaDataResource
import so.engage.android.sdk.util.getMetaDataString
import so.engage.android.sdk.util.toColorOrNull
import java.net.URL
import kotlin.math.abs

class NotificationService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        println("NOTIFICATION DATA ${remoteMessage.data}")
        val bundle: Bundle by lazy {
            Bundle().apply {
                remoteMessage.data.forEach { entry ->
                    putString(entry.key, entry.value)
                    println("ENGAGE MESSAGE PAYLOAD KEY: ${entry.key} VALUE: ${entry.value}")
                }
            }
        }

        val messageId = bundle.getString(Constants.MESSAGEID) ?: return
        // Handle received message if messageId is available
        NotificationHandler.instance.trackMessageDelivered(this, messageId)
        // Handle received message
        showNotification(bundle, remoteMessage)
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Handle the updated token
        println("Token Renewed $token")
        Engage.instance.setDeviceToken(token)
    }

    private fun showNotification(bundle: Bundle, remoteMessage: RemoteMessage) {
        val messageId = bundle.getString(Constants.MESSAGEID)
        val applicationName = this.applicationInfo.loadLabel(this.packageManager).toString()
        val requestCode = abs(System.currentTimeMillis().toInt())

        bundle.putInt(Constants.NOTIFICATION_REQUEST_CODE, requestCode)

        val applicationInfo = try {
            this.packageManager.getApplicationInfo(
                this.packageName,
                PackageManager.GET_META_DATA
            )
        } catch (ex: Exception) {
            println("Package not found ${ex.message}")
            null
        }
        val appMetaData = applicationInfo?.metaData

        @DrawableRes
        val smallIcon: Int =
            remoteMessage.notification?.icon?.let { iconName -> this.getDrawableByName(iconName) }
                ?: appMetaData?.getMetaDataResource(name = Constants.FCM_METADATA_DEFAULT_NOTIFICATION_ICON)
                ?: this.applicationInfo.icon

        @ColorInt
        val tintColor: Int? =
            remoteMessage.notification?.color?.toColorOrNull()
                ?: appMetaData?.getMetaDataResource(name = Constants.FCM_METADATA_DEFAULT_NOTIFICATION_COLOR)
                    ?.let { id -> this.getColorOrNull(id) }
                ?: appMetaData?.getMetaDataString(name = Constants.FCM_METADATA_DEFAULT_NOTIFICATION_COLOR)
                    ?.toColorOrNull()
        // set title and body
        val title = bundle.getString(Constants.TITLE_KEY) ?: remoteMessage.notification?.title ?: ""
        val body = bundle.getString(Constants.BODY_KEY) ?: remoteMessage.notification?.body ?: ""

        val channelId = this.packageName
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
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
            val notificationImage = bundle.getString(Constants.IMAGE_KEY) ?: remoteMessage.notification?.imageUrl?.toString()
            if (notificationImage != null) {
                addImage(notificationImage, notificationBuilder, body)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channelName = "$applicationName Notifications"

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        // set pending intent
        val notifyIntent = Intent(this, NotificationActivity::class.java)
        notifyIntent.putExtra(Constants.MESSAGEID, messageId)
        // In Android M, you must specify the mutability of each PendingIntent
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        notificationBuilder.setContentIntent(
            PendingIntent.getActivity(this, requestCode, notifyIntent, flags)
        )
        val notification = notificationBuilder.build()
        notificationManager.notify(requestCode, notification)
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
