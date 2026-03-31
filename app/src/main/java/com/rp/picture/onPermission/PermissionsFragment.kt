package com.rp.picture.onPermission

import android.Manifest.permission
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.rp.picture.onPermission.OnGlobalPermission.Companion.onCameraPermission

class PermissionsFragment : Fragment() {

    private val TAG = PermissionsFragment::class.java.simpleName
    private var callback: PermissionCallback? = null

    interface PermissionCallback {
        fun onPermissionsResult(allPermissionsGranted: Boolean)
    }

    private val multiplePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val allGranted = !permissions.containsValue(false)
            callback?.onPermissionsResult(allGranted)
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is PermissionCallback) {
            callback = context
        }
    }

    fun requestPermissions(mActivity: Activity?, opt: String) {

        when (opt) {

            "DOWNLOAD_ACCESS_", "DOWNLOAD_ACCESS" -> {
                multiplePermissionLauncher.launch(arrayOf())
            }

            "NOTIFICATION" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    multiplePermissionLauncher.launch(arrayOf(permission.POST_NOTIFICATIONS))
                }
            }

            "RECORD_AUDIO" -> {
                multiplePermissionLauncher.launch(arrayOf(permission.RECORD_AUDIO))
            }

            "LOCATION" -> {
                mActivity?.let {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                            permission.ACCESS_FINE_LOCATION)) {
                        buildAlertMessageNoGps(mActivity)
                    } else {
                        multiplePermissionLauncher.launch(arrayOf(permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION))
                    }
                }

            }

            else -> {

                if (onCameraPermission == true) {
                    multiplePermissionLauncher.launch(arrayOf(permission.CAMERA))
                } else {
                    multiplePermissionLauncher.launch(arrayOf())
                }
            }
        }
    }

    fun requestPermissions(opt: String) {

        when (opt) {

            "DOWNLOAD_ACCESS_", "DOWNLOAD_ACCESS" -> {}

            "NOTIFICATION" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    multiplePermissionLauncher.launch(
                        arrayOf(permission.POST_NOTIFICATIONS)
                    )
                }
            }

            "RECORD_AUDIO" -> {
                multiplePermissionLauncher.launch(
                    arrayOf(permission.RECORD_AUDIO)
                )
            }

            else -> {

                if (onCameraPermission == true) {
                    multiplePermissionLauncher.launch(arrayOf(permission.CAMERA))
                } else {
                    multiplePermissionLauncher.launch(arrayOf())
                }
            }
        }
    }

    private fun buildAlertMessageNoGps(mActivity: Activity) {

        val builder = AlertDialog.Builder(mActivity)

        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->

                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS

                val uri = Uri.fromParts("package", mActivity.packageName, null)
                intent.data = uri

                mActivity.startActivity(intent)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
            }

        val alert = builder.create()
        alert.show()
    }
}
