package com.rp.picture.pickerMultiple.imagePicker

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.rp.picture.pickerMultiple.imageCompression.ImageCompression
import com.rp.picture.pickerMultiple.imageCompression.ImageCompressionListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ImagePicker {

    private val TAG = ImagePicker::class.java.simpleName

    private var activity: Activity? = null
    private var fragment: Fragment? = null

    private var isCompress = true
    private var isCamera = true
    private var isGallery = true
    private var isCameraYesNo = true
    private var fontCameraOPT = false

    private var imageCompressionListener: ImageCompressionListener? = null

    companion object {
        const val SELECT_IMAGE = 121
    }

    fun withActivity(activity: Activity): ImagePicker {
        this.activity = activity
        return this
    }

    fun withFragment(fragment: Fragment): ImagePicker {
        this.fragment = fragment
        return this
    }

    fun chooseFromCamera(
        isCamera: Boolean,
        isCameraYesNo: Boolean,
        fontCameraOPT: Boolean
    ): ImagePicker {
        this.isCamera = isCamera
        this.isCameraYesNo = isCameraYesNo
        this.fontCameraOPT = fontCameraOPT
        return this
    }

    fun chooseFromGallery(isGallery: Boolean): ImagePicker {
        this.isGallery = isGallery
        return this
    }

    fun withCompression(isCompress: Boolean): ImagePicker {
        this.isCompress = isCompress
        return this
    }

    fun start() {

        if (activity != null && fragment != null) {
            throw IllegalStateException("Cannot add both activity and fragment")
        }

        if (activity == null && fragment == null) {
            throw IllegalStateException("Activity and fragment both are null")
        }

        if (!isCamera && !isGallery) {
            throw IllegalStateException("select source to pick image")
        }

        val context = activity ?: fragment?.activity

        val intent: Intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2
            ) {

                Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                    type = "image/*"
                    putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 5)
                }

            } else {
                getPickImageChooserIntent()
            }

        if (activity != null) {
            activity!!.startActivityForResult(intent, SELECT_IMAGE)
        } else {
            fragment!!.startActivityForResult(intent, SELECT_IMAGE)
        }
    }

    private fun getPickImageChooserIntent(): Intent {

        val allIntents: MutableList<Intent> = ArrayList()

        val packageManager =
            activity?.packageManager ?: fragment?.activity?.packageManager

        if (!isCameraYesNo && isGallery) {

            val intentPicker = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )

            intentPicker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

            val listGallery =
                packageManager?.queryIntentActivities(intentPicker, 0)

            listGallery?.forEach { res ->

                val intent = Intent(intentPicker)

                intent.component = ComponentName(
                    res.activityInfo.packageName,
                    res.activityInfo.name
                )

                intent.setPackage(res.activityInfo.packageName)

                allIntents.add(intent)
            }
        }

        var mainIntent = allIntents[allIntents.size - 1]

        for (intent in allIntents) {
            if (intent.component?.className ==
                "com.android.documentsui.DocumentsActivity"
            ) {
                mainIntent = intent
                break
            }
        }

        allIntents.remove(mainIntent)

        val chooserIntent = Intent.createChooser(mainIntent, "Select images")

        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            allIntents.toTypedArray()
        )

        return chooserIntent
    }

    private fun getCaptureImageOutputUri(): Uri? {

        val context = activity ?: fragment?.activity

        val getImage = context?.getExternalFilesDir("")

        return getImage?.let {

            val file = File(it, "IMG_${System.currentTimeMillis()}.jpg")

            if (!file.exists()) {
                file.createNewFile()
            }

            FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )
        }
    }

    private fun getPickImageResultFilePath(data: Intent?): String? {

        val isCamera = data == null || data.data == null

        return if (isCamera)
            getCaptureImageOutputUri()?.path
        else
            getRealPathFromURI(data.data!!)
    }

    private fun getRealPathFromURI(contentUri: Uri): String? {

        val file = File(getFilename())

        return try {

            if (file.createNewFile()) {

                val context = activity ?: fragment?.context

                val inputStream =
                    context?.contentResolver?.openInputStream(contentUri)

                val bytes = getBytes(inputStream!!)

                val out = FileOutputStream(file)

                out.write(bytes)

                out.close()

                file.absolutePath
            } else null

        } catch (e: Exception) {
            null
        }
    }

    private fun getBytes(inputStream: InputStream): ByteArray {

        val byteBuffer = ByteArrayOutputStream()

        val buffer = ByteArray(1024)

        var len: Int

        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }

        return byteBuffer.toByteArray()
    }

    fun getImageFilePath(data: Intent?): String? {

        if (!isCompress)
            return getPickImageResultFilePath(data)

        val path = getPickImageResultFilePath(data)

        if (path != null) {
            ImageCompression(
                activity ?: fragment?.activity,
                path,
                imageCompressionListener
            ).compress()
        }

        return null
    }

    fun addOnCompressListener(listener: ImageCompressionListener) {
        this.imageCompressionListener = listener
    }

    private fun getFilename(): String {

        val context = activity ?: fragment?.context

        val mediaStorageDir =
            File(context?.getExternalFilesDir(""), "uncompressed")

        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs()
        }

        val imageName = "IMG_${System.currentTimeMillis()}.png"

        return mediaStorageDir.absolutePath + "/" + imageName
    }
}
