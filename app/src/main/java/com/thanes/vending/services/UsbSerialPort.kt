package com.thanes.vending.services

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import android.util.Log
import kotlinx.coroutines.*
import java.io.*

class SerialPortManager private constructor(context: Context) {
  private val appContext: Context = context.applicationContext
  private var inputStreamS1: InputStream? = null
  private var outputStreamS1: OutputStream? = null

  private var inputStreamS2: InputStream? = null
  private var outputStreamS2: OutputStream? = null

  private var jobReaderS1: Job? = null
  private var jobReaderS2: Job? = null

  private var isConnected = false

  private val prefs: SharedPreferences =
    appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

  companion object {
    private var INSTANCE: SerialPortManager? = null

    fun getInstance(context: Context): SerialPortManager {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: SerialPortManager(context).also { INSTANCE = it }
      }
    }

    private const val PREF_RUNNING = "running_counter"
    private const val PREF_NAME = "vending_prefs"
    private const val TAG = "SerialPortManager"
    private const val TTY_S1 = "/dev/ttyS1"
    private const val TTY_S2 = "/dev/ttyS2"
  }

  fun connect(): Boolean {
    try {
      val s1File = File(TTY_S1)
      val s2File = File(TTY_S2)

      if (!s1File.canRead() || !s1File.canWrite() || !s2File.canRead() || !s2File.canWrite()) {
        Log.e(TAG, "Permission denied on serial ports")
        return false
      }

      inputStreamS1 = FileInputStream(s1File)
      outputStreamS1 = FileOutputStream(s1File)

      inputStreamS2 = FileInputStream(s2File)
      outputStreamS2 = FileOutputStream(s2File)

      isConnected = true
      Log.d(TAG, "Connected to ttyS1 and ttyS2")
      return true
    } catch (e: Exception) {
      Log.e(TAG, "Error connecting serial ports: ${e.message}")
      disconnectPorts()
      return false
    }
  }

  fun writeSerialttyS1Every(): Boolean {
    val commands = mutableListOf(0xfa, 0xfb, 0x42, 0x00, 0x43)
    val cmdBytes = commands.map { it.toByte() }.toByteArray()

    return try {
      outputStreamS1?.write(cmdBytes)
      outputStreamS1?.flush()
      Log.d(TAG, "Sent to ttyS1: ${cmdBytes.joinToString(",")}")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Error writing to ttyS1: ${e.message}")
      false
    }
  }

  fun writeSerialttyS1(slot: Int): Boolean {
    if (outputStreamS1 == null) {
      Log.e(TAG, "Cannot write to ttyS1: Port not connected")
      return false
    }

    val running = getRunning()
    val newRunning = if (running == 255) 1 else running + 1
    saveRunning(newRunning)

    val commands = mutableListOf(0xfa, 0xfb, 0x06, 0x05, newRunning, 0x00, 0x00, 0x00, slot)
    var checksum = 0
    for (element in commands) {
      checksum = if (element == 0xFA) 0xFA else checksum xor element
    }
    commands.add(checksum)

    val cmdBytes = commands.map { it.toByte() }.toByteArray()

    return try {
      outputStreamS1?.write(cmdBytes)
      outputStreamS1?.flush()
      Log.d(TAG, "Sent to ttyS1: ${cmdBytes.joinToString(",")}")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Error writing to ttyS1: ${e.message}")
      false
    }
  }

  fun writeSerialttyS2(command: String): Boolean {
    if (outputStreamS2 == null) {
      Log.e(TAG, "Cannot write to ttyS2: Port not connected")
      return false
    }

    return try {
      outputStreamS2?.write(command.toByteArray(Charsets.US_ASCII))
      outputStreamS2?.flush()
      Log.d(TAG, "Sent to ttyS2: $command")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Error writing to ttyS2: ${e.message}")
      false
    }
  }

  fun readSerialttyS1(onDataReceived: (ByteArray) -> Unit): Boolean {
    if (inputStreamS1 == null) {
      Log.e(TAG, "Cannot read from ttyS1: Port not connected")
      return false
    }

    jobReaderS1?.cancel()
    jobReaderS1 = CoroutineScope(Dispatchers.IO).launch {
      val buffer = ByteArray(256)
      while (isActive) {
        try {
          val len = inputStreamS1?.read(buffer) ?: -1
          if (len > 0) onDataReceived(buffer.copyOf(len))
        } catch (e: Exception) {
          Log.e(TAG, "Read error on ttyS1: ${e.message}")
          break
        }
      }
    }
    return true
  }

  fun readSerialttyS2(onDataReceived: (ByteArray) -> Unit): Boolean {
    if (inputStreamS2 == null) {
      Log.e(TAG, "Cannot read from ttyS2: Port not connected")
      return false
    }

    jobReaderS2?.cancel()
    jobReaderS2 = CoroutineScope(Dispatchers.IO).launch {
      val buffer = ByteArray(256)
      while (isActive) {
        try {
          val len = inputStreamS2?.read(buffer) ?: -1
          if (len > 0) onDataReceived(buffer.copyOf(len))
        } catch (e: Exception) {
          Log.e(TAG, "Read error on ttyS2: ${e.message}")
          break
        }
      }
    }
    return true
  }

  fun disconnectPorts() {
    try {
      jobReaderS1?.cancel()
      jobReaderS2?.cancel()

      inputStreamS1?.close()
      outputStreamS1?.close()
      inputStreamS2?.close()
      outputStreamS2?.close()

      inputStreamS1 = null
      outputStreamS1 = null
      inputStreamS2 = null
      outputStreamS2 = null

      isConnected = false
      Log.d(TAG, "Disconnected serial ports")
    } catch (e: Exception) {
      Log.e(TAG, "Error disconnecting ports: ${e.message}")
    }
  }

  fun resetRunning() {
    saveRunning(1)
  }

  fun getRunning(): Int {
    Log.d("TEST", "Passed get")
    return prefs.getInt(PREF_RUNNING, 1)
  }

  fun saveRunning(value: Int) {
    Log.d("TEST", "Passed add")
    prefs.edit() { putInt(PREF_RUNNING, value) }
  }

  fun isConnected(): Boolean = isConnected
}
