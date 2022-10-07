package com.example.weather.Utilities

import com.example.weather.POJO.ModelClass
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


interface ApiInterface {

    @GET("weather")
    fun getCurrentWeatherData(
        @Query("lat") latitude:String,
        @Query("lon") longitude:String,
        @Query("APPID") api_key:String
    ): Call<ModelClass>

    @GET("weather")
    fun getCityWeather(
        @Query("q") cityame:String,
        @Query("APPID") api_key:String
    ): Call<ModelClass>

}