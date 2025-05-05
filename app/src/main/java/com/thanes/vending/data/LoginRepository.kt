package com.thanes.vending.data

import com.thanes.vending.remote.LoginRequest
import com.thanes.vending.remote.RetrofitInstance
import retrofit2.Response

object LoginRepository {
  suspend fun login(userName: String, userPassword: String): Response<LoginResponse> {
    val request = LoginRequest(userName, userPassword)
    return RetrofitInstance.api.login(request)
  }
}