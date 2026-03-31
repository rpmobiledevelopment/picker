package com.rp.picture.selectSource

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.rp.picture.onPermission.OnGlobalPermission
import java.io.File

class ImagePicker {

    private val TAG = ImagePicker::class.java.simpleName

    private var activity: Activity? = null
    private var fragment: Fragment? = null

    private var isCompress = true
    private var isCamera = true
    private var isGallery = true
    private var fontCameraOPT = false
    private var limitSetup: Int = 5

    var outputFileUri: Uri? = null
    var outputFilePath: String? = null

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

    fun chooseFromCamera(isCamera: Boolean, fontCameraOPT: Boolean): ImagePicker {
        this.isCamera = isCamera
        this.fontCameraOPT = fontCameraOPT
        return this
    }

    fun chooseFromGallery(isGallery: Boolean, limit: Int): ImagePicker {
        this.isGallery = isGallery
        this.limitSetup = limit
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

        if (isGallery) {

            val intent =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2
                ) {

                    Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                        type = "image/*"
                        putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, limitSetup)
                    }

                } else {
                    getPickImageChooserIntent()
                }

            activity?.startActivityForResult(intent, SELECT_IMAGE)

        } else {

            val intent = getCameraIntent()
            activity?.startActivityForResult(intent, SELECT_IMAGE)
        }
    }

    private fun getPickImageChooserIntent(): Intent {

        val intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        return Intent.createChooser(intent, "Select images")
    }

    private fun getCameraIntent(): Intent {

        val outputFileUri = getCaptureImageOutputUri()

        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        captureIntent.putExtra(
            "android.intent.extras.CAMERA_FACING",
            if (fontCameraOPT) 1 else 0
        )

        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)

        return captureIntent
    }

    private fun getCaptureImageOutputUri(): Uri? {

        val context = activity ?: fragment?.activity

        val getImage = context?.getExternalFilesDir("")

        return getImage?.let {

            val file = File(it, "IMG_${System.currentTimeMillis()}.jpg")

            if (!file.exists()) {
                file.createNewFile()
            }

            outputFilePath = file.absolutePath

            outputFileUri = FileProvider.getUriForFile(
                context,
                context.packageName + ".${OnGlobalPermission.appProvider}",
                file
            )

            outputFileUri
        }
    }
}
