package com.example.mydiplom.ui.message


data class Message(
    val content: String,
    val senderId: Long,
    val chatId: Long,
    val senderName: String
)

data class MessageDTO(
    val SenderId: Long,
    val Content: String
)