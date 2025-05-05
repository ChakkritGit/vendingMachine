package com.thanes.vending.screens

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.thanes.vending.data.UserData
import com.thanes.vending.dataStore.DataManager
import com.thanes.vending.navHost.Routes
import com.thanes.vending.services.RabbitMQService
import com.thanes.vending.services.SerialPortManager
import com.thanes.vending.services.sendToMachine
import com.thanes.vending.ui.components.BarcodeInputField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun HomeScreen(navController: NavHostController, context: Context) {
  val usbManager = SerialPortManager.getInstance(context)
  val rabbitMQ = RabbitMQService.getInstance()
  val scope = rememberCoroutineScope()
  var userData by remember { mutableStateOf<UserData?>(null) }
  var text by remember { mutableStateOf("") }
  val dataS1 = remember { mutableStateListOf<String>() }
  val dataS2 = remember { mutableStateListOf<String>() }
  val running = remember { mutableIntStateOf(1) }

  val position = remember { mutableIntStateOf(1) }

  LaunchedEffect(Unit) {
    userData = DataManager.getUserData(context)
  }

//  LaunchedEffect(Unit) {
//    usbManager.readSerialttyS1 { data ->
//      val hexStringS1 = data.joinToString(" ") { byte -> "%02x".format(byte.toInt() and 0xFF) }
//      Log.d("DataS1", "Received from ttyS1: $hexStringS1")
//      CoroutineScope(Dispatchers.Main).launch {
//        dataS1.add(hexStringS1)
//      }
//    }
//
//    usbManager.readSerialttyS2 { data ->
//      val hexStringS2 = data.joinToString(" ") { byte -> "%02x".format(byte.toInt() and 0xFF) }
//      Log.d("DataS2", "Received from ttyS2: $hexStringS2")
//      CoroutineScope(Dispatchers.Main).launch {
//        dataS2.add(hexStringS2)
//      }
//    }
//  }

  LazyColumn(modifier = Modifier.fillMaxSize()) {
    item {
      Text("Home")

      AsyncImage(
        model = "https://picsum.photos/id/101/128/128",
        contentDescription = "test",
      )

      Text("ยินดีต้อนรับ ${userData?.display}")
      Text("Role: ${userData?.role}")

      Spacer(modifier = Modifier.height(5.dp))

      BarcodeInputField { scanned ->
        text = scanned
        Log.d("Barcode", "Scanned: $scanned")
      }

      Text("Scanned: $text")

      Spacer(modifier = Modifier.height(15.dp))

      Button(
        onClick = {
          scope.launch(Dispatchers.IO) {
            rabbitMQ.ack()
          }
        },
      ) {
        Text("Ack Message")
      }

      Text("Running: ${running.intValue}")

      Button(onClick = {
        scope.launch(Dispatchers.IO) {
          running.intValue = usbManager.getRunning()
        }
      }) {
        Text("Get running")
      }
      Button(onClick = {
        scope.launch(Dispatchers.IO) {
          usbManager.saveRunning(running.intValue + 1)
        }
      }) {
        Text("Save running")
      }

      OutlinedTextField(
        value = position.intValue.toString(),
        onValueChange = { position.intValue = it.toInt() },
        label = { Text("Position") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
          .fillMaxWidth()
          .height(70.dp)
          .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(50.dp)
      )

      Button(onClick = {
        scope.launch(Dispatchers.IO) {
          usbManager.writeSerialttyS1(position.intValue)
        }
      }) {
        Text("Dispense")
      }
    }


//    if (dataS1.isNotEmpty()) {
//      item {
//        Spacer(modifier = Modifier.height(10.dp))
//        Text("Received data s1:")
//        LazyColumn {
//          items(dataS1.size) { index ->
//            Text(dataS1[index])
//          }
//        }
//      }
//    }
//
//    if (dataS2.isNotEmpty()) {
//      item {
//        Spacer(modifier = Modifier.height(10.dp))
//        Text("Received data s2:")
//        LazyColumn {
//          items(dataS2.size) { index ->
//            Text(dataS2[index])
//          }
//        }
//      }
//    }

    item {
      Spacer(modifier = Modifier.height(30.dp))

      Button(onClick = {
        scope.launch {
          var continueReturn = sendToMachine(dispenseQty = 3, position = 11, context = context)
          Log.d("sendToMachine", "continue: $continueReturn")
        }
      }) {
        Text("Send To Machine")
      }

      Spacer(modifier = Modifier.height(30.dp))

      Button(onClick = {
        scope.launch {
          DataManager.clearAll(context)

          navController.navigate(Routes.Login.route) {
            popUpTo(Routes.Home.route) { inclusive = true }
          }
        }
      }) {
        Text("Logout")
      }
    }
  }
}
