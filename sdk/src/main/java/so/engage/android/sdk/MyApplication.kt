package so.engage.android.sdk

import android.app.Application
import so.engage.android.sdk.handler.NotificationHandler

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHandler.instance.createNotificationChannel(this)
    }
}