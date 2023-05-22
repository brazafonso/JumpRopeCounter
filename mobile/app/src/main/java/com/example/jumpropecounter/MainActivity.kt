package com.example.jumpropecounter


import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.jumpropecounter.Camera.Preview
import com.example.jumpropecounter.DB.Fragments.PhotoSender
import com.example.jumpropecounter.Exercise.JumpRope
import com.example.jumpropecounter.Hub.Hub
import com.example.jumpropecounter.JumpCounter.JumpCounter
import com.example.jumpropecounter.User.User
import com.example.jumpropecounter.User.activity.Create_User
import com.example.jumpropecounter.User.activity.LoginUserActivity
import com.example.jumpropecounter.Utils.ConcurrentFifo
import com.example.jumpropecounter.Utils.Frame
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

const val EXTRA_USER = "user"
const val JUMP_TYPE_ACTIVITY = "jump_rope"
class MainActivity : AppCompatActivity() {
    private lateinit var icon: ImageButton
    private var user: User? = null
    private val TAG : String =  "MainActivity"
    private var u: FirebaseUser? = null
    var frameRate = 20


    /**
     * Creating app
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check logged info (using firebase)
        u = FirebaseAuth.getInstance().currentUser

        Log.d(TAG,"Loading View")
        setContentView(R.layout.activity_first_screen)
        Log.d(TAG,"Starting Program")

        // Create shared preferences
        val sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("app_path", application.dataDir.absolutePath)
        editor.putInt("framerate",frameRate)
        editor.apply()

        // Get permissions
        get_permissions()

        icon = findViewById(R.id.app_icon)
        icon.setOnClickListener { _ ->
            Log.d(TAG,"Start Button")
            if(u!=null){
                //u.photoUrl
                Log.d(TAG,"Logged User")
                CoroutineScope(Dispatchers.IO).launch {
                    Log.d(TAG,"Geting user information")
                    val exists = get_user(u!!)
                    Log.d(TAG,"Got user information")
                    if(!exists){
                        Log.d(TAG,"Creating user")
                        go_create_user_activity()
                    }else {
                        Log.d(TAG,"Going to hub ${user!!.username} ${user!!.age}")
                        go_hub_activity()
                    }
                }
            }else{
                val myIntent =  Intent(this,LoginUserActivity::class.java)
                startActivity(myIntent)
            }
        }


    }


    override fun onRestart() {
        super.onRestart()
        Log.d(TAG,"Restarting")

        u = FirebaseAuth.getInstance().currentUser
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
    suspend fun get_user(u:FirebaseUser):Boolean{
        Log.d(TAG, "User ${u.uid}")
        Log.d(TAG, "User ${u.displayName}")
        Log.d(TAG, "User ${u.email}")
        user = User(u.uid,u.displayName,u.email)
        val exists = user!!.exists_user()
        user!!.get_user_data()
        Log.d(TAG, "User ${user!!.username} ${user!!.age} ${user!!.weight} ${user!!.height}")
        return exists
    }



    /**
     * Goes to create user activity
     */
    fun go_create_user_activity(){
        val intent =  Intent(this, Create_User::class.java)
        intent.putExtra(EXTRA_USER,user)
        startActivity(intent)
    }

    /**
     * Goes to home activity
     */
    fun go_hub_activity(){
        val intent =  Intent(this, Hub::class.java)
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