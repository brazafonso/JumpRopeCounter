package com.example.jumpropecounter.User

import android.util.Log
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.sql.Timestamp
import kotlin.properties.Delegates


const val DB_SESSION_PATH = "sessions"
class Session(user_id:String) {
    private val TAG = "Session"
    val start = System.currentTimeMillis()
    var end:Long = -1
    val user_id = user_id
    var total_reps = 0


    /**
     * Gets session data from database
     * Creates it if it doenst exist
     */
    fun get_session_data(){
        val db_users_reference = Firebase.database.reference.child("sessions/$user_id/$start")
        db_users_reference.get()
            .addOnSuccessListener {
                Log.d(TAG,"Got session data")
                // user exists
                if(it.value != null) {
                    update_from_map(it.value)
                }
                // user doesnt exist
                else{
                    create_session_data()
                }
            }
            .addOnFailureListener {
                Log.d(TAG,"Error getting user")
            }
    }


    /**
     * Creates session in database
     */
    fun create_session_data(){
        Firebase.database.reference.child("sessions/$user_id/$start").setValue(this)
            .addOnSuccessListener { Log.d(TAG,"Session created successfully") }
            .addOnFailureListener { Log.d(TAG,"Error creating session") }
    }

    /**
     * Updates session from map
     */
    fun update_from_map(value: Any?) {
        val map = value as Map<*, *>
        total_reps = map["total_reps"].toString().toInt()
        end = map["end"].toString().toLong()
    }




}