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
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(DelicateCoroutinesApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
suspend fun sendToMachine(dispenseQty: Int, position: Int, context: Context): Boolean =
  coroutineScope {
    val manager = SerialPortManager.getInstance(context)
    val result = CompletableDeferred<Boolean>()
    var qty = dispenseQty
    var floor = -1
    var progress = "ready"
    var isDispense = false
    var ackReceived = false

    fun buildCheckMachineStatus(): ByteArray {
      val stx = byteArrayOf(0xFA.toByte(), 0xFB.toByte())
      val cmd = 0x63.toByte()
      val length = 0x01.toByte()
      val packNo = 0x01.toByte()
      val xor = (stx + byteArrayOf(
        cmd,
        length,
        packNo
      )).reduce { acc, b -> (acc.toInt() xor b.toInt()).toByte() }
      return stx + byteArrayOf(cmd, length, packNo, xor)
    }

    suspend fun waitForAck(): Boolean = withTimeoutOrNull(200) {
      var gotAck = false
      manager.readSerialttyS1 { rawData ->
        if (rawData.size >= 5 &&
          rawData[0] == 0xFA.toByte() &&
          rawData[1] == 0xFB.toByte() &&
          rawData[2] == 0x42.toByte()
        ) {
          gotAck = true
        }
      }
      delay(200)
      gotAck
    } ?: false

    // เช็คสถานะเครื่องก่อนเริ่มงาน
    manager.writeSerialttyS1(buildCheckMachineStatus())

    isDispense = true
    manager.writeSerialttyS2("# 1 1 3 1 6")

    manager.readSerialttyS1 { rawData ->
      val response = rawData.joinToString(" ") { "%02x".format(it.toInt() and 0xFF) }.uppercase()

      when {
        response == "FA FB 41 00 40" -> {
          manager.writeSerialttyS1Ack()

          if (qty > 0) {
            manager.writeSerialttyS1(position)
            GlobalScope.launch {
              ackReceived = waitForAck()
              if (!ackReceived) {
                Log.e("Vending", "No ACK received for dispense command")
              }
            }
            qty--
          }
        }

        response.startsWith("FA FB 04 04") -> {
          val statusCode = rawData.getOrNull(5)?.toInt()?.and(0xFF) ?: 0

          when (statusCode) {
            0x02 -> { // Dispense success
              if (progress == "dispensing" && qty <= 0) {
                progress = "liftDown"
                GlobalScope.launch {
                  delay(1000)
                  manager.writeSerialttyS2("# 1 1 1 -1 2")
                }
              }
            }

            0x03 -> Log.e("Vending", "Selection jammed")
            0x04 -> Log.e("Vending", "Motor did not stop normally")
            else -> manager.writeSerialttyS1Ack()
          }
        }

        else -> {
          manager.writeSerialttyS1Ack()
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
              manager.writeSerialttyS1(position)
              progress = "dispensing"
              Log.d("DataS2", "Send to ttyS1: $position")
            } else {
              manager.writeSerialttyS2("# 1 1 6 10 18")
              progress = "doorClosed"
            }
          }

          "26,31,d,a,32,d,a,36,d,a,31,d,a,31,30,d,a" -> {
            manager.writeSerialttyS2("# 1 1 3 0 5")
            progress = "rackUnlocked"
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
