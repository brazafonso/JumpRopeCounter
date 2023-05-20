package com.example.jumpropecounter.User

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.coroutines.coroutineContext

const val DB_USER_PATH = "users"

/**
 * Class to represent a user
 * Saves information such as profile values and can get records from previous sessions
 */
class User (user_id:String,username:String?,email:String?):Parcelable{
    val TAG = "User"
    var user_id = user_id
    var username = username ?: user_id
    var email = email ?: user_id
    var total_jumps = 0
    var permission_level = 0
    var created = System.currentTimeMillis()


    /**
     * Changes user username
     */
    fun change_username(new_usernam:String){
        username = new_usernam
        update_user_data()
    }


    /**
     * Signs out current user (using firebase authenticate)
     */
    fun sign_out(){
        if(FirebaseAuth.getInstance().currentUser != null)
            FirebaseAuth.getInstance().signOut()
    }

    /**
     * Creates a new session
     */
    fun create_session(type_activity:String?): Session {
        return Session(user_id, type_activity)
    }

    /**
     * Adds session to firebase database
     */
    fun add_session(session: Session){
        val db_session_reference = Firebase.database.reference.child("$DB_SESSION_PATH/$user_id/${session.start}")
        db_session_reference.setValue(session)
    }

    /**
     * Gets all of the user's sessions
     */
    suspend fun get_sessions(): ArrayList<Session> {
        val session_list = ArrayList<Session>()
        val db_session_reference = Firebase.database.reference.child("$DB_SESSION_PATH/$user_id")
        val list = db_session_reference.get().await().children
        for(child in list){
            val session = Session("user_id",null)
            session.update_from_map(child.value)
            session_list.add(session)
        }
        return session_list
    }

    /**
     * Resets all of the user's data
     */
    fun reset_sessions(){
        val db_session_reference = Firebase.database.reference.child("$DB_SESSION_PATH/$user_id")
        db_session_reference.removeValue()
            .addOnSuccessListener {
                Log.d(TAG,"Data reset")
        }
            .addOnFailureListener {
                Log.d(TAG,"Error reseting data")
            }
    }



    /**
     * Returns a dictionary of stats from sessions of given activity
     */
    fun get_stats(sessions:ArrayList<Session>,type_activity: String?):Map<String,Any>{
        var total_reps = 0 // total reps in history
        var streak = 0     // consecutive days using the app, where last cant be more than a day ago
        val daily_reps  = LinkedHashMap<LocalDate, Int>() // list of jumps per day
        var last_day: LocalDate? = null
        val now = Instant.now().toEpochMilli()
        val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()


        if(sessions.isNotEmpty()) {
            val filteres_sessions = sessions.filter { s -> s.type_activity == type_activity }.sortedBy { s -> s.start }
            val instant = Instant.ofEpochMilli(filteres_sessions[0].start).atZone(ZoneId.systemDefault())
            val day = instant.toLocalDate()
            for (session in filteres_sessions) {
                total_reps += session.total_reps


                val instant = Instant.ofEpochMilli(session.start).atZone(ZoneId.systemDefault())
                val day = instant.toLocalDate()
                // Update daily jumps
                if(daily_reps.containsKey(day)){
                    daily_reps[day] = daily_reps[day]!! + session.total_reps
                }else{
                    daily_reps[day] = session.total_reps
                }
                // Check daily streak
                if (last_day == null) {
                    streak = 1
                } else {
                    if (last_day.plusDays(1).dayOfYear == day.dayOfYear) {
                        streak += 1
                    } else {
                        streak = 1
                    }
                }
                last_day = day
            }
            // Last streak check with current day
            if (last_day != null) {
                if (last_day.plusDays(1).dayOfYear == today.dayOfYear)
                    streak += 1
                else if (last_day != today)
                    streak = 0
            }
        }

        return mapOf(
        "total_reps" to total_reps,
        "streak" to streak,
        "daily_reps" to daily_reps
        )

    }


    /**
     * Get total reps from sessions of given activity
     */
    fun get_total_reps(sessions:ArrayList<Session>,type_activity: String?): Int {
        var total = 0
        for(session in sessions.filter { s -> s.type_activity == type_activity }){
            total += session.total_reps
        }
        return total
    }



    /**
     * Gets a user from the firebase realtime database
     * Creates it if he doesnt exist
     */
    fun get_user_data(){
        val db_users_reference = Firebase.database.reference.child("$DB_USER_PATH/$user_id")
        db_users_reference.get()
            .addOnSuccessListener {
                Log.d(TAG,"Got user data")
                // user exists
                if(it.value != null) {
                    update_from_map(it.value)
                }
                // user doesnt exist
                else{
                    create_user_data()
            }
        }
            .addOnFailureListener {
                Log.d(TAG,"Error getting user")
            }
    }


    /**
     * Update user in firebase
     */
    fun update_user_data(){
        Firebase.database.reference.child("$DB_USER_PATH/$user_id").updateChildren(to_map())
    }


    /**
     * Creates user in database
     */
    fun create_user_data(){
        Firebase.database.reference.child("$DB_USER_PATH/$user_id").setValue(this)
            .addOnSuccessListener { Log.d(TAG,"User created successfully") }
            .addOnFailureListener { Log.d(TAG,"Error creating user") }
    }


    /**
     * Updates user from map
     */
    fun update_from_map(value: Any?) {
        val map = value as Map<*, *>
        username = map["username"].toString()
        email = map["email"].toString()
        total_jumps = map["total_jumps"].toString().toInt()
        permission_level = map["permission_level"].toString().toInt()
        created = map["permission_level"].toString().toLong()
    }

    fun to_map():Map<String, Any?>{
        return mapOf(
            "username" to username,
            "email" to email,
            "total_jumps" to total_jumps,
            "permission_level" to permission_level,
            "created" to created
        )
    }



    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(user_id)
        parcel.writeString(username)
        parcel.writeString(email)
        parcel.writeInt(total_jumps)
        parcel.writeLong(created)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }


}