package so.engage.android.sdk.util

import android.content.Context
import android.os.Build
import com.google.gson.Gson

val Map<String, Any>.toJson: String
    get() {
       return Gson().toJson(this)
    }

val Context.version: String
    get() {
        val packageInfo = this.packageManager.getPackageInfo(this.packageName, 0)
        return packageInfo.versionName
    }

val Context.build: String
    get() {
        val packageInfo = this.packageManager.getPackageInfo(this.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            "${packageInfo.longVersionCode}"
        } else {
            @Suppress("DEPRECATION")
            "${packageInfo.versionCode}"
        }
    }
