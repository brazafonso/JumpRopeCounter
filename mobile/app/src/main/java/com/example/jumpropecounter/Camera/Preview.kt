package com.example.jumpropecounter.Camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.*
import android.opengl.Visibility
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.AppCompatToggleButton
import androidx.fragment.app.Fragment
import com.example.jumpropecounter.DB.Fragments.PhotoSender
import com.example.jumpropecounter.JumpCounter.JumpCounter
import com.example.jumpropecounter.R
import com.example.jumpropecounter.User.Session
import com.example.jumpropecounter.Utils.ConcurrentFifo
import com.example.jumpropecounter.Utils.Frame
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.concurrent.thread
import kotlin.io.path.*
import kotlin.properties.Delegates

class Preview: Fragment(R.layout.preview) {
    // Default values for capture
    private val TAG = "preview"
    private lateinit var type_activity:String
    private val MAX_PREVIEW_WIDTH = 320
    private val MAX_PREVIEW_HEIGHT = 240
    private var FRAME_WIDTH = 320
    private var FRAME_HEIGTH = 240
    private var FRAMERATE = 20
    private var min_capture_rest:Long = (1/FRAMERATE * 1000).toLong() // miliseconds between frame capture
    private var MODE = 0 // operation mode (normal-0 or dev-1), defines if frames are sent to firebase or to ai model
    private var framesFifo =  ConcurrentFifo<Frame>() // stack to store frames
    private var current_lens = CameraCharacteristics.LENS_FACING_BACK //default lens
    private var last_capture:Long = 0
    private lateinit var swap_camera_btn:ImageButton
    private lateinit var capture_btn:AppCompatToggleButton
    private lateinit var camera_timer:Spinner
    private lateinit var timer_countdown:TextView
    private var recording = false

    // Setup observable counter for the activities that create this fragment
    var counter_refreshListListeners = ArrayList<InterfaceRefreshList>()
    var total_reps:Int by Delegates.observable(0){ property, oldValue, newValue ->
        counter_refreshListListeners.forEach {
            it.refreshListRequest()
        }
    }

    // Setup observable frame sequence number for the activities that create this fragment
    var N_SEQ_refreshListListeners = ArrayList<InterfaceRefreshList>()
    var N_SEQ:Int by Delegates.observable(0){ property, oldValue, newValue ->
        N_SEQ_refreshListListeners.forEach {
            it.refreshListRequest()
        }
    }

    interface InterfaceRefreshList {
        fun refreshListRequest()
    }

    // Necessary elements for capture
    private lateinit var activity:Activity
    private lateinit var previewTextureView :TextureView
    private lateinit var imageReader: ImageReader
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraDevice: CameraDevice
    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var start_thread : Thread
    private val cameraManager by lazy {
        activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }


    private lateinit var counter: Thread // Thread to run ai model
    private lateinit var sender: Thread // Thread to run sender of frames to firebase storage
    private var session: Session? = null// Session that will be sent to firebase to save the results of activity
    private var user  = FirebaseAuth.getInstance().currentUser // current logged user


    // For saving video or frames
    private lateinit var video_storage: Path
    private lateinit var video_file: File


    companion object {
        fun newInstance(frameRate:Int,video_storage:String?,type_activity:String?,mode:Int):Preview{
            val fragment = Preview()
            val args = Bundle()
            args.putInt("FRAMERATE",frameRate)
            args.putString("video_storage",video_storage)
            args.putString("type_activity",type_activity)
            args.putInt("mode",mode)
            fragment.arguments = args
            return fragment
        }
    }

    // Camera open callback
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

    // Preview surface listener
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



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            Log.d(TAG,"Got Bundle")
            FRAMERATE = requireArguments().getInt("FRAMERATE")
            video_storage = requireArguments().getString("video_storage")?.let { Path(it) }!!
            type_activity = requireArguments().getString("type_activity")?:""
            MODE = requireArguments().getInt("mode")
            min_capture_rest = (1/FRAMERATE * 1000).toLong()
        }
        Log.d(TAG,"Framerate of $FRAMERATE")
        Log.d(TAG,"VideoStorage at $video_storage")
        Log.d(TAG,"Type activity $type_activity")

    }



    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG,"Starting preview fragment")

        activity = requireActivity()
        capture_btn = activity.findViewById(R.id.btn_camera)
        swap_camera_btn = activity.findViewById(R.id.swap_camera)
        previewTextureView = activity.findViewById(R.id.textView)
        camera_timer = activity.findViewById(R.id.camera_timer)
        timer_countdown = activity.findViewById(R.id.timer_countdown)

        prepare_timer()


        // Capture button
        capture_btn.setOnCheckedChangeListener{ _, isChecked ->
                if(isChecked){
                    // Start recording
                    Log.d(TAG,"Capturing video")
                    disable_swap_camera() // Cant change camera during recording
                    start_capturing()
                }
                else{
                    // Stop recording
                    stop_capturing()
                }
            }
        // Swap camera button
        swap_camera_btn.setOnClickListener { _ ->
            if(current_lens == CameraCharacteristics.LENS_FACING_BACK)
                current_lens = CameraCharacteristics.LENS_FACING_FRONT
            else
                current_lens = CameraCharacteristics.LENS_FACING_BACK
            closeCamera()
            openCamera()
        }

        startBackgroundThread()
        // ImageReader allows to save frames
        imageReader = ImageReader.newInstance(FRAME_WIDTH, FRAME_HEIGTH, ImageFormat.YUV_420_888, 1)
        imageReader.setOnImageAvailableListener({ reader ->
            //Log.d(TAG, "Image available")
            if (reader != null) {
                val current_time = System.currentTimeMillis()
                val image = reader.acquireNextImage()
                // only treats frame if its in recording mode and respects frame rate
                if (recording && current_time - last_capture >= min_capture_rest ) {
                    //Log.d(TAG,"Saving frame")
                    last_capture = current_time
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
        Log.d(TAG, "Ended recording with $N_SEQ frames")
        closeCamera()
        stopBackgroundThread()
    }

    override fun onDestroy() {
        super.onDestroy()
        recording = false
        Log.d(TAG, "Ended recording with $N_SEQ frames")
        closeCamera()
        stopBackgroundThread()
        activity.finish()
    }


    /**
     * Starts capturing, having into account the chosen timer
     */
    private fun start_capturing(){
        disable_timer()
        disable_capture_btn()
        var time = camera_timer.selectedItem as String
        if(time == "No wait") time = "0 s"
        var time_value = time.split(" ")[0].toInt()
        update_countdown_timer(time_value.toString())
        if(time_value > 0)
            enable_countdown_timer()
        start_thread = Thread {
            while(time_value > 0){
                update_countdown_timer(time_value.toString())
                Thread.sleep(1000)
                time_value -= 1
            }
            disable_countdown_timer()

            if(MODE==0) // Normal mode (count activity)
                start_counter()
            else // Dev mode, send frames to storage
                start_sender()
            N_SEQ = 0
            recording = true
            enable_capture_btn()
        }
        start_thread.start()
    }

    /**
     * Stops capturing session
     */
    private fun stop_capturing(){
        recording = false
        start_thread.interrupt()
        N_SEQ = 0
        enable_swap_camera()
        enable_timer()
        if(MODE==0)
            stop_counter()
        else
            stop_sender()
    }


    /**
     * Prepares timer spinner
     */
    private fun prepare_timer(){
        val items = arrayOf("No wait","2 s","4 s","6 s","8 s","10 s")
        val adapter = ArrayAdapter(requireContext(),R.layout.spinner,items)
        camera_timer.adapter = adapter
    }



    /**
     * Updates value of countdown text
     */
    fun update_countdown_timer(time_value:String){
        activity.runOnUiThread {
            timer_countdown.text = time_value
        }
    }

    /**
     * Enables countdown text
     */
    fun enable_countdown_timer(){
        activity.runOnUiThread {
            timer_countdown.visibility = View.VISIBLE
        }
    }

    /**
     * Disables countdown text
     */
    fun disable_countdown_timer(){
        activity.runOnUiThread {
            timer_countdown.visibility = View.INVISIBLE
        }
    }

    /**
     * Enables the camera timer
     */
    private fun enable_timer(){
        activity.runOnUiThread {
            camera_timer.visibility = View.VISIBLE
        }
    }

    /**
     * Disables the camera timer
     */
    private fun disable_timer(){
        activity.runOnUiThread {
            camera_timer.visibility = View.INVISIBLE
        }
    }

    /**
     * Enables the capture button
     */
    private fun enable_capture_btn(){
        activity.runOnUiThread {
            capture_btn.visibility = View.VISIBLE
        }
    }

    /**
     * Disables the capture button
     */
    private fun disable_capture_btn(){
        activity.runOnUiThread {
            capture_btn.visibility = View.INVISIBLE
        }
    }

    /**
     * Thread that will analyse the frames and check whether event happened
     */
    private fun start_counter(){
        // if user available will create a session to save results
        if(user!=null) {
            session = Session(user!!.uid,type_activity)
            session!!.update_with_user_date()
            // updates total reps value with session counter
            session!!.counter_refreshListListeners.add(object : Session.InterfaceRefreshList {
                override fun refreshListRequest() {
                    total_reps = session!!.total_reps
                }
            })
        }
        counter = JumpCounter(framesFifo,session)
        counter.start()
        // Send the start frame, with no image
        framesFifo.enqueue(Frame(null,-1,true))
    }

    /**
     * Sends empty frame that makes counter thread stop
     */
    private fun stop_counter(){
        // Send end frame, with no image, allows thread to know when to stop
        framesFifo.enqueue(Frame(null,-1, isEnd = true))
        Log.d(TAG,"Total jumps: $total_reps")
    }

    /**
     * Thread that will send the frames to the database
     */
    private fun start_sender(){
        sender = PhotoSender(framesFifo, FRAMERATE)
        sender.start()
        framesFifo.enqueue(Frame(null,-1,true))
    }

    /**
     * Stop sender thread
     */
    private fun stop_sender(){
        framesFifo.enqueue(Frame(null,-1, isEnd = true))
    }



    /**
     * Create a session to capture from camera
     */
    private fun previewSession() {

        val surfaceTexture = previewTextureView.surfaceTexture
        surfaceTexture!!.setDefaultBufferSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
        val textureSurface = Surface(surfaceTexture)

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        // Send image to preview surface and image reader
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

    /**
     * Terminates the capture session
     */
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


    /**
     * Enable swap camera button
     */
    private fun enable_swap_camera(){
        swap_camera_btn.visibility = View.VISIBLE
        swap_camera_btn.isEnabled = true
    }

    /**
     * Disable swap camera button
     */
    private fun disable_swap_camera(){
        swap_camera_btn.visibility = View.INVISIBLE
        swap_camera_btn.isEnabled = false
    }

}