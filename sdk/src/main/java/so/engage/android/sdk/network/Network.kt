package so.engage.android.sdk.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import so.engage.android.sdk.utils.Constants
import so.engage.android.sdk.utils.Preference
import java.io.IOException
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Network(private val preference: Preference) : NetworkInterface {
    private val contentType = "application/json; charset=UTF-8".toMediaType()

    private suspend fun <T> request(builder: Builder, typeToken: TypeToken<T>? = null): T? = suspendCancellableCoroutine { continuation ->
        val client = OkHttpClient()
        val publicKey = preference.getString(Constants.PUBLIC_KEY) ?: ""
        val credential: String = Credentials.basic(publicKey, "")
        println("CREDENTIAL $credential")
        val request: Request = builder
            .header("Authorization", credential)
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("ENGAGE: Failure: ${e.message}")
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        println("ENGAGE: Response body: $responseBody")
                        try {
                            if (typeToken != null && responseBody != null) {
                                val gson = Gson()
                                val data: T? = gson.fromJson(responseBody, typeToken.type)
                                continuation.resume(data)
                            } else {
                                continuation.resume(null)
                            }
                        } catch (e: Exception) {
                            println("ENGAGE: JSON parsing error: ${e.message}\n${request.url}\n${request.body}")
                            continuation.resumeWithException(e)
                        }
                    } else {
                        val errorBody = response.body?.string()
                        println("ENGAGE: Error body: $errorBody\n${request.url}\n${request.body}")
                        continuation.resumeWithException(Exception("Request failed with code ${response.code}: $errorBody"))
                    }
                }
            }
        })

        // Handle cancellation
        continuation.invokeOnCancellation {
            call.cancel()
        }
    }

    override suspend fun <T> get(url: URL, typeToken: TypeToken<T>): T {
        return try {
            val builder = Builder().url(url).get()
            request(builder, typeToken) ?: throw Exception("Response body is null")
        } catch (e: Exception) {
            println("ENGAGE: Error in get: ${e.message}")
            throw e
        }
    }

    override suspend fun get(url: URL) {
        try {
            val builder = Builder().url(url).get()
            request<Any>(builder, null)
        } catch (e: Exception) {
            println("ENGAGE: Error in get: ${e.message}")
            throw e
        }
    }

    override suspend fun <T> post(url: URL, body: String, typeToken: TypeToken<T>): T {
        return try {
            val builder = Builder().url(url).post(body.toRequestBody(contentType))
            request(builder, typeToken) ?: throw Exception("Response body is null")
        } catch (e: Exception) {
            println("ENGAGE: Error in post: ${e.message}")
            throw e
        }
    }

    override suspend fun post(url: URL, body: String) {
        try {
            val builder = Builder().url(url).post(body.toRequestBody(contentType))
            request<Any>(builder, null)
        } catch (e: Exception) {
            println("ENGAGE: Error in post: ${e.message}")
            throw e
        }
    }

    override suspend fun <T> put(url: URL, body: String, typeToken: TypeToken<T>): T {
        return try {
            val builder = Builder().url(url).put(body.toRequestBody(contentType))
            request(builder, typeToken) ?: throw Exception("Response body is null")
        } catch (e: Exception) {
            println("ENGAGE: Error in put: ${e.message}")
            throw e
        }
    }

    override suspend fun put(url: URL, body: String) {
        try {
            val builder = Builder().url(url).put(body.toRequestBody(contentType))
            request<Any>(builder, null)
        } catch (e: Exception) {
            println("ENGAGE: Error in put: ${e.message}")
            throw e
        }
    }

    override suspend fun <T> delete(url: URL, typeToken: TypeToken<T>): T {
        return try {
            val builder = Builder().url(url).delete()
            request(builder, typeToken) ?: throw Exception("Response body is null")
        } catch (e: Exception) {
            println("ENGAGE: Error in delete: ${e.message}")
            throw e
        }
    }

    override suspend fun delete(url: URL) {
        try {
            val builder = Builder().url(url).delete()
            request<Any>(builder, null)
        } catch (e: Exception) {
            println("ENGAGE: Error in delete: ${e.message}")
            throw e
        }
    }
}