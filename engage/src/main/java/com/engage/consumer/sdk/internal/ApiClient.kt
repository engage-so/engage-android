package com.engage.consumer.sdk.internal

import com.engage.consumer.sdk.EngageConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


class ApiClient(config: EngageConfig) {
    private val root: String = "https://api.engage.so/v1"

    private var client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val cred = Credentials.basic(config.apiKey, config.apiSecret ?: "")
                    val request: Request = chain
                        .request()
                        .newBuilder()
                        .addHeader("Authorization", cred)
                        .build()
                    return chain.proceed(request)
                }
            }
        )
        .build()

    fun GET(url: String, callback: Callback): Call {
        val request = Request.Builder()
            .url(root+url)
            .build()

        val call = client.newCall(request)
        call.enqueue(callback)
        return call
    }

    fun PUT(url: String, parameters: HashMap<String, String>, callback: Callback): Call {
        val builder = FormBody.Builder()
        val it = parameters.entries.iterator()
        while (it.hasNext()) {
            val pair = it.next() as Map.Entry<*, *>
            builder.add(pair.key.toString(), pair.value.toString())
        }

        val formBody = builder.build()
        val request = Request.Builder()
            .url(root+url)
            .put(formBody)
            .build()


        val call = client.newCall(request)
        call.enqueue(callback)
        return call
    }

    fun POST(url: String, body: JSONObject, callback: Callback): Call {
        val request = Request.Builder()
            .url(root+url)
            .post(body.toString().toRequestBody(JSON))
            .build()


        val call = client.newCall(request)
        call.enqueue(callback)
        return call
    }


    fun PUT(url: String, body: JSONObject, callback: Callback): Call {
        val request = Request.Builder()
            .url(root+url)
            .put(body.toString().toRequestBody(JSON))
            .build()


        val call = client.newCall(request)
        call.enqueue(callback)
        return call
    }

    companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}