package com.thanes.vending.services

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
suspend fun sendToMachine(dispenseQty: Int, position: Int, context: Context): Boolean =
  coroutineScope {
    val result = CompletableDeferred<Boolean>()
    var qty = dispenseQty
    var floor = -1
    var progress = "ready"
    var isDispense = false

    val manager = SerialPortManager.getInstance(context)

    isDispense = true
    manager.writeSerialttyS2("# 1 1 3 1 6")

    manager.readSerialttyS1 { rawData ->
      val response = rawData.joinToString(" ") { "%02x".format(it.toInt() and 0xFF) }

//      Log.d("DataS1", "Received from ttyS1: $response")

      when {
        response == "fa,fb,41,0,40" -> {
          if (qty > 0) {
            manager.writeSerialttyS1(position)
            qty--
          } else {
            manager.writeSerialttyS1Every()
          }
        }

        response.startsWith("fa,fb,4,4") -> {
          if (progress == "dispensing") {
            if (qty <= 0) {
              progress = "liftDown"
              GlobalScope.launch {
                delay(1000)
                manager.writeSerialttyS2("# 1 1 1 -1 2")
              }
            }
          }
        }

        else -> {
          manager.writeSerialttyS1Every()
        }
      }
    }

    manager.readSerialttyS2 { rawData ->
      val response = rawData.joinToString(",") { "%x".format(it) }

      Log.d("DataS2", "Received from ttyS2: $response")

      if (isDispense) {
        when (response) {
          "26,31,d,a,32,d,a,33,d,a,31,d,a,37,d,a" -> {
            manager.writeSerialttyS2("# 1 1 5 10 17")
            progress = "doorOpened"
          }

          "26,31,d,a,32,d,a,35,d,a,31,d,a,39,d,a" -> {
            floor = when (position) {
              in 1..10 -> 1400
              in 11..20 -> 1210
              in 21..30 -> 1010
              in 31..40 -> 790
              in 41..50 -> 580
              in 51..60 -> 360
              else -> 20
            }
            manager.writeSerialttyS2("# 1 1 1 $floor ${floor + 3}")
            progress = "liftUp"
          }

          "26,31,d,a,32,d,a,31,d,a,31,d,a,35,d,a" -> {
            if (progress == "liftUp") {
              progress = "dispensing"
              manager.writeSerialttyS1(position)
            } else {
              manager.writeSerialttyS2("# 1 1 6 10 18")
              progress = "doorClosed"
            }
          }

          "26,31,d,a,32,d,a,36,d,a,31,d,a,31,30,d,a" -> {
            progress = "rackUnlocked"
            manager.writeSerialttyS2("# 1 1 3 0 5")
          }

          "26,31,d,a,32,d,a,33,d,a,30,d,a,36,d,a" -> {
            GlobalScope.launch {
              delay(200)
              result.complete(true)
            }
            qty = 0
            floor = 20
            progress = "ready"
            isDispense = false
          }

          else -> {
          }
        }
      }
    }

    return@coroutineScope result.await()
  }
