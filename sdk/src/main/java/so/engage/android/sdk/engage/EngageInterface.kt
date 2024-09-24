package so.engage.android.sdk.engage

import android.content.Context
import java.util.Date

interface EngageInterface {
    fun initialise(context: Context, publicKey: String): Engage
    fun identify(uid: String, properties: Map<String, Any>)
    fun setDeviceToken(deviceToken: String, uid: String? = null)
    fun logout(deviceToken: String? = null, uid: String? = null)
    fun addToAccount(aid: String, role: String? = null, uid: String? = null)
    fun addAttributes(properties: Map<String, Any>, uid: String? = null)
    fun removeFromAccount(aid: String, uid: String? = null)
    fun changeAccountRole(aid: String, role: String, uid: String? = null)
    fun convertToCustomer(uid: String? = null)
    fun convertToAccount(uid: String? = null)
    fun merge(source: String, destination: String)
    fun track(event: String, value: Any? = null, date: Date? = null, uid: String? = null)
    fun onMessageOpened(handler:  (Map<String, Any>) -> Unit)
    fun onMessageReceived(handler:  (Map<String, Any>) -> Unit)
}
