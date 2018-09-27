package com.saad.nearbymosques.view.activities

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View

import android.widget.Toast
import java.net.URL
import org.jetbrains.anko.async
import org.jetbrains.anko.uiThread
import com.beust.klaxon.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.saad.nearbymosques.R
import org.jetbrains.anko.toast

class MapsActivity : AppCompatActivity(), OnMapReadyCallback{

    private var mLocationRequest:LocationRequest? = null
    private lateinit var mMap: GoogleMap
    private  var currentLatitude : Double?=null
    private  var currentLongitude: Double?=null
    private var lat:Double = 0.0
    private var lng:Double = 0.0
    private var desLat:Double = 0.0
    private var desLng:Double = 0.0
    var locat:Location? = null
    private var mLastKnownLocation: Location? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var mPermissionDenied = false
    private var mCameraPosition: CameraPosition? = null
    private val KEY_CAMERA_POSITION = "camera_position"
    var mGoogleApiClient: GoogleApiClient? = null
    private val mDefaultLocation = LatLng(-33.8523341, 151.2106085)
    private var destinationName:String?= null
    private var currentLocation:String?=null
    private val DEFAULT_ZOOM = 15
    private val KEY_LOCATION = "location"
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationManager: LocationManager? = null
    private var lastActivityData: Bundle? = null
    var floatingActionButton:FloatingActionButton?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        getLastActivityInfo()
        floatingActionButton = findViewById<FloatingActionButton>(R.id.fab)
        floatingActionButton!!.setOnClickListener(View.OnClickListener {
            drawMap()
        })
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if(mMap != null) {
            //            updateLocationUI();
//            mMap.isMyLocationEnabled = true
//            mMap.uiSettings.isMyLocationButtonEnabled = true
//            getDeviceLocation()
//            mMap.isMyLocationEnabled = true

            lat = lastActivityData!!.getDouble("point_one_lat")
            lng = lastActivityData!!.getDouble("point_one_lon")
            desLat = lastActivityData!!.getDouble("point_two_lat")
            desLng = lastActivityData!!.getDouble("point_two_lon")
            destinationName = lastActivityData!!.getString("point_location_two_title")
            currentLocation = lastActivityData!!.getString("point_location_one_title")
            val opera = LatLng(desLat, desLng)
            val LatLongB = LatLngBounds.Builder()
            LatLongB.include(opera)
            val bounds = LatLongB.build()
            mMap.addMarker(MarkerOptions().position(opera).title(destinationName))
            mMap.setOnMapLoadedCallback(object:GoogleMap.OnMapLoadedCallback {
                override fun onMapLoaded() {
//                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(opera,15.5f),2000,null)
                    floatingActionButton!!.visibility = View.VISIBLE
                }
            })
        }
    }

    private fun getLastActivityInfo() {
        lastActivityData = intent.extras
    }
    fun drawMap(){
        val LatLongB = LatLngBounds.Builder()
        val sydney = LatLng(lat, lng)
//        val opera = LatLng(lastActivityData!!.getDouble("point_two_lat"), lastActivityData!!.getDouble("point_two_lon"))
        val opera = LatLng(desLat, desLng)
        mMap.addMarker(MarkerOptions().position(sydney).title(currentLocation))
        mMap.addMarker(MarkerOptions().position(opera).title(destinationName))

        // Declare polyline object and set up color and width
        val options = PolylineOptions()
        options.color(Color.BLUE)
        options.width(5f)

        // build URL to call API
        val url = getURL(sydney, opera)

        async {
            // Connect to URL, download content and convert into string asynchronously
            val result = URL(url).readText()
            uiThread {
                // When API call is done, create parser and convert into JsonObjec
                val parser: Parser = Parser()
                val stringBuilder: StringBuilder = StringBuilder(result)
                val json: JsonObject = parser.parse(stringBuilder) as JsonObject
                // get to the correct element in JsonObject
                val routes = json.array<JsonObject>("routes")
                val points = routes!!["legs"]["steps"][0] as JsonArray<JsonObject>
                // For every element in the JsonArray, decode the polyline string and pass all points to a List
                val polypts = points.flatMap { decodePoly(it.obj("polyline")?.string("points")!!) }
                // Add  points to polyline and bounds
                options.add(sydney)
                LatLongB.include(sydney)
                for (point in polypts) {
                    options.add(point)
                    LatLongB.include(point)
                }
                options.add(opera)
                LatLongB.include(opera)
                // build bounds
                val bounds = LatLongB.build()
                // add polyline to the map
                mMap!!.addPolyline(options)
                // show map with route centered
//                mMap!!.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,50),2000,null)
            }
        }

    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private fun enableMyLocation() {
         if (mMap != null) {

            val LatLongB = LatLngBounds.Builder()
          val  sydney = LatLng(32.0, 75.0)
          val  opera = LatLng(31.458394, 74.272632)

            mMap!!.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
            mMap!!.addMarker(MarkerOptions().position(opera).title("Opera House"))
            val options = PolylineOptions()
            options.color(Color.RED)
            options.width(5f)
            val url = getURL(sydney, opera)

            async {
                // Connect to URL, download content and convert into string asynchronously
                val result = URL(url).readText()
                uiThread {
                    // When API call is done, create parser and convert into JsonObjec
                    val parser: Parser = Parser()
                    val stringBuilder: StringBuilder = StringBuilder(result)
                    val json: JsonObject = parser.parse(stringBuilder) as JsonObject
                    // get to the correct element in JsonObject
                    val routes = json.array<JsonObject>("routes")
                    val points = routes!!["legs"]["steps"][0] as JsonArray<JsonObject>
                    // For every element in the JsonArray, decode the polyline string and pass all points to a List
                    val polypts = points.flatMap { decodePoly(it.obj("polyline")?.string("points")!!) }
                    // Add  points to polyline and bounds
                    options.add(sydney)
                    LatLongB.include(sydney)
                    for (point in polypts) {
                        options.add(point)
                        LatLongB.include(point)
                    }
                    options.add(opera)
                    LatLongB.include(opera)
                    // build bounds
                    val bounds = LatLongB.build()
                    // add polyline to the map
                    mMap!!.addPolyline(options)
                    // show map with route centered
                    mMap!!.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                }
            }

        }

        //    mMap.isMyLocationEnabled = true

    }

    @SuppressLint("MissingPermission")

    private fun getURL(from: LatLng, to: LatLng): String {
        val origin = "origin=" + from.latitude + "," + from.longitude
        val dest = "destination=" + to.latitude + "," + to.longitude
        val sensor = "sensor=false"
        val params = "$origin&$dest&$sensor"
        return "https://maps.googleapis.com/maps/api/directions/json?$params"
    }



    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5,
                    lng.toDouble() / 1E5)
            poly.add(p)
        }

        return poly
    }




    private fun displayGpsStatus(): Boolean {
        val contentResolver = baseContext
                .contentResolver
        val gpsStatus = Settings.Secure
                .isLocationProviderEnabled(contentResolver,
                        LocationManager.GPS_PROVIDER)
        return if (gpsStatus) {
            true

        } else {
            false
        }
    }



}
