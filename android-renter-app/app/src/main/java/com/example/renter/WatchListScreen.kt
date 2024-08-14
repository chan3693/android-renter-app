package com.example.renter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.renter.adapters.ListingsAdapter
import com.example.renter.adapters.WatchListAdapter
import com.example.renter.databinding.ActivityWatchListScreenBinding
import com.example.renter.models.LandlordProfile
import com.example.renter.models.Property
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class WatchListScreen : AppCompatActivity() {
    lateinit var binding: ActivityWatchListScreenBinding
    lateinit var adapter: WatchListAdapter

    lateinit var auth: FirebaseAuth
    lateinit var uid: String
    var propertyList:MutableList<Property> = mutableListOf()
    val db = Firebase.firestore
    val onClick = {
            rowNumber:Int ->
        val intent = Intent(this@WatchListScreen, PropertyDetailsScreen::class.java)
        intent.putExtra("PROPERTY_DETAILS", propertyList[rowNumber])
        intent.putExtra("KEY_IS_WATCHLIST", true)
        startActivity(intent)
    }
    val removeButtonClicked:(String)->Unit = {
            docId:String->
        db.collection("RenterProfiles")
            .document(uid)
            .collection("WatchList")
            .document(docId)
            .delete()
            .addOnSuccessListener {
                Log.d("TESTING", "Remove success!")

                loadData(uid)

            }
            .addOnFailureListener {
                    exception ->
                Log.d("TESTING", "Error when remove from WatchList subcollection")
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWatchListScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.myToolbar)

        // get logged in user id
        auth = Firebase.auth
        uid = auth.currentUser?.uid.toString()

        if (auth.currentUser != null) {
            loadUserData()
        }

        adapter = WatchListAdapter(propertyList, onClick,removeButtonClicked)
        binding.rv.adapter = adapter
        binding.rv.layoutManager = LinearLayoutManager(this)
        binding.rv.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
            )
        )

        loadData(uid)

    }
    override fun onResume() {
        super.onResume()
        Log.d("TESTING", "onResume() executing....")
        // to refresh screen and show maxprice filtered list after click button
        loadData(uid)
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

    fun loadData(uid:String) {
        db.collection("RenterProfiles")
            .document(uid)
            .collection("WatchList")
            .get()
            .addOnSuccessListener {
                    result: QuerySnapshot ->
                val propertyListFromDB:MutableList<Property> = mutableListOf()
                for (document: QueryDocumentSnapshot in result) {
                    val property:Property = document.toObject(Property::class.java)
                    propertyListFromDB.add(property)
                }
                Log.d("TESTING", propertyListFromDB.toString())
                propertyList.clear()
                propertyList.addAll(propertyListFromDB)
                if (adapter.yourListData.isEmpty()){
                    binding.tv2.isVisible = false
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                    exception ->
                Log.d("TESTING", "Error retrieving data", exception)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_items, menu)
        menu.findItem(R.id.mi_option_Login).isVisible = false
        menu.findItem(R.id.mi_option_watchList).isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mi_option_map -> {
                val intent = Intent(this@WatchListScreen, SearchMapScreen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                true
            }
            R.id.mi_option_list -> {
                val intent = Intent(this@WatchListScreen, SearchListScreen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                true
            }
            R.id.mi_option_Logout -> {
                auth.signOut()
                val intent = Intent(this@WatchListScreen, SearchMapScreen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}