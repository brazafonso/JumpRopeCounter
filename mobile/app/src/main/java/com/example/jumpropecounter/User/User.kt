package com.example.jumpropecounter.User

import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.prefs.Preferences
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeBytes

class User (username:String){
    private var username = username
    private var total_jumps = 0


    fun sign_out(){
        FirebaseAuth.getInstance().signOut()
    }




}