package so.engage.android.sdk.network

import java.net.URL

object Endpoint {
    private const val USERS = "https://api.engage.so/v1/users"
    private const val MESSAGES = "https://api.engage.so/v1/messages/mobile/push"

    fun identify(uid: String): URL = URL("$USERS/$uid")
    fun setDeviceToken(uid: String): URL = URL("$USERS/$uid")
    fun logout(uid: String, deviceToken: String): URL = URL("$USERS/$uid/tokens/$deviceToken")
    fun addToAccount(uid: String): URL = URL("$USERS/$uid/accounts")
    fun removeFromAccount(uid: String, aid: String): URL = URL("$USERS/$uid/accounts/$aid")
    fun changeAccountRole(uid: String, aid: String): URL = URL("$USERS/$uid/accounts/$aid")
    fun convertToCustomer(uid: String): URL = URL("$USERS/$uid/convert")
    fun convertToAccount(uid: String): URL = URL("$USERS/$uid/convert")
    val merge: URL = URL("$USERS/merge")
    fun track(uid: String): URL = URL("$USERS/$uid/events")
    fun trackNotification(id: String): URL = URL("$MESSAGES/$id/track")

}
