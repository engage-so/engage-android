package com.engage.consumer.sdk

import android.content.Context
import android.util.Log
import com.engage.consumer.sdk.internal.ApiClient
import com.engage.consumer.sdk.internal.SingletonHolder
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class Engage private constructor(context: Context){

    private lateinit var user: EngageUser
    private lateinit var config: EngageConfig
    private lateinit var apiClient: ApiClient
    private var initialized:Boolean = false

    fun init(config: EngageConfig) {
        this.config = config
        this.apiClient = ApiClient(config)

        this.initialized = true
    }

    fun getUser(): EngageUser {
        return this.user
    }

    fun setUser(_user: EngageUser) {
        if(!initialized) {
            Log.e("Engage", "SDK not yet initialized")
            return
        }

        this.apiClient.PUT("/users/${_user.id}", _user.toJSON(), object : Callback {
            override fun onResponse(call: Call, response: Response) {
                Log.i("Engage", "Update User. Response: ${response.body?.string()}")
                if(response.code == 200) {
                    user = _user
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.i("Engage", "Update User Failed. Response: ${e.localizedMessage}")
            }
        })
    }

    fun setUserProperties(properties: Map<String, Any>) {

        if(!this::user.isInitialized){
            Log.e("Engage","User has not yet been initialized")
            return
        }

        val obj = JSONObject()
        obj.put("id", this.user.id)
        obj.put("meta", JSONObject(properties))

        this.apiClient.PUT("/users/${user.id}", obj, object : Callback {
            override fun onResponse(call: Call, response: Response) {
                Log.i("Engage", "Update User Properties. Response: ${response.body?.string()}")
                if(response.code == 200){
                    user.meta = properties
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.i("Engage", "Failed to update user properties. ${e.localizedMessage}")
            }
        })
    }

    fun trackEvent(event: EngageEvent) {
        if(!this::user.isInitialized){
            Log.e("Engage","User has not yet been initialized")
            return
        }

        this.apiClient.POST("/users/${user.id}/events", event.toJSON(), object : Callback {
            override fun onResponse(call: Call, response: Response) {
                Log.i("Engage", "Track Event. Response: ${response.body?.string()}")
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.i("Engage", "Failed to track user event. ${e.localizedMessage}")
            }
        })
    }

    companion object : SingletonHolder<Engage, Context> (::Engage)
}