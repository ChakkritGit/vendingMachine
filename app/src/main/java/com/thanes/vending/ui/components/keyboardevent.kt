package com.thanes.vending.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned

@Composable
fun BarcodeInputField(
  onScanComplete: (String) -> Unit
) {
  val focusRequester = remember { FocusRequester() }
  var isBoxAttached by remember { mutableStateOf(false) }
  var inputBuffer by remember { mutableStateOf("") }

  LaunchedEffect(isBoxAttached) {
    if (isBoxAttached) {
      focusRequester.requestFocus()
    }
  }

  Box(
    modifier = Modifier
      .onGloballyPositioned {
        isBoxAttached = true
      }
      .focusRequester(focusRequester)
      .focusable()
      .onKeyEvent {
        if (it.type == KeyEventType.KeyDown) {
          when (it.key) {
            Key.Enter -> {
              if (inputBuffer.isNotBlank()) {
                onScanComplete(inputBuffer)
                inputBuffer = ""
              }
              true
            }

            Key.Backspace -> {
              inputBuffer = inputBuffer.dropLast(1)
              true
            }

            Key.ShiftLeft, Key.ShiftRight,
            Key.CtrlLeft, Key.CtrlRight,
            Key.AltLeft, Key.AltRight,
            Key.Tab, Key.DirectionLeft, Key.DirectionRight,
            Key.DirectionUp, Key.DirectionDown -> {
              false
            }

            else -> {
              val char = it.nativeKeyEvent.unicodeChar.toChar()
              if (char.isLetterOrDigit()) {
                inputBuffer += char
              }
              true
            }
          }
        } else {
          false
        }
      }
  )
}