package com.example.jumpropecounter.Hub

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import com.example.jumpropecounter.EXTRA_USER
import com.example.jumpropecounter.Hub.Fragments.Exercise
import com.example.jumpropecounter.Hub.Fragments.Home
import com.example.jumpropecounter.Hub.Fragments.Settings
import com.example.jumpropecounter.R
import com.example.jumpropecounter.User.User
import com.google.android.material.bottomnavigation.BottomNavigationView

class Hub : AppCompatActivity() {
    private var TAG = "Hub"
    private lateinit var user: User



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "On Hub Activity")

        user = intent.getParcelableExtra(EXTRA_USER)!!
        Log.d(TAG, "User ${user.username} ${user.age} ${user.weight} ${user.height}")
        if(user.is_signed_in()) {
            Log.d(TAG, "User ${user.username} ${user.age} ${user.weight} ${user.height}")

            setContentView(R.layout.navbar_agregator)

            //default fragment home
            addFragment(Home.newInstance(user), "home")


            // Prepare nav_bar
            val nav_bar = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            nav_bar.setOnNavigationItemSelectedListener(navListener)
        }else{
            finish()
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        Log.d(TAG,"BackPress")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG,"Destroying activity")
        this.finish()
    }


    private val navListener = BottomNavigationView.OnNavigationItemSelectedListener {
        // By using switch we can easily get
        // the selected fragment
        // by using there id.
        lateinit var selectedFragment: Fragment
        lateinit var tag: String
        when (it.itemId) {
            R.id.home -> {
                selectedFragment = Home.newInstance(user)
                tag = "home"
            }
            R.id.exercise -> {
                selectedFragment = Exercise.newInstance(user)
                tag = "exercise"
            }
            R.id.settings -> {
                selectedFragment = Settings.newInstance(user)
                tag = "settings"
            }
        }

        if(supportFragmentManager.fragments.isEmpty()
            || supportFragmentManager.fragments[supportFragmentManager.fragments.size-1].tag != tag)
            addFragment(selectedFragment,tag)
        // It will help to replace the
        // one fragment to other.
        true
    }


    /**
     * Adds a new fragment to the stack and starts it
     */
    private fun addFragment(fragment: Fragment?,tag:String?) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.addToBackStack(fragment.toString())
        fragmentTransaction.replace(R.id.frame_layout,fragment!!,tag)
        fragmentTransaction.commit()
    }

}