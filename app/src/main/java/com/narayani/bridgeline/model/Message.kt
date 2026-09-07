package com.narayani.bridgeline.model

data class Message(
    val id: String,
    val text: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val ttl: Int,
    val priority: String,
    val type: MessageType
)

enum class MessageType {
    CRITICAL,
    DETAILED
}