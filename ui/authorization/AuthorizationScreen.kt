package com.example.mydiplom.ui.authorization

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.mydiplom.ui.api.ApiRepository
import com.example.mydiplom.ui.navigation.AppNavigation
import com.example.mydiplom.ui.navigation.Routes
import com.example.mydiplom.ui.tokenManager.TokenManager
import com.example.mydiplom.ui.viewmodel.AuthViewModel


@Composable
fun AuthorizationScreenDraw(
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
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    if (isLoading.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            errorMessage.value?.let { message ->
                Text(
                    text = message,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            OutlinedTextField(
                value = email.value,
                onValueChange = { email.value = it },
                placeholder = { Text("Электронная почта") },
                singleLine = true,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(24.dp),
//                colors = OutlinedTextFieldDefaults.colors(
//                    unfocusedContainerColor = Color.LightGray,
//                    focusedContainerColor = Color.White,
//                    unfocusedTextColor = Color.White,
//                    focusedTextColor = Color.Black
//                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                placeholder = { Text("Пароль") },
                singleLine = true,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(24.dp),
//                colors = OutlinedTextFieldDefaults.colors(
//                    unfocusedContainerColor = Color.LightGray,
//                    focusedContainerColor = Color.White,
//                    unfocusedTextColor = Color.White,
//                    focusedTextColor = Color.Black
//                ),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.value.isNotEmpty() && password.value.isNotEmpty()) {
                        isLoading.value = true
                        errorMessage.value = null

                        viewModel.login(
                            email = email.value,
                            password = password.value,
                            onSuccess = { success ->
                                isLoading.value = false
                                if (success) {
                                    navHostController.navigate(Routes.Main.route) {
                                        popUpTo(Routes.Auth.route) {
                                            inclusive = true
                                        }
                                    }
                                }
                            },
                            onError = { error ->
                                isLoading.value = false
                                errorMessage.value = error
                            }
                        )
                    } else {
                        errorMessage.value = "Заполните все поля"
                    }
                }
            ) {
                Text(text = "Авторизация")
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun AppPreview(){
//    val navController = rememberNavController()
//    AppNavigation(navController)
//}