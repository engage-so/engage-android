package so.engage.android.sdk.network

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import so.engage.android.sdk.util.Preference
import java.io.IOException
import java.net.URL


class Network(private val preference: Preference) : NetworkInterface {
    private val contentType = "application/json; charset=UTF-8".toMediaType()

    private fun request(builder: Builder) {
        val client = OkHttpClient()
        val publicKey = preference.getString("publicKey") ?: ""
        val credential: String = Credentials.basic(publicKey, "")
        val request: Request = builder
            .header("Authorization", credential)
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("ENGAGE: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                println("ENGAGE: ${response.code}")
            }
        })
    }

    override fun post(url: URL, body: String) {
        try {
            request(Builder().url(url).post(body.toRequestBody(contentType)))
        } catch (e: Exception) {
            println(e.toString())
        }
    }

    override fun put(url: URL, body: String) {
        try {
            request(Builder().url(url).put(body.toRequestBody()))
        } catch (e: Exception) {
            println(e.toString())
        }
    }

    override fun delete(url: URL) {
        try {
            request(Builder().url(url).delete())
        } catch (e: Exception) {
            println(e.toString())
        }
    }
}
