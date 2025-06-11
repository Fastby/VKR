package com.example.mydiplom.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.mydiplom.ui.api.ApiRepository
import com.example.mydiplom.ui.api.NotificationDto
import com.example.mydiplom.ui.api.UserResponse
import com.example.mydiplom.ui.chat.Chat
import com.example.mydiplom.ui.message.Message
import com.example.mydiplom.ui.navigation.Routes
import com.example.mydiplom.ui.tasks.Task
import com.example.mydiplom.ui.websocket.MessageDto
import com.example.mydiplom.ui.websocket.WebSocketListener
import com.example.mydiplom.ui.websocket.WebSocketManager
import com.google.gson.Gson
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: ApiRepository) : ViewModel() {

    val chats = mutableStateOf<List<Chat>>(emptyList())
    val tasks = mutableStateOf<List<Task>>(emptyList())
    val foundChats = mutableStateOf<List<Chat>>(emptyList())
    val messages = mutableStateOf<List<Message>>(emptyList())
    val mailings = mutableStateOf<List<NotificationDto>>(emptyList())
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val userOnlineStatus = mutableStateOf<Map<Long, Boolean>>(emptyMap())
    val typingUsers = mutableStateOf<Set<Long>>(emptySet())
    val users = mutableStateOf<List<UserResponse>>(emptyList())
    var usersLoading =  mutableStateOf(false)
    var usersError = mutableStateOf<String?>(null)
    // Состояние для создания чата
    var showCreateChatDialog by mutableStateOf(false)
    var newChatName by mutableStateOf("")
    var selectedUsers by mutableStateOf<Set<Long>>(emptySet())
    var createChatError by mutableStateOf<String?>(null)


    private lateinit var webSocketManager: WebSocketManager
    private var currentChatId: Long = 0
    private var isWebSocketInitialized = false // Флаг для предотвращения повторной инициализации

    /*------------------------ API - ФУНКЦИИ -----------------------------*/

    fun login(
        email: String,
        password: String,
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        repository.login(
            email = email,
            password = password,
            onSuccess = { success ->
                onSuccess(success)
                if (success) {
                    loadChats()
                }
            },
            onError = onError
        )
    }

    fun loadChats() {
        if (repository.token == null) {
            errorMessage.value = "Требуется авторизация"
            return
        }

        isLoading.value = true
        errorMessage.value = null

        repository.fetchChats(
            onSuccess = { loadedChats ->
                chats.value = loadedChats
                isLoading.value = false
            },
            onError = { error ->
                errorMessage.value = error
                isLoading.value = false
            }
        )
    }

    fun loadTasks() {
        if (repository.token == null) {
            errorMessage.value = "Требуется авторизация"
            return
        }

        isLoading.value = true
        errorMessage.value = null

        repository.fetchTasks(
            onSuccess = { loadedTasks ->
                tasks.value = loadedTasks
                isLoading.value = false
            },
            onError = { error ->
                errorMessage.value = error
                isLoading.value = false
            }
        )
    }

    fun createTask(
        name: String,
        description: String,
        completionTime: Int,
        userIds: List<Long>,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        if (repository.token == null) {
            onError("Требуется авторизация")
            return
        }

        isLoading.value = true
        errorMessage.value = null

        repository.createTask(
            name = name,
            description = description,
            completionTime = completionTime,
            userIds = userIds,
            onSuccess = { taskId ->
                isLoading.value = false
                onSuccess(taskId)
                loadTasks()
            },
            onError = { error ->
                isLoading.value = false
                errorMessage.value = error
                onError(error)
            }
        )
    }

    fun getMessages(chatId: Long) {
        if (repository.token == null) {
            errorMessage.value = "Требуется авторизация"
            return
        }
        isLoading.value = true
        errorMessage.value = null

        repository.fetchMessages(
            chatId = chatId,
            onSuccess = { loadedMessages ->
                messages.value = loadedMessages
                isLoading.value = false
                webSocketManager.markMessagesAsRead(chatId.toLong())
            },
            onError = { error ->
                errorMessage.value = error
                isLoading.value = false
            }
        )
    }

    fun searchChats(
        query: String,
        onSuccess: (List<Chat>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (repository.token == null) {
            onError("Требуется авторизация")
            return
        }

        isLoading.value = true
        errorMessage.value = null

        repository.searchChats(
            query = query,
            onSuccess = { foundChatList ->
                isLoading.value = false
                onSuccess(foundChatList)
            },
            onError = { error ->
                isLoading.value = false
                errorMessage.value = error
                onError(error)
            }
        )
    }

    fun taskStart(
        taskId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (repository.token == null) {
            errorMessage.value = "Требуется авторизация"
            return
        }

        isLoading.value = true
        errorMessage.value = null

        repository.TaskStart(
            taskId = taskId,
            onSuccess = {
                isLoading.value = false
                onSuccess()
                loadTasks() // Обновляем список задач после изменения статуса
            },
            onError = { error ->
                isLoading.value = false
                errorMessage.value = error
                onError(error)
            }
        )
    }

    fun taskSubmit(
        taskId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (repository.token == null) {
            errorMessage.value = "Требуется авторизация"
            return
        }

        isLoading.value = true
        errorMessage.value = null

        repository.TaskSubmit(
            taskId = taskId,
            onSuccess = {
                isLoading.value = false
                onSuccess()
                loadTasks()
            },
            onError = { error ->
                isLoading.value = false
                errorMessage.value = error
                onError(error)
            }
        )
    }

    fun getUserRole(): String? {
        return repository.tokenManager.getCurrentUserRole()
    }

    fun getUserName(): String? {
        return repository.tokenManager.getCurrentUserName()
    }

    fun taskComplete(
        taskId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (repository.token == null) {
            errorMessage.value = "Требуется авторизация"
            return
        }

        isLoading.value = true
        errorMessage.value = null

        repository.TaskComplete(
            taskId = taskId,
            onSuccess = {
                isLoading.value = false
                onSuccess()
                loadTasks()
            },
            onError = { error ->
                isLoading.value = false
                errorMessage.value = error
                onError(error)
            }
        )
    }

    fun taskReturn(
        taskId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (repository.token == null) {
            errorMessage.value = "Требуется авторизация"
            return
        }

        isLoading.value = true
        errorMessage.value = null

        repository.TaskReturn(
            taskId = taskId,
            onSuccess = {
                isLoading.value = false
                onSuccess()
                loadTasks()
            },
            onError = { error ->
                isLoading.value = false
                errorMessage.value = error
                onError(error)
            }
        )
    }

    fun sendMessage(chatId: Long, messageText: String) {
        val currentUserId = repository.tokenManager.getCurrentUserId() ?: run {
            errorMessage.value = "Не удалось определить отправителя"
            return
        }

        if (messageText.isBlank()) {
            errorMessage.value = "Сообщение не может быть пустым"
            return
        }

        // Отправляем через WebSocket
        webSocketManager.sendMessage(chatId, messageText)

        // Оптимистично обновляем UI
        val newMessage = Message(
            content = messageText,
            senderId = currentUserId,
            chatId = chatId,
            senderName = "You"
        )
        messages.value = messages.value.toMutableList().apply { add(newMessage) }
    }

    fun initWebSocket(context: Context) {
        if (isWebSocketInitialized) {
            Log.d("WebSocket", "WebSocket already initialized")
            return
        }

        webSocketManager = WebSocketManager(context)
        isWebSocketInitialized = true

        webSocketManager.addListener(object : WebSocketListener {
            override fun onConnected() {
                Log.d("WebSocket", "Connected to WebSocket")
                if (currentChatId != 0L) {
                    webSocketManager.joinChat(currentChatId)
                }
            }

            override fun onDisconnected() {
                Log.d("WebSocket", "WebSocket disconnected")
                errorMessage.value = "WebSocket disconnected"
                isWebSocketInitialized = false
            }

            override fun onMessageReceived(message: MessageDto) {
                Log.d("WebSocket", "Message received: $message, currentChatId: $currentChatId")
                if (message.chatId == currentChatId && message.senderId != repository.tokenManager.getCurrentUserId()) {
                    val newMessage = Message(
                        content = message.content,
                        senderId = message.senderId,
                        chatId = message.chatId,
                        senderName = message.senderName?: "Аноним"
                    )
                    messages.value = messages.value.toMutableList().apply { add(newMessage) }
                    Log.d("WebSocket", "Messages updated: ${messages.value.size}")
                }
            }

            override fun onUserStatusChanged(userId: Long, isOnline: Boolean) {
                Log.d("WebSocket", "User $userId isOnline: $isOnline")
                userOnlineStatus.value = userOnlineStatus.value.toMutableMap().apply {
                    put(userId, isOnline)
                }
            }

            override fun onUserTyping(userId: Long, chatId: String) {
                Log.d("WebSocket", "User $userId typing in chat $chatId")
                if (chatId.toLong() == currentChatId) {
                    typingUsers.value = typingUsers.value + userId
                }
            }

            override fun onUserStoppedTyping(userId: Long, chatId: String) {
                Log.d("WebSocket", "User $userId stopped typing in chat $chatId")
                if (chatId.toLong() == currentChatId) {
                    typingUsers.value = typingUsers.value - userId
                }
            }

            override fun onMessagesRead(userId: Long, chatId: Long) {
                Log.d("WebSocket", "Messages read by user $userId in chat $chatId")
                if (chatId == currentChatId) {
                    // Обновить UI, если нужно
                }
            }
        })

        webSocketManager.startConnection { error ->
            Log.e("WebSocket", "Connection error: $error")
            errorMessage.value = error
            isWebSocketInitialized = false
        }
    }

    fun setCurrentChatId(chatId: Long) {
        currentChatId = chatId
    }

    fun joinChat(chatId: Long) {
        if (webSocketManager.isConnected()) {
            webSocketManager.joinChat(chatId)
        }
    }

    fun leaveChat(chatId: Long) {
        if (webSocketManager.isConnected()) {
            webSocketManager.leaveChat(chatId)
        }
    }

    fun loadUsers() {
        if (repository.token == null) {
            errorMessage.value = "Требуется авторизация"
            return
        }

        isLoading.value = true
        repository.getUsers(
            onSuccess = { loadedUsers ->
                users.value = loadedUsers
                isLoading.value = false
            },
            onError = { error ->
                errorMessage.value = error
                isLoading.value = false
            }
        )
    }


    fun createGroupChat(onSuccess: (Long) -> Unit) {
        if (newChatName.isBlank()) {
            createChatError = "Название группы не может быть пустым"
            return
        }

        if (selectedUsers.isEmpty()) {
            createChatError = "Выберите хотя бы одного участника"
            return
        }

        viewModelScope.launch {
            try {
                val response = repository.createGroupChat(newChatName, selectedUsers.toList())
                if (response.isSuccessful) {
                    response.body()?.let {
                        onSuccess(it.id)
                        showCreateChatDialog = false
                        newChatName = ""
                        selectedUsers = emptySet()
                        loadChats() // Обновляем список чатов
                    }
                } else {
                    createChatError = response.errorBody()?.string() ?: "Ошибка создания чата"
                }
            } catch (e: Exception) {
                createChatError = e.message ?: "Ошибка сети"
            }
        }
    }

    fun loadMailings() {
        if (repository.token == null) {
            errorMessage.value = "Требуется авторизация"
            return
        }

        isLoading.value = true
        errorMessage.value = null

        repository.fetchNotifications(
            onSuccess = { loadedMailings ->
                mailings.value = loadedMailings
                isLoading.value = false
            },
            onError = { error ->
                errorMessage.value = error
                isLoading.value = false
            }
        )
    }

    fun markMailingAsRead(mailingId: Long) {
        repository.markNotificationAsRead(
            notificationId = mailingId,
            onSuccess = {},
            onError = { error ->
                errorMessage.value = "Ошибка пометки рассылки: $error"
                Log.e("AuthViewModel", "Mailing mark error: $error")
            }
        )
    }

    /*---------------------- COMPOSE - ФУНКЦИИ ----------------------*/

    @Composable
    fun ChatList(navHostController: NavHostController) {
        val chats = chats.value
        val isLoading = isLoading.value
        val errorMessage = errorMessage.value

        LaunchedEffect(Unit) {
            loadChats()
            loadUsers() // Загружаем пользователей для выбора в диалоге
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Добавляем кнопку создания чата
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { showCreateChatDialog = true },
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать чат")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Создать чат")
                }
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                !errorMessage.isNullOrEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = errorMessage, color = Color.Red)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(chats) { chat ->
                            ChatItem(chat, navHostController)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Диалог создания чата
            if (showCreateChatDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showCreateChatDialog = false
                        createChatError = null
                    },
                    title = { Text("Создать новый чат") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newChatName,
                                onValueChange = { newChatName = it },
                                label = { Text("Название чата*") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = newChatName.isBlank()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Выберите участников:", style = MaterialTheme.typography.labelMedium)

                            UserSelectionList(
                                users = users.value,
                                loading = usersLoading.value,
                                error = usersError.value,
                                selectedUsers = selectedUsers,
                                onUserSelected = { userId ->
                                    selectedUsers = if (selectedUsers.contains(userId)) {
                                        selectedUsers - userId
                                    } else {
                                        selectedUsers + userId
                                    }
                                },
                                onRetry = { loadUsers() }
                            )

                            createChatError?.let { error ->
                                Text(error, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                createGroupChat { chatId ->
                                    loadChats()
                                }
                            },
                            enabled = newChatName.isNotBlank() && selectedUsers.isNotEmpty()
                        ) {
                            Text("Создать")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showCreateChatDialog = false
                                createChatError = null
                            }
                        ) {
                            Text("Отмена")
                        }
                    }
                )
            }
        }
    }


    @Composable
    fun MailingsScreen(navHostController: NavHostController) {
        val isLoading = isLoading.value
        val errorMessage = errorMessage.value

        LaunchedEffect(Unit) {
            loadMailings()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (!errorMessage.isNullOrEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = errorMessage, color = Color.Red)
                    }
                }
            } else if (mailings.value.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "У вас пока нет уведомлений", color = Color.Gray)
                    }
                }
            } else {
                items(mailings.value) { mail ->
                    MailItem(mail, navHostController)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    @Composable
    fun MailItem(mail: NotificationDto, navHostController: NavHostController) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    mail.id?.let { markMailingAsRead(it) }
                    val jsonMail = Gson().toJson(mail)
                    navHostController.navigate(Routes.Mailings.route.replace("{mail}", jsonMail))
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Рассылка",
                modifier = Modifier.size(48.dp),
                tint = if (mail.isRead == true) Color.Gray else MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = mail.title ?: "Без заголовка",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (mail.isRead == true) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = mail.sentAt ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MailScreen(mail: NotificationDto, navHostController: NavHostController) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Детали уведомления") },
                    navigationIcon = {
                        IconButton(onClick = { navHostController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Назад"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    text = mail.title ?: "Без заголовка",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mail.sentAt ?: "Дата неизвестна",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mail.message ?: "Сообщение отсутствует",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    @Composable
    fun TasksScreen(navHostController: NavHostController) {
        val tasks = tasks.value
        val isLoading = isLoading.value
        val errorMessage = errorMessage.value
        var users = users.value
        var showCreateDialog by remember { mutableStateOf(false) }
        var taskName by remember { mutableStateOf("") }
        var taskDescription by remember { mutableStateOf("") }
        var completionTime by remember { mutableStateOf("") }
        var selectedUsers by remember { mutableStateOf<Set<Long>>(emptySet()) }
        var usersLoading by remember { mutableStateOf(false) }
        var usersError by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            loadTasks()
            usersLoading = true
            repository.getUsers(
                onSuccess = { loadedUsers ->
                    users = loadedUsers
                    usersLoading = false
                },
                onError = { error ->
                    usersError = error
                    usersLoading = false
                    Log.e("TasksScreen", "Error loading users: $error")
                }
            )
        }


        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (!errorMessage.isNullOrEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = errorMessage, color = Color.Red)
                    }
                }
            } else {
                items(tasks) { task ->
                    TaskItem(task, navHostController)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(50.dp),
                        onClick = { showCreateDialog = true }
                    ) {
                        Text("Создать задачу")
                    }
                }
            }
        }


        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Новая задача") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = taskName,
                            onValueChange = { taskName = it },
                            label = { Text("Название*") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = taskName.isBlank()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = taskDescription,
                            onValueChange = { taskDescription = it },
                            label = { Text("Описание") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = completionTime,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() }) {
                                    completionTime = it
                                }
                            },
                            label = { Text("Время на выполнение (часы)*") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = completionTime.isBlank()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Назначить пользователей:", style = MaterialTheme.typography.labelMedium)

                        // Список пользователей
                        UserSelectionList(
                            users = users,
                            loading = usersLoading,
                            error = usersError,
                            selectedUsers = selectedUsers,
                            onUserSelected = { userId ->
                                selectedUsers = if (selectedUsers.contains(userId)) {
                                    selectedUsers - userId
                                } else {
                                    selectedUsers + userId
                                }
                            },
                            onRetry = {
                                usersLoading = true
                                usersError = null
                                repository.getUsers(
                                    onSuccess = { loadedUsers ->
                                        users = loadedUsers
                                        usersLoading = false
                                    },
                                    onError = { error ->
                                        usersError = error
                                        usersLoading = false
                                    }
                                )
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (taskName.isNotBlank() && completionTime.isNotBlank()) {
                                createTask(
                                    name = taskName,
                                    description = taskDescription,
                                    completionTime = completionTime.toIntOrNull() ?: 0,
                                    userIds = selectedUsers.toList(),
                                    onSuccess = {
                                        showCreateDialog = false
                                        // Сброс полей
                                        taskName = ""
                                        taskDescription = ""
                                        completionTime = ""
                                        selectedUsers = emptySet()
                                        // Обновление списка задач
                                        loadTasks()
                                    },
                                    onError = { error ->
                                        usersError = "Ошибка создания задачи: $error"
                                    }
                                )
                            }
                        },
                        enabled = taskName.isNotBlank() && completionTime.isNotBlank()
                    ) {
                        Text("Создать")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCreateDialog = false }
                    ) {
                        Text("Отмена")
                    }
                }
            )
        }
    }

    @Composable
    private fun UserSelectionList(
        users: List<UserResponse>,
        loading: Boolean,
        error: String?,
        selectedUsers: Set<Long>,
        onUserSelected: (Long) -> Unit,
        onRetry: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
        ) {
            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRetry) {
                            Text("Повторить")
                        }
                    }
                }
                users.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Нет доступных пользователей", color = MaterialTheme.colorScheme.outline)
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(users) { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUserSelected(user.id) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Checkbox(
                                    checked = selectedUsers.contains(user.id),
                                    onCheckedChange = { onUserSelected(user.id) }
                                )
                                Text(
                                    text = user.fullName?: "Тест${user.id}",
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Divider(modifier = Modifier.padding(start = 56.dp))
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ChatScreen(chat: Chat, navHostController: NavHostController) {
        val context = LocalContext.current

        LaunchedEffect(chat.id) {
            setCurrentChatId(chat.id)
            if (!isWebSocketInitialized) {
                initWebSocket(context)
            }
            joinChat(chat.id)
            getMessages(chat.id)
        }

        DisposableEffect(chat.id) {
            onDispose {
                leaveChat(chat.id)
            }
        }

        var messageText by remember { mutableStateOf("") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(chat.name ?: "Чат")
                            val isOnline = userOnlineStatus.value[chat.id] ?: false
                            Text(
                                text = if (isOnline) "Online" else "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnline) Color.Green else Color.Gray
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navHostController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            },
            bottomBar = {
                MessageInputBar(
                    messageText = messageText,
                    onMessageChange = { newText ->
                        messageText = newText
                        if (newText.isNotBlank()) {
                            webSocketManager.notifyTyping(chat.id.toLong())
                        } else {
                            webSocketManager.notifyStoppedTyping(chat.id.toLong())
                        }
                    },
                    onSendMessage = {
                        sendMessage(chat.id, messageText)
                        messageText = ""
                        webSocketManager.notifyStoppedTyping(chat.id.toLong())
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when {
                    isLoading.value -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    !errorMessage.value.isNullOrEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = errorMessage.value!!, color = Color.Red)
                        }
                    }
                    else -> {
                        Column {
                            MessagesList(
                                messages = messages.value,
                                modifier = Modifier.weight(1f)
                            )
                            if (typingUsers.value.isNotEmpty()) {
                                Text(
                                    text = "Typing: ${typingUsers.value.joinToString()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MessagesList(messages: List<Message>, modifier: Modifier = Modifier) {
        val listState = rememberLazyListState()

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        LazyColumn(
            state = listState,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isMyMessage = (message.senderId == repository.tokenManager.getCurrentUserId())
                )
            }
        }
    }

    @Composable
    fun MessageBubble(message: Message, isMyMessage: Boolean) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            contentAlignment = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMyMessage)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = message.senderName ?: "Никита",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMyMessage)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = message.content ?: "Пустое сообщение",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMyMessage)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    @Composable
    fun MessageInputBar(
        messageText: String,
        onMessageChange: (String) -> Unit,
        onSendMessage: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageChange,
                placeholder = { Text("Введите сообщение...") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendMessage() }),
                singleLine = false,
                maxLines = 3
            )

            IconButton(
                onClick = onSendMessage,
                enabled = messageText.isNotBlank(),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Отправить",
                    tint = if (messageText.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }

    @Composable
    fun ChatItem(chat: Chat, navHostController: NavHostController) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val jsonTask = Gson().toJson(chat)
                    navHostController.navigate(Routes.ChatScreen.route.replace("{chat}", jsonTask))
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Чат",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = chat.name ?: "Без названия",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = chat.lastMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = chat.time ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                if (chat.unreadCount > 0) {
                    Text(
                        text = chat.unreadCount.toString(),
                        color = Color.White,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TaskScreen(task: Task, navHostController: NavHostController) {
        val currentRole = repository.tokenManager.getCurrentUserRole()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Детали задачи")
                    },
                    navigationIcon = {
                        IconButton(onClick = { navHostController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Время на выполнение: ${task.completionTime} hours",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.completionTime >= 24) Color.Green else Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Статус: " + when (task.status) {
                        0 -> "не начато"
                        1 -> "в процессе выполнения"
                        2 -> "результат на рассмотрении"
                        3 -> "завершено успешно"
                        4 -> "провалено"
                        else -> "неизвестный статус"
                    }
                )

                if (currentRole != null) {
                    if (task.status == 2 && currentRole == "Manager") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                modifier = Modifier
                                    .widthIn(min = 200.dp)
                                    .padding(vertical = 8.dp),
                                onClick = {
                                    taskComplete(
                                        task.id,
                                        onSuccess = { loadTasks() },
                                        onError = { error ->
                                            Log.e("TaskScreen", "Error completing task: $error")
                                        }
                                    )
                                }
                            ) {
                                Text("Завершить")
                            }

                            Button(
                                modifier = Modifier
                                    .widthIn(min = 200.dp)
                                    .padding(vertical = 8.dp),
                                onClick = {
                                    taskReturn(
                                        task.id,
                                        onSuccess = { loadTasks() },
                                        onError = { error ->
                                            Log.e("TaskScreen", "Error returning task: $error")
                                        }
                                    )
                                }
                            ) {
                                Text("Вернуть на доработку")
                            }
                        }
                    } else if (task.status < 2 && currentRole != "Manager") {
                        Button(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .widthIn(min = 200.dp)
                                .padding(top = 16.dp),
                            onClick = {
                                when (task.status) {
                                    0 -> taskStart(
                                        task.id,
                                        onSuccess = { loadTasks() },
                                        onError = { error ->
                                            Log.e("TaskScreen", "Error starting task: $error")
                                        }
                                    )
                                    1 -> taskSubmit(
                                        task.id,
                                        onSuccess = { loadTasks() },
                                        onError = { error ->
                                            Log.e("TaskScreen", "Error submitting task: $error")
                                        }
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = when (task.status) {
                                    0 -> "Начать выполнение"
                                    1 -> "Отправить на проверку"
                                    else -> "Действие"
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Не удалось определить вашу роль",
                        color = Color.Red,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun TaskItem(task: Task, navHostController: NavHostController){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val jsonTask = Gson().toJson(task)
                    navHostController.navigate(Routes.TaskScreen.route.replace("{task}", jsonTask))
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Задача",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${task.completionTime} hours",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if(task.completionTime>=24)Color.Green else Color.Red
                )
            }
        }
    }



    override fun onCleared() {
        super.onCleared()
        if (::webSocketManager.isInitialized) {
            webSocketManager.stopConnection()
        }
    }
}