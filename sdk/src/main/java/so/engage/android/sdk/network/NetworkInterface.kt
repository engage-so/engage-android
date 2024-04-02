package so.engage.android.sdk.network

import java.net.URL

interface NetworkInterface {
    fun post(url: URL, body: String)
    fun put(url: URL, body: String)
    fun delete(url: URL)
}