package com.thanes.vending.data

data class UserData(
  val id: String,
  val username: String,
  val display: String,
  val picture: String,
  val status: Boolean,
  val role: String,
  val token: String
)