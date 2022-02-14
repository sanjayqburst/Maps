package com.example.maps

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import java.io.IOException
import java.util.*

class ReverseGeocodingActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reverse_geocoding)

        val ai: ApplicationInfo = applicationContext.packageManager.getApplicationInfo(
            applicationContext.packageName,
            PackageManager.GET_META_DATA
        )
        val value = ai.metaData["com.google.android.geo.API_KEY"]
        val apiKey = value.toString()

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        val india = LatLng(28.673, 77.211)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(india, 17f))
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setPadding(0, 0, 0, 300)
        mMap.setOnCameraIdleListener {
            val lat = mMap.cameraPosition.target.latitude
            val lng = mMap.cameraPosition.target.longitude
            val addressTv = findViewById<TextView>(R.id.tv)
            val mGeocoder = Geocoder(applicationContext, Locale.getDefault())
            var addressString = ""
            try {
                val addressList: List<Address> = mGeocoder.getFromLocation(lat, lng, 1)
                if (addressList.isNotEmpty()) {
                    val address = addressList[0]
                    val sb = StringBuilder()
                    for (i in 0 until address.maxAddressLineIndex) {
                        sb.append(address.getAddressLine(i)).append("\n")
                    }
                    if (address.premises != null) {
                        sb.append(address.premises).append(", ")
                    }
                    sb.append(address.subAdminArea).append("\n")
                    sb.append(address.locality).append(", ")
                    sb.append(address.adminArea).append("\n")
                    sb.append(address.countryName).append(", ")
                    sb.append(address.postalCode)
                    addressString = sb.toString()

                }
            } catch (e: IOException) {
                Toast.makeText(this, getString(R.string.connection_error), Toast.LENGTH_SHORT)
                    .show()
            }
            addressTv.text =
                getString(R.string.lat_lang_text, lat.toString(), lng.toString(), addressString)
        }
    }
}