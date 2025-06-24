package com.example.mydiplom.ui.chat

data class Chat(
    val id: Long = 0,
    val name: String? = null,
    val lastMessage: String? = null,
    val time: String? = null,
    val unreadCount: Int = 0
)


