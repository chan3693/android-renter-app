package com.example.renter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.renter.databinding.ActivitySearchMapScreenBinding
import com.example.renter.models.LandlordProfile
import com.example.renter.models.Property
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SearchMapScreen : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {
    private lateinit var binding: ActivitySearchMapScreenBinding
    private lateinit var auth: FirebaseAuth
//    private var propertyList:MutableList<Property> = mutableListOf()
    private val db = Firebase.firestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // GoogleMap
    private lateinit var map: GoogleMap
    private val locationPermissionCode = 101
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchMapScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        auth = Firebase.auth


        checkLocationPermission()

        binding.btnSearch.setOnClickListener {
            val maxPriceFromUI = binding.etPriceFilter.text.toString()
            if (maxPriceFromUI.isEmpty()) {
                val snackbar = Snackbar.make(binding.root, "Please Enter maxmum price!", Snackbar.LENGTH_LONG)
                snackbar.show()
            } else {
                loadLandlordData(maxPriceFromUI.toDouble())
            }
        }
    }

//     refresh data
    override fun onResume() {
        super.onResume()
        Log.d("TESTING", "onResume() executing....")
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setSupportActionBar(binding.myToolbar)
        if (auth.currentUser != null){
            loadUserData()
        }
    }
    override fun onPause() {
        super.onPause()
        Log.d("TESTING", "SearchMapScreen onPause() executing....")
    }

    fun loadUserData() {
        val uid = auth.currentUser?.uid.toString()
        db.collection("RenterProfiles")
            .document(uid)
            .get()
            .addOnSuccessListener {
                    document: DocumentSnapshot ->
                val profileData: LandlordProfile? = document.toObject(LandlordProfile::class.java)
                if (profileData == null) {
                    Log.d("TESTING", "No matching user profile found")
                    return@addOnSuccessListener
                }
                Log.d("TESTING", profileData.toString())
                binding.myToolbar.setTitle(auth.currentUser?.email)

            }.addOnFailureListener {
                    exception ->
                Log.w("TESTING", "Error getting user profile", exception)
            }
    }
    // googleMap handler
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("TESTING", "Permissions granted")
                    return
                }
                map.isMyLocationEnabled = true
            } else {
                // Handle the case where permission is denied.
                // show a dialog explaining why the permission is needed and how to enable it.
                Log.d("TESTING", "Permissions denied")
            }
        }
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
        map = googleMap

        // Enable zoom controls on the map
        map.uiSettings.isZoomControlsEnabled = true

        // Check if permission is granted (considering that permission might have been denied)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            //  move the camera to current location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val cameraLatLng = LatLng(location.latitude, location.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(cameraLatLng, 10.0f))
                }
            }
        }

        map.setOnInfoWindowClickListener(this)

        loadLandlordData()
    }

    override fun onInfoWindowClick(p0: Marker) {
        Log.d("TESTING", "on clock mark:${p0}")
        val property = p0.tag as Property
        val intent = Intent(this@SearchMapScreen, PropertyDetailsScreen::class.java)
        intent.putExtra("PROPERTY_DETAILS", property)
        startActivity(intent)
    }

    // option menu handler
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_items, menu)
        menu.findItem(R.id.mi_option_map).isVisible = false
        if (auth.currentUser == null) {
            menu.findItem(R.id.mi_option_Logout).isVisible = false
        } else {
            menu.findItem(R.id.mi_option_Login).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mi_option_list -> {
                val intent = Intent(this@SearchMapScreen, SearchListScreen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                true
            }
            R.id.mi_option_watchList -> {
                if (auth.currentUser != null){
                    val intent = Intent(this@SearchMapScreen, WatchListScreen::class.java)
                    startActivity(intent)
                } else {
                    Snackbar.make(binding.root,
                        "Only logged in users be able to access watch list screen",
                        Snackbar.LENGTH_LONG)
                        .show()
                }
                true
            }
            R.id.mi_option_Login -> {
                val intent = Intent(this@SearchMapScreen, LoginScreen::class.java)
                startActivity(intent)
                true
            }
            R.id.mi_option_Logout -> {
                auth.signOut()
                recreate()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // retrieve all landlords
    fun loadLandlordData(maxPrice: Double? = null) {
        db.collection("LandlordProfiles")
            .get()
            .addOnSuccessListener {
                    result: QuerySnapshot ->
                val landlordProfileListFromDB:MutableList<LandlordProfile> = mutableListOf()
                for (document: QueryDocumentSnapshot in result) {
                    val landlordProfile = document.toObject(LandlordProfile::class.java)
                    landlordProfileListFromDB.add(landlordProfile)
                }
                Log.d("TESTING", landlordProfileListFromDB.toString())
                loadPropertyData(landlordProfileListFromDB, maxPrice)
            }
            .addOnFailureListener {
                    exception ->
                Log.d("TESTING", "Error retrieving data", exception)
            }
    }

    // retrieve landlord's properties
    fun loadPropertyData(landlordProfileList:List<LandlordProfile>, maxPrice: Double? = null) {
        // all property list
        val propertyListFromDB:MutableList<Property> = mutableListOf()
        // task list
        val tasks = mutableListOf<Task<QuerySnapshot>>()
        if (maxPrice != null) {
            for (landlordId in landlordProfileList){
                val task = db.collection("LandlordProfiles")
                    .document(landlordId.id)
                    .collection("Properties")
                    .whereLessThanOrEqualTo("monthlyRentalPrice", maxPrice)
                    .whereEqualTo("isAvailable", true)
                    .get()
                    .addOnSuccessListener {
                            result: QuerySnapshot ->
                        for (document: QueryDocumentSnapshot in result) {
                            val property: Property = document.toObject(Property::class.java)
                            propertyListFromDB.add(property)
                        }
                        Log.d("TESTING", propertyListFromDB.toString())
                    }
                    .addOnFailureListener {
                            exception ->
                        Log.d("TESTING", "Error retrieving data", exception)
                    }
                //add task to task list
                tasks.add(task)
            }
        } else {
            for (landlordId in landlordProfileList){
                val task = db.collection("LandlordProfiles")
                    .document(landlordId.id)
                    .collection("Properties")
                    .whereEqualTo("isAvailable", true)
                    .get()
                    .addOnSuccessListener {
                            result: QuerySnapshot ->
                        for (document: QueryDocumentSnapshot in result) {
                            val property: Property = document.toObject(Property::class.java)
                            propertyListFromDB.add(property)
                        }
                        Log.d("TESTING", propertyListFromDB.toString())
                    }
                    .addOnFailureListener {
                            exception ->
                        Log.d("TESTING", "Error retrieving data", exception)
                    }
                //add task to task list
                tasks.add(task)
            }
        }

        // when all tasks completed add markers googleMap
        Tasks.whenAllSuccess<QuerySnapshot>(tasks)
            .addOnSuccessListener {
                Log.d("TESTING","propertyListFromDB.count:${propertyListFromDB.count()}")
                Log.d("TESTING", "All properties retrieved")
                // clear markers
                map.clear()
                // add markers
                for (property: Property in propertyListFromDB) {

                    val latLng = LatLng(property.latitude, property.longitude)
                    val price = "${property.monthlyRentalPrice}$"
                    val marker = map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(property.address)
                            .snippet(price)
//            .icon(BitmapDescriptorFactory.fromResource(R.drawable.location))
                    )
                    marker?.tag = property
                }
            }.addOnFailureListener {
                    exception ->
                Log.d("TESTING", "Error retrieving data", exception)
            }
    }
}