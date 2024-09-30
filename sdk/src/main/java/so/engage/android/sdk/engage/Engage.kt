package so.engage.android.sdk.engage

import android.content.Context
import com.google.firebase.messaging.RemoteMessage
import so.engage.android.sdk.handler.NotificationHandler
import so.engage.android.sdk.network.Endpoint
import so.engage.android.sdk.network.Network
import so.engage.android.sdk.util.Constants
import so.engage.android.sdk.util.Preference
import so.engage.android.sdk.util.build
import so.engage.android.sdk.util.toJson
import so.engage.android.sdk.util.version
import java.util.Date
import java.util.UUID


@Suppress("NAME_SHADOWING")
class Engage private constructor() : EngageInterface {
    companion object {
        private var _instance: Engage? = null

        val instance: Engage
            get() {
                return  _instance ?: Engage().also { _instance = it }
            }
    }

    private lateinit var preference: Preference
    private lateinit var network: Network

    private var version: String = ""
    private var build: String = ""


    private fun userId(uid: String?): String {
        val id = uid ?: preference.getString(Constants.UID)
        if (id == null)  {
            val anonymous = UUID.randomUUID().toString()
            preference.putString(mapOf(Constants.UID to  anonymous))
            return anonymous
        }
        return id
    }

    override fun initialise(context: Context, publicKey: String): Engage {
        preference = Preference(context)
        network = Network(preference)
        version = context.version
        build = context.build
        preference.putString(mapOf(Constants.PUBLIC_KEY to  publicKey))

        return instance
    }

    override fun identify(uid: String, properties: Map<String, Any>) {
        val id = preference.getString(Constants.UID)
        if (id != null && id != uid)  {
            merge(id, uid)
        }

        preference.putString(mapOf(Constants.UID to  uid))

        val data: HashMap<String, Any> = HashMap()
        val meta: HashMap<String, Any> = HashMap()
        val standardAttributes = listOf("is_account", "first_name", "last_name", "email", "number", "created_at", "tz")

        for (key in properties.keys) {
            if (standardAttributes.contains(key)) {
                data[key] = properties[key] as Any
            } else {
                meta[key] = properties[key] as Any
            }
        }
        data["meta"] = meta

        network.put(Endpoint.identify(uid), data.toJson)
        val hasUsageActivity = preference.getBoolean(Constants.HAS_USAGE_ACTIVITY)
        if (!hasUsageActivity) {
            preference.putBoolean(mapOf(Constants.HAS_USAGE_ACTIVITY to  true))
        }
    }

    override fun setDeviceToken(deviceToken: String, uid: String?) {
        preference.putString(mapOf(Constants.DEVICE_TOKEN to  deviceToken))

        val uid = userId(uid)
        val data: HashMap<String, Any> = HashMap()
        data["device_token"] = deviceToken
        data["device_platform"] = "android"
        data["app_version"] = version
        data["app_build"] = build
        data["app_last_active"] = Date()

        network.put(Endpoint.setDeviceToken(uid), data.toJson)
        val hasUsageActivity = preference.getBoolean(Constants.HAS_USAGE_ACTIVITY)
        if (!hasUsageActivity) {
            preference.putBoolean(mapOf(Constants.HAS_USAGE_ACTIVITY to  true))
        }
    }

    override fun logout(deviceToken: String?, uid: String?) {
        val uid = userId(uid)
        val token = deviceToken ?: preference.getString(Constants.DEVICE_TOKEN) ?: ""
        network.delete(Endpoint.logout(uid, token))
    }

    override fun addToAccount(aid: String, role: String?, uid: String?) {
        val uid = userId(uid)
        val account: HashMap<String, Any> = HashMap()
        account["id"] = aid
        if (role != null) {
            account["role"] = role
        }
        val accounts = listOf(account)

        val data: HashMap<String, Any> = HashMap()
        data["accounts"] = accounts
        network.post(Endpoint.addToAccount(uid), data.toJson)
    }

    override fun addAttributes(properties: Map<String, Any>, uid: String?) {
        val uid = userId(uid)
        identify(uid, properties)
    }

    override fun removeFromAccount(aid: String, uid: String?) {
        val uid = userId(uid)
        network.delete(Endpoint.removeFromAccount(uid, aid))
    }

    override fun changeAccountRole(aid: String, role: String, uid: String?) {
        val uid = userId(uid)
        val data: HashMap<String, Any> = HashMap()
        data["role"] = role

        network.put(Endpoint.changeAccountRole(uid, aid), data.toJson)
    }

    override fun convertToCustomer(uid: String?) {
        val uid = userId(uid)
        val data: HashMap<String, Any> = HashMap()
        data["type"] = "customer"

        network.post(Endpoint.convertToCustomer(uid), data.toJson)
    }

    override fun convertToAccount(uid: String?) {
        val uid = userId(uid)
        val data: HashMap<String, Any> = HashMap()
        data["type"] = "account"

        network.post(Endpoint.convertToAccount(uid), data.toJson)
    }

    override fun merge(source: String, destination: String) {
        val data: HashMap<String, Any> = HashMap()
        data["source"] = source
        data["destination"] = destination

        network.post(Endpoint.merge, data.toJson)
    }

    override fun track(event: String, value: Any?, date: Date?, uid: String?) {
        val uid = userId(uid)
        val data: HashMap<String, Any> = HashMap()
        data["event"] = event
        if (value is Date && date == null) {
            data["timestamp"] = value
        } else if (value is Map<*, *>) {
            data["properties"] = value
        } else if (value != null) {
            data["value"] = value
        }
        if (date != null) {
            data["timestamp"] = date
        }

        network.post(Endpoint.track(uid), data.toJson)
        val hasUsageActivity = preference.getBoolean(Constants.HAS_USAGE_ACTIVITY)
        if (!hasUsageActivity) {
            preference.putBoolean(mapOf(Constants.HAS_USAGE_ACTIVITY to  true))
        }
    }

    override fun onMessageOpened(handler: (RemoteMessage) -> Unit) {
        NotificationHandler.instance.setOnMessageOpened(handler)
    }

    override fun onMessageReceived(handler: (RemoteMessage) -> Unit) {
        NotificationHandler.instance.setOnMessageReceived(handler)
    }

    override fun handleMessageReceived(context: Context, remoteMessage: RemoteMessage): Boolean {
        return NotificationHandler.instance.trackMessageDelivered(context, remoteMessage)
    }
}
