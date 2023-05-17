package com.example.jumpropecounter.JumpCounter
import android.annotation.SuppressLint
import android.content.Context
import com.example.jumpropecounter.Utils.ConcurrentFifo
import com.example.jumpropecounter.Utils.Frame
import android.util.Log
import com.firebase.ui.auth.AuthUI.getApplicationContext
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class JumpCounter (val concurrentFIFO: ConcurrentFifo<Frame>) : Thread() {

    private val TAG : String =  "JumpCounter"

    private val JUMP : String = "jump"
    private val NO_JUMP : String = "no_jump"
    private val ERROR : String = "error"

    // Shared FIFO between Preview and JumpCounter
    private val _concurrentFIFO : ConcurrentFifo<Frame> = concurrentFIFO


    @SuppressLint("RestrictedApi")
    override fun run()
    {
        try {
            // Load ML model
            val module : Module = Module.load(assetFilePath(getApplicationContext(), "model.pt"))

            // Start jump counter
            while (true)
            {
                jumpCounter(module)
            }

        }
        catch (e: Exception)
        {
            Log.d(TAG, "Failed to load ML model")
        }
    }

    private fun jumpCounter(module : Module)
    {
        var jumpCount = 0

        // List of jump/non-jump frames. True = jump, False = non-jump
        val jumpList = mutableListOf<Boolean>()

        if (!_concurrentFIFO.isEmpty()) {
            val frame = _concurrentFIFO.dequeue()
            if (frame.is_start())
            {
                jumpCount = 0
                jumpList.clear()
            }

            val jumping : Boolean = getPrediction(module, frame)

            // Check if we add a jump to the counter
            if(jumping)
            {
                if (jumpList.isEmpty() or !jumpList.last())
                {
                    jumpCount++
                }
            }
            jumpList.add(jumping)

            // Check if we reached the end of the video
            if (frame.is_end())
            {
                Log.d(TAG, "Total jump count: $jumpCount")
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