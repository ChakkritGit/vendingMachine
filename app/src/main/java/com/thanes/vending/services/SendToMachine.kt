package com.thanes.vending.services

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(DelicateCoroutinesApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
suspend fun sendToMachine(dispenseQty: Int, position: Int, context: Context): Boolean = coroutineScope {
  val manager = SerialPortManager.getInstance(context)
  val result = CompletableDeferred<Boolean>()
  var qty = dispenseQty
  var progress = "ready"
  val isCommandPending = AtomicBoolean(false)

  val pollChannel = Channel<Unit>(Channel.UNLIMITED)
  val dataChannel = Channel<ByteArray>(Channel.UNLIMITED)

  manager.writeSerialttyS2("# 1 1 3 1 6")

  fun buildDispenseCommand(slot: Int): ByteArray {
    val running = manager.getRunning()
    val newRunning = if (running == 255) 1 else running + 1
    manager.saveRunning(newRunning)

    val commands = mutableListOf(0xFA, 0xFB, 0x06, 0x05, newRunning, 0x00, 0x00, 0x00, slot)
    var checksum = 0
    for (element in commands) checksum = if (element == 0xFA) 0xFA else checksum xor element
    commands.add(checksum)

    return commands.map { it.toByte() }.toByteArray()
  }

  suspend fun waitForAck(): Boolean {
    val ackDeferred = CompletableDeferred<Boolean>()
    val timeoutJob = withTimeoutOrNull(200) {
      while (true) {
        val data = dataChannel.receive()
        if (data.size >= 5 && data[2] == 0x42.toByte()) {
          ackDeferred.complete(true)
          break
        }
      }
    }
    return timeoutJob != null && ackDeferred.await()
  }

  suspend fun sendCommandAfterPoll(cmd: ByteArray): Boolean {
    pollChannel.receive()
    manager.writeSerialttyS1Ack()
    delay(30)
    manager.writeSerialttyS1Raw(cmd)
    return waitForAck()
  }

  suspend fun repeatDispenseUntilDone() {
    while (qty > 0) {
      val cmd = buildDispenseCommand(position)
      val success = sendCommandAfterPoll(cmd)
      if (success) {
        qty--
      } else {
        Log.e("Vending", "Failed to send command. Retrying...")
        delay(300)
      }
    }
  }

  fun startSerialReader() {
    manager.readSerialttyS1 { rawData ->
      if (rawData.size >= 5 && rawData[2] == 0x41.toByte()) {
        pollChannel.trySend(Unit)
      } else {
        dataChannel.trySend(rawData)
      }

      val response = rawData.joinToString(" ") { "%02x".format(it.toInt() and 0xFF) }.uppercase()
      when {
        response.startsWith("FA FB 04 04") -> {
          val statusCode = rawData.getOrNull(5)?.toInt()?.and(0xFF) ?: 0
          when (statusCode) {
            0x02 -> {
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
      }
    }

    manager.readSerialttyS2 { rawData ->
      val response = rawData.joinToString(",") { "%x".format(it) }
      when (response) {
        "26,31,d,a,32,d,a,33,d,a,31,d,a,37,d,a" -> {
          progress = "doorOpened"
          manager.writeSerialttyS2("# 1 1 5 10 17")
        }
        "26,31,d,a,32,d,a,35,d,a,31,d,a,39,d,a" -> {
          progress = "liftUp"
          val floor = when (position) {
            in 1..10 -> 1400
            in 11..20 -> 1210
            in 21..30 -> 1010
            in 31..40 -> 790
            in 41..50 -> 580
            in 51..60 -> 360
            else -> 20
          }
          manager.writeSerialttyS2("# 1 1 1 $floor ${floor + 3}")
        }
        "26,31,d,a,32,d,a,31,d,a,31,d,a,35,d,a" -> {
          if (progress == "liftUp") {
            progress = "dispensing"
            GlobalScope.launch {
              manager.writeSerialttyS1Ack()
              delay(100)
              repeatDispenseUntilDone()
            }
          } else {
            progress = "doorClosed"
            manager.writeSerialttyS2("# 1 1 6 10 18")
          }
        }
        "26,31,d,a,32,d,a,36,d,a,31,d,a,31,30,d,a" -> {
          progress = "rackUnlocked"
          manager.writeSerialttyS2("# 1 1 3 0 5")
        }
        "26,31,d,a,32,d,a,33,d,a,30,d,a,36,d,a" -> {
          GlobalScope.launch {
            qty = 0
            progress = "ready"
            delay(200)
            result.complete(true)
          }
        }
      }
    }
  }

  startSerialReader()
  delay(300)
  return@coroutineScope result.await()
}
