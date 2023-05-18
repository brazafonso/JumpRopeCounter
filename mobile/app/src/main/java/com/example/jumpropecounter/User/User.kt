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
     * Signs out current user (using firebase authenticate)
     */
    fun sign_out(){
        if(FirebaseAuth.getInstance().currentUser != null)
            FirebaseAuth.getInstance().signOut()
    }

    /**
     * Creates a new session
     */
    fun create_session(): Session {
        return Session(user_id)
    }

    /**
     * Adds session to firebase database
     */
    fun add_session(session: Session){
        val db_session_reference = Firebase.database.reference.child("$DB_SESSION_PATH/$user_id/${session.start}")
        db_session_reference.setValue(session)
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