package com.example.mydiplom.ui.api

import android.content.Context
import com.example.mydiplom.ui.api.ApiClient.apiService
import com.example.mydiplom.ui.chat.Chat
import com.example.mydiplom.ui.message.Message
import com.example.mydiplom.ui.message.MessageDTO
import com.example.mydiplom.ui.tasks.Task
import com.example.mydiplom.ui.tasks.TaskDto
import com.example.mydiplom.ui.tokenManager.TokenManager
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.security.MessageDigest


interface ApiService {
    @POST("auth/login")
    fun login(
        @Body request: AuthRequest
    ): Call<AuthResponse>

    @GET("Chat/GetChats")
    fun getChats(
        @Header("Authorization") token: String
    ): Call<ChatListResponse>

    @GET("Tasks/GetTasks")
    fun getTasks(
        @Header("Authorization") token: String
    ): Call<TaskListResponse>

    @POST("Tasks/create-task")
    fun createTask(
        @Header("Authorization") token: String,
        @Body taskDto: TaskDto
    ): Call<CreateTaskResponse>

    @GET("Chat/{chatId}/messages")
    fun getMessages(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: Long
    ): Call<MessageListResponse>

    @POST("create-group")
    fun createGroupChat(
        @Header("Authorization") token: String,
        @Body request: CreateGroupChatDto
    ): Response<CreateChatResponse>

    @POST("Chat/{chatId}/send")
    fun sendMessage(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: Long,
        @Body messageDTO: MessageDTO
    ): Call<Unit>

    @PUT("Tasks/{taskId}/start")
    fun startTask(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: Long
    ): Call<Unit>

    @PUT("Tasks/{taskId}/submit")
    fun submitTask(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: Long
    ):Call<Unit>



    @GET("Chat/search")
    fun searchChats(
        @Header("Authorization") token: String,
        @Query("query") query: String
    ): Call<ChatListResponse>

    @GET("notification/user")
    fun getUserNotifications(
        @Header("Authorization") token: String
    ): Call<NotificationListResponse>

    @POST("notification/mark-read/{notificationId}")
    fun markNotificationAsRead(
        @Header("Authorization") token: String,
        @Path("notificationId") notificationId: Long
    ): Call<Unit>

    @GET("User/GetUsers")
    fun getUsers(
        @Header("Authorization") token: String
    ): Call<UsersListResponse> // Изменяем возвращаемый тип

    @PUT("Tasks/{taskId}/complete")
    fun completeTask(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: Long
    ):Call<Unit>

    @PUT("Tasks/{taskId}/return")
    fun returnTask(
        @Header("Authorization") token: String,
        @Path("taskId") taskId: Long
    ):Call<Unit>

}

data class CreateTaskResponse(
    @SerializedName("\$message")
    val message: String,
    @SerializedName("\$id")
    val id: Int
)

data class NotificationDto(
    @SerializedName("\$id")
    val id: Long?,
    val title: String?,
    val message: String?,
    val isRead: Boolean?,
    val sentAt: String?
)

data class NotificationListResponse(
    @SerializedName("\$id")
    val id: String,
    @SerializedName("\$values")
    val values: List<NotificationDto>
)

data class MessageListResponse(
    @SerializedName("\$id")
    val id: Int,

    @SerializedName("\$values")
    val values: List<Message>
)

data class UsersListResponse(
    @SerializedName("\$values")
    val values: List<UserResponse>
)

data class UserResponse(
    @SerializedName("\$id")
    val id: Long,
    @SerializedName("\$full_Name")
    val fullName: String?
)

data class CreateGroupChatDto(
    val name: String,
    val userIds: List<Long>
)



data class CreateChatResponse(
    val id: Long,
    val name: String,
    val participantsCount: Int
)

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    val token: String? = null
)

data class ChatListResponse(
    @SerializedName("\$id")
    val id: String,

    @SerializedName("\$values")
    val values: List<Chat>
)

data class TaskListResponse(
    @SerializedName("\$values")
    val values: List<Task>
)

object ApiClient {
    private const val BASE_URL = "https://corptest123.loca.lt/api/"

    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()

    private val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(httpLoggingInterceptor)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

class ApiRepository(context: Context) {
    val tokenManager = TokenManager(context)
    var token: String?
        get() = tokenManager.getToken()
        private set(value) {
            value?.let { tokenManager.saveToken(it) }
                ?: tokenManager.clearToken()
        }

    // Функция для хеширования строки с помощью SHA-256
    private fun sha256(input: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(input.toByteArray())
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun fetchNotifications(
        onSuccess: (List<NotificationDto>) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentToken = token ?: run {
            onError("Требуется авторизация")
            return
        }

        ApiClient.apiService.getUserNotifications("Bearer $currentToken").enqueue(object : Callback<NotificationListResponse> {
            override fun onResponse(call: Call<NotificationListResponse>, response: Response<NotificationListResponse>) {
                when {
                    response.isSuccessful -> {
                        response.body()?.let { notificationListResponse ->
                            if (notificationListResponse.values.isEmpty()) {
                                onError("У вас пока нет уведомлений")
                            } else {
                                onSuccess(notificationListResponse.values)
                            }
                        } ?: onError("Пустой ответ от сервера")
                    }
                    response.code() == 401 -> onError("Требуется авторизация")
                    else -> onError("Ошибка сервера: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<NotificationListResponse>, t: Throwable) {
                onError("Ошибка сети: ${t.message ?: "Неизвестная ошибка"}")
            }
        })
    }

    fun markNotificationAsRead(
        notificationId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentToken = token ?: run {
            onError("Требуется авторизация")
            return
        }

        ApiClient.apiService.markNotificationAsRead("Bearer $currentToken", notificationId).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                when {
                    response.isSuccessful -> onSuccess()
                    response.code() == 401 -> onError("Требуется авторизация")
                    response.code() == 404 -> onError("Уведомление не найдено")
                    else -> onError("Ошибка сервера: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                onError("Ошибка сети: ${t.message ?: "Неизвестная ошибка"}")
            }
        })
    }

    fun login(
        email: String,
        password: String,
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val hashedEmail = email
        val hashedPassword = password

        val request = AuthRequest(hashedEmail, hashedPassword)

        ApiClient.apiService.login(request).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse?.success == true) {
                        onSuccess(true)
                        token = authResponse.token
                    } else {
                        onError(authResponse?.message ?: "Ошибка авторизации")
                    }
                } else {
                    onError("Ошибка сервера: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                onError("Ошибка сети: ${t.message}")
            }
        })
    }


    fun fetchChats(
        onSuccess: (List<Chat>) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentToken = token ?: run {
            onError("Требуется авторизация")
            return
        }

        ApiClient.apiService.getChats("Bearer $currentToken").enqueue(object : Callback<ChatListResponse> {
            override fun onResponse(call: Call<ChatListResponse>, response: Response<ChatListResponse>) {
                when {
                    response.isSuccessful -> {
                        response.body()?.let { chatListResponse ->
                            if (chatListResponse.values.isEmpty()) {
                                onError("У вас пока нет чатов")
                            } else {
                                onSuccess(chatListResponse.values)
                            }
                        } ?: onError("Пустой ответ от сервера")
                    }
                    response.code() == 401 -> onError("Требуется авторизация")
                    else -> onError("Ошибка сервера: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ChatListResponse>, t: Throwable) {
                onError("Ошибка сети: ${t.message ?: "Неизвестная ошибка"}")
            }
        })
    }





    fun fetchTasks(
        onSuccess: (List<Task>) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentToken = token ?: run {
            onError("Требуется авторизация")
            return
        }

        ApiClient.apiService.getTasks("Bearer $currentToken").enqueue(object : Callback<TaskListResponse> {
            override fun onResponse(call: Call<TaskListResponse>, response: Response<TaskListResponse>) {
                when {
                    response.isSuccessful -> {
                        response.body()?.let { TaskListResponse ->
                            if (TaskListResponse.values.isEmpty()) {
                                onError("У вас пока нет чатов")
                            } else {
                                onSuccess(TaskListResponse.values)
                            }
                        } ?: onError("Пустой ответ от сервера")
                    }
                    response.code() == 401 -> onError("Требуется авторизация")
                    else -> onError("Ошибка сервера: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<TaskListResponse>, t: Throwable) {
                onError("Ошибка сети: ${t.message ?: "Неизвестная ошибка"}")
            }
        })
    }

    fun createTask(
        name: String,
        description: String,
        completionTime: Int,
        userIds: List<Long> = emptyList(),
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentToken = token ?: run {
            onError("Требуется авторизация")
            return
        }

        val taskDto = TaskDto(
            name = name,
            description = description,
            completionTime = completionTime,
            userIds = userIds
        )

        ApiClient.apiService.createTask("Bearer $currentToken", taskDto).enqueue(object : Callback<CreateTaskResponse> {
                override fun onResponse(
                    call: Call<CreateTaskResponse>,
                    response: Response<CreateTaskResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            onSuccess(it.id)
                        } ?: onError("Пустой ответ от сервера")
                    } else {
                        onError("Ошибка сервера: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<CreateTaskResponse>, t: Throwable) {
                    onError("Ошибка сети: ${t.message}")
                }
            })
    }

    fun fetchMessages(
        chatId: Long,
        onSuccess: (List<Message>) -> Unit,
        onError: (String) -> Unit
    ){
        val currentToken = token ?: run {
            onError("Требуется авторизация")
            return
        }

        ApiClient.apiService.getMessages("Bearer $currentToken",chatId).enqueue(object : Callback<MessageListResponse> {
            override fun onResponse(call: Call<MessageListResponse>, response: Response<MessageListResponse>) {
                when {
                    response.isSuccessful -> {
                        response.body()?.let { MessageListResponse ->
                            if (MessageListResponse.values.isEmpty()) {
                                onError("У вас пока нет сообщений")
                            } else {
                                onSuccess(MessageListResponse.values)
                            }
                        } ?: onError("Пустой ответ от сервера")
                    }
                    response.code() == 401 -> onError("Требуется авторизация")
                    else -> onError("Ошибка сервера: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<MessageListResponse>, t: Throwable) {
                onError("Ошибка сети: ${t.message ?: "Неизвестная ошибка"}")
            }
        })
    }


    fun TaskStart(
        taskId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        val currentToken = token ?: run {
            onError("Требуется авторизация")
            return
        }

        ApiClient.apiService.startTask("Bearer $currentToken",taskId).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                when {
                    response.isSuccessful -> onSuccess()
                    response.code() == 401 -> onError("Требуется авторизация")
                    response.code() == 403 -> onError("Вы не назначены на эту задачу")
                    response.code() == 400 -> onError("Невозможно начать задачу")
                    response.code() == 404 -> onError("Задача не найдена")
                    else -> onError("Ошибка сервера: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                onError("Ошибка сети: ${t.message ?: "Неизвестная ошибка"}")
            }
        })
    }

    fun TaskSubmit(
        taskId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentToken = token ?: run {
            onError("Требуется авторизация")
            return
        }

        ApiClient.apiService.submitTask("Bearer $currentToken", taskId).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                when {
                    response.isSuccessful -> onSuccess()
                    response.code() == 401 -> onError("Требуется авторизация")
                    response.code() == 403 -> onError("Вы не назначены на эту задачу")
                    response.code() == 400 -> onError("Невозможно отправить задачу на проверку")
                    response.code() == 404 -> onError("Задача не найдена")
                    else -> onError("Ошибка сервера: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                onError("Ошибка сети: ${t.message ?: "Неизвестная ошибка"}")
            }
        })
    }

    fun TaskComplete(
        taskId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentToken = token ?: run {
            onError("Требуется авторизация")
            return
        }

        ApiClient.apiService.completeTask("Bearer $currentToken", taskId).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                when {
                    response.isSuccessful -> onSuccess()
                    response.code() == 401 -> onError("Требуется авторизация")
                    response.code() == 403 -> onError("Вы не можете завершить эту задачу")
                    response.code() == 400 -> onError("Невозможно завершить задачу")
                    response.code() == 404 -> onError("Задача не найдена")
                    else -> onError("Ошибка сервера: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                onError("Ошибка сети: ${t.message ?: "Неизвестная ошибка"}")
            }
        })
    }

    fun TaskReturn(
        taskId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentToken = token ?: run {
            onError("Требуется авторизация")
            return
        }

        ApiClient.apiService.returnTask("Bearer $currentToken", taskId).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                when {
                    response.isSuccessful -> onSuccess()
                    response.code() == 401 -> onError("Требуется авторизация")
                    response.code() == 403 -> onError("Вы не можете вернуть эту задачу")
                    response.code() == 400 -> onError("Невозможно вернуть задачу")
                    response.code() == 404 -> onError("Задача не найдена")
                    else -> onError("Ошибка сервера: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                onError("Ошибка сети: ${t.message ?: "Неизвестная ошибка"}")
            }
        })
    }

    suspend fun createGroupChat(name: String, userIds: List<Long>): Response<CreateChatResponse> {
        val currentToken = tokenManager.getToken() ?: throw Exception("Требуется авторизация")
        val request = CreateGroupChatDto(name, userIds)
        return apiService.createGroupChat("Bearer $currentToken", request)
    }


    fun searchChats(
        query: String,
        onSuccess: (List<Chat>) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentToken = token ?: run {
            onError("Требуется авторизация")
            return
        }

        ApiClient.apiService.searchChats("Bearer $currentToken", query)
            .enqueue(object : Callback<ChatListResponse> {
                override fun onResponse(call: Call<ChatListResponse>, response: Response<ChatListResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.let { chatListResponse ->
                            onSuccess(chatListResponse.values)
                        } ?: onError("Пустой ответ от сервера")
                    } else {
                        onError("Ошибка сервера: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ChatListResponse>, t: Throwable) {
                    onError("Ошибка сети: ${t.message}")
                }
            })
    }

    fun getUsers(
        onSuccess: (List<UserResponse>) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentToken = token ?: run {
            onError("Требуется авторизация")
            return
        }

        ApiClient.apiService.getUsers("Bearer $currentToken")
            .enqueue(object : Callback<UsersListResponse> {
                override fun onResponse(
                    call: Call<UsersListResponse>,
                    response: Response<UsersListResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { usersResponse ->
                            onSuccess(usersResponse.values)
                        } ?: onError("Пустой ответ от сервера")
                    } else {
                        onError("Ошибка сервера: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<UsersListResponse>, t: Throwable) {
                    onError("Ошибка сети: ${t.message}")
                }
            })
    }

}
