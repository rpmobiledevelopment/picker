package com.rp.picture.pickerMultiple

import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Window
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AppCompatActivity
import com.rp.picture.R
import com.rp.picture.onPermission.OnPermission
import com.rp.picture.pickerMultiple.imageCompression.ImageCompressionListenerArray
import com.rp.picture.pickerMultiple.imageCompression.ImageCompressionTask
import com.rp.picture.pickerMultiple.imagePicker.ImagePicker
import com.ui.helper.constant.OnSnackBar

class ImgSelectMultipleAct : AppCompatActivity() {

    private val TAG = ImgSelectMultipleAct::class.java.simpleName

    private var imagePicker: ImagePicker? = null

    private var isCompress = true
    private var isCamera = true
    private var isGallery = true
    private var isCameraYesNo = true

    private lateinit var pbLoad: ProgressBar

    companion object {

        const val FLAG_COMPRESS = "flag_compress"
        const val FLAG_CAMERA = "flag_camera"
        const val FLAG_GALLERY = "flag_gallery"
        const val FLAG_CAMERA_YES_NO = "FLAG_CAMERA_YES_NO"

        const val RESULT_FILE_PATH = "result_file_path"
    }

    private val multiplePermissionLauncher =
        registerForActivityResult(
            RequestMultiplePermissions()
        ) { isGranted ->

            if (isGranted.containsValue(true) &&
                OnPermission().checkBool(this, "STORAGE")) {

                imagePicker
                    ?.withActivity(this)
                    ?.chooseFromGallery(isGallery)
                    ?.chooseFromCamera(isCamera, isCameraYesNo, false)
                    ?.withCompression(isCompress)
                    ?.start()

            } else {
                OnSnackBar(pbLoad, "Permission declined")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(R.layout.act_image_cap)

        intent?.let {
            isCompress = it.getBooleanExtra(FLAG_COMPRESS, true)
            isCamera = it.getBooleanExtra(FLAG_CAMERA, true)
            isGallery = it.getBooleanExtra(FLAG_GALLERY, true)
            isCameraYesNo = it.getBooleanExtra(FLAG_CAMERA_YES_NO, true)
        }

        pbLoad = findViewById(R.id.pb_load)

        imagePicker = ImagePicker()

        if (OnPermission().checkBool(this, "STORAGE")) {

            imagePicker
                ?.withActivity(this)
                ?.chooseFromGallery(isGallery)
                ?.chooseFromCamera(isCamera, isCameraYesNo, false)
                ?.withCompression(isCompress)
                ?.start()

        } else {
            OnPermission(multiplePermissionLauncher, "STORAGE")
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ImagePicker.SELECT_IMAGE &&
            resultCode == RESULT_OK
        ) {

            val filePaths = ArrayList<String>()

            data?.clipData?.let {

                for (i in 0 until it.itemCount) {

                    val uri = it.getItemAt(i).uri
                    val filePath = getRealPathFromURI(uri)

                    filePath?.let { path ->
                        filePaths.add(path)
                    }
                }

            } ?: data?.data?.let {

                val filePath = getRealPathFromURI(it)

                filePath?.let { path ->
                    filePaths.add(path)
                }
            }

            if (filePaths.isNotEmpty()) {

                ImageCompressionTask(this,
                    object : ImageCompressionListenerArray {

                        override fun onStart() {
                            Log.e(TAG, "Compression started...")
                        }

                        override fun onCompressed(filePaths: MutableList<String>) {
                            Log.e(TAG, "Compression finished: $filePaths")

                            val intent = Intent()
                            intent.putStringArrayListExtra(RESULT_FILE_PATH, ArrayList(filePaths))
                            setResult(RESULT_OK, intent)
                            finish()
                        }
                    }).compress(filePaths)
            }

            Log.e(TAG, "Selected Images: $filePaths")

        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        var result: String? = null
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)

        cursor?.let {
            val index = it.getColumnIndex(MediaStore.Images.Media.DATA)
            if (index != -1) {
                it.moveToFirst()
                result = it.getString(index)
            }
            it.close()
        }
        return result
    }
}