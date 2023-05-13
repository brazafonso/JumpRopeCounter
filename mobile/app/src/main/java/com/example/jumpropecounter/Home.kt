package com.example.jumpropecounter

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.jumpropecounter.User.User

class Home : AppCompatActivity() {
    private var TAG = "Home"
    lateinit var user: User
    private lateinit var logout_btn: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG,"On Home Activity")

        user = intent.getParcelableExtra(EXTRA_USER)!!
        Log.d(TAG,"User ${user.username}")

        setContentView(R.layout.activity_home)


        logout_btn = findViewById(R.id.logout_btn)

        logout_btn.setOnClickListener { _ ->
            Log.d(TAG,"Logout button")
            user.sign_out()
            finish()
        }

    }

}