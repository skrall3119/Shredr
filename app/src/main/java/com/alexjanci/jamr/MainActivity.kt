package com.alexjanci.jamr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.replace
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    lateinit var toolbar: ActionBar
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest

    private val PERMISSION_REQUEST = 1000

    val auth = FirebaseAuth.getInstance()
    val store = FirebaseFirestore.getInstance()


    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when(item.itemId){
            R.id.navigation_profile -> {
                toolbar.title = "My Profile"
                val profileFragment = Profile.newInstance()
                openFragment(profileFragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_main -> {
                toolbar.title = "Discover"
                val mainFragment = MainFragment.newInstance()
                openFragment(mainFragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_messages -> {
                toolbar.title = "Messages"
                val messages = Messages.newInstance()
                openFragment(messages)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser == null) {
            startActivity(Intent(this, SignIn::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar))

        toolbar = supportActionBar!!
        val bottomNavigation: BottomNavigationView = navigation_view
        bottomNavigation.selectedItemId = R.id.navigation_main
        bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        val transaction = supportFragmentManager.beginTransaction()
        val defaultFragment = MainFragment.newInstance()

        toolbar.title = "Discover"

        if (savedInstanceState == null) {
            transaction.replace(R.id.container, defaultFragment)
            transaction.commit()
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation()
    }


    private fun getLastLocation(){
        if(checkPermission()){
            if(isLocationEnabled()){
                fusedLocationProviderClient.lastLocation.addOnCompleteListener {
                    if (!it.isSuccessful) return@addOnCompleteListener
                    val location = it.result
                    if(location == null){
                        getNewLocation()
                    } else {
                        saveLocationToDatabase(location)
                    }
                }
            } else {
                Toast.makeText(this, "Please enable your location service", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestPermissions()
        }
    }

    private fun getNewLocation(){
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 2
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.myLooper()
        )

    }

    private val locationCallback = object: LocationCallback(){
        override fun onLocationResult(p0: LocationResult?) {
            val lastLocation = p0?.lastLocation ?: return
            saveLocationToDatabase(lastLocation)
        }
    }

    private fun saveLocationToDatabase(location: Location){
        val uid = auth.uid ?: return
        Thread {
            val city = try {
                Geocoder(this, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()?.locality
            } catch (e: Exception) { null }
            if (city.isNullOrBlank()) return@Thread
            runOnUiThread {
                if (!isFinishing && auth.uid == uid) {
                    store.collection("users").document(uid).set(mapOf("city" to city), SetOptions.merge())
                        .addOnFailureListener { Log.w("Shredr", "Could not save city", it) }
                }
            }
        }.start()
    }

    override fun onStop() {
        if (::fusedLocationProviderClient.isInitialized) fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_REQUEST){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getLastLocation()
            }
        }
    }

    private fun isLocationEnabled(): Boolean{
        return true
    }

    private fun requestPermissions(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), PERMISSION_REQUEST
        )
    }

    private fun checkPermission(): Boolean{
        if(
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ){
            return true
        }
        return false
    }

    fun openFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(container.id, fragment)
        transaction.commit()
    }
}
