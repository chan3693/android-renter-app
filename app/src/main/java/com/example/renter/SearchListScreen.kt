package com.example.renter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.renter.adapters.ListingsAdapter
import com.example.renter.databinding.ActivitySearchListScreenBinding
import com.example.renter.models.LandlordProfile
import com.example.renter.models.Property
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

class SearchListScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySearchListScreenBinding
    private lateinit var auth: FirebaseAuth
    private var propertyList:MutableList<Property> = mutableListOf()
    private lateinit var adapter: ListingsAdapter
    private val db = Firebase.firestore

    val onClick = {
        rowNumber:Int ->
        val intent = Intent(this@SearchListScreen, PropertyDetailsScreen::class.java)
        intent.putExtra("PROPERTY_DETAILS", propertyList[rowNumber])
        startActivity(intent)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchListScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.myToolbar)

        // -----------------------
        auth = Firebase.auth

        adapter = ListingsAdapter(propertyList, onClick)
        binding.rv.adapter = adapter
        binding.rv.layoutManager = LinearLayoutManager(this)
        binding.rv.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
            )
        )

        if (auth.currentUser != null){
            loadUserData()
        }

        loadLandlordData()

        binding.btnSearch.setOnClickListener {
            val maxPriceFromUI = binding.etPriceFilter.text.toString()
            if (maxPriceFromUI.isEmpty()) {
                val snackbar = Snackbar.make(binding.root,
                    "Please Enter maximum price!", Snackbar.LENGTH_LONG)
                snackbar.show()
            } else {
                loadLandlordData(maxPriceFromUI.toDouble())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("TESTING", "onResume() executing....")
        loadLandlordData()
        setSupportActionBar(binding.myToolbar)
        if (auth.currentUser != null){
            loadUserData()
        }
    }
    override fun onPause() {
        super.onPause()
        Log.d("TESTING", "SearchListScreen onPause() executing....")
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

    // retrieve all landlords
    fun loadLandlordData(maxPrice: Double? = null) {
        db.collection("LandlordProfiles")
            .get()
            .addOnSuccessListener {
                    result: QuerySnapshot ->
                val landlordListFromDB:MutableList<LandlordProfile> = mutableListOf()
                for (document: QueryDocumentSnapshot in result) {
                    val landlordProfile: LandlordProfile = document.toObject(LandlordProfile::class.java)
                    landlordListFromDB.add(landlordProfile)
                }
                Log.d("TESTING", landlordListFromDB.toString())
                loadPropertyData(landlordListFromDB, maxPrice)
            }
            .addOnFailureListener {
                    exception ->
                Log.d("TESTING", "Error retrieving data", exception)
            }
    }

    // retrieve landlord's properties
    fun loadPropertyData(landlordList:List<LandlordProfile>, maxPrice: Double? = null) {
        // all property list
        val propertyListFromDB:MutableList<Property> = mutableListOf()
        // task list
        val tasks = mutableListOf<Task<QuerySnapshot>>()
        if (maxPrice != null) {
            for (landlordId in landlordList){
                val task = db.collection("LandlordProfiles")
                    .document(landlordId.id)
                    .collection("Properties")
                    .whereEqualTo("isAvailable", true)
                    .whereLessThanOrEqualTo("monthlyRentalPrice", maxPrice)
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
            for (landlordId in landlordList){
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

        // when all tasks completed add to task list
        Tasks.whenAllSuccess<QuerySnapshot>(tasks)
            .addOnSuccessListener {
                // update adapter
                propertyList.clear()
                propertyList.addAll(propertyListFromDB)
                adapter.notifyDataSetChanged()
                Log.d("TESTING", "All property list retrieved")
            }.addOnFailureListener {
                    exception ->
                Log.d("TESTING", "Error retrieving data", exception)
            }
    }
    // option menu handler
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_items, menu)
        menu.findItem(R.id.mi_option_list).isVisible = false
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
                val intent = Intent(this@SearchListScreen, SearchMapScreen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                true
            }
            R.id.mi_option_watchList -> {
                if (auth.currentUser != null){
                    val intent = Intent(this@SearchListScreen, WatchListScreen::class.java)
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
                val intent = Intent(this@SearchListScreen, LoginScreen::class.java)
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
}