package com.example.jumpropecounter.DB.Fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.graphics.drawable.BitmapDrawable
import android.media.Image
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT
import android.util.Log
import com.example.jumpropecounter.Camera.ImageUtils
import com.example.jumpropecounter.Utils.ConcurrentFifo
import com.example.jumpropecounter.Utils.Frame
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.random.Random


/**
 * Class that receives a folder from where to check if any new photos are available
 * and sends them to firebase db (deleting them from the folder)
 */
class PhotoSender(concurrentFIFO: ConcurrentFifo<Frame>, frameRate: Int) : Thread() {
    private var FRAMERATE = frameRate
    private val _concurrentFIFO : ConcurrentFifo<Frame> = concurrentFIFO
    private val TAG : String =  "photoSenderThread"
    private val storage = Firebase.storage("gs://sa-g4-a91ed.appspot.com")
    private var current_file : String? = null
    private var current_frame : Int = 0
    private var processing_files : ArrayList<String> = ArrayList()


    /**
     * Checks if folder exists and further checks for videos
     * If videos are found, gets their frames and sends them to db
     * TODO sleeps till awakened
     */
    override fun run() {
        while(true) {
            // Checks videos folders
            if (!_concurrentFIFO.isEmpty()){
                Log.d(TAG,"Fifo size: ${_concurrentFIFO.size()}")
                val frame = _concurrentFIFO.dequeue()
                if(frame.is_end()){
                    this.interrupt()
                }
                else if (!frame.is_start())
                    sendFrame(frame)
            }
        }
    }



    /**
     * Sends a file to firebase, with listeners for completion
     */
     private fun sendFrame(frame:Frame){
        Log.d(TAG, "Sending frame : ${frame.seqNum} ")
        val bitmap = frame.get_frame()
        if (bitmap != null) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream)
            val bytes = byteArrayOutputStream.toByteArray()
            byteArrayOutputStream.close()
            val tsLong = System.currentTimeMillis() / 1000
            val ts = tsLong.toString()
            val myRef = storage.reference
            val im = myRef.child("images/${ts}.jpeg")
            Log.d(TAG, "Sending image : $ts ")
            im.putBytes(bytes).addOnSuccessListener {
                im.metadata.addOnSuccessListener {metadata->
                    if (metadata.sizeBytes > 0) {
                        Log.d(TAG, "Image sent at : $ts ")
                    }else{
                        Log.d(TAG, "Failed to send image : $ts ")
                    }
                }
            }.addOnCanceledListener {
                Log.d(TAG, "Canceled to send image : $ts ")
            }.addOnFailureListener{
                Log.d(TAG, "Failed to send image : $ts ")
            }
        }

    }

}