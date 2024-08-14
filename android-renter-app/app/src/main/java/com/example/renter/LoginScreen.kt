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
import com.example.renter.databinding.ActivityLoginScreenBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginScreen : AppCompatActivity() {
    private lateinit var binding: ActivityLoginScreenBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // ---------------------
        setSupportActionBar(binding.myToolbar)
        auth = Firebase.auth

        binding.btnLogin.setOnClickListener {
            val emailFromUI = binding.userEmail.text.toString()
            val passwordFromUI = binding.etPassword.text.toString()
            loginUser(emailFromUI, passwordFromUI)
            binding.tvResults.isVisible = true
        }
    }

    fun loginUser(email:String, password:String) {
        if (auth.currentUser != null) {
            binding.tvResults.text = "User already logged in"
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this){
                    task ->
                if (task.isSuccessful){
                    Log.d("TESTING", "singInWithEmail:success")
                    binding.tvResults.text = "${email} logged in successfully"
                    finish()
                } else {
                    Log.w("TESTING", "signInWithEmail:failure", task.exception)
                    binding.tvResults.text = "Login failed, please check your Email or Password"
                }
            }
    }

}