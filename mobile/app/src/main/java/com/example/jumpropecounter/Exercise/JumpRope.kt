package com.example.jumpropecounter.Exercise

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.jumpropecounter.Camera.Preview
import com.example.jumpropecounter.Hub.Fragments.Home
import com.example.jumpropecounter.R

class JumpRope: AppCompatActivity() {
    val TAG = "JumpRope"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG,"Started Activity")
        setContentView(R.layout.jump_rope)
        val framerate = getSharedPreferences("prefs",MODE_PRIVATE).getInt("framerate",10)
        val recordingFolder = getSharedPreferences("prefs",MODE_PRIVATE).getString("recording_folder","")
        val mode = 0
        if(recordingFolder!=null)
            addFragment(Preview.newInstance(framerate,recordingFolder,mode))

    }



    /**
     * Adds a new fragment to the stack and starts it
     */
    private fun addFragment(fragment: Fragment?) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.addToBackStack(fragment.toString())
        fragmentTransaction.replace(R.id.JumpRopeCameraPreview,fragment!!)
        fragmentTransaction.commit()
    }
}