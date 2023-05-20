package com.example.jumpropecounter.JumpCounter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.jumpropecounter.Utils.ConcurrentFifo
import com.example.jumpropecounter.Utils.Frame
import android.util.Log
import com.example.jumpropecounter.User.Session
import com.firebase.ui.auth.AuthUI.getApplicationContext
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class JumpCounter (val concurrentFIFO: ConcurrentFifo<Frame>,session: Session?) : Thread() {

    private val TAG : String =  "JumpCounter"

    private val JUMP : String = "jump"
    private val NO_JUMP : String = "no_jump"
    private val ERROR : String = "error"


    // Shared FIFO between Preview and JumpCounter
    private val _concurrentFIFO : ConcurrentFifo<Frame> = concurrentFIFO

    // Current jump count
    private var _jumpCount : Int = 0
    val session = session


    fun loadBitmapFromAssets(context: Context, fileName: String): Bitmap? {
        return try {
            val inputStream = context.assets.open(fileName)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    @SuppressLint("RestrictedApi")
    override fun run()
    {
        Log.d(TAG, "Starting jump counter")

        // TEST ONLY: Load images from assets
        val img_test_jumping : Bitmap? = loadBitmapFromAssets(getApplicationContext(), "test_jumping.jpg")
        val img_test_not_jumping : Bitmap? = loadBitmapFromAssets(getApplicationContext(), "test_not_jumping.jpg")

        Log.d(TAG, "Adding test images to FIFO. Is empty: ${this._concurrentFIFO.isEmpty()}")
        //this._concurrentFIFO.enqueue(Frame(img_test_jumping, 0, isStart = true))
        //this._concurrentFIFO.enqueue(Frame(img_test_not_jumping, 1, isEnd = true))
        Log.d(TAG, "Added test images to FIFO. Is empty: ${this._concurrentFIFO.isEmpty()}")

        var module : Module? = null
        try {
            // Load ML model
            val afp = assetFilePath(getApplicationContext(), "model.ptl")
            module = Module.load(afp)
        }
        catch (e: Exception)
        {
            Log.d(TAG, "Failed to load ML model")
            Log.d(TAG, e.toString())
        }

        // Start jump counter
        if (module != null)
        {
            Log.d(TAG, "Starting jump counter")
            jumpCounter(module)
        }
    }

    private fun jumpCounter(module : Module)
    {
        // List of jump/non-jump frames. True = jump, False = non-jump
        val jumpList = mutableListOf<Boolean>()

        while (true)
        {
            if (!_concurrentFIFO.isEmpty()) {
                val frame = _concurrentFIFO.dequeue()
                if (frame.is_start())
                {
                    this._jumpCount = 0
                    jumpList.clear()
                }

                // Check if frame is null
                if(frame.get_frame() != null)
                {
                    val jumping : Boolean = getPrediction(module, frame)

                    // Check if we add a jump to the counter
                    if(jumping)
                    {
                        if (jumpList.isEmpty() or (!jumpList.isEmpty() && !jumpList.last()))
                        {
                            this._jumpCount++
                            if (session!=null)
                                session.total_reps++
                        }
                    }
                    jumpList.add(jumping)
                }

                // Check if we reached the end of the video
                if (frame.is_end())
                {
                    if (session!= null) { // Sends session to firebase
                        session.end = System.currentTimeMillis()
                        session.create_session_data()
                    }
                    Log.d(TAG, "Total jump count: ${this._jumpCount}")
                    Log.d(TAG, "Jump list: $jumpList")
                }
            }
        }
    }

    private fun getPrediction(module: Module, frame : Frame) : Boolean
    {
        val inputTensor : Tensor = TensorImageUtils.bitmapToFloat32Tensor(
            frame.get_frame(),
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )

        val outputTensor : Tensor = module.forward(IValue.from(inputTensor)).toTensor()
        val score : FloatArray = outputTensor.dataAsFloatArray
        val prediction : Int = score.indices.maxByOrNull { score[it] } ?: -1
        val predictionLabel : String = outputLabel(prediction)

        Log.d(TAG, "Prediction: $predictionLabel")

        return predictionLabel == JUMP
    }

    private fun assetFilePath(context: Context, asset: String): String {
        val file = File(context.filesDir, asset)

        try {
            val inpStream: InputStream = context.assets.open(asset)
            try {
                val outStream = FileOutputStream(file, false)
                val buffer = ByteArray(4 * 1024)
                var read: Int

                while (true) {
                    read = inpStream.read(buffer)
                    if (read == -1) {
                        break
                    }
                    outStream.write(buffer, 0, read)
                }
                outStream.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun outputLabel(prediction: Int): String {
        return when (prediction) {
            0 -> NO_JUMP
            1 -> JUMP
            else -> ERROR
        }
    }
}