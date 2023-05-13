package com.example.jumpropecounter.Hub

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.jumpropecounter.EXTRA_USER
import com.example.jumpropecounter.Hub.Fragments.Exercise
import com.example.jumpropecounter.Hub.Fragments.Home
import com.example.jumpropecounter.Hub.Fragments.Settings
import com.example.jumpropecounter.R
import com.example.jumpropecounter.User.User
import com.google.android.material.bottomnavigation.BottomNavigationView

class Hub : AppCompatActivity() {
    private var TAG = "Hub"
    lateinit var user: User
    private lateinit var logout_btn: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "On Hub Activity")

        user = intent.getParcelableExtra(EXTRA_USER)!!
        Log.d(TAG, "User ${user.username}")

        setContentView(R.layout.navbar_agregator)

        // Prepare nav_bar
        val nav_bar = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        nav_bar.setOnNavigationItemSelectedListener(navListener)
        // Default page on nav_bar is home
        addFragment(Home.newInstance(user))
    }

    private val navListener = BottomNavigationView.OnNavigationItemSelectedListener {
        // By using switch we can easily get
        // the selected fragment
        // by using there id.
        lateinit var selectedFragment: Fragment
        when (it.itemId) {
            R.id.home -> {
                selectedFragment = Home.newInstance(user)
            }
            R.id.exercise -> {
                selectedFragment = Exercise()
            }
            R.id.settings -> {
                selectedFragment = Settings()
            }
        }
        // It will help to replace the
        // one fragment to other.
        addFragment(selectedFragment)
        true
    }


    /**
     * Adds a new fragment to the stack and starts it
     */
    private fun addFragment(fragment: Fragment?) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.addToBackStack(fragment.toString())
        fragmentTransaction.replace(R.id.frame_layout,fragment!!)
        fragmentTransaction.commit()
    }

}