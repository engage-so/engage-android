package so.engage.android.sdk.network

import com.google.gson.reflect.TypeToken
import java.net.URL

interface NetworkInterface {
    suspend fun <T> get(url: URL, typeToken: TypeToken<T>): T
    suspend fun get(url: URL)
    suspend fun <T> post(url: URL, body: String, typeToken: TypeToken<T>): T
    suspend fun post(url: URL, body: String)
    suspend fun <T> put(url: URL, body: String, typeToken: TypeToken<T>): T
    suspend fun put(url: URL, body: String)
    suspend fun <T> delete(url: URL, typeToken: TypeToken<T>): T
    suspend fun delete(url: URL)
}