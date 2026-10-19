package com.aim.vpn.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aim.vpn.presentation.home.HomeScreen
import com.aim.vpn.presentation.home.HomeViewModel
import com.aim.vpn.presentation.login.LoginScreen
import com.aim.vpn.presentation.login.LoginViewModel
import com.aim.vpn.presentation.permissions.PermissionOnboardingScreen
import com.aim.vpn.ui.theme.VipCoolWireTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VipCoolWireTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0F)
                ) {
                    val navController = rememberNavController()
                    val loginViewModel: LoginViewModel = hiltViewModel()
                    val sessionValid by loginViewModel.sessionValid.collectAsState()
                    val sessionChecked by loginViewModel.sessionChecked.collectAsState()

                    if (!sessionChecked) return@Surface

                    NavHost(
                        navController = navController,
                        startDestination = if (sessionValid) "home" else "login"
                    ) {
                        composable("login") {
                            LoginScreen(
                                viewModel = loginViewModel,
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("home") {
                            val homeViewModel: HomeViewModel = hiltViewModel()
                            HomeScreen(
                                viewModel = homeViewModel,
                                onRequestPermissions = {
                                    navController.navigate("permissions")
                                },
                                onExit = { finishAffinity() }
                            )
                        }
                        composable("permissions") {
                            PermissionOnboardingScreen(
                                onComplete = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
