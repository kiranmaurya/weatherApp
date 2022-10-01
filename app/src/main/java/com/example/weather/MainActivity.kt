package com.example.weather

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.audiofx.Equalizer.Settings
import android.os.Bundle
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.PermissionRequest
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.CallbackRegistry
import androidx.databinding.DataBindingUtil
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weather.POJO.ModelClass
import com.example.weather.Utilities.ApiUtilites
import com.example.weather.databinding.ActivityMainBinding
import com.google.android.gms.common.api.internal.ApiKey
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import java.math.RoundingMode
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.logging.SimpleFormatter
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding=DataBindingUtil.setContentView(this,R.layout.activity_main)
        supportActionBar?.hide()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        activityMainBinding.cityFinder.visibility=View.GONE
        getcurrentLocation();
        activityMainBinding.etGetCityName.setOnEditorActionListener{v,actionId,keyEvent ->
            if (actionId==EditorInfo.IME_ACTION_SEARCH)
            {
                getCityWeather(activityMainBinding.etGetCityName.text.toString())
                val view=this.currentFocus
                if(view!=null)
                {
                    val imn:InputMethodManager=getSystemService(INPUT_METHOD_SERVICE)as InputMethodManager
                    imn.hideSoftInputFromWindow(view.windowToken,0)
                    activityMainBinding.etGetCityName.clearFocus()
                }
                true
            }
            else false
        }
    }

    private fun getCityWeather(cityName: String) {
        activityMainBinding.pbLoading.visibility=View.VISIBLE
        ApiUtilites.getApiInterface()?.getCityWeather(cityName, API_KEY)?.enqueue(object :Callback<ModelClass>
        {
            override fun onResponse(
                call: Call<ModelClass>,
                response: retrofit2.Response<ModelClass>
            ) {
              setDataOnViews(response.body())
            }

            override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                Toast.makeText(applicationContext,"Not a Vaild City Name",Toast.LENGTH_SHORT).show()

            }

        })

    }

    private fun getcurrentLocation(){
        if(checkPermissions()){
            if(isLocationEnable())
            {
                //final latitude and longitude code here
                if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)
                    !=PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions()
                    return
                }

                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this){
                    task ->
                    val location : Location?=task.result
                    if (location==null)
                    {
                        Toast.makeText(this,"Null Recieved",Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        fetchCurrentLocationWeather(location.latitude.toString(),location.longitude.toString())
                    }
                }
            }
            else
            {
                //setting open here
                Toast.makeText(this,"Turn on location",Toast.LENGTH_SHORT).show()
                val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else{
            //request permission here
            requestPermissions()
        }
    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {

        activityMainBinding.pbLoading.visibility=View.VISIBLE
        ApiUtilites.getApiInterface()?.getCurrentWeatherData(latitude,longitude,API_KEY)?.enqueue(object:
            Callback<ModelClass>{
            override fun onResponse(call: Call<ModelClass>,response: retrofit2.Response<ModelClass>)
            {
                if(response.isSuccessful)
                {
                    setDataOnViews(response.body())
                }
            }

            override fun onFailure(call: Call<ModelClass>, t: Throwable) {
                Toast.makeText(applicationContext,"ERROR",Toast.LENGTH_SHORT).show()
            }

        })

    }

    private fun setDataOnViews(body: ModelClass?) {

        val sdf=SimpleDateFormat("dd/MM/yyyy hh:mm")
        val currentDate=sdf.format(Date())
        activityMainBinding.tvDateAndTime.text=currentDate
        activityMainBinding.tvDayMaxTemp.text="Day"+kelvinToCelsius(body!!.main.temp_max) +""
        activityMainBinding.tvDayMinTemp.text="Night"+kelvinToCelsius(body!!.main.temp_min) +""
        activityMainBinding.tvTemp.text=""+kelvinToCelsius(body!!.main.temp) +""
        activityMainBinding.tvFeelsLike.text=""+kelvinToCelsius(body!!.main.feels_like) +""
        activityMainBinding.tvWeatherType.text=body.weather[0].main
        activityMainBinding.tvSunrise.text= timeStamToLocalDate(body.sys.sunrise.toLong())
        activityMainBinding.tvSunset.text= timeStamToLocalDate(body.sys.sunset.toLong())
        activityMainBinding.tvPressure.text= body.main.pressure.toString()
        activityMainBinding.tvHumidity.text= body.main.humidity.toString()+"%"
        activityMainBinding.tvWindSpeed.text=body.wind.speed.toString()+"n/s"
        activityMainBinding.tvTempFarenhite.text=""+((kelvinToCelsius(body.main.temp)).times(1.8).plus(32).roundToInt())
        activityMainBinding.etGetCityName.setText(body.name)

        updateUI(body.weather[0].id)
    }

    private fun updateUI(id: Int) {
        if(id in 200..232){
            //thunderstone
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor=resources.getColor(R.color.thunderstorm)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.thunderstorm))
            activityMainBinding.SubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.thunderstrom_bg
            )
            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.thunderstrom_bg
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.thunderstrom_bg
            )
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.thunderstrom_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.thunderstrom)

        }
        else if(id in 300..321){
            //drizzle
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor=resources.getColor(R.color.drizzle)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.drizzle))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.drizzle_bg
            )
            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.drizzle_bg
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.drizzle_bg
            )
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.drizzle_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.drizzle)
        }
        else if(id in 500..531){
            //rain
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor=resources.getColor(R.color.rain)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.rain))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.rainy_bg
            )
            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.rainy_bg
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.rainy_bg
            )
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.rainy_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.rain)
        }
        else if (id in 600..620)run run {
            //snow
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = resources.getColor(R.color.snow)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.snow))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.snow_bg
            )
            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.snow_bg
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.snow_bg
            )
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.snow_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.snow)
        }
        else if (id in 701..781){
            //mist  color atmosphere
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor=resources.getColor(R.color.atmosphere)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.atmosphere))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.mist_bg
            )
            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.mist_bg
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.mist_bg
            )
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.mist_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.atmosphere)
        }
        else if (id == 800){
            //clear
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor=resources.getColor(R.color.rain)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.clear))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clear_bg
            )
            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clear_bg
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clear_bg
            )
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.clear_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.clear)
        }
        else{
            //clouds
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor=resources.getColor(R.color.clouds)
            activityMainBinding.rlToolbar.setBackgroundColor(resources.getColor(R.color.clouds))
            activityMainBinding.rlSubLayout.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clouds_bg
            )
            activityMainBinding.llMainBgBelow.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clouds_bg
            )
            activityMainBinding.llMainBgAbove.background = ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.clouds_bg
            )
            activityMainBinding.ivWeatherBg.setImageResource(R.drawable.clouds_bg)
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.clouds)
        }
        activityMainBinding.pbLoading.visibility=View.GONE
        activityMainBinding.rlMainLayout.visibility=View.VISIBLE
    }

    private fun timeStamToLocalDate(timestamp: Long):String{
        val localTime = timestamp.let {
            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }
        return localTime.toString()
    }


    private fun kelvinToCelsius(temp: Double): Double {
        var intTemp = temp
        intTemp = intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1,RoundingMode.UP).toDouble()
    }


    private fun isLocationEnable():Boolean{
        val locationManager:LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    companion object{
        private const val PERMISSION_REQUEST_ACCESS_LOCATION=100
        const val API_KEY = "dab3af44de7d24ae7ff86549334e45bd"
    }
    private fun checkPermissions():Boolean {
        if(ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                ==PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED)
        {
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode== PERMISSION_REQUEST_ACCESS_LOCATION)
        {
            if(grantResults.isNotEmpty()&& grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(applicationContext,"Denied",Toast.LENGTH_SHORT).show()
            }
        }
    }

}