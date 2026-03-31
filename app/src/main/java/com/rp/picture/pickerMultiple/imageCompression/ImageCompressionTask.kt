package com.rp.picture.pickerMultiple.imageCompression

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream

class ImageCompressionTask(private val context: Context,
    private val listener: ImageCompressionListenerArray) {

    companion object {
        private const val maxHeight = 700f
        private const val maxWidth = 700f
    }

    fun compress(filePaths: List<String>) {

        listener.onStart()

        Thread {

            val compressedPaths = ArrayList<String>()

            for (path in filePaths) {
                compressedPaths.add(compressImage(path))
            }

            Handler(Looper.getMainLooper()).post {
                listener.onCompressed(compressedPaths)
            }

        }.start()
    }

    private fun compressImage(imagePath: String): String {

        var scaledBitmap: Bitmap? = null

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        BitmapFactory.decodeFile(imagePath, options)

        var actualHeight = options.outHeight
        var actualWidth = options.outWidth

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
        options.inDither = false
        options.inTempStorage = ByteArray(16 * 1024)

        var bmp: Bitmap? = null

        try {
            bmp = BitmapFactory.decodeFile(imagePath, options)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        }

        try {
            scaledBitmap = createBitmap(actualWidth, actualHeight, Bitmap.Config.RGB_565)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        }

        val ratioX = actualWidth / options.outWidth.toFloat()
        val ratioY = actualHeight / options.outHeight.toFloat()

        val middleX = actualWidth / 2f
        val middleY = actualHeight / 2f

        val matrix = Matrix()
        matrix.setScale(ratioX, ratioY, middleX, middleY)

        if (scaledBitmap != null && bmp != null) {

            val canvas = Canvas(scaledBitmap)
            canvas.setMatrix(matrix)

            canvas.drawBitmap(
                bmp,
                middleX - bmp.width / 2,
                middleY - bmp.height / 2,
                Paint(Paint.FILTER_BITMAP_FLAG)
            )

            bmp.recycle()
        }

        // Rotation Fix (AndroidX ExifInterface)
        try {

            val exif = androidx.exifinterface.media.ExifInterface(imagePath)

            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )

            val matrixExif = Matrix()

            when (orientation) {

                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 ->
                    matrixExif.postRotate(90f)

                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 ->
                    matrixExif.postRotate(180f)

                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 ->
                    matrixExif.postRotate(270f)
            }

            scaledBitmap?.let {

                scaledBitmap = Bitmap.createBitmap(
                    it,
                    0,
                    0,
                    it.width,
                    it.height,
                    matrixExif,
                    true
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        val filepath = getFilename()

        try {

            val out = FileOutputStream(filepath)

            scaledBitmap?.compress(
                Bitmap.CompressFormat.JPEG,
                60,
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
                (height.toFloat() / reqHeight.toFloat()).toInt()

            val widthRatio =
                (width.toFloat() / reqWidth.toFloat()).toInt()

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

        return mediaStorageDir.absolutePath +
                "/IMG_${System.currentTimeMillis()}.jpg"
    }
}