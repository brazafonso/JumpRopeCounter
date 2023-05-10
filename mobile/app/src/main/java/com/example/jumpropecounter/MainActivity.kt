package com.example.jumpropecounter


import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatToggleButton
import androidx.fragment.app.Fragment
import com.example.jumpropecounter.Fragments.Preview
import com.example.jumpropecounter.db.PhotoSender
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.math.log


class MainActivity : AppCompatActivity() {
    lateinit var photoSender: PhotoSender
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

        getDir("files",Context.MODE_PRIVATE)
        // Activity to show and capture video
        val previewFragment = Preview.newInstance(frameRate,application.dataDir.absolutePath + recording_folder)
        addFragment(previewFragment)



        // Firebase thread
        photoSender = PhotoSender(application.dataDir.absolutePath + recording_folder, frameRate)
        photoSender.start()
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
        fragmentTransaction.add(R.id.fragmentContainer, fragment!!)
        fragmentTransaction.commit()
    }
}