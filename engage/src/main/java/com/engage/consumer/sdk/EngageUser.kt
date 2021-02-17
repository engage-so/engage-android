package com.engage.consumer.sdk

import org.json.JSONObject

class EngageUser (val id: String) {
    var emailAddress: String? = null
    var phoneNumber: String? = null
    var firstName: String? = null
    var lastName: String? = null
    var createdAt: String? = null
    var meta: Map<String, Any> = mutableMapOf()


    fun toJSON() : JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("email", emailAddress)
        obj.put("number", phoneNumber)
        obj.put("first_name", firstName)
        obj.put("last_name", lastName)
        obj.put("created_at", createdAt)

        return obj;
    }
}