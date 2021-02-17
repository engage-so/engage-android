package com.engage.consumer.sdk

import org.json.JSONObject

class EngageEvent (val event: String){
    private var value: Any? = true

    constructor(event: String, value: String) : this(event) {
        this.value = value
    }

    constructor(event: String, value: Int) : this(event) {
        this.value = value
    }

    constructor(event: String, value: Boolean) : this(event) {
        this.value = value
    }

    constructor(event: String, value: Any) : this(event) {
        this.value = value
    }

    var timestamp: String? = null
    var properties: Map<String, Any> = HashMap()

    fun toJSON() : JSONObject {
        val obj = JSONObject()

        obj.put("event", event)
        obj.put("value", value ?: true)
        obj.put("timestamp", timestamp)
        if(properties.isNotEmpty()){
            obj.put("properties", JSONObject(properties))
        }
        return obj
    }
}