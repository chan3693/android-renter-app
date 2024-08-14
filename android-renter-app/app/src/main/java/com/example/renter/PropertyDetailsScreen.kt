package com.example.renter

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.recreate
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.renter.databinding.ActivityPropertyDetailsScreenBinding
import com.example.renter.models.LandlordProfile
import com.example.renter.models.Property
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.integrity.internal.f
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import java.lang.Exception

class PropertyDetailsScreen : AppCompatActivity() {
    lateinit var binding: ActivityPropertyDetailsScreenBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPropertyDetailsScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.myToolbar)
        auth = Firebase.auth
        val uid = auth.currentUser?.uid.toString()

        if (auth.currentUser != null) {
            loadUserData()
        }

        val isWatchList = intent.getBooleanExtra("KEY_IS_WATCHLIST", false)

        if (isWatchList == true) {
            loadDetails()
            binding.btnAddToWatchList.isVisible = false
        } else {
            loadDetails()
        }

        binding.btnAddToWatchList.setOnClickListener {
            if (auth.currentUser != null) {
                // ADD TO WATCH LIST CODE
                addToWatchList(uid)
            } else {
                Snackbar.make(
                    binding.root,
                    "Only logged in users be able to add to watch list",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("TESTING", "onResume() executing....")
        loadDetails()
        setSupportActionBar(binding.myToolbar)
        if (auth.currentUser != null) {
            loadUserData()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("TESTING", "DetailsScreen onPause() executing....")
    }

    fun loadUserData() {
        val uid = auth.currentUser?.uid.toString()
        db.collection("RenterProfiles")
            .document(uid)
            .get()
            .addOnSuccessListener { document: DocumentSnapshot ->
                val profileData: LandlordProfile? = document.toObject(LandlordProfile::class.java)
                if (profileData == null) {
                    Log.d("TESTING", "No matching user profile found")
                    return@addOnSuccessListener
                }
                Log.d("TESTING", profileData.toString())
                binding.myToolbar.setTitle(auth.currentUser?.email)

            }.addOnFailureListener { exception ->
                Log.w("TESTING", "Error getting user profile", exception)
            }
    }

    fun loadDetails() {
        val currItem: Property = intent.getSerializableExtra("PROPERTY_DETAILS") as Property

        val landlordListFromDB:MutableList<LandlordProfile> = mutableListOf()

        db.collection("LandlordProfiles")
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                for (document: QueryDocumentSnapshot in result) {
                    val landlordProfile: LandlordProfile = document.toObject(LandlordProfile::class.java)
                    landlordListFromDB.add(landlordProfile)
                }
                Log.d("TESTING", landlordListFromDB.toString())
                for (landlordId in landlordListFromDB){
                    db.collection("LandlordProfiles")
                        .document(landlordId.id)
                        .collection("Properties")
                        .document(currItem.id)
                        .get()
                        .addOnSuccessListener{ document: DocumentSnapshot ->
                            val propertyData: Property? = document.toObject(Property::class.java)
                            if (propertyData == null) {
                                Log.d("TESTING", "No matching property profile found")
                                return@addOnSuccessListener
                            }
                            Log.d("TESTING", propertyData.toString())
                            binding.tvAddress.setText(propertyData.address)
                            binding.tvDetails.setText("""
                            • Monthly rental price: $${
                                            if (propertyData.monthlyRentalPrice % 1.0 == 0.0)
                                                propertyData.monthlyRentalPrice.toInt() else propertyData.monthlyRentalPrice
                                        }
                            • Rental type : ${propertyData.rentalType}
                            • Number of bedrooms: ${propertyData.numberOfBedrooms}
                            • Status: ${if (propertyData.isAvailable == true) "Available" else "Not Available"}
                            """.trimIndent()
                            )
                            Glide.with(this@PropertyDetailsScreen)
                                .load(propertyData.imageUrl)
                                .into(binding.tvImage)
                        }
                        .addOnFailureListener {
                                exception ->
                            Log.d("TESTING", "Error retrieving property data", exception)
                        }
                }
            }.addOnFailureListener {
                    exception ->
                Log.d("TESTING", "Error retrieving landlord data", exception)
            }
    }


    fun addToWatchList(uid:String){
        val currItem: Property = intent.getSerializableExtra("PROPERTY_DETAILS") as Property
        val address:String = currItem.address

        // database fields
        val data:MutableMap<String, Any> = HashMap()
        data["address"] = address


        // add to database
        db.collection("RenterProfiles")
            .document(uid)
            .collection("WatchList")
            .document(currItem.id)
            .set(data)
            .addOnSuccessListener { docRef ->
                Log.d("TESTING", " Watch Listing successfully added with ID ${currItem.id}")
            }
            .addOnFailureListener { ex ->
                Log.e("TESTING", "Exception occurred while adding listing : $ex", )
                Snackbar.make(binding.root, "ERROR TO ADD TO WATCH LIST", Snackbar.LENGTH_LONG).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_items, menu)
        if (auth.currentUser == null) {
            menu.findItem(R.id.mi_option_Logout).isVisible = false
        } else {
            menu.findItem(R.id.mi_option_Login).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mi_option_map -> {
                val intent = Intent(this@PropertyDetailsScreen, SearchMapScreen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                true
            }
            R.id.mi_option_list -> {
                val intent = Intent(this@PropertyDetailsScreen, SearchListScreen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                true
            }
            R.id.mi_option_watchList -> {
                if (auth.currentUser != null){
                    val intent = Intent(this@PropertyDetailsScreen, WatchListScreen::class.java)
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
                val intent = Intent(this@PropertyDetailsScreen, LoginScreen::class.java)
                startActivity(intent)
                true
            }
            R.id.mi_option_Logout -> {
                auth.signOut()
                val intent = Intent(this@PropertyDetailsScreen, SearchMapScreen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}