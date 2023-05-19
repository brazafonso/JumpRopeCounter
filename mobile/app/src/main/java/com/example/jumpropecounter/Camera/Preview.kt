package com.example.jumpropecounter.Camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.SparseIntArray
import android.view.*
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatToggleButton
import androidx.fragment.app.Fragment
import com.example.jumpropecounter.DB.Fragments.PhotoSender
import com.example.jumpropecounter.JumpCounter.JumpCounter
import com.example.jumpropecounter.R
import com.example.jumpropecounter.Utils.ConcurrentFifo
import com.example.jumpropecounter.Utils.Frame
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.*

class Preview: Fragment(R.layout.preview) {

    private lateinit var activity:Activity
    private lateinit var previewTextureView :TextureView
    private lateinit var imageReader: ImageReader
    private lateinit var swap_camera_btn:ImageButton
    private lateinit var capture_btn:AppCompatToggleButton
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraDevice: CameraDevice
    private lateinit var counter: Thread
    private lateinit var sender: Thread


    companion object {
        fun newInstance(frameRate:Int,video_storage:String,mode:Int):Preview{
            val fragment = Preview()
            val args = Bundle()
            args.putInt("FRAMERATE",frameRate)
            args.putString("video_storage",video_storage)
            args.putInt("mode",mode)
            fragment.arguments = args
            return fragment
        }

        private val TAG = "preview"
        private val MAX_PREVIEW_WIDTH = 1280
        private val MAX_PREVIEW_HEIGHT = 720
        private var FRAME_WIDTH = 320
        private var FRAME_HEIGTH = 240
        private var FRAMERATE = 20
        private var MODE = 0 // opertaion mode (normal or dev)
        private var N_SEQ = 0
        private var recording = false
        private var framesFifo =  ConcurrentFifo<Frame>() // stack to store frames
        private lateinit var backgroundThread: HandlerThread
        private lateinit var backgroundHandler: Handler
        

        // For saving video or frames
        private lateinit var video_storage: Path
        private lateinit var video_file: File

        private var current_lens = CameraCharacteristics.LENS_FACING_BACK

    }


    private val deviceStateCallback = object: CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "camera device opened")
            cameraDevice = camera
            previewSession()

        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "camera device disconnected")
            camera.close()
        }

        override fun onError(camera: CameraDevice, p1: Int) {
            Log.d(TAG, "camera device error")
            this@Preview.activity.finish()
        }
    }

    private val surfaceListener = object: TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture) = Unit

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean = true

        override fun onSurfaceTextureAvailable(p0: SurfaceTexture, width: Int, height: Int) {
            Log.d(TAG, "textureSurface width: $width height: $height")
            openCamera()
        }

    }



    private val cameraManager by lazy {
        activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            Log.d(TAG,"Got Bundle")
            FRAMERATE = requireArguments().getInt("FRAMERATE")
            video_storage = requireArguments().getString("video_storage")?.let { Path(it) }!!
            MODE = requireArguments().getInt("mode")
        }
        Log.d(TAG,"Framerate of $FRAMERATE")
        Log.d(TAG,"VideoStorage at $video_storage")

    }





    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG,"Starting preview fragment")

        activity = requireActivity()
        /**
        //Creating file to store video
        if(!video_storage.exists())
            video_storage.createDirectories()
        video_file = createVideoFile()
*/
        capture_btn = activity.findViewById(R.id.btn_camera)
        swap_camera_btn = activity.findViewById(R.id.swap_camera)
        previewTextureView = activity.findViewById(R.id.textView)
        // Capture button
        capture_btn.setOnCheckedChangeListener{ _, isChecked ->
                if(isChecked){
                    Log.d(TAG,"Capturing video")
                    disable_swap_camera()
                    if(MODE==0)
                        start_counter()
                    else
                        start_sender()
                    recording = true
                }
                else{
                    recording = false
                    enable_swap_camera()
                    if(MODE==0)
                        stop_counter()
                    else
                        stop_sender()
                }
            }
        swap_camera_btn.setOnClickListener { _ ->
            if(current_lens == CameraCharacteristics.LENS_FACING_BACK)
                current_lens = CameraCharacteristics.LENS_FACING_FRONT
            else
                current_lens = CameraCharacteristics.LENS_FACING_BACK
            closeCamera()
            openCamera()
        }

        startBackgroundThread()
        imageReader = ImageReader.newInstance(FRAME_WIDTH, FRAME_HEIGTH, ImageFormat.YUV_420_888, 1)
        imageReader.setOnImageAvailableListener({ reader ->
            //Log.d(TAG, "Image available")
            if (reader != null) {
                val image = reader.acquireNextImage()
                if (recording) {
                    Log.d(TAG,"Saving frame")
                    val bitmap = ImageUtils.yuv420ToBitmap(image, context)
                    val frame = Frame(bitmap, N_SEQ)
                    framesFifo.enqueue(frame)
                    N_SEQ++
                }
                image.close()
            }
        }
        , backgroundHandler)

    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (previewTextureView.isAvailable) {
            openCamera()
        }
        else
            previewTextureView.surfaceTextureListener = surfaceListener
    }

    override fun onPause() {
        super.onPause()
        recording = false
        closeCamera()
        stopBackgroundThread()
    }

    /**
     * Thread that will analyse the frames and check whether event happened
     */
    private fun start_counter(){
        counter = JumpCounter(framesFifo)
        counter.start()
        // mandar frame start vazia
        framesFifo.enqueue(Frame(null,-1,true))
    }

    /**
     * Sends empty frame that makes counter thread stop
     */
    private fun stop_counter(){
        // mandar frame end vazia
        framesFifo.enqueue(Frame(null,-1, isEnd = true))
    }

    /**
     * TODO
     * Thread that will send the frames to the database
     */
    private fun start_sender(){
        sender = PhotoSender(framesFifo, FRAMERATE)
        sender.start()
        framesFifo.enqueue(Frame(null,-1,true))
    }

    /**
     * TODO
     * Stop sender thread
     */
    private fun stop_sender(){
        framesFifo.enqueue(Frame(null,-1, isEnd = true))
    }



    /**
     * Create a recording capture to camera
     */
    private fun previewSession() {

        val surfaceTexture = previewTextureView.surfaceTexture
        surfaceTexture!!.setDefaultBufferSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
        val textureSurface = Surface(surfaceTexture)

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(textureSurface)
        captureRequestBuilder.addTarget(imageReader.surface)
        val surfaces = ArrayList<Surface>().apply {
            add(textureSurface)
            add(imageReader.surface)
        }

        cameraDevice.createCaptureSession(surfaces,
            object: CameraCaptureSession.StateCallback(){
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "creating record session failed!")
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                }

            }, backgroundHandler)
    }

    private fun closeCamera() {
        if (this::captureSession.isInitialized)
            captureSession.close()
        if (this::cameraDevice.isInitialized)
            cameraDevice.close()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camara2 Kotlin").also { it.start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        try {
            backgroundThread.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
    }


    /**
     * Returns the list of characteristics of the camera
     */
    private fun <T> cameraCharacteristics(cameraId: String, key: CameraCharacteristics.Key<T>) : T? {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        return when (key) {
            CameraCharacteristics.LENS_FACING -> characteristics.get(key)
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP -> characteristics.get(key)
            CameraCharacteristics.SENSOR_ORIENTATION -> characteristics.get(key)
            else -> throw  IllegalArgumentException("Key not recognized")
        }
    }

    /**
     * Gets camera id with the given characteristics
     */
    private fun cameraId(lens: Int) : String {
        var deviceId = listOf<String>()
        try {
            val cameraIdList = cameraManager.cameraIdList
            deviceId = cameraIdList.filter { lens == cameraCharacteristics(it, CameraCharacteristics.LENS_FACING) }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
        return deviceId[0]
    }




    @SuppressLint("MissingPermission")
    /**
     * Connect to camera using lens with given characteristic (such as facing back)
     */
    private fun connectCamera(LensCharacteristics: Int) {
        val deviceId = cameraId(LensCharacteristics)
        Log.d(TAG, "deviceId: $deviceId")
        try {
            cameraManager.openCamera(deviceId, deviceStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Exception: $e")
        } catch (e: InterruptedException) {
            Log.e(TAG, "Open camera device interrupted while opened")
        }
    }


    private fun openCamera() {
        connectCamera(current_lens)
    }


    private fun enable_swap_camera(){
        swap_camera_btn.visibility = View.VISIBLE
        swap_camera_btn.isEnabled = true
    }

    private fun disable_swap_camera(){
        swap_camera_btn.visibility = View.INVISIBLE
        swap_camera_btn.isEnabled = false
    }

}