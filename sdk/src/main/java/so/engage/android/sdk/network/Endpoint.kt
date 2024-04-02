package so.engage.android.sdk.network

import java.net.URL

object Endpoint {
    private const val baseUrl = "https://api.engage.so/v1/users"

    fun identify(uid: String): URL = URL("$baseUrl/$uid")
    fun setDeviceToken(uid: String): URL = URL("$baseUrl/$uid")
    fun logout(uid: String, deviceToken: String): URL = URL("$baseUrl/$uid/tokens/$deviceToken")
    fun addToAccount(uid: String): URL = URL("$baseUrl/$uid/accounts")
    fun removeFromAccount(uid: String, aid: String): URL = URL("$baseUrl/$uid/accounts/$aid")
    fun changeAccountRole(uid: String, aid: String): URL = URL("$baseUrl/$uid/accounts/$aid")
    fun convertToCustomer(uid: String): URL = URL("$baseUrl/$uid/convert")
    fun convertToAccount(uid: String): URL = URL("$baseUrl/$uid/convert")
    val merge: URL = URL("$baseUrl/merge")
    fun track(uid: String): URL = URL("$baseUrl/$uid/events")
}
