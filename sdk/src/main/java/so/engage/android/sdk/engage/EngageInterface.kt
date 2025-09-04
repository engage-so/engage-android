package so.engage.android.sdk.engage

import android.content.Context
import com.google.firebase.messaging.RemoteMessage
import java.util.Date

interface EngageInterface {
    fun init(context: Context, publicKey: String): Engage
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
    fun onMessageOpened(handler:  (RemoteMessage) -> Unit)
    fun onMessageReceived(handler:  (RemoteMessage) -> Unit)
    suspend fun handleMessageReceived(context: Context, remoteMessage: RemoteMessage) : Boolean
    fun showDialog(context: Context, isCarousel: Boolean)
    fun openChat(context: Context, uid: String)
}
