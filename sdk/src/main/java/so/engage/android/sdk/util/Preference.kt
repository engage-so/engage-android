package so.engage.android.sdk.util

import android.content.Context

class Preference(private val context: Context) {
    fun getString(key: String): String? {
        val pref = context.getSharedPreferences("preference", Context.MODE_PRIVATE)
        return pref.getString(key, "")
    }

    fun putString( properties: Map<String?, String?>) {
        val pref = context.getSharedPreferences("preference", Context.MODE_PRIVATE)
        val editor = pref.edit()
        for ((key, value) in properties) {
            editor.putString(key, value)
        }
        editor.apply()
    }
}