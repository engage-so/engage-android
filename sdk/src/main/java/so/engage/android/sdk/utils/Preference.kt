package so.engage.android.sdk.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import so.engage.android.sdk.models.MessageModel
import so.engage.android.sdk.models.ThreadModel

class Preference(context: Context) {
    private val gson = Gson()
    private val keyPrefix = "engage_rn:"
    private val sharedPreferences =
        context.getSharedPreferences("engage_preference", Context.MODE_PRIVATE)

    fun getString(key: String): String? = sharedPreferences.getString(keyPrefix + key, "")


    fun putString(properties: Map<String?, String?>) {
        try {
            val editor = sharedPreferences.edit()
            for ((key, value) in properties) {
                editor.putString(keyPrefix + key, value)
            }
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getBoolean(key: String): Boolean = sharedPreferences.getBoolean(keyPrefix + key, false)

    fun putBoolean(properties: Map<String?, Boolean>) {
        try {
            val editor = sharedPreferences.edit()
            for ((key, value) in properties) {
                editor.putBoolean(keyPrefix + key, value)
            }
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveData(key: String, value: Any) {
        try {
            with(sharedPreferences.edit()) {
                putString(keyPrefix + key, gson.toJson(value))
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

     fun getData(key: String): Any? {
        return try {
            val json = sharedPreferences.getString(keyPrefix + key, null)
            json?.let { gson.fromJson(it, Any::class.java) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveThreads(key: String, threads: List<ThreadModel>) {
        try {
            with(sharedPreferences.edit()) {
                putString(keyPrefix + key, gson.toJson(threads))
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadThreads(key: String): List<ThreadModel> {
        return try {
            val json = sharedPreferences.getString(keyPrefix + key, null)
            json?.let {
                gson.fromJson(it, object : TypeToken<List<ThreadModel>>() {}.type)
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveMessages(threadId: String, messages: List<MessageModel>) {
        try {
            with(sharedPreferences.edit()) {
                putString(keyPrefix + threadId, gson.toJson(messages))
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadMessages(threadId: String): List<MessageModel> {
        return try {
            val json = sharedPreferences.getString(keyPrefix + threadId, null)
            json?.let {
                gson.fromJson(it, object : TypeToken<List<MessageModel>>() {}.type)
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun clear(key: String) {
        try {
            with(sharedPreferences.edit()) {
                remove(keyPrefix + key)
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
