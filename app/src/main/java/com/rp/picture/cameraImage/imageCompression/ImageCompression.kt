package com.rp.picture.cameraImage.imageCompression

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.graphics.createBitmap
import kotlin.math.roundToInt

class ImageCompression(
    private val context: Context,
    private val filePath: String,
    private val listener: ImageCompressionListener
) {

    companion object {
        private const val maxHeight = 700f
        private const val maxWidth = 700f
    }

    fun compress() {

        CoroutineScope(Dispatchers.IO).launch {

            val result = compressImage(filePath)

            withContext(Dispatchers.Main) {
                listener.onCompressed(result)
            }
        }
    }

    private fun compressImage(imagePath: String): String {

        var scaledBitmap: Bitmap? = null

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        BitmapFactory.decodeFile(imagePath, options)

        var actualHeight = options.outHeight
        var actualWidth = options.outWidth

        // FIX 1 — Avoid crash if invalid image
        if (actualHeight <= 0 || actualWidth <= 0) {
            return imagePath
        }

        var imgRatio = actualWidth.toFloat() / actualHeight.toFloat()
        val maxRatio = maxWidth / maxHeight

        if (actualHeight > maxHeight || actualWidth > maxWidth) {

            if (imgRatio < maxRatio) {

                imgRatio = maxHeight / actualHeight
                actualWidth = (imgRatio * actualWidth).toInt()
                actualHeight = maxHeight.toInt()

            } else if (imgRatio > maxRatio) {

                imgRatio = maxWidth / actualWidth
                actualHeight = (imgRatio * actualHeight).toInt()
                actualWidth = maxWidth.toInt()

            } else {

                actualHeight = maxHeight.toInt()
                actualWidth = maxWidth.toInt()
            }
        }

        options.inSampleSize =
            calculateInSampleSize(options, actualWidth, actualHeight)

        options.inJustDecodeBounds = false

        var bmp: Bitmap? = null

        try {
            bmp = BitmapFactory.decodeFile(imagePath, options)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        }

        // FIX 2 — Null check
        if (bmp == null) {
            return imagePath
        }

        // FIX 3 — width height check
        if (actualWidth <= 0 || actualHeight <= 0) {
            return imagePath
        }

        scaledBitmap = createBitmap(actualWidth, actualHeight, Bitmap.Config.RGB_565)

        val ratioX = actualWidth / options.outWidth.toFloat()
        val ratioY = actualHeight / options.outHeight.toFloat()

        val middleX = actualWidth / 2f
        val middleY = actualHeight / 2f

        val matrix = Matrix()
        matrix.setScale(ratioX, ratioY, middleX, middleY)

        val canvas = Canvas(scaledBitmap)
        canvas.setMatrix(matrix)

        canvas.drawBitmap(
            bmp,
            middleX - bmp.width / 2,
            middleY - bmp.height / 2,
            Paint(Paint.FILTER_BITMAP_FLAG)
        )

        bmp.recycle()

        // Rotate using EXIF
        try {

            val exif = ExifInterface(imagePath)

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                0
            )

            val matrixExif = Matrix()

            when (orientation) {
                6 -> matrixExif.postRotate(90f)
                3 -> matrixExif.postRotate(180f)
                8 -> matrixExif.postRotate(270f)
            }

            scaledBitmap = Bitmap.createBitmap(
                scaledBitmap,
                0,
                0,
                scaledBitmap.width,
                scaledBitmap.height,
                matrixExif,
                true
            )

        } catch (e: IOException) {
            e.printStackTrace()
        }

        val filepath = getFilename()

        try {

            val out = FileOutputStream(filepath)

            scaledBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                80,
                out
            )

            out.flush()
            out.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return filepath
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {

        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val heightRatio =
                (height.toFloat() / reqHeight.toFloat()).roundToInt()

            val widthRatio =
                (width.toFloat() / reqWidth.toFloat()).roundToInt()

            inSampleSize = minOf(heightRatio, widthRatio)
        }

        return inSampleSize
    }

    private fun getFilename(): String {

        val mediaStorageDir =
            File(context.getExternalFilesDir(null), "compressed")

        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs()
        }

        val imageName = "IMG_${System.currentTimeMillis()}.jpg"

        return mediaStorageDir.absolutePath + "/" + imageName
    }
}
