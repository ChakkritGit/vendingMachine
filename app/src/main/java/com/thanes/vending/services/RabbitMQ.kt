package com.thanes.vending.services

import android.util.Log
import com.rabbitmq.client.*
import com.thanes.vending.utils.*

class RabbitMQService private constructor() {

  private var connection: Connection? = null
  private var channel: Channel? = null
  private var isListening = false
  private var ackMessage: Envelope? = null
  private var messageBody = ""

  companion object {
    @Volatile
    private var instance: RabbitMQService? = null

    fun getInstance(): RabbitMQService {
      return instance ?: synchronized(this) {
        instance ?: RabbitMQService().also { instance = it }
      }
    }
  }

  fun connect() {
    if (connection?.isOpen == true && channel?.isOpen == true) {
      Log.d("RabbitMq", "Already connected.")
      return
    }

    synchronized(this) {
      if (connection?.isOpen == true && channel?.isOpen == true) {
        Log.d("RabbitMq", "Already connected (inside sync).")
        return
      }

      val factory = ConnectionFactory().apply {
        host = RabbitHost
        port = RabbitPort
        username = RabbitUser
        password = RabbitPass
        virtualHost = "/"
        isAutomaticRecoveryEnabled = true
      }

      var attempt = 0
      val maxRetries = 3

      while (attempt < maxRetries) {
        try {
          attempt++
          connection = factory.newConnection()
          channel = connection!!.createChannel()
          channel!!.basicQos(1)

          Log.d("RabbitMq", "New connection established on attempt $attempt.")
          break
        } catch (e: Exception) {
          Log.e("RabbitMq", "Failed to connect on attempt $attempt: ${e.message}", e)
          if (attempt >= maxRetries) {
            Log.e("RabbitMq", "All connection attempts failed.")
          } else {
            Thread.sleep(2000)
          }
        }
      }
    }
  }

  @Synchronized
  fun listenToQueue(queueName: String) {
    if (isListening) {
      Log.d("RabbitMq", "Already listening to queue.")
      return
    }

    if (channel == null || !channel!!.isOpen) {
      Log.d("RabbitMq", "Channel is not open. Call connect() first.")
      return
    }

    try {
      channel!!.queueDeclare(queueName, true, false, false, null)

      val consumer = object : DefaultConsumer(channel) {
        override fun handleDelivery(
          consumerTag: String?,
          envelope: Envelope,
          properties: AMQP.BasicProperties?,
          body: ByteArray
        ) {
          Log.d("RabbitMq", "Message: ${String(body)}")
          ackMessage = envelope
          messageBody = String(body)
        }
      }

      channel!!.basicConsume(queueName, false, consumer)
      isListening = true
      Log.d("RabbitMq", "Started listening to $queueName")
    } catch (e: Exception) {
      Log.e("RabbitMq", "Failed to listening: ${e.message}", e)
    }
  }

  fun ack() {
    try {
      val envelope = ackMessage
      if (envelope != null) {
        channel?.basicAck(envelope.deliveryTag, false)
        Log.d("RabbitMq", "Acked message: $messageBody")
        ackMessage = null
        messageBody = ""
      } else {
        Log.e("RabbitMq", "Cannot ack: ackMessage is null")
      }
    } catch (e: Exception) {
      Log.e("RabbitMq", "Cannot ack: ${e.message}", e)
    }
  }

  fun reject(envelope: Envelope, requeue: Boolean = true) {
    try {
      channel?.basicReject(envelope.deliveryTag, requeue)
    } catch (e: Exception) {
      Log.e("RabbitMq", "Cannot reject ${e.message.toString()}")
    }
  }

  fun disconnect() {
    try {
      channel?.close()
      connection?.close()
    } catch (e: Exception) {
      Log.d("RabbitMq", "Error while disconnect ${e.message.toString()}")
    } finally {
      channel = null
      connection = null
      isListening = false
    }
  }
}