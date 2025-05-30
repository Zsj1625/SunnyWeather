package com.sunnyweather.android.ui.weather

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.sunnyweather.android.R
import com.sunnyweather.android.databinding.ActivityMainBinding
import com.sunnyweather.android.databinding.ActivityWeatherBinding
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.model.getSky
//import kotlinx.android.synthetic.main.activity_weather.*
//import kotlinx.android.synthetic.main.forecast.*
//import kotlinx.android.synthetic.main.life_index.*
//import kotlinx.android.synthetic.main.now.*
import java.text.SimpleDateFormat
import java.util.*

class WeatherActivity : AppCompatActivity() {

    val viewModel by lazy { ViewModelProviders.of(this).get(WeatherViewModel::class.java) }
//    private lateinit var binding: ActivityWeatherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        var swipeRefresh: SwipeRefreshLayout= findViewById(R.id.swipeRefresh)
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)

//        binding = ActivityWeatherBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
//        val drawerLayout = findViewById<drawerlayout>(R.id.drawerLayout)

        if (Build.VERSION.SDK_INT >= 21) {
            val decorView = window.decorView
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.statusBarColor = Color.TRANSPARENT
        }
        if (viewModel.locationLng.isEmpty()) {
            viewModel.locationLng = intent.getStringExtra("location_lng") ?: ""
        }
        if (viewModel.locationLat.isEmpty()) {
            viewModel.locationLat = intent.getStringExtra("location_lat") ?: ""
        }
        if (viewModel.placeName.isEmpty()) {
            viewModel.placeName = intent.getStringExtra("place_name") ?: ""
        }


        viewModel.weatherLiveData.observe(this, Observer { result ->
            val weather = result.getOrNull()

            if (weather != null) {
                showWeatherInfo(weather)
            } else {
                Toast.makeText(this, "无法成功获取天气信息", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
            swipeRefresh.isRefreshing = false

        })
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        refreshWeather()
        swipeRefresh.setOnRefreshListener {
            refreshWeather()
        }

        val navBtn : Button = findViewById(R.id.navBtn)
        navBtn.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {}

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerClosed(drawerView: View) {
                val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                manager.hideSoftInputFromWindow(drawerView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        })
        initAutoRefresh()

    }

    fun refreshWeather() {
        var swipeRefresh: SwipeRefreshLayout= findViewById(R.id.swipeRefresh)
        viewModel.refreshWeather(viewModel.locationLng, viewModel.locationLat)
//        swipeRefresh.isRefreshing = true
        Snackbar.make(findViewById(android.R.id.content), "天气数据已更新", Snackbar.LENGTH_SHORT).show()

    }

    private val refreshInterval = 3000000L
    private val refreshHandle = Handler(Looper.getMainLooper())
    private lateinit var refreshRunnable: Runnable

    private fun initAutoRefresh() {
        refreshRunnable = object  : Runnable {
            override fun run() {
                refreshWeather()
                refreshHandle.postDelayed(this, refreshInterval)
            }
        }
        refreshHandle.postDelayed(refreshRunnable, refreshInterval)
    }

    fun closeNavigationDrawer() {
       val drawerLayout: DrawerLayout= findViewById(R.id.drawerLayout)
        drawerLayout.closeDrawers()
//      drawerLayout.closeDrawers() // 使用View Binding的情况
    }

    private fun showWeatherInfo(weather: Weather) {

        lateinit var placeName: TextView
        lateinit var currentTemp: TextView
        lateinit var currentSky: TextView
        lateinit var currentAQI: TextView
        lateinit var currentvis: TextView
        lateinit var nowLayout: ViewGroup

        placeName = findViewById(R.id.placeName)
        currentTemp = findViewById(R.id.currentTemp)
        currentSky = findViewById(R.id.currentSky)
        currentAQI = findViewById(R.id.currentAQI)
        currentvis = findViewById(R.id.visibility)
        nowLayout = findViewById(R.id.nowLayout)

        placeName.text = viewModel.placeName
        val realtime = weather.realtime
        val daily = weather.daily

        // 填充now.xml布局中数据
        val currentTempText = "${realtime.temperature.toInt()} ℃"
        currentTemp.text = currentTempText
        currentSky.text = getSky(realtime.skycon).info

        val currentPM25Text = "空气指数 ${realtime.airQuality.aqi.chn.toInt()}"
        currentAQI.text = currentPM25Text

        val currentVisText = "能见度 ${realtime.visibility.toInt()}"

        currentvis.text = currentVisText

        nowLayout.setBackgroundResource(getSky(realtime.skycon).bg)

        // 填充forecast.xml布局中的数据
        lateinit var forecastLayout: ViewGroup
        forecastLayout = findViewById(R.id.forecastLayout)

        forecastLayout.removeAllViews()
        val days = daily.skycon.size
        for (i in 0 until days) {
            val skycon = daily.skycon[i]
            val temperature = daily.temperature[i]
            val view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false)
            val dateInfo = view.findViewById(R.id.dateInfo) as TextView
            val skyIcon = view.findViewById(R.id.skyIcon) as ImageView
            val skyInfo = view.findViewById(R.id.skyInfo) as TextView
            val temperatureInfo = view.findViewById(R.id.temperatureInfo) as TextView
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateInfo.text = simpleDateFormat.format(skycon.date)
            val sky = getSky(skycon.value)
            skyIcon.setImageResource(sky.icon)
            skyInfo.text = sky.info
            val tempText = "${temperature.min.toInt()} ~ ${temperature.max.toInt()} ℃"
            temperatureInfo.text = tempText
            forecastLayout.addView(view)
        }
        // 填充life_index.xml布局中的数据
        val lifeIndex = daily.lifeIndex

        lateinit var coldRiskText: TextView
        lateinit var dressingText: TextView
        lateinit var ultravioletText: TextView
        lateinit var carWashingText: TextView
        lateinit var weatherLayout: ViewGroup

        coldRiskText = findViewById(R.id.coldRiskText)
        dressingText = findViewById(R.id.dressingText)
        ultravioletText = findViewById(R.id.ultravioletText)
        carWashingText = findViewById(R.id.carWashingText)
        weatherLayout = findViewById(R.id.weatherLayout)

        coldRiskText.text = lifeIndex.coldRisk[0].desc
        dressingText.text = lifeIndex.dressing[0].desc
        ultravioletText.text = lifeIndex.ultraviolet[0].desc
        carWashingText.text = lifeIndex.carWashing[0].desc
        weatherLayout.visibility = View.VISIBLE
    }

}
