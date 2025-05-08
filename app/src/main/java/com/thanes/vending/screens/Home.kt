package com.thanes.vending.screens

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.thanes.vending.data.UserData
import com.thanes.vending.dataStore.DataManager
import com.thanes.vending.navHost.Routes
import com.thanes.vending.services.SerialPortManager
import com.thanes.vending.services.sendToMachine
import kotlinx.coroutines.launch
import com.thanes.vending.R
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay

@OptIn(DelicateCoroutinesApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun HomeScreen(navController: NavHostController, context: Context) {
  val usbManager = SerialPortManager.getInstance(context)
//  val rabbitMQ = RabbitMQService.getInstance()
  val scope = rememberCoroutineScope()
  var userData by remember { mutableStateOf<UserData?>(null) }
//  var text by remember { mutableStateOf("") }
//  val dataS1 = remember { mutableStateListOf<String>() }
//  val dataS2 = remember { mutableStateListOf<String>() }
//  val running = remember { mutableIntStateOf(1) }

  LaunchedEffect(Unit) {
    userData = DataManager.getUserData(context)
  }

  LaunchedEffect(Unit) {
    usbManager.readSerialttyS1 { data ->
      val response = data.joinToString(" ") { "%02x".format(it.toInt() and 0xFF) }.uppercase()
      Log.d("DataS1", "Received from ttyS1: $response")
    }
  }
//
//    usbManager.readSerialttyS2 { data ->
//      val hexStringS2 = data.joinToString(" ") { byte -> "%02x".format(byte.toInt() and 0xFF) }
//      Log.d("DataS2", "Received from ttyS2: $hexStringS2")
//      CoroutineScope(Dispatchers.Main).launch {
//        dataS2.add(hexStringS2)
//      }
//    }
//  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(10.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("ยินดีต้อนรับ ${userData?.display}", style = TextStyle(fontSize = 18.sp))
      Spacer(modifier = Modifier.width(10.dp))
      Text("-", style = TextStyle(fontSize = 18.sp))
      Spacer(modifier = Modifier.width(10.dp))
      Text("สิทธิ์: ${userData?.role}", style = TextStyle(fontSize = 18.sp))
      Spacer(modifier = Modifier.width(15.dp))
      Button(onClick = {
        scope.launch {
          DataManager.clearAll(context)

          navController.navigate(Routes.Login.route) {
            popUpTo(Routes.Home.route) { inclusive = true }
          }
        }
      }) {
        Image(
          painter = painterResource(id = R.drawable.logout_24px),
          contentDescription = "Logout",
          modifier = Modifier
            .height(30.dp)
            .width(30.dp),
          contentScale = ContentScale.Fit,
        )
      }
    }

//      Spacer(modifier = Modifier.height(5.dp))

//      BarcodeInputField { scanned ->
//        text = scanned
//        Log.d("Barcode", "Scanned: $scanned")
//      }

//      Text("Scanned: $text")

    Spacer(modifier = Modifier.height(20.dp))

//      Button(
//        onClick = {
//          scope.launch(Dispatchers.IO) {
//            rabbitMQ.ack()
//          }
//        },
//      ) {
//        Text("Ack Message")
//      }

//      Text("Running: ${running.intValue}")
//
//      Button(onClick = {
//        scope.launch(Dispatchers.IO) {
//          running.intValue = usbManager.getRunning()
//        }
//      }) {
//        Text("Get running")
//      }
//      Button(onClick = {
//        scope.launch(Dispatchers.IO) {
//          usbManager.saveRunning(running.intValue + 1)
//        }
//      }) {
//        Text("Save running")
//      }

    SlotGridWithBottomSheet(context = context)

    Spacer(modifier = Modifier.height(20.dp))

    Button(onClick = {
      scope.launch {
        usbManager.writeSerialttyS1Ack()
        usbManager.writeSerialttyS2("# 1 1 1 -1 2")
      }
    }) {
      Text("Back to home")
    }
  }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotGridWithBottomSheet(context: Context) {
  val numbers = (1..60).toList()
  val sheetState = rememberModalBottomSheetState()
  val scope = rememberCoroutineScope()
  var showBottomSheet by remember { mutableStateOf(false) }
  var selectedNumber by remember { mutableIntStateOf(1) }
  val qty = remember { mutableIntStateOf(1) }

  Column {
    for (row in 0 until 6) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        for (col in 0 until 10) {
          val index = row * 10 + col
          if (index < numbers.size) {
            val number = numbers[index]
            Card(
              modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .clickable {
                  selectedNumber = number
                  showBottomSheet = true
                },
              elevation = CardDefaults.cardElevation(4.dp)
            ) {
              Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
              ) {
                Text(text = number.toString())
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
    }
  }

  if (showBottomSheet) {
    ModalBottomSheet(
      onDismissRequest = {
        showBottomSheet = false
      },
      sheetState = sheetState
    ) {
      Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(10.dp)
      ) {
        Text(
          "ช่องที่เลือก: $selectedNumber",
          style = TextStyle(fontSize = 32.sp)
        )
        Spacer(modifier = Modifier.height(15.dp))
        Text("จำนวน", style = TextStyle(fontSize = 32.sp))
        Spacer(modifier = Modifier.height(15.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxWidth()
        ) {
          Button(onClick = {
            if (qty.intValue > 1) {
              qty.intValue = qty.intValue - 1
            }
          }) {
            Text("-", style = TextStyle(fontSize = 32.sp))
          }
          Spacer(modifier = Modifier.width(24.dp))
          Text(qty.intValue.toString(), style = TextStyle(fontSize = 42.sp))
          Spacer(modifier = Modifier.width(24.dp))
          Button(onClick = {
            if (qty.intValue < 10) {
              qty.intValue = qty.intValue + 1
            }
          }) {
            Text("+", style = TextStyle(fontSize = 32.sp))
          }
        }
        Spacer(modifier = Modifier.height(30.dp))
        Button(
          modifier = Modifier.fillMaxWidth(fraction = 0.75f),
          onClick = {
            scope.launch {
              var continueReturn =
                sendToMachine(
                  dispenseQty = qty.intValue,
                  position = selectedNumber,
                  context = context
                )
              if (continueReturn) {
                qty.intValue = 1
                selectedNumber = 1
              }
              Log.d("sendToMachine", "continue: $continueReturn")
            }
            scope.launch { sheetState.hide() }.invokeOnCompletion {
              if (!sheetState.isVisible) {
                showBottomSheet = false
              }
            }
          }) {
          Text("Dispense")
        }
      }
    }
  }
}

