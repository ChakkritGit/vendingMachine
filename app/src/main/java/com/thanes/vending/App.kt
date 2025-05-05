package com.thanes.vending

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import com.thanes.vending.services.RabbitMQService
import com.thanes.vending.services.SerialPortManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {

  private val applicationScope = CoroutineScope(Dispatchers.IO)

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  override fun onCreate() {
    super.onCreate()
    applicationScope.launch {
      val usbManager = SerialPortManager.getInstance(this@App)
      usbManager.connect()
      RabbitMQService.getInstance().connect()
      RabbitMQService.getInstance().listenToQueue("vdOrder")
    }
  }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  override fun onTerminate() {
    super.onTerminate()
    applicationScope.launch {
      val usbManager = SerialPortManager.getInstance(this@App)
      usbManager.disconnectPorts()
      RabbitMQService.getInstance().disconnect()
    }
  }
}