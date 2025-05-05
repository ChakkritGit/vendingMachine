package com.thanes.vending.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.thanes.vending.R
import com.thanes.vending.data.LoginRepository
import com.thanes.vending.dataStore.DataManager
import com.thanes.vending.navHost.Routes
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun LoginScreen(navController: NavHostController, context: Context) {
  var userName by remember { mutableStateOf("") }
  var userPassword by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  var inputErrorMessage by remember { mutableStateOf("") }

  val scope = rememberCoroutineScope()

  fun handleLogin() {
    inputErrorMessage = ""
    errorMessage = ""
    isLoading = true

    scope.launch {
      if (userName.isEmpty() || userPassword.isEmpty()) {
        inputErrorMessage = "Please fill in both fields"
        isLoading = false
        return@launch
      }

      try {
        val response = LoginRepository.login(userName, userPassword)

        if (response.isSuccessful) {
          val token = response.body()?.data?.token.orEmpty()
          val user = response.body()?.data

          if (token.isNotEmpty() && user != null) {
            DataManager.saveToken(context, token)
            DataManager.saveUserData(context, user)
            navController.navigate(Routes.Home.route) {
              popUpTo(Routes.Login.route) { inclusive = true }
            }
          } else {
            errorMessage = "ข้อมูลผู้ใช้ไม่สมบูรณ์"
          }
        } else {
          val errorJson = response.errorBody()?.string()
          val message = try {
            JSONObject(errorJson ?: "").getString("message")
          } catch (e: Exception) {
            "เกิดข้อผิดพลาดบางอย่าง"
          }
          errorMessage = message
        }
      } catch (e: Exception) {
        errorMessage = "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์"
      } finally {
        isLoading = false
      }
    }
  }

  Box(
    modifier = Modifier
      .padding(10.dp)
      .fillMaxHeight()
  ) {
    Column(
//      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxHeight()
    ) {
      Text("Login")

      Image(
        painter = painterResource(id = R.drawable.ic_launcher_foreground),
        contentDescription = "Login banner",
        modifier = Modifier
          .fillMaxWidth()
          .height(256.dp),
        contentScale = ContentScale.Fit,
      )

      Spacer(modifier = Modifier.height(20.dp))

      OutlinedTextField(
        value = userName,
        onValueChange = { userName = it },
        label = { Text("Username") },
        modifier = Modifier
          .fillMaxWidth()
          .height(70.dp),
        shape = RoundedCornerShape(50.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = userPassword,
        onValueChange = { userPassword = it },
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier
          .fillMaxWidth()
          .height(70.dp),
        shape = RoundedCornerShape(50.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          if (isLoading) return@Button
          handleLogin()
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(60.dp),
        enabled = !isLoading
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(24.dp)
          )
        } else {
          Text("Login")
        }
      }

      if (inputErrorMessage.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = inputErrorMessage, color = MaterialTheme.colorScheme.error)
      }

      if (errorMessage.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
      }
    }
  }
}