package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.services.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest.create
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat


class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var binding:ActivityMainBinding? = null
    private lateinit var storePreference:SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        storePreference = getSharedPreferences(Constant.STORE_OBJECT_DATA, MODE_PRIVATE)
        setupUI()
        setupDexterCurrentLocation()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.load_location,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.iLoad -> {
                Toast.makeText(this,"Load Data",Toast.LENGTH_SHORT).show()
                requestNewLocationData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }


    private fun setupDexterCurrentLocation() {
        if(isEnableLocation()){
            Dexter.withContext(this).withPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ).withListener(object: MultiplePermissionsListener{
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    if(p0!!.areAllPermissionsGranted()){
                        requestNewLocationData()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    showPermissionRationale()
                }

            }).check()
        }else{
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }



    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = create()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mLocationRequest.priority = LocationRequest.QUALITY_HIGH_ACCURACY
        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.requestLocationUpdates(mLocationRequest, object : LocationCallback(){
            override fun onLocationResult(p0: LocationResult) {
                val lastLocation = p0.lastLocation
                val long = lastLocation.longitude
                val lat = lastLocation.latitude
                Log.e("---",long.toString())
                if(Constant.isInternetAvailable(this@MainActivity)){
                    val retrofit = Retrofit.Builder()
                        .baseUrl(Constant.BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val weatherService: WeatherService = retrofit.create(WeatherService::class.java)
                    weatherService.getObjectWeather(lat = lat, long = long, api = Constant.API_KEY, unit = Constant.Units)
                        .enqueue(object: Callback<WeatherResponse>{
                            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                                if(response.isSuccessful){
                                    val edit = storePreference.edit()
                                    edit.putString(Constant.STORE_OBJECT_KEY,Gson().toJson(response.body()))
                                    edit.apply()
                                    setupUI()
                                }else{
                                    Log.e("---", response.code().toString())
                                }
                            }

                            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                                Log.e("---","Error")
                            }

                        })
                }else{
                    Toast.makeText(this@MainActivity,"disconnected",Toast.LENGTH_SHORT).show()
                }
            }
        }, Looper.myLooper()!!)
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        val json = storePreference.getString(Constant.STORE_OBJECT_KEY,"")
        if(!json.isNullOrEmpty()){
            val body = Gson().fromJson(json,WeatherResponse::class.java)
            when(body!!.weather[0].icon){
                "01d" -> binding?.ivWeather?.setImageResource(R.drawable.sunny)
                "02d" -> binding?.ivWeather?.setImageResource(R.drawable.cloud)
                "03d" -> binding?.ivWeather?.setImageResource(R.drawable.cloud)
                "04d" -> binding?.ivWeather?.setImageResource(R.drawable.cloud)
                "04n" -> binding?.ivWeather?.setImageResource(R.drawable.cloud)
                "10d" -> binding?.ivWeather?.setImageResource(R.drawable.rain)
                "11d" -> binding?.ivWeather?.setImageResource(R.drawable.storm)
                "13d" -> binding?.ivWeather?.setImageResource(R.drawable.snowflake)
                "01n" -> binding?.ivWeather?.setImageResource(R.drawable.cloud)
                "02n" -> binding?.ivWeather?.setImageResource(R.drawable.cloud)
                "03n" -> binding?.ivWeather?.setImageResource(R.drawable.cloud)
                "10n" -> binding?.ivWeather?.setImageResource(R.drawable.cloud)
                "11n" -> binding?.ivWeather?.setImageResource(R.drawable.rain)
                "13n" -> binding?.ivWeather?.setImageResource(R.drawable.snowflake)
            }
            binding?.tvMain?.text = body.weather[0].main
            binding?.tvMainDescription?.text = body.weather[0].description
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding?.tvTemp?.text = body.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
            }
            binding?.tvHumidity?.text = body.main.humidity.toString() + " per cent"
            binding?.tvMin?.text = body.main.temp_min.toString()
            binding?.tvMax?.text = body.main.temp_max.toString()
            binding?.tvSpeed?.text = body.wind.speed.toString()
            binding?.tvCity?.text = body.name
            binding?.tvCountry?.text = body.sys.country
            binding?.tvSunRise?.text = this.convertLongToTime(body.sys.sunrise * 1000)
            binding?.tvSunSet?.text = this.convertLongToTime(body.sys.sunset * 1000)
        }
    }


    @SuppressLint("SimpleDateFormat")
    fun convertLongToTime(time:Long):String{
        return SimpleDateFormat("HH:mm").format(time)
    }


    private fun getUnit(value: String): String {
        var vl = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            vl = "°F"
        }
        return vl
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this).setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton("Go to settings"){ _,_ ->
                try{
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package",packageName,null)
                    startActivity(intent)
                }catch (e: ClassNotFoundException){
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel"){ e,_ ->
                e.dismiss()
            }.show()
    }

    private fun isEnableLocation():Boolean{
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}