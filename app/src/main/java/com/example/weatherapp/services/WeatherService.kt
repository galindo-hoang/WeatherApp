package com.example.weatherapp.services

import com.example.weatherapp.models.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("data/2.5/weather")
    fun getObjectWeather(@Query("lat") lat:Double,
                         @Query("lon") long:Double,
                         @Query("appid") api:String,
                         @Query("units") unit:String): Call<WeatherResponse>
}