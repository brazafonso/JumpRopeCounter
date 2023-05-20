package com.example.jumpropecounter.Exercise

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.jumpropecounter.Camera.Preview
import com.example.jumpropecounter.Hub.Fragments.Home
import com.example.jumpropecounter.JUMP_TYPE_ACTIVITY
import com.example.jumpropecounter.R
import com.example.jumpropecounter.User.Session
import com.example.jumpropecounter.User.User
import java.util.ArrayList
import kotlin.properties.Delegates

class JumpRope: AppCompatActivity() {
    private lateinit var text_counter:TextView
    private val TAG = "JumpRope"

    // Making and observable counter
    // Text_counter will be updated based on this observed value
    private var counter_refreshListListeners = ArrayList<InterfaceRefreshList>()
    private var total_reps:Int by Delegates.observable(0){ property, oldValue, newValue ->
        counter_refreshListListeners.forEach {
            it.refreshListRequest()
        }
    }
    interface InterfaceRefreshList {
        fun refreshListRequest()
    }
    private var counter_thread = Thread {
        counter_refreshListListeners.add(object : InterfaceRefreshList {
            override fun refreshListRequest() {
                runOnUiThread {
                    text_counter.text = total_reps.toString()
                }
            }
        })
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getIntExtra("MODE",0)
        Log.d(TAG,"Started Activity")
        setContentView(R.layout.jump_rope)


        val framerate = getSharedPreferences("prefs",MODE_PRIVATE).getInt("framerate",10)
        val recordingFolder = getSharedPreferences("prefs",MODE_PRIVATE).getString("recording_folder","")
        text_counter = findViewById(R.id.TextViewJumpRopeCounter)
        counter_thread.start()

        if(recordingFolder!=null) {
            Log.d(TAG,"Type : $JUMP_TYPE_ACTIVITY")
            val preview_fragment = Preview.newInstance(framerate, recordingFolder,JUMP_TYPE_ACTIVITY, mode)
            // setup observer for activity counter
            preview_fragment.counter_refreshListListeners.add(
                object : Preview.InterfaceRefreshList {
                    override fun refreshListRequest() {
                        total_reps = preview_fragment.total_reps
                    }
                }
            )
            addFragment(preview_fragment)
        }
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