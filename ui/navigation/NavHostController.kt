package com.example.mydiplom.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mydiplom.ui.api.ApiRepository
import com.example.mydiplom.ui.api.NotificationDto
import com.example.mydiplom.ui.authorization.AuthorizationScreenDraw
import com.example.mydiplom.ui.chat.Chat
import com.example.mydiplom.ui.main.MainLobbyDraw
import com.example.mydiplom.ui.profile.ProfileScreenDraw
import com.example.mydiplom.ui.settings.SettingsScreenDraw
import com.example.mydiplom.ui.tasks.Task
import com.example.mydiplom.ui.viewmodel.AuthViewModel
import com.google.gson.Gson

sealed class Routes(
    val route: String
) {
    object Auth : Routes("authorization")
    object Main : Routes("main")
    object Mailings : Routes("mailScreen?mail={mail}")
    object Tasks : Routes("tasks")
    object Profile : Routes("profile")
    object ChatScreen : Routes("chatScreen?chat={chat}")
    object Settings : Routes("settings")
    object TaskScreen : Routes("taskScreen?task={task}")
}

@Composable
fun AppNavigation(navHostController: NavHostController) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(ApiRepository(context)) as T
            }
        }
    )

    NavHost(
        navController = navHostController,
        startDestination = Routes.Auth.route
    ) {
        composable(Routes.Auth.route) {
            AuthorizationScreenDraw(navHostController)
        }

        composable(Routes.Main.route) {
            MainLobbyDraw(navHostController)
        }

        composable(Routes.Profile.route) {
            ProfileScreenDraw(navHostController)
        }

        composable(
            route = Routes.ChatScreen.route,
            arguments = listOf(navArgument("chat") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatJson = backStackEntry.arguments?.getString("chat")
            if (chatJson != null) {
                val chat = try {
                    Gson().fromJson(chatJson, Chat::class.java)
                } catch (e: Exception) {
                    null
                }
                if (chat != null) {
                    viewModel.ChatScreen(chat, navHostController)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Ошибка загрузки чата")
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Данные чата не переданы")
                }
            }
        }

        composable(Routes.Settings.route) {
            SettingsScreenDraw(navHostController)
        }

        composable(
            route = Routes.TaskScreen.route,
            arguments = listOf(navArgument("task") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskJson = backStackEntry.arguments?.getString("task")
            if (taskJson != null) {
                val task = try {
                    Gson().fromJson(taskJson, Task::class.java)
                } catch (e: Exception) {
                    null
                }
                if (task != null) {
                    viewModel.TaskScreen(task, navHostController)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Ошибка загрузки задачи")
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Данные задачи не переданы")
                }
            }
        }

        composable(
            route = Routes.Mailings.route,
            arguments = listOf(navArgument("mail") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskJson = backStackEntry.arguments?.getString("mail")
            if (taskJson != null) {
                val task = try {
                    Gson().fromJson(taskJson, NotificationDto::class.java)
                } catch (e: Exception) {
                    null
                }
                if (task != null) {
                    viewModel.MailScreen(task,navHostController)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Ошибка загрузки рассылки")
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Данные о рассылках не переданы")
                }
            }
        }
    }
}