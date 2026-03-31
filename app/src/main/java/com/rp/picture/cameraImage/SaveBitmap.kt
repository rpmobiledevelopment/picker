package com.rp.picture.cameraImage

import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import com.rp.picture.onPermission.OnGlobalPermission
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SaveBitmap {

    private val TAG = SaveBitmap::class.java.simpleName

    fun save(bmp: Bitmap): File? {

        val extStorageDirectory =
            Environment.getExternalStorageDirectory().toString()

        var file = File(extStorageDirectory, "temp.jpg")

        if (file.exists()) {
            file.delete()
            file = File(extStorageDirectory, "temp.jpg")
        }

        return try {

            val outStream = FileOutputStream(file)

            bmp.compress(Bitmap.CompressFormat.JPEG, 100, outStream)

            outStream.flush()
            outStream.close()

            file

        } catch (e: Exception) {
            null
        }
    }

    fun saveFile(bmp: Bitmap, s: String): File? {

        val extStorageDirectory =
            Environment.getExternalStorageDirectory().toString()

        var file = File(
            extStorageDirectory,
            "${getDateTime()}temp.jpg"
        )

        if (file.exists()) {
            file.delete()
            file = File(extStorageDirectory, "${getDateTime()}temp.jpg")
        }

        return try {

            val outStream = FileOutputStream(file)

            bmp.compress(Bitmap.CompressFormat.JPEG, 100, outStream)

            outStream.flush()
            outStream.close()

            file

        } catch (e: Exception) {
            null
        }
    }

    fun saveImage(data: Bitmap, imgPro: String): File? {

        val createFolder: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM
                    ).toString() + "/${OnGlobalPermission.saveFolderName}"
                )
            } else {
                File(
                    Environment.getExternalStorageDirectory()
                        .toString() + "/${OnGlobalPermission.saveFolderName}"
                )
            }

        if (!createFolder.exists())
            createFolder.mkdir()

        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

        val saveImage = File(
            createFolder,
            "$timeStamp$imgPro.jpg"
        )

        return try {

            val outputStream = FileOutputStream(saveImage)

            data.compress(
                Bitmap.CompressFormat.JPEG,
                100,
                outputStream
            )

            outputStream.flush()
            outputStream.close()

            saveImage

        } catch (e: Exception) {
            null
        }
    }

    fun getDateTime(): String {

        val dateFormat =
            SimpleDateFormat(
                "dd MMM yyyy HH:mm:ss",
                Locale.getDefault()
            )

        return dateFormat.format(Date())
    }
}
