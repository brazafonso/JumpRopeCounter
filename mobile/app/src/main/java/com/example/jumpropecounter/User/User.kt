package com.example.jumpropecounter.User

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val DB_USER_PATH = "users"

/**
 * Class to represent a user
 * Saves information such as profile values and can get records from previous sessions
 */
class User (user_id:String,username:String?,email:String?):Parcelable{
    val TAG = "User"
    var user_id = user_id
    var weight = 60F // in kg
    var height = 170F // in cm
    var age = 18
    var username = username ?: user_id
    var email = email ?: user_id
    var total_jumps = 0
    var permission_level = 0
    var created = System.currentTimeMillis()


    /**
     * Changes user username
     */
    fun change_username(new_username:String){
        username = new_username
        update_user_data()
    }

    /**
     * Changes user age
     */
    fun change_age(new_age:Int){
        age = new_age
        update_user_data()
    }

    /**
     * Changes user height
     */
    fun change_height(new_height:Float){
        height = new_height
        update_user_data()
    }

    /**
     * Changes user weight
     */
    fun change_weight(new_weight:Float){
        weight = new_weight
        update_user_data()
    }



    /**
     * Checks if user is signed in
     */
    fun is_signed_in(): Boolean {
        var signed_in = false
        val u = FirebaseAuth.getInstance().currentUser
        if (u != null) {
            signed_in = user_id == u.uid
        }
        return  signed_in
    }


    /**
     * Signs out current user (using firebase authenticate)
     */
    fun sign_out(){
        if(FirebaseAuth.getInstance().currentUser != null) {
            FirebaseAuth.getInstance().signOut()
            while (is_signed_in()) {
                Log.d(TAG,"Still logged")
                Thread.sleep(1 * 1000)
            }
        }

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
            val session = Session(user_id,null)
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
        val daily_reps_count  = LinkedHashMap<String, Int>() // list of jumps per day
        val daily_reps_time  = LinkedHashMap<String, MutableMap<String,Any>>() // list of time per day
        var last_day: LocalDate? = null
        val now = Instant.now().toEpochMilli()
        val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        sessions.map { s->s.update_with_user_date() }

        if(sessions.isNotEmpty()) {
            val filteres_sessions = sessions.filter { s -> s.type_activity == type_activity }.sortedBy { s -> s.start }
            var instant = Instant.ofEpochMilli(filteres_sessions[0].start).atZone(ZoneId.systemDefault())
            var date = instant.toLocalDate()
            for (session in filteres_sessions) {
                Log.d(TAG,"$date ${session.get_seconds()} seconds")
                // Filtering only useful sessions
                if(session.total_reps > 0 && session.get_seconds() > 0) {
                    total_reps += session.total_reps

                    instant = Instant.ofEpochMilli(session.start).atZone(ZoneId.systemDefault())
                    date = instant.toLocalDate()
                    val date_str = date.toString()

                    // Update daily jumps and time
                    if (daily_reps_count.containsKey(date_str)) {
                        daily_reps_count[date_str] =
                            daily_reps_count[date_str]!! + session.total_reps
                        daily_reps_time[date_str]?.computeIfPresent("duration") { _, d -> d as Long + session.get_seconds() }
                    } else {
                        daily_reps_count[date_str] = session.total_reps
                        daily_reps_time[date_str] = mutableMapOf(
                            "duration" to session.get_seconds(),
                            "weight" to session.weight,
                            "height" to session.height,
                            "age" to session.age
                        )
                    }
                    // Check daily streak
                    if (last_day == null) {
                        streak = 1
                    } else {
                        if (last_day.plusDays(1).dayOfYear == date.dayOfYear) {
                            streak += 1
                        } else if (last_day.dayOfYear != date.dayOfYear) {
                            streak = 1
                            //fill linkedHash with missing days
                            var missing_day = last_day.plusDays(1)
                            while (missing_day.dayOfYear != date.dayOfYear) {
                                daily_reps_count[missing_day.toString()] = 0
                                missing_day = missing_day.plusDays(1)
                            }
                        }
                    }
                    last_day = date
                }
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
        "daily_reps_count" to daily_reps_count,
        "daily_reps_time" to daily_reps_time
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
     suspend fun get_user_data(){
        val db_users_reference = Firebase.database.reference.child("$DB_USER_PATH/$user_id")
        val result = db_users_reference.get().await()
        if(result.exists()){
            Log.d(TAG,"Got user data")
            // user exists
            if(result.value != null) {
                update_from_map(result.value)
            }
            // user doesnt exist
            else{
                create_user_data()
            }
        }else{
            Log.d(TAG,"Error getting user")
        }
    }

    /**
     * Checks if user already exists
     */
    suspend fun exists_user():Boolean{
        val db_users_reference = Firebase.database.reference.child("$DB_USER_PATH/$user_id")
        val result = db_users_reference.get().await()
        return if(result.exists()){
            Log.d(TAG,"Got user data")
            // user exists
            result.value != null
        }else{
            Log.d(TAG,"Error getting user")
            false
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
        Log.d(TAG,"Got user $map")
        username = map["username"].toString()
        email = map["email"].toString()
        weight = map["weight"].toString().toFloat()
        height = map["height"].toString().toFloat()
        age = map["age"].toString().toInt()
        total_jumps = map["total_jumps"].toString().toInt()
        permission_level = map["permission_level"].toString().toInt()
        created = map["permission_level"].toString().toLong()
    }

    fun to_map():Map<String, Any?>{
        return mapOf(
            "username" to username,
            "email" to email,
            "weight" to weight,
            "height" to height,
            "age" to age,
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
        parcel.writeFloat(weight)
        parcel.writeFloat(height)
        parcel.writeInt(age)
        parcel.writeLong(created)
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