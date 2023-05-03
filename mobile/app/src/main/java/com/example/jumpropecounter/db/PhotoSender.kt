package com.example.jumpropecounter.db

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream


/**
 * Class that receives a folder from where to check if any new photos are available
 * and sends them to firebase db (deleting them from the folder)
 */
class PhotoSender(photo_storage: Path) : Thread() {

    var dir : Path = photo_storage
    private val TAG : String =  "photoSenderThread"
    private val storage = Firebase.storage("gs://sa-g4-a91ed.appspot.com")


    /**
     * Checks if folder exists and further checks for photos within
     * If photos are found sends them to db
     * TODO sleeps till awakened
     */
    override fun run() {
        while(true) {
            val exists = Files.exists(dir)
            Log.d(TAG,"Checking if folder ${dir.toString()} exists : $exists")
            if (exists and Files.isDirectory(dir)){
                var files = Files.list(dir)
                Log.d(TAG,"Checking photos")
                for(file in files){
                    sendFile(file)
                    files = Files.list(dir)
                    // Sleep to avoid too many requests
                    sleep(500)
                    }
            }
            Log.d(TAG,"Sleeping")
            sleep(5*1000)
        }
    }

    /**
     * Sends a file to firebase, with listeners for completion
     */
     private fun sendFile(file:Path){
         Log.d(TAG, "Sending file : ${file.toString()} ")
        val inputStream = file.inputStream()
        var bytes = inputStream.readBytes()
        val tsLong = System.currentTimeMillis() / 1000
        val ts = tsLong.toString()
        val myRef = storage.reference
        val im = myRef.child("images/$ts.jpg")
        im.putBytes(bytes).addOnSuccessListener {
            im.metadata.addOnSuccessListener {metadata->
                if (metadata.sizeBytes > 0) {
                Log.d(TAG, "Image sent at : $ts ")
                // Delete Photo
                file.deleteIfExists()
            }else{
                    Log.d(TAG, "Failed to send image : $ts ")
                }
            }
            inputStream.close()
        }.addOnCanceledListener {
            Log.d(TAG, "Failed to send image : $ts ")
            inputStream.close()
        }.addOnFailureListener{
            Log.d(TAG, "Failed to send image : $ts ")
            inputStream.close()
        }


    }

}