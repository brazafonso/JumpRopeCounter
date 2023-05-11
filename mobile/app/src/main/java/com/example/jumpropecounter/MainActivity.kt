package com.example.jumpropecounter


import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.jumpropecounter.Camera.Preview
import com.example.jumpropecounter.DB.Fragments.PhotoSender
import com.example.jumpropecounter.User.activity.RegisterUserActivity


class MainActivity : AppCompatActivity() {
    private lateinit var photoSender: PhotoSender
    private lateinit var register_btn: Button
    private lateinit var login_btn: Button
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

        // Get permissions
        get_permissions()
        //var user = User("username","password")

        val go_capture_btn = findViewById<Button>(R.id.go_capture_btn)
        register_btn = findViewById(R.id.register_btn)
        login_btn = findViewById(R.id.login_btn)

        go_capture_btn.setOnClickListener {
            Log.d(TAG,"Capture mode")
            getDir("files", Context.MODE_PRIVATE)
            // Activity to show and capture video
            val previewFragment = Preview.newInstance(frameRate,application.dataDir.absolutePath + recording_folder)
            addFragment(previewFragment)

            // Firebase thread
            photoSender = PhotoSender(application.dataDir.absolutePath + recording_folder, frameRate)
            photoSender.start()
        }

        register_btn.setOnClickListener { _ ->
            val myIntent =  Intent(this,RegisterUserActivity::class.java)
            startActivity(myIntent)
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




    private fun addFragment(fragment: Fragment?) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.addToBackStack(fragment.toString())
        fragmentTransaction.add(fragment!!,null)
        fragmentTransaction.commit()
    }
}