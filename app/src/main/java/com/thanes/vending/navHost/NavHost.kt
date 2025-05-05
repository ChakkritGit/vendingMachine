package com.thanes.vending.navHost

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.thanes.vending.dataStore.DataManager
import com.thanes.vending.screens.HomeScreen
import com.thanes.vending.screens.LoginScreen

@Composable
fun AppNavigation(navController: NavHostController, innerPadding: PaddingValues, context: Context) {
  var token by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    token = DataManager.getToken(context)
  }

  token?.let { tokenValue ->
    val startDestination = if (tokenValue.isNotEmpty()) Routes.Home.route else Routes.Login.route

    NavHost(navController = navController, startDestination = startDestination, modifier = Modifier.padding(innerPadding)) {
      composable(route = Routes.Login.route) {
        LoginScreen(navController, context)
      }
      composable(route = Routes.Home.route) {
        HomeScreen(navController, context)
      }
    }
  }
}