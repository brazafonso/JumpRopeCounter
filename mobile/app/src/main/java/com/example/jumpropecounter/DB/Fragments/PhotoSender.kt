package com.example.jumpropecounter.DB.Fragments

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.random.Random


/**
 * Class that receives a folder from where to check if any new photos are available
 * and sends them to firebase db (deleting them from the folder)
 */
class PhotoSender(recording_folder: String,frameRate: Int) : Thread() {
    private var FRAMERATE = frameRate
    private var recording_folder : Path = Path(recording_folder)
    private var frames_folder : Path = Path(recording_folder.toString()+"frames")
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
            val exists = Files.exists(recording_folder)
            Log.d(TAG,"Checking if folder ${recording_folder.toString()} exists : $exists")
            // Checks videos folders
            if (exists and Files.isDirectory(recording_folder)){
                var files = Files.list(recording_folder).toArray()
                if(files.isNotEmpty()) {
                    files.sort()
                    Log.d(TAG, "Checking videos")
                    while (files.isNotEmpty()) {
                        val file = files[0] as Path
                        if (file.fileSize() >0) {
                            val path = get_frames(file)
                            if (path != null) {
                                send_frames(path)
                            }
                        }else
                            file.deleteIfExists()
                        files = Files.list(recording_folder).toArray()
                        if (files.isNotEmpty()) {
                            files.sort()
                        }
                    }
                }
                // Check frames folders
                else{
                    Log.d(TAG, "Checking frames folders")
                    if (Files.exists(frames_folder) and Files.isDirectory(frames_folder)){
                        var folders = Files.list(frames_folder).toArray()
                        if(folders.isNotEmpty()) {
                            folders.sort()
                            Log.d(TAG, "Checking frames")
                            val folder = folders[0] as Path
                            send_frames(folder)
                            folder.deleteIfExists()
                        }
                    }
                }
            }
            Log.d(TAG,"Sleeping")
            sleep(5*1000)
        }
    }

    /**
     * Transform a video into frames and saves them in a separate folder
     * Returns the path of said folder
     */
    fun get_frames(file:Path):Path?{
        Log.d(TAG,"Geting frames from video")
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.toString())
        var picture_folder:Path? = null
        var n_frames = retriever.extractMetadata(METADATA_KEY_VIDEO_FRAME_COUNT)?.toInt()
        if (n_frames!=null && n_frames > 0) {
            picture_folder = Path("$frames_folder/${file.nameWithoutExtension}")
            try {
                picture_folder.createDirectories()
            }
            catch (_:Exception){

            }
            var frame = 0
            if (file.toString() == current_file) {
                frame = current_frame
            } else {
                current_file = file.toString()
            }
            // Creates a picture for each frame
            while (frame < n_frames!!) {
                // TODO: Somewhere here, put the bitmap in a Frame object and send it to the database
                //  Use the ConcurrentFifo class to enqueue the frames

                val frameBitMap = retriever.getFrameAtIndex(frame)
                if (frameBitMap != null) {
                    val newFile = Path("$frames_folder/${file.nameWithoutExtension}/" +"${file.nameWithoutExtension}_$frame.jpeg")
                    newFile.createFile()
                    val fout = newFile.outputStream()
                    frameBitMap.compress(Bitmap.CompressFormat.JPEG,100,fout)
                    fout.flush()
                    fout.close()
                }
                frame++
                n_frames = retriever.extractMetadata(METADATA_KEY_VIDEO_FRAME_COUNT)?.toInt()
            }
            current_frame = frame
            // If all frames have been read, deletes file
            if (retriever.extractMetadata(METADATA_KEY_VIDEO_FRAME_COUNT)?.toInt() == frame)
                file.deleteIfExists()
        }
        retriever.close()
        return picture_folder
    }

    /**
     * Goes to frames folder and sends all frames to database
     */
    fun send_frames(path: Path){
        Log.d(TAG,"Sending Frames from $path")
        var files = Files.list(path).toArray()
        while(files.isNotEmpty()){
            files = Files.list(path).toArray()
            try {
                val file = files[Random(262).nextInt(0,files.size)] as Path
                Log.d(TAG, "Frame $file")
                if (file.fileSize() > 0) {
                    if (!processing_files.contains(file.toString())) {
                        processing_files.add(file.toString())
                        sendFile(file)
                    }
                } else {
                    file.deleteIfExists()
                }
            }
            catch (e:Exception){

            }
            // Sleep to avoid too many requests
            sleep(500)
        }
    }

    /**
     * Sends a file to firebase, with listeners for completion
     */
     private fun sendFile(file:Path){
         Log.d(TAG, "Sending file : ${file.toString()} ")
        val inputStream = file.inputStream()
        var bytes = inputStream.readBytes()
        inputStream.close()
        val tsLong = System.currentTimeMillis() / 1000
        val ts = tsLong.toString()
        val myRef = storage.reference
        val im = myRef.child("images/${file.fileName}")
        im.putBytes(bytes).addOnSuccessListener {
            im.metadata.addOnSuccessListener {metadata->
                if (metadata.sizeBytes > 0) {
                Log.d(TAG, "Image sent at : $ts ")
                processing_files.remove(file.toString())
                // Delete Photo
                file.deleteIfExists()
            }else{
                    Log.d(TAG, "Failed to send image : $ts ")
                    processing_files.remove(file.toString())
                }
            }
        }.addOnCanceledListener {
            Log.d(TAG, "Failed to send image : $ts ")
            processing_files.remove(file.toString())
        }.addOnFailureListener{
            Log.d(TAG, "Failed to send image : $ts ")
            processing_files.remove(file.toString())
        }


    }

}