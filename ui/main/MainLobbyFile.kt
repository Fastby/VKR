package com.example.mydiplom.ui.main

import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.mydiplom.ui.navigation.Routes
import com.example.mydiplom.ui.navigationPanel.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mydiplom.ui.api.ApiRepository
import com.example.mydiplom.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainLobbyDraw(
    navHostController: NavHostController,
    startRoute: String = Routes.Main.route
) {

    var currentTab by rememberSaveable { mutableStateOf(Routes.Main.route) }

    val onTabSelected: (String) -> Unit = { newTab ->
        if (currentTab != newTab) {
            currentTab = newTab
        }
    }

    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(ApiRepository(context)) as T
            }
        }
    )


    Scaffold(
        topBar = { TopPanel(navHostController) },
        bottomBar = {
            BottomNavigationPanel(
                currentRoute = currentTab,
                onTabSelected = onTabSelected
            )
        }
    ) { padding ->

        AnimatedContent(
            targetState = currentTab,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) with
                        fadeOut(animationSpec = tween(300))
            }
        ) { targetRoute ->
            when (targetRoute) {
                Routes.Main.route -> viewModel.ChatList(navHostController)
                Routes.Mailings.route -> viewModel.MailingsScreen(navHostController)
                Routes.Tasks.route-> viewModel.TasksScreen(navHostController)
                else -> viewModel.ChatList(navHostController)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun AppPreview(){
    val navController = rememberNavController()
    MainLobbyDraw(navController)
}