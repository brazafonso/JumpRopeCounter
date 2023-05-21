package com.example.jumpropecounter.User.activity

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.jumpropecounter.EXTRA_USER
import com.example.jumpropecounter.R
import com.example.jumpropecounter.User.User
import com.google.android.material.textfield.TextInputEditText

class Create_User:AppCompatActivity() {
    private val TAG = "Create User"
    private lateinit var user: User
    private lateinit var username_input:TextInputEditText
    private lateinit var age_input:TextInputEditText
    private lateinit var weight_input:TextInputEditText
    private lateinit var height_input:TextInputEditText
    private lateinit var submit_btn:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG,"On create user")

        setContentView(R.layout.activity_new_login_form)

        username_input = findViewById(R.id.username)
        age_input = findViewById(R.id.age)
        weight_input = findViewById(R.id.weight)
        height_input = findViewById(R.id.height)
        submit_btn = findViewById(R.id.submit_data)

        user = intent.getParcelableExtra(EXTRA_USER)!!

        username_input.hint = user.username

        submit_btn.setOnClickListener {
            Log.d(TAG,"Submit button")
            if (data_ready()){
                update_user()
                Log.d(TAG,"Updated user")
                finish()
            }else{
                Toast.makeText(this,"Please fill all the fields",Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Checks if form is filled
     */
    private fun data_ready(): Boolean {
        var ready = true
        if (age_input.text.isNullOrBlank()
            || weight_input.text.isNullOrBlank()
            || height_input.text.isNullOrBlank())
            ready = false
        return ready
    }

    /**
     * Updates user values
     */
    private fun update_user(){
        if (!username_input.text.isNullOrBlank()){
            user.username = username_input.text.toString()
        }
        user.age = age_input.text.toString().toInt()
        user.weight = weight_input.text.toString().toFloat()
        user.height = height_input.text.toString().toFloat()
        user.update_user_data()
    }
}