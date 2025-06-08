package com.example.mydiplom.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavHostController
import com.example.mydiplom.ui.navigation.Routes
import com.example.mydiplom.ui.theme.*
import com.example.mydiplom.ui.tokenManager.TokenManager

@Composable
fun SettingsScreenDraw(
    navController: NavHostController
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val tokenManager = remember { TokenManager(context) }
    var currentTheme by remember { mutableIntStateOf(settingsManager.getTheme()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Выбор темы
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Тема приложения",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = currentTheme == THEME_WHITE,
                        onClick = {
                            currentTheme = THEME_WHITE
                            settingsManager.saveTheme(THEME_WHITE)
                        }
                    )
                    Text(
                        text = "Светлая тема",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = currentTheme == THEME_BLACK,
                        onClick = {
                            currentTheme = THEME_BLACK
                            settingsManager.saveTheme(THEME_BLACK)
                        }
                    )
                    Text(
                        text = "Тёмная тема",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // Кнопка сброса настроек
        Button(
            onClick = {
                settingsManager.clearTheme()
                currentTheme = THEME_WHITE
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сбросить настройки")
        }

        // Кнопка выхода из аккаунта
        Button(
            onClick = {
                tokenManager.clearToken()
                navController.navigate(Routes.Auth.route) {
                    popUpTo(Routes.Main.route) { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Выйти из аккаунта")
        }
    }
}

// Предполагаемые константы, определенные где-то в коде
const val THEME_WHITE = 0
const val THEME_BLACK = 1

// Заглушка для SettingsManager, если она не определена
class SettingsManager(context: android.content.Context) {
    fun getTheme(): Int = THEME_WHITE
    fun saveTheme(theme: Int) {}
    fun clearTheme() {}
}