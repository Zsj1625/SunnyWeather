package com.sunnyweather.android.logic.model

import androidx.transition.Visibility
import com.google.gson.annotations.SerializedName

class RealtimeResponse(val status: String, val result: Result) {

    class Result(val realtime: Realtime)

    class Realtime(val skycon: String, val temperature: Float,val visibility: Float ,@SerializedName("air_quality") val airQuality: AirQuality)

    class AirQuality(val aqi: AQI)

    class AQI(val chn: Float)


}