package com.thanes.vending.remote

import com.thanes.vending.data.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val username: String, val password: String)

interface ApiService {
  @POST("auth/login")
  suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
