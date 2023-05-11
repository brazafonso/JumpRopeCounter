package com.example.jumpropecounter.User.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.example.jumpropecounter.R

class RegisterUserActivity : AppCompatActivity() {
    var TAG = "RegisterUser"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_user)
        Log.d(TAG,"In Register User")


        var submit_btn = findViewById<Button>(R.id.register_submit_btn)
        submit_btn.setOnClickListener {
            Log.d(TAG,"Submiting")
            finish()
        }
    }
}