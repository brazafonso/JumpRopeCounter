package com.example.jumpropecounter.Camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.media.Image.Plane
import android.renderscript.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


/** Utility class for manipulating images.  */
object ImageUtils {
    // This value is 2 ^ 18 - 1, and is used to clamp the RGB values before their ranges
    // are normalized to eight bits.


    fun yuv420ToBitmap(image: Image, context: Context?): Bitmap? {
        val rs = RenderScript.create(context)
        val script = ScriptIntrinsicYuvToRGB.create(
            rs, Element.U8_4(rs)
        )

        // Refer the logic in a section below on how to convert a YUV_420_888 image
        // to single channel flat 1D array. For sake of this example I'll abstract it
        // as a method.
        val yuvByteArray: ByteArray = toYuvImage(image)!!.yuvData
        val yuvType: Type.Builder = Type.Builder(rs, Element.U8(rs))
            .setX(yuvByteArray.size)
        val `in` = Allocation.createTyped(
            rs, yuvType.create(), Allocation.USAGE_SCRIPT
        )
        val rgbaType: Type.Builder = Type.Builder(rs, Element.RGBA_8888(rs))
            .setX(image.width)
            .setY(image.height)
        val out = Allocation.createTyped(
            rs, rgbaType.create(), Allocation.USAGE_SCRIPT
        )

        // The allocations above "should" be cached if you are going to perform
        // repeated conversion of YUV_420_888 to Bitmap.
        `in`.copyFrom(yuvByteArray)
        script.setInput(`in`)
        script.forEach(out)
        val bitmap: Bitmap = Bitmap.createBitmap(
            image.width, image.height, Bitmap.Config.ARGB_8888
        )
        out.copyTo(bitmap)
        return bitmap
    }


    fun toJpegImage(image: Image, imageQuality: Int): ByteArray? {
        require((image.format == ImageFormat.YUV_420_888)) { "Invalid image format" }
        val yuvImage = toYuvImage(image)
        val width = image.width
        val height = image.height

        // Convert to jpeg
        var jpegImage: ByteArray? = null
        ByteArrayOutputStream().use { out ->
            yuvImage!!.compressToJpeg(Rect(0, 0, width, height), imageQuality, out)
            jpegImage = out.toByteArray()
        }
        return jpegImage
    }




    fun toYuvImage(image: Image): YuvImage? {
        require(image.format == ImageFormat.YUV_420_888) { "Invalid image format" }
        val width: Int = image.width
        val height: Int = image.height

        // Order of U/V channel guaranteed, read more:
        // https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888
        val yPlane: Plane = image.planes[0]
        val uPlane: Plane = image.planes[1]
        val vPlane: Plane = image.planes[2]
        val yBuffer: ByteBuffer = yPlane.buffer
        val uBuffer: ByteBuffer = uPlane.buffer
        val vBuffer: ByteBuffer = vPlane.buffer

        // Full size Y channel and quarter size U+V channels.
        val numPixels = (width * height * 1.5f).toInt()
        val nv21 = ByteArray(numPixels)
        var index = 0

        // Copy Y channel.
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        for (y in 0 until height) {
            for (x in 0 until width) {
                nv21[index++] = yBuffer.get(y * yRowStride + x * yPixelStride)
            }
        }

        // Copy VU data; NV21 format is expected to have YYYYVU packaging.
        // The U/V planes are guaranteed to have the same row stride and pixel stride.
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride
        val uvWidth = width / 2
        val uvHeight = height / 2
        for (y in 0 until uvHeight) {
            for (x in 0 until uvWidth) {
                val bufferIndex = y * uvRowStride + x * uvPixelStride
                // V channel.
                nv21[index++] = vBuffer.get(bufferIndex)
                // U channel.
                nv21[index++] = uBuffer.get(bufferIndex)
            }
        }
        return YuvImage(
            nv21, ImageFormat.NV21, width, height,  /* strides = */null
        )
    }

    // untested function
    fun toYUVByteArray(scaled: Bitmap): ByteArray? {
        val inputWidth = scaled.width
        val inputHeight = scaled.height
        val argb = IntArray(inputWidth * inputHeight)
        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        val yuv = ByteArray(inputWidth * inputHeight * 3 / 2)
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight)
        scaled.recycle()
        return yuv
    }

    fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        var a: Int
        var R: Int
        var G: Int
        var B: Int
        var Y: Int
        var U: Int
        var V: Int
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                a = argb[index] and -0x1000000 shr 24 // a is not used obviously
                R = argb[index] and 0xff0000 shr 16
                G = argb[index] and 0xff00 shr 8
                B = argb[index] and 0xff shr 0

                // well known RGB to YUV algorithm
                Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
                U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
                V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                    yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
                }
                index++
            }
        }
    }



}
