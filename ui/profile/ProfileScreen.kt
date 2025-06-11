package com.example.mydiplom.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.mydiplom.ui.api.ApiRepository
import com.example.mydiplom.ui.navigation.Routes
import com.example.mydiplom.ui.viewmodel.AuthViewModel

@Composable
fun ProfileScreenDraw(
    navHostController: NavHostController
){
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(ApiRepository(context)) as T
            }
        }
    )
    val phonenumber = "+79237300209"
    val name = viewModel.getUserName()
    val post = viewModel.getUserRole()
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .size(66.dp)
                .background(Color.LightGray),
            horizontalArrangement = Arrangement.Absolute.Left

        ) {
            IconButton(
                onClick = { navHostController.navigate(Routes.Main.route) },

                modifier = Modifier.size(66.dp).clip(CircleShape).fillMaxHeight(),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Назад"
                )
            }
        }
        Icon(
            imageVector = Icons.Default.AccountBox,
            contentDescription = "Профиль",
            modifier = Modifier.fillMaxWidth().height(150.dp)
        )
        Spacer(modifier = Modifier.height(64.dp))
        Text(
            text = "ФИО: $name",
            style = TextStyle(
                fontSize = 24.sp,
                textAlign = TextAlign.Left
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Телефон: $phonenumber",
            style = TextStyle(
                fontSize = 24.sp,
                textAlign = TextAlign.Left
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Должность: $post",
            style = TextStyle(
                fontSize = 24.sp,
                textAlign = TextAlign.Left
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        )

    }
}