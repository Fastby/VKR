package com.example.mydiplom.ui.websocket

import android.content.Context
import android.util.Log
import com.example.mydiplom.ui.tokenManager.TokenManager
import com.google.gson.annotations.SerializedName
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WebSocketManager(private val context: Context) {
    private val tokenManager = TokenManager(context)
    private var hubConnection: HubConnection? = null
    private val listeners = mutableListOf<WebSocketListener>()

    fun addListener(listener: WebSocketListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: WebSocketListener) {
        listeners.remove(listener)
    }

    fun startConnection(onError: (String) -> Unit = {}) {
        val token = tokenManager.getToken() ?: run {
            Log.e("WebSocket", "No token available")
            onError("Требуется авторизация")
            return
        }

        Log.d("WebSocket", "Token value: $token")

        try {
            hubConnection = HubConnectionBuilder.create("https://corptest123.loca.lt/chatHub")
                .withAccessTokenProvider(Single.just(token))
                .build()
        } catch (e: Exception) {
            Log.e("WebSocket", "Error creating HubConnection: ${e.message}")
            onError("Ошибка создания HubConnection: ${e.message}")
            return
        }

        hubConnection?.on("ReceiveMessage", { message ->
            Log.d("WebSocket", "Received message: $message")
            listeners.forEach { it.onMessageReceived(message) }
        }, MessageDto::class.java)

        hubConnection?.on("UserStatusChanged", { userId, isOnline ->
            Log.d("WebSocket", "UserStatusChanged: userId=$userId, isOnline=$isOnline")
            listeners.forEach { it.onUserStatusChanged(userId as Long, isOnline as Boolean) }
        }, Long::class.java, Boolean::class.java)

        hubConnection?.on("UserTyping", { userId, chatId ->
            Log.d("WebSocket", "UserTyping: userId=$userId, chatId=$chatId")
            listeners.forEach { it.onUserTyping(userId as Long, chatId as String) }
        }, Long::class.java, String::class.java)

        hubConnection?.on("UserStoppedTyping", { userId, chatId ->
            Log.d("WebSocket", "UserStoppedTyping: userId=$userId, chatId=$chatId")
            listeners.forEach { it.onUserStoppedTyping(userId as Long, chatId as String) }
        }, Long::class.java, String::class.java)

        hubConnection?.on("MessagesRead", { userId, chatId ->
            Log.d("WebSocket", "MessagesRead: userId=$userId, chatId=$chatId")
            listeners.forEach { it.onMessagesRead(userId as Long, chatId as Long) }
        }, Long::class.java, Long::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                hubConnection?.start()?.blockingAwait()
                Log.d("WebSocket", "Connection started")
                listeners.forEach { it.onConnected() }
            } catch (e: Exception) {
                Log.e("WebSocket", "Error starting connection: ${e.message}")
                onError("Ошибка подключения к WebSocket: ${e.message}")

                delay(5000)
                startConnection(onError)
            }
        }
    }

    fun stopConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                hubConnection?.stop()?.blockingAwait()
                Log.d("WebSocket", "Connection stopped")
                listeners.forEach { it.onDisconnected() }
            } catch (e: Exception) {
                Log.e("WebSocket", "Error stopping connection: ${e.message}")
            }
        }
    }

    fun isConnected(): Boolean {
        return hubConnection?.connectionState == HubConnectionState.CONNECTED
    }

    fun joinChat(chatId: Long) {
        if (isConnected()) {
            hubConnection?.send("JoinChat", chatId.toString())
            Log.d("WebSocket", "Joined chat $chatId")
        } else {
            Log.e("WebSocket", "Cannot join chat $chatId: WebSocket not connected")
        }
    }

    fun leaveChat(chatId: Long) {
        if (isConnected()) {
            hubConnection?.send("LeaveChat", chatId.toString())
            Log.d("WebSocket", "Left chat $chatId")
        }
    }

    fun sendMessage(chatId: Long, messageText: String) {
        if (isConnected()) {
            hubConnection?.send("SendMessage", chatId, messageText)
            Log.d("WebSocket", "Sent message to chat $chatId: $messageText")
        } else {
            Log.e("WebSocket", "Cannot send message to chat $chatId: WebSocket not connected")
        }
    }

    fun notifyTyping(chatId: Long) {
        if (isConnected()) {
            hubConnection?.send("UserTyping", chatId.toString())
            Log.d("WebSocket", "Notified typing in chat $chatId")
        }
    }

    fun notifyStoppedTyping(chatId: Long) {
        if (isConnected()) {
            hubConnection?.send("UserStoppedTyping", chatId.toString())
            Log.d("WebSocket", "Notified stopped typing in chat $chatId")
        }
    }

    fun markMessagesAsRead(chatId: Long) {
        if (isConnected()) {
            hubConnection?.send("MarkMessagesAsRead", chatId)
            Log.d("WebSocket", "Marked messages as read in chat $chatId")
        }
    }
}

interface WebSocketListener {
    fun onConnected()
    fun onDisconnected()
    fun onMessageReceived(message: MessageDto)
    fun onUserStatusChanged(userId: Long, isOnline: Boolean)
    fun onUserTyping(userId: Long, chatId: String)
    fun onUserStoppedTyping(userId: Long, chatId: String)
    fun onMessagesRead(userId: Long, chatId: Long)
}

data class MessageDto(
    @SerializedName("id") val id: Long,
    @SerializedName("senderId") val senderId: Long,
    @SerializedName("senderName") val senderName: String,
    @SerializedName("chatId") val chatId: Long,
    @SerializedName("content") val content: String,
    @SerializedName("sentAt") val sentAt: String
)