package com.narayani.bridgeline.networking

import org.json.JSONObject

data class BridgelinePacket(
    val id: String,
    val text: String,
    val lat: Double,
    val long: Double,
    val priority: Int,
    val peopleCount: Int,
    val status: String,
    val isSOS: Boolean,
    val ttl: Int = 15
) {

    fun toJson(): String {
        return JSONObject().apply {
            put("id", id)
            put("text", text)
            put("lat", lat)
            put("long", long)
            put("priority", priority)
            put("peopleCount", peopleCount)
            put("status", status)
            put("isSOS", isSOS)
            put("ttl", ttl)
        }.toString()
    }

    companion object {

        fun fromJson(json: String): BridgelinePacket {
            val obj = JSONObject(json)

            return BridgelinePacket(
                id = obj.getString("id"),
                text = obj.getString("text"),
                lat = obj.getDouble("lat"),
                long = obj.getDouble("long"),
                priority = obj.getInt("priority"),
                peopleCount = obj.getInt("peopleCount"),
                status = obj.getString("status"),
                isSOS = obj.getBoolean("isSOS"),
                ttl = obj.optInt("ttl", 15)
            )
        }
    }
}