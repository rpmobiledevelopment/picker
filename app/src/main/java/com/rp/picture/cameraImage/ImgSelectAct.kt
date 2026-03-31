package com.rp.picture.cameraImage

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Window
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AppCompatActivity
import com.rp.picture.R
import com.rp.picture.cameraImage.imageCompression.ImageCompressionListener
import com.rp.picture.cameraImage.imagePicker.ImagePicker
import com.rp.picture.onPermission.OnPermission
import com.ui.helper.constant.OnSnackBar

class ImgSelectAct : AppCompatActivity() {

    private val TAG = ImgSelectAct::class.java.simpleName
    private val EXTERNAL_PERMISSION_CODE = 1234
    private lateinit var imagePicker: ImagePicker
    private var isCompress = true
    private var isCamera = true
    private var isGallery = true
    private var isCameraYesNo = true

    companion object {
        const val FLAG_COMPRESS = "flag_compress"
        const val FLAG_CAMERA = "flag_camera"
        const val FLAG_GALLERY = "flag_gallery"
        const val FLAG_CAMERA_YES_NO = "FLAG_CAMERA_YES_NO"
        const val RESULT_FILE_PATH = "result_file_path"
    }

    private lateinit var pb_load: ProgressBar

    private lateinit var multiplePermissionsContract: ActivityResultContracts.RequestMultiplePermissions
    private lateinit var multiplePermissionLauncher: ActivityResultLauncher<Array<String>>

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

        pb_load = findViewById(R.id.pb_load)

        imagePicker = ImagePicker()

        multiplePermissionsContract = RequestMultiplePermissions()

        multiplePermissionLauncher =
            registerForActivityResult(multiplePermissionsContract) { isGranted ->

                if (isGranted.containsValue(true) && OnPermission().checkBool(this, "CAMERA_ACCESS")) {
                    onAccessFile()
                } else {
                    OnSnackBar(pb_load, "Permission declined")
                }
            }

        onAccessFile()
    }

    private fun onAccessFile() {

        if (isCameraYesNo) {

            if (OnPermission().checkBool(this, "CAMERA_ACCESS")) {

                imagePicker.withActivity(this)
                    .chooseFromGallery(isGallery)
                    .chooseFromCamera(isCamera, isCameraYesNo, false)
                    .withCompression(isCompress)
                    .start()

            } else {

                OnPermission(this, multiplePermissionLauncher, "CAMERA_ACCESS")
            }

        } else {

            imagePicker.withActivity(this)
                .chooseFromGallery(isGallery)
                .chooseFromCamera(isCamera, isCameraYesNo, false)
                .withCompression(isCompress)
                .start()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == EXTERNAL_PERMISSION_CODE) {

            if (grantResults.size == 2 &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {

                imagePicker.withActivity(this)
                    .chooseFromGallery(isGallery)
                    .chooseFromCamera(isCamera, isCameraYesNo, false)
                    .withCompression(isCompress)
                    .start()

            } else {

                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ImagePicker.SELECT_IMAGE) {

            if (resultCode == RESULT_OK) {

                imagePicker.addOnCompressListener(object :
                    ImageCompressionListener {

                    override fun onStart() {}

                    override fun onCompressed(filePath: String?) {

                        if (filePath != null && isCompress) {

                            val intent = Intent()
                            intent.putExtra(RESULT_FILE_PATH, filePath)

                            setResult(RESULT_OK, intent)
                            finish()
                        }
                    }
                })

                val filePath = imagePicker.getImageFilePath(data)

                if (filePath != null && !isCompress) {

                    val intent = Intent()
                    intent.putExtra(RESULT_FILE_PATH, filePath)

                    setResult(RESULT_OK, intent)
                    finish()
                }

            } else {

                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }
}