package so.engage.android.sdk.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
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

@ColorInt
internal fun String.toColorOrNull(): Int? = try {
    Color.parseColor(this)
} catch (ex: IllegalArgumentException) {
    println("Invalid color string $this, ${ex.message}")
    null
}

@SuppressLint("DiscouragedApi")
@DrawableRes
internal fun Context.getDrawableByName(name: String?): Int? {
    if (name.isNullOrBlank()) {
        return null
    }
    // Using getIdentifier is discouraged, but it's the only way to get drawable resource by name.
    // We need this to get the icon from push payload.
    return resources?.getIdentifier(name, "drawable", packageName)?.takeUnless { id ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            id == Resources.ID_NULL
        } else {
            id == 0
        }
    }
}

@ColorInt
internal fun Context.getColorOrNull(@ColorRes id: Int): Int? = try {
    ContextCompat.getColor(this, id)
} catch (ex: Resources.NotFoundException) {
    println("Invalid resource $id, ${ex.message}")
    null
}

private val RESOURCE_ID_NULL: Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Resources.ID_NULL else 0

internal fun Bundle.getMetaDataResource(name: String): Int? {
    return getInt(name, RESOURCE_ID_NULL).takeUnless { id -> id == RESOURCE_ID_NULL }
}

internal fun Bundle.getMetaDataString(name: String): String? {
    return getString(name, null).takeUnless { value -> value.isNullOrBlank() }
}