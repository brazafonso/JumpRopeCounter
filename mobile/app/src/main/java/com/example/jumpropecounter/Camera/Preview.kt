package com.example.jumpropecounter.Camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.SparseIntArray
import android.view.*
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatToggleButton
import androidx.fragment.app.Fragment
import com.example.jumpropecounter.R
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.io.path.*

class Preview: Fragment(R.layout.preview) {

    private lateinit var activity:Activity
    private val TAG = "preview"
    private val MAX_PREVIEW_WIDTH = 1280
    private val MAX_PREVIEW_HEIGHT = 720
    private var FRAMERATE = 20
    private lateinit var video_storage: Path
    private lateinit var video_file: File
    private lateinit var previewTextureView :TextureView
    private lateinit var capture_btn:AppCompatToggleButton
    private lateinit var swap_camera_btn:ImageButton
    private var current_lens = CameraCharacteristics.LENS_FACING_BACK
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private val mediaRecorder by lazy {
        MediaRecorder()
    }

    private lateinit var cameraDevice: CameraDevice

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

    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    //TODO: permitir trocar entre as diferentes camaras disponiveis (pelo menos frente e tras)
    companion object {
        fun newInstance(frameRate:Int,video_storage:String):Preview{
            val fragment = Preview()
            val args = Bundle()
            args.putInt("FRAMERATE",frameRate)
            args.putString("video_storage",video_storage)
            fragment.arguments = args
            return fragment
        }
        private val SENSOR_DEFAULT_ORINTATION_DEGREES = 90
        private val SENSOR_INVERSE_ORINTATION_DEGREES = 270
        private val DEFAULT_ORIENTATION = SparseIntArray().apply {
            append(Surface.ROTATION_0, 90)
            append(Surface.ROTATION_90, 0)
            append(Surface.ROTATION_180, 270)
            append(Surface.ROTATION_270, 180)
        }
        private val INVERSE_ORIENTATION = SparseIntArray().apply {
            append(Surface.ROTATION_0, 270)
            append(Surface.ROTATION_90, 180)
            append(Surface.ROTATION_180, 90)
            append(Surface.ROTATION_270, 0)
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
        }
        Log.d(TAG,"Framerate of $FRAMERATE")
        Log.d(TAG,"VideoStorage at $video_storage")

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG,"Starting preview fragment")

        activity = requireActivity()
        activity.setContentView(R.layout.preview)
        //Creating file to store video
        if(!video_storage.exists())
            video_storage.createDirectories()
        video_file = createVideoFile()

        capture_btn = activity.findViewById(R.id.btn_camera)
        swap_camera_btn = activity.findViewById(R.id.swap_camera)
        previewTextureView = activity.findViewById(R.id.textView)
        // Capture button
        capture_btn.setOnCheckedChangeListener{ _, isChecked ->
                if(isChecked){
                    Log.d(TAG,"Capturing video")
                    disable_swap_camera()
                    startRecordSession()
                }
                else{
                    stopRecordSession()
                    enable_swap_camera()
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
    }





    /**
     * Create a request for the camera, used while not recording to preview camera capture
     */
    private fun previewSession() {
        val surfaceTexture = previewTextureView.surfaceTexture
        surfaceTexture!!.setDefaultBufferSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
        val surface = Surface(surfaceTexture)

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)

        cameraDevice.createCaptureSession(Arrays.asList(surface),
            object: CameraCaptureSession.StateCallback(){
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "creating capture session failded!")
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    if (session != null) {
                        captureSession = session
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                    }
                }

            }, backgroundHandler)
    }

    /**
     * Create a recording request to camera, used while recording
     */
    private fun recordSession() {

        setupMediaRecorder()

        val surfaceTexture = previewTextureView.surfaceTexture
        surfaceTexture!!.setDefaultBufferSize(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
        val textureSurface = Surface(surfaceTexture)
        val recordSurface = mediaRecorder.surface

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        captureRequestBuilder.addTarget(textureSurface)
        captureRequestBuilder.addTarget(recordSurface)
        val surfaces = ArrayList<Surface>().apply {
            add(textureSurface)
            add(recordSurface)
        }

        cameraDevice.createCaptureSession(surfaces,
            object: CameraCaptureSession.StateCallback(){
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "creating record session failed!")
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    if (session != null) {
                        captureSession = session
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                        mediaRecorder.start()
                    }
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

    @SuppressLint("SimpleDateFormat")
    private fun createVideoFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return "VIDEO_${timestamp}.mp4"
    }

    private fun createVideoFile(): File {
        val videoFile = File(video_storage.toString(), createVideoFileName())
        return videoFile
    }



    private fun setupMediaRecorder() {
        val rotation = activity.windowManager?.defaultDisplay?.rotation
        val sensorOrientation = cameraCharacteristics(
            cameraId(CameraCharacteristics.LENS_FACING_BACK),
            CameraCharacteristics.SENSOR_ORIENTATION
        )
        //TODO: verificar resoluções diponiveis e escolher a menor mais apropriada
        val resolutions = cameraManager.getCameraCharacteristics(cameraId(CameraCharacteristics.LENS_FACING_BACK)).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?.getOutputSizes(ImageFormat.JPEG)
        if (resolutions != null) {
            Log.d(TAG, "Resolutions" )
            for (resolution in resolutions){
                Log.d(TAG, resolution.toString())
            }
        }

        when (sensorOrientation) {
            SENSOR_DEFAULT_ORINTATION_DEGREES ->
                mediaRecorder.setOrientationHint(DEFAULT_ORIENTATION.get(rotation!!))
            SENSOR_INVERSE_ORINTATION_DEGREES ->
                mediaRecorder.setOrientationHint(INVERSE_ORIENTATION.get(rotation!!))
        }
        mediaRecorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(video_file.outputStream().fd)
            setVideoEncodingBitRate(10000000)
            setVideoFrameRate(FRAMERATE)
            setVideoSize(320,240)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            prepare()
        }
    }

    private fun stopMediaRecorder() {
        mediaRecorder.apply {
            try {
                stop()
                reset()
            } catch (e: IllegalStateException) {
                Log.e(TAG, e.toString())
            }
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


    private fun startRecordSession() {
        recordSession()
    }

    private fun stopRecordSession() {
        stopMediaRecorder()
        previewSession()
    }


    override fun onResume() {
        super.onResume()

        startBackgroundThread()
        if (previewTextureView.isAvailable)
            openCamera()
        else
            previewTextureView.surfaceTextureListener = surfaceListener
    }

    override fun onPause() {

        closeCamera()
        stopBackgroundThread()
        super.onPause()
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