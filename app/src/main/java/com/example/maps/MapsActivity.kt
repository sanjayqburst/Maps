package com.example.maps

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.maps.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.Marker
import java.lang.Exception

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation:Location
    private lateinit var locationCallback:LocationCallback
    private lateinit var locationRequest:LocationRequest
    private var locationUpdateState=false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient=LocationServices.getFusedLocationProviderClient(this)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        locationCallback= object:LocationCallback(){
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                lastLocation=p0.lastLocation
                placeLocationMarker(LatLng(lastLocation.latitude,lastLocation.longitude))
            }
        }
        createLocationRequest()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        fusedLocationClient=LocationServices.getFusedLocationProviderClient(this)
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        mMap.uiSettings.isZoomControlsEnabled=true
        mMap.setOnMarkerClickListener(this)
        setUpMap()
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode== REQUEST_CHECK_SETTINGS){
            if (resultCode== RESULT_OK){
                locationUpdateState=true
                startLocationUpdate()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if (!locationUpdateState){
            startLocationUpdate()
        }
    }
    companion object{
        private const val LOCATION_PERMISSION_REQUEST_CODE=1
        private const val TAG="MAPS"
        private const val REQUEST_CHECK_SETTINGS=2
    }
    private fun setUpMap(){
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        }
        mMap.isMyLocationEnabled=true
        fusedLocationClient.lastLocation.addOnSuccessListener {location->
            if (location!=null){
                lastLocation=location
                val currentLatLng=LatLng(lastLocation.latitude,lastLocation.longitude)
                placeLocationMarker(currentLatLng)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,12f))
            }
        }
    }
    private fun placeLocationMarker(location: LatLng){
        val markerOptions=MarkerOptions().position(location)
        val address=geoCoding(location)
        markerOptions.title(address)
        mMap.addMarker(markerOptions)

    }
    private fun geoCoding(latLng: LatLng):String{
        val geocoder:Geocoder= Geocoder(this)
        val addresses:List<Address>?
        val address:Address?
        var addressText=""
        try {
            addresses=geocoder.getFromLocation(latLng.latitude,latLng.longitude,1)
            if (null!=addresses && addresses.isNotEmpty()){
                address=addresses[0]
                for (i in 0 until address.maxAddressLineIndex){
                    addressText+=if (i==0) address.getAddressLine(i) else "\n"+address.getAddressLine(i)
                }
            }
        }catch (e:Exception){
            Log.d(TAG, "Error ${e.localizedMessage}")
        }
        return addressText
    }
    private fun startLocationUpdate(){
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_CHECK_SETTINGS)
        }
        Looper.myLooper()?.let {
            fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,it)
        }

    }
    private fun createLocationRequest(){
        locationRequest= LocationRequest.create()
        locationRequest.interval=5000
        locationRequest.fastestInterval=1000
        locationRequest.priority=LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder=LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client=LocationServices.getSettingsClient(this)
        val task=client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            locationUpdateState=true
            startLocationUpdate()
        }
        task.addOnFailureListener {
            e->
            if (e is ResolvableApiException){
                try {
                    e.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                }catch (sendEx:IntentSender.SendIntentException){
                    Log.d(TAG,"Error : ${sendEx.localizedMessage}")
                }
            }
        }
    }
}