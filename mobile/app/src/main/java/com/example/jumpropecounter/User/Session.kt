package com.example.jumpropecounter.User

import android.util.Log
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates


const val DB_SESSION_PATH = "sessions"
class Session(val user_id: String, var type_activity:String?) {
    val TAG = "Session"
    // Session start and end
    var start = System.currentTimeMillis()
    var end:Long = -1
    var height = 0F
    var weight = 0F
    var age = 0

    // Counter can be observed by other classes
    var counter_refreshListListeners = ArrayList<InterfaceRefreshList>()
    var total_reps:Int by Delegates.observable(0){ property, oldValue, newValue ->
        counter_refreshListListeners.forEach {
            it.refreshListRequest()
        }
    }
    interface InterfaceRefreshList {
        fun refreshListRequest()
    }


    /**
     * Returns the duration of the session in seconds
     */
    fun get_seconds(): Long {
        val diff = end - start
        return TimeUnit.MILLISECONDS.toSeconds(diff)
    }


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
        Firebase.database.reference.child("$DB_SESSION_PATH/$user_id/$start").setValue(to_map())
            .addOnSuccessListener { Log.d(TAG,"Session created successfully") }
            .addOnFailureListener { Log.d(TAG,"Error creating session") }
    }

    /**
     * Update session in firebase
     */
    fun update_session_data(){
        Firebase.database.reference.child("$DB_SESSION_PATH/$user_id/$start").updateChildren(to_map())
    }


    /**
     * Updates session from map
     */
    fun update_from_map(value: Any?) {
        val map = value as Map<*, *>
        start = map["start"].toString().toLong()
        end = map["end"].toString().toLong()
        total_reps = map["total_reps"].toString().toInt()
        type_activity = map["type_activity"].toString()
        height = map["height"].toString().toFloat()
        weight = map["weight"].toString().toFloat()
        age = map["age"].toString().toInt()
    }

    /**
     * Updates values using user data
     */
    fun update_with_user_date(){
        val db_users_reference = Firebase.database.reference.child("$DB_USER_PATH/$user_id")
        db_users_reference.get()
            .addOnSuccessListener {
                //Log.d(TAG,"Got user ${it.value} from $DB_USER_PATH/$user_id")
                val user_map = it.value as Map<*, *>
                height = user_map["height"].toString().toFloat()
                weight = user_map["weight"].toString().toFloat()
                age = user_map["age"].toString().toInt()
                update_session_data()
            }
            .addOnFailureListener { Log.d(TAG,"Error geting user") }
    }

    /**
     * Turn session to map
     */
    fun to_map():Map<String, Any?> {
        return mapOf(
            "user_id" to user_id,
            "start" to start,
            "end" to end,
            "total_reps" to total_reps,
            "type_activity" to type_activity,
            "age" to age,
            "height" to height,
            "weight" to weight
        )
    }

}