package com.thanes.vending.data

data class LoginResponse(
  val message: String,
  val success: Boolean,
  val data: UserData
)