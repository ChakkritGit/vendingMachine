package com.thanes.vending.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
  val api: ApiService by lazy {
    Retrofit.Builder()
      .baseUrl("https://wardstockapi.thanespgm.com/api/")
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(ApiService::class.java)
  }
}