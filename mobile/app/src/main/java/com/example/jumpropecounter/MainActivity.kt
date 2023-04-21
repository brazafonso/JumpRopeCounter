package com.example.jumpropecounter


import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage


class MainActivity : AppCompatActivity() {
    lateinit var handler: Handler
    lateinit var handlerThread: HandlerThread
    lateinit var capReq: CaptureRequest.Builder
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var capture_btn: Button
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
    private val TAG : String =  "MainActivity"
    lateinit var imageReader: ImageReader
    val storage = Firebase.storage("gs://sa-g4-a91ed.appspot.com")

    /**
     * Creating app
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG,"Loading View")
        setContentView(R.layout.activity_main)
        Log.d(TAG,"Starting Program")

        get_permissions()


        textureView = findViewById(R.id.textView)
        capture_btn = findViewById(R.id.btn_camera)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler((handlerThread).looper)

        // element that will show the captured images
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }
        }
        capture_btn.apply {
            setOnClickListener{
                capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                capReq.addTarget(imageReader.surface)
                cameraCaptureSession.capture(capReq.build(),null,null)
            }
        }
        imageReader = ImageReader.newInstance(1080,1920,ImageFormat.JPEG,1)
        imageReader.setOnImageAvailableListener(object : ImageReader.OnImageAvailableListener{
            override fun onImageAvailable(p0: ImageReader?) {
                // get image
                var image = p0?.acquireLatestImage()
                // get image bytes
                var buffer = image!!.planes[0].buffer
                var bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                val tsLong = System.currentTimeMillis() / 1000
                val ts = tsLong.toString()
                Log.d(TAG, ts)
                val myRef = storage.reference
                val im = myRef.child("images/$ts.jpg")

                im.putBytes(bytes)

                image.close()
                Log.d(TAG,"Image Captured")
                Toast.makeText(this@MainActivity,"image captured",Toast.LENGTH_SHORT).show()
            }
        },handler)


        Log.d(TAG,"Finishing Program")
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

    /**
     * Opens camera and starts casting to texture surface
     */
    @SuppressLint("MissingPermission")
    fun open_camera(){
        Log.d(TAG,"Opening Camera")
        // openCamera chooses receives a camera and then the callback functions
        cameraManager.openCamera(cameraManager.cameraIdList[0],object: CameraDevice.StateCallback() {
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0
                capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                var surface = Surface(textureView.surfaceTexture)
                // define what to do with the captured images. send them to the texture element
                capReq.addTarget(surface)
                cameraDevice.createCaptureSession(listOf(surface,imageReader.surface),object: CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        cameraCaptureSession = p0
                        cameraCaptureSession.setRepeatingRequest(capReq.build(),null,null)
                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                        TODO("Not yet implemented")
                    }
                },handler)
            }

            override fun onDisconnected(p0: CameraDevice) {
                Log.d(TAG,"Camera disconnected")
            }

            override fun onError(p0: CameraDevice, p1: Int) {
                Log.d(TAG,"Camera error")
            }
        },handler)
    }

}