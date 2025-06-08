package com.example.mydiplom.ui.navigationPanel

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.mydiplom.ui.api.ApiRepository
import com.example.mydiplom.ui.chat.Chat
import com.example.mydiplom.ui.navigation.Routes
import com.example.mydiplom.ui.viewmodel.AuthViewModel
import androidx.compose.material3.*
import com.google.gson.Gson

@Composable
fun TopPanel(
    navHostController: NavHostController
) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(ApiRepository(context)) as T
            }
        }
    )


    val searchText = remember { mutableStateOf("") }
    val showResults = remember { mutableStateOf(false) }
    val searchResults = remember { mutableStateOf<List<Chat>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchText.value,
                onValueChange = { newText ->
                    searchText.value = newText
                    showResults.value = newText.isNotEmpty()
                    if (newText.isNotEmpty()) {
                        viewModel.searchChats(
                            query = newText,
                            onSuccess = { results ->
                                searchResults.value = results
                            },
                            onError = { error ->
                                Log.e("Search", error)
                                searchResults.value = emptyList()
                            }
                        )
                    } else {
                        searchResults.value = emptyList()
                    }
                },
                placeholder = { Text("Поиск чатов") }, // Исправлено здесь
                singleLine = true,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.Black
                )
            )

            IconButton(
                onClick = { navHostController.navigate(Routes.Settings.route) },
                modifier = Modifier.size(50.dp).clip(CircleShape),
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.LightGray)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Меню")
            }

            IconButton(
                onClick = { navHostController.navigate(Routes.Profile.route) },
                modifier = Modifier.size(50.dp).clip(CircleShape),
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.LightGray)
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Профиль")
            }
        }

        // Показать результаты поиска
        if (showResults.value && searchResults.value.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    items(searchResults.value) { chat ->
                        ChatSearchItem(
                            chat = chat,
                            onClick = {
                                val jsonChat = Gson().toJson(chat)
                                navHostController.navigate(
                                    Routes.ChatScreen.route.replace("{chat}", jsonChat)
                                )
                                searchText.value = ""
                                showResults.value = false
                            }
                        )
                    }
                }
            }
        }

        // Показать индикатор загрузки
        if (viewModel.isLoading.value && showResults.value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Показать сообщение, если нет результатов
        if (showResults.value && searchResults.value.isEmpty() && !viewModel.isLoading.value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Чаты не найдены", color = Color.Gray)
            }
        }
    }
}

@Composable
fun ChatSearchItem(chat: Chat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Чат",
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = chat.name ?: "Без названия",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = chat.lastMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1
            )
        }
    }
    Divider(modifier = Modifier.padding(start = 52.dp))
}

@Composable
fun BottomNavigationPanel(
    currentRoute: String,
    onTabSelected: (String) -> Unit
) {
    BottomAppBar {
        listOf(Routes.Main, Routes.Mailings, Routes.Tasks).forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onTabSelected(tab.route) },
                icon = {
                    Icon(
                        when (tab) {
                            is Routes.Main -> Icons.Default.Home
                            is Routes.Mailings -> Icons.Default.Notifications
                            is Routes.Tasks -> Icons.Default.DateRange
                            else -> Icons.Default.Home
                        },
                        contentDescription = tab.route
                    )
                },
                label = {
                    Text(
                        when (tab) {
                            is Routes.Main -> "Главное"
                            is Routes.Mailings -> "Рассылки"
                            is Routes.Tasks -> "Задачи"
                            else -> ""
                        },
                        fontSize = 10.sp
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}