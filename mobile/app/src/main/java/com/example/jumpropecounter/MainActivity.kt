package com.example.jumpropecounter


import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.jumpropecounter.Camera.Preview
import com.example.jumpropecounter.DB.Fragments.PhotoSender
import com.example.jumpropecounter.User.User
import com.example.jumpropecounter.User.activity.LoginUserActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlin.system.exitProcess

const val EXTRA_USER = "user"
class MainActivity : AppCompatActivity() {
    private lateinit var photoSender: PhotoSender
    private lateinit var login_btn: Button
    private lateinit var logout_btn: Button
    private lateinit var go_capture_btn: Button
    private var user: User? = null
    private val TAG : String =  "MainActivity"
    var recording_folder:String = "/app_files/recording"
    var frameRate = 10


    /**
     * Creating app
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG,"Loading View")
        setContentView(R.layout.activity_main)
        Log.d(TAG,"Starting Program")

        // Create shared preferences
        val sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("app_path", application.dataDir.absolutePath)
        editor.commit()

        // Get permissions
        get_permissions()


        /*// Firebase thread
        photoSender = PhotoSender(application.dataDir.absolutePath + recording_folder, frameRate)
        photoSender.start()
        */

        go_capture_btn = findViewById(R.id.go_capture_btn)
        login_btn = findViewById(R.id.login_btn)
        logout_btn = findViewById(R.id.logout_btn)

        // Check logged info (using firebase)
        val u = FirebaseAuth.getInstance().currentUser
        if(u!=null){
            Log.d(TAG,"Logged User")
            get_user(u)
            go_home_activity()
        }else{
            Log.d(TAG,"User not logged")
            enable_logout(false)
            enable_login(true)
        }


        go_capture_btn.setOnClickListener {
            Log.d(TAG,"Capture mode")
            getDir("files", MODE_PRIVATE)
            // Activity to show and capture video
            val previewFragment = Preview.newInstance(frameRate,getSharedPreferences("prefs",MODE_PRIVATE).getString("app_path",null) + recording_folder)
            addFragment(previewFragment)

        }


        login_btn.setOnClickListener { _ ->
            Log.d(TAG,"Login Button")
            val myIntent =  Intent(this,LoginUserActivity::class.java)
            startActivity(myIntent)
        }

        logout_btn.setOnClickListener { _ ->
            Log.d(TAG,"Logout button")
            user?.sign_out()
            user = null
            enable_logout(false)
            enable_login(true)
        }

    }



    override fun onRestart() {
        super.onRestart()
        Log.d(TAG,"Restarting")

        val u = FirebaseAuth.getInstance().currentUser
        if(u!=null){
            Log.d(TAG,"Logged User")
            get_user(u)
            go_home_activity()
        }else{
            Log.d(TAG,"User not logged")
            enable_logout(false)
            enable_login(true)
        }
    }





    /**
     * Function to gather necessary permissions for the program
     */
    fun get_permissions(){
        var permissionsLst = mutableListOf<String>()
        val granted = PackageManager.PERMISSION_GRANTED
        if(checkSelfPermission(android.Manifest.permission.CAMERA) != granted){
            permissionsLst.add(android.Manifest.permission.CAMERA)
        }
        if(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != granted){
            permissionsLst.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if(checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != granted){
            permissionsLst.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if(permissionsLst.size > 0){
            requestPermissions(permissionsLst.toTypedArray(),42)
        }

    }

    /**
     * After the permission requests have been answered, check if they are met
     * If not, requests them again
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if (it != PackageManager.PERMISSION_GRANTED)
                get_permissions()
        }
    }


    /**
     * Gets and updates firebase user
     */
    fun get_user(u:FirebaseUser){
        user = User(u.uid,u.displayName!!,u.email)
        user!!.get_user_data()
        Log.d(TAG, "User ${user!!.username}")
    }



    /**
     * Changes the status of the login button
     */
    fun enable_login(enable:Boolean){
        if(enable)
            login_btn.visibility = View.VISIBLE
        else
            login_btn.visibility = View.INVISIBLE
    }

    /**
     * Changes the status of the logout button
     */
    fun enable_logout(enable:Boolean){
        if(enable)
            logout_btn.visibility = View.VISIBLE
        else
            logout_btn.visibility = View.INVISIBLE
    }


    /**
     * Goes to home activity
     */
    fun go_home_activity(){
        val intent =  Intent(this,Home::class.java)
        intent.putExtra(EXTRA_USER,user)
        startActivity(intent)
    }


    /**
     * Adds a new fragment to the stack and starts it
     */
    private fun addFragment(fragment: Fragment?) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.addToBackStack(fragment.toString())
        fragmentTransaction.replace(R.id.fragmentPreviewContainer,fragment!!)
        fragmentTransaction.commit()
    }
}