package com.rp.picture.selectSource

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AppCompatActivity
import com.rp.picture.R
import com.rp.picture.onPermission.OnPermission
import com.rp.picture.selectSource.imageCompression.ImageCompressionListenerArray
import com.rp.picture.selectSource.imageCompression.ImageCompressionTask
import com.ui.helper.log.IsLog

class SelectImageSourceAct : AppCompatActivity() {

    private val TAG = SelectImageSourceAct::class.java.simpleName

    private var imagePicker: ImagePicker? = null

    private var isCompress = true
    private var isCamera = true
    private var isGallery = true
    private var isLimited = 1

    companion object {
        const val FLAG_COMPRESS = "flag_compress"
        const val FLAG_CAMERA = "flag_camera"
        const val FLAG_GALLERY = "flag_gallery"
        const val RESULT_FILE_PATH = "result_file_path"
        const val FLAG_GALLERY_LIMITED = "flag_gallery_limited"
    }
    private lateinit var pb_load: ProgressBar

    private lateinit var multiplePermissionsContract: RequestMultiplePermissions
    private lateinit var multiplePermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.act_image_cap)

        intent?.let {
            isCompress = it.getBooleanExtra(FLAG_COMPRESS, true)
            isCamera = it.getBooleanExtra(FLAG_CAMERA, true)
            isGallery = it.getBooleanExtra(FLAG_GALLERY, true)
            isLimited = it.getIntExtra(FLAG_GALLERY_LIMITED, 1)
        }
        pb_load = findViewById(R.id.pb_load)

        imagePicker = ImagePicker()

        multiplePermissionsContract = RequestMultiplePermissions()

        multiplePermissionLauncher = registerForActivityResult(multiplePermissionsContract) { isGranted ->

            if (isGranted.containsValue(true) && OnPermission().checkBool(this, "CAMERA_ACCESS")) {
                onAccessFile()
            } else {
                IsLog(TAG,"Permission declined")
                Toast.makeText(this,"Permission declined", Toast.LENGTH_LONG).show()
                finish()
            }
        }
        onAccessFile()
    }

    private fun onAccessFile() {

        if (isCamera) {
            if (OnPermission().checkBool(this, "CAMERA_ACCESS")) {
                imagePicker?.withActivity(this)
                    ?.chooseFromGallery(isGallery, isLimited)
                    ?.chooseFromCamera(isCamera, false)
                    ?.withCompression(isCompress)
                    ?.start()
            } else {
                OnPermission(this, multiplePermissionLauncher, "CAMERA_ACCESS")
            }
        } else {
            imagePicker?.withActivity(this)
                ?.chooseFromGallery(isGallery, isLimited)
                ?.chooseFromCamera(isCamera, false)
                ?.withCompression(isCompress)
                ?.start()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ImagePicker.SELECT_IMAGE &&
            resultCode == RESULT_OK
        ) {

            val filePaths = ArrayList<String>()

            // Camera
            if (data == null) {
                imagePicker?.outputFilePath?.let {
                    filePaths.add(it)
                }
            } else {

                data.clipData?.let { it ->

                    for (i in 0 until it.itemCount) {

                        val uri = it.getItemAt(i).uri

                        val path = getFilePath(uri)

                        path?.let {
                            filePaths.add(it)
                        }
                    }

                } ?: data.data?.let { it ->

                    val path = getFilePath(it)

                    path?.let {
                        filePaths.add(it)
                    }
                }
            }

            if (filePaths.isNotEmpty()) {
                if (isCompress) {
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
                } else {
                    val intent = Intent()
                    intent.putStringArrayListExtra(RESULT_FILE_PATH, filePaths)
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun getFilePath(uri: Uri): String? {

        val projection = arrayOf(MediaStore.Images.Media.DATA)

        contentResolver.query(uri, projection, null, null, null)?.use {

            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            it.moveToFirst()

            return it.getString(columnIndex)
        }

        return null
    }

    override fun onResume() {
        super.onResume()

        window.decorView.isClickable = true
        window.decorView.isFocusable = true

        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        window.decorView.isClickable = true

        IsLog(TAG,"onResume")

    }
    override fun onPause() {
        super.onPause()

        IsLog(TAG,"onPause")
    }
}