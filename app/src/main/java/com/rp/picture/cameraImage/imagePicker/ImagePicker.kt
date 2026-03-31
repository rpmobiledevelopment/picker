package com.rp.picture.cameraImage.imagePicker

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.rp.picture.cameraImage.imageCompression.ImageCompression
import com.rp.picture.cameraImage.imageCompression.ImageCompressionListener
import com.rp.picture.onPermission.OnPermission
import java.io.File
import java.io.FileOutputStream

class ImagePicker {

    private val TAG = ImagePicker::class.java.simpleName

    private var activity: Activity? = null
    private var fragment: Fragment? = null

    private var isCompress = true
    private var isCamera = true
    private var isGallery = true
    private var isCameraYesNo = true
    private var fontCameraOPT = false

    private lateinit var imageCompressionListener: ImageCompressionListener

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

        } else if (activity == null && fragment == null) {
            throw IllegalStateException("Activity and fragment both are null")

        } else {

            if (OnPermission().checkBool(activity, "STORAGE")) {

                if (!isCamera && !isGallery) {
                    throw IllegalStateException("select source to pick image")
                }

                val intent = getPickImageChooserIntent()

                activity?.startActivityForResult(intent, SELECT_IMAGE)
                    ?: fragment?.startActivityForResult(intent, SELECT_IMAGE)

            } else {
                throw IllegalStateException("Write External Permission not found")
            }
        }
    }

    private fun getPickImageChooserIntent(): Intent {

        val outputFileUri = getCaptureImageOutputUri()

        val allIntents = ArrayList<Intent>()

        val packageManager =
            activity?.packageManager ?: activity?.packageManager

        if (!isCameraYesNo) {

            if (isGallery) {

                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2
                ) {

                    Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                        type = "image/*"
                    }

                } else {

                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/*"
                    }
                }

                allIntents.add(intent)
            }
        }

        if (isCameraYesNo && isCamera) {

            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            captureIntent.putExtra(
                "android.intent.extras.CAMERA_FACING",
                if (fontCameraOPT) 1 else 0
            )

            val listCam = packageManager?.queryIntentActivities(captureIntent, 0)

            if (listCam != null) {
                for (res in listCam) {

                    val intent = Intent(captureIntent)

                    intent.component =
                        ComponentName(res.activityInfo.packageName, res.activityInfo.name)

                    intent.setPackage(res.activityInfo.packageName)

                    outputFileUri?.let {
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, it)
                    }

                    allIntents.add(intent)
                }
            }
        }

        val mainIntent = getIntent(allIntents)

        allIntents.remove(mainIntent)

        val chooserIntent = Intent.createChooser(mainIntent, "Select source")

        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            allIntents.toTypedArray()
        )

        return chooserIntent
    }

    private fun getIntent(allIntents: List<Intent>): Intent {

        var mainIntent = allIntents.last()

        for (intent in allIntents) {

            val comp = intent.component

            if (comp != null &&
                "com.android.documentsui.DocumentsActivity" == comp.className
            ) {
                mainIntent = intent
                break
            }
        }

        return mainIntent
    }

    private fun getCaptureImageOutputUri(): Uri? {

        val context = activity ?: fragment?.activity ?: return null

        val getImage = context.getExternalFilesDir("")

        return getImage?.let {

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.app_file_provider",
                File(it.path, "profile.png")
            )
        }
    }

    private fun getPickImageResultFilePath(data: Intent?): String? {

        val isCamera = data == null || data.data == null

        return if (isCamera) {
            getCaptureImageOutputUri()?.path
        } else {
            getRealPathFromURI(data.data!!)
        }
    }

    private fun getRealPathFromURI(contentUri: Uri): String? {

        val file = File(getFilename())

        return try {

            if (file.createNewFile()) {

                val inputStream =
                    activity?.contentResolver?.openInputStream(contentUri)
                        ?: fragment?.context?.contentResolver?.openInputStream(contentUri)

                val bytes = inputStream?.readBytes()

                FileOutputStream(file).use {
                    it.write(bytes)
                }

                file.absolutePath

            } else null

        } catch (e: Exception) {
            null
        }
    }

    fun getImageFilePath(data: Intent?): String? {

        val path = getPickImageResultFilePath(data)

        return if (!isCompress) {
            path
        } else {

            path?.let {

                (activity ?: fragment?.activity)?.let { it1 ->
                    ImageCompression(
                        it1,
                        it,
                        imageCompressionListener
                    )
                }?.compress()
            }

            null
        }
    }

    fun addOnCompressListener(listener: ImageCompressionListener) {
        imageCompressionListener = listener
    }

    private fun getFilename(): String {

        val context = activity ?: fragment?.context

        val mediaStorageDir =
            File(context?.getExternalFilesDir(""), "uncompressed")

        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs()
        }

        val imageName = "IMG_${System.currentTimeMillis()}.png"

        return "${mediaStorageDir.absolutePath}/$imageName"
    }
}
