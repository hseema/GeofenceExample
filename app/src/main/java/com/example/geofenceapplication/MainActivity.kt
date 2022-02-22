package com.example.geofenceapplication

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import android.location.LocationManager
import android.os.Handler
import com.google.android.gms.location.LocationServices

import com.google.android.gms.location.LocationResult

import com.google.android.gms.location.LocationCallback





class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient:FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private val geofenceIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
    private val geofenceList =ArrayList<Geofence>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val response =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this@MainActivity)
        if (response != ConnectionResult.SUCCESS) {
            Log.d(Constants.TAG, "Google Play Service Not Available")
            GoogleApiAvailability.getInstance().getErrorDialog(this@MainActivity, response, 1)
                .show()
        } else {
            Log.d("GEOFENCE_TAG", "Google play service available")
        }

        createChannel(this)

        showCurrentLocation()
        buildGeofence()
    }

    private fun buildGeofence() {
        geofencingClient = LocationServices.getGeofencingClient(this)
        val latitude = 12.8803482//12.87920443595687
        val longitude = 77.6116657//77.6099444186932
        val radius = 200f

        geofenceList.add(Geofence.Builder()
            .setRequestId("g1")
            .setCircularRegion(latitude,longitude,radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)

            .build())

        addGeofence()
    }

    //adding a geofence
    private fun addGeofence(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= 29
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                ),
                102
            )
            return
        }
        geofencingClient?.addGeofences(seekGeofencing(), geofenceIntent)?.run {
            addOnSuccessListener {
                Toast.makeText(this@MainActivity, "Geofences added", Toast.LENGTH_LONG).show()
                Log.i(Constants.TAG,"Geofences added")
                /*Handler().postDelayed({
                    fusedLocationProviderClient.setMockMode(BuildConfig.DEBUG)
                    val newLocation = Location("flp")
                    newLocation.latitude = 12.87920443595687 //12.8802782
                    newLocation.longitude = 77.6099444186932//6117777
                    newLocation.accuracy = 3.0f
                    fusedLocationProviderClient.setMockLocation(newLocation)
                        .addOnSuccessListener { Log.i(Constants.TAG, "location mocked") }
                        .addOnFailureListener { Log.i(Constants.TAG, "mock failed") }
                    Log.i(Constants.TAG,"Mock location to enter geofence")
                }, 8000)*/
            }
            addOnFailureListener {
                Toast.makeText(this@MainActivity, "Failed to add geofences", Toast.LENGTH_LONG).show()

            }
        }
    }

    //removing a geofence
    private fun removeGeofence(){
        geofencingClient?.removeGeofences(geofenceIntent)?.run {
            addOnSuccessListener {
                Toast.makeText(this@MainActivity, "Geofences removed", Toast.LENGTH_SHORT).show()

            }
            addOnFailureListener {
                Toast.makeText(this@MainActivity, "Failed to remove geofences", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun seekGeofencing(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    private fun showCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                101
            )
            return
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        /*fusedLocationProviderClient.setMockMode(BuildConfig.DEBUG)
        val newLocation = Location("flp")
        newLocation.latitude = 12.87923521799858
        newLocation.longitude = 77.61004365892433
        newLocation.accuracy = 3.0f
        fusedLocationProviderClient.setMockLocation(newLocation)
            .addOnSuccessListener {
                Log.i(Constants.TAG, "location mocked")
                Log.i(Constants.TAG,"Mock location to away from geofence")
                //Display current Location here
            }
            .addOnFailureListener { Log.i(Constants.TAG, "mock failed") }*/


        // for continuous location update
        /*val mLocationRequest = LocationRequest.create()
        mLocationRequest.interval = 60000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val mLocationCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult == null) {
                    Log.i(Constants.TAG,"Location is null")
                    return
                }
                for (location in locationResult.locations) {
                    if (location != null) {
                        //TODO: UI updates.
                        val ltext: TextView = findViewById(R.id.txtlocation)
                        ltext.text = location.latitude.toString() + " " + "\n" +
                                location.longitude
                    }
                }
            }
        }
        fusedLocationProviderClient
            .requestLocationUpdates(mLocationRequest, mLocationCallback, null)*/
        try {
            fusedLocationProviderClient!!.lastLocation.addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result != null) {
                    val location = task.result
                    val ltext: TextView = findViewById(R.id.txtlocation)
                    ltext.text = location.latitude.toString() + " " + "\n" +
                            location.longitude
                    Log.i(
                        Constants.TAG, location.latitude.toString() + " " + "\n" +
                                location.longitude
                    )
                }
            }
            fusedLocationProviderClient!!.lastLocation.addOnFailureListener {
                Log.i(Constants.TAG,"Get Location failed") }
        }catch(e:Exception){
            Log.i(Constants.TAG,e.toString())
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101) {
            if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                showCurrentLocation()
            }
        }
        else{
            addGeofence()
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeGeofence()
    }

}