package com.rp.picture.cameraImage

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.rp.picture.R
import com.rp.picture.cameraImage.SaveBitmap.save
import com.rp.picture.onPermission.OnGlobalPermission
import com.rp.picture.onPermission.OnGlobalPermission.Companion.onDialogShow
import com.rp.uihelpher.log.IsLog
import java.io.File
import java.io.Serializable

class OnPicker() : Fragment() {

    private val TAG = OnPicker::class.java.simpleName

    private var onPickerListener: OnPickerListener? = null
    private var mActivity: Activity? = null
    private var selectLang: String? = null

    private lateinit var tv_header: TextView
    private lateinit var tv_camera: TextView
    private lateinit var tv_gallery: TextView
    private lateinit var tv_cancel: TextView

    constructor(mActivity: Activity, selectLang: String, listener: OnPickerListener) : this() {

        this.mActivity = mActivity
        this.selectLang = selectLang
        this.onPickerListener = listener

        if (onDialogShow == true) {
            onDlg()
        } else {

            Handler(Looper.getMainLooper()).postDelayed({

                val intent = Intent(mActivity, ImgSelectAct::class.java)
                intent.putExtra(ImgSelectAct.FLAG_COMPRESS, true)
                intent.putExtra(ImgSelectAct.FLAG_CAMERA, true)
                intent.putExtra(ImgSelectAct.FLAG_GALLERY, true)
                intent.putExtra(ImgSelectAct.FLAG_CAMERA_YES_NO, false)

                someActivityResultLauncher.launch(intent)

            }, 200)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mActivity = activity

        arguments?.let {
            selectLang = it.getString("selectLang")
            onPickerListener = it.getSerializable("listener") as? OnPickerListener
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        mActivity = activity
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun onDlg() {

        mActivity?.let { mActivity ->
            val dialog = BottomSheetDialog(mActivity)
            val inflater = mActivity.layoutInflater

            val convertView = inflater.inflate(R.layout.dlg_pic_opt, null)

            tv_header = convertView.findViewById(R.id.tv_header)
            tv_camera = convertView.findViewById(R.id.tv_camera)
            tv_gallery = convertView.findViewById(R.id.tv_gallery)
            tv_cancel = convertView.findViewById(R.id.tv_cancel)

            if (selectLang == "AR") {
                tv_header.text = OnGlobalPermission.TAG_SELECT_SOURCE_ARA
                tv_camera.text = OnGlobalPermission.TAG_CAMERA_ARA
                tv_gallery.text = OnGlobalPermission.TAG_GALLERY_ARA
                tv_cancel.text = OnGlobalPermission.TAG_CANCEL_ARA
            } else {
                tv_header.text = OnGlobalPermission.TAG_SELECT_SOURCE_ENG
                tv_camera.text = OnGlobalPermission.TAG_CAMERA_ENG
                tv_gallery.text = OnGlobalPermission.TAG_GALLERY_ENG
                tv_cancel.text = OnGlobalPermission.TAG_CANCEL_ENG
            }

            convertView.findViewById<View>(R.id.cv_bg)
                .background = resources.getDrawable(R.drawable.bg_cv_dlg)

            convertView.findViewById<View>(R.id.tv_cancel)
                .setOnClickListener { dialog.dismiss() }

            convertView.findViewById<View>(R.id.ll_gallery)
                .setOnClickListener {

                    val intent = Intent(mActivity, ImgSelectAct::class.java)
                    intent.putExtra(ImgSelectAct.FLAG_COMPRESS, true)
                    intent.putExtra(ImgSelectAct.FLAG_CAMERA, true)
                    intent.putExtra(ImgSelectAct.FLAG_GALLERY, true)
                    intent.putExtra(ImgSelectAct.FLAG_CAMERA_YES_NO, false)

                    someActivityResultLauncher.launch(intent)
                    dialog.dismiss()
                }

            convertView.findViewById<View>(R.id.ll_camera)
                .setOnClickListener {

                    val intent = Intent(mActivity, ImgSelectAct::class.java)
                    intent.putExtra(ImgSelectAct.FLAG_COMPRESS, true)
                    intent.putExtra(ImgSelectAct.FLAG_CAMERA, true)
                    intent.putExtra(ImgSelectAct.FLAG_GALLERY, true)
                    intent.putExtra(ImgSelectAct.FLAG_CAMERA_YES_NO, true)

                    someActivityResultLauncher.launch(intent)
                    dialog.dismiss()
                }

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val lp = WindowManager.LayoutParams()
            val window = dialog.window
            lp.copyFrom(window?.attributes)
            val display = mActivity.windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            lp.width = size.x
            lp.height = size.y
            window?.attributes = lp
            dialog.setCancelable(true)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(convertView)
            dialog.show()
        }

    }

    private val someActivityResultLauncher = registerForActivityResult(StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                val data = result.data

                try {

                    val filePath =
                        data?.getStringExtra(ImgSelectAct.RESULT_FILE_PATH)

                    var imageUri = Uri.parse("")

                    imageUri = Uri.fromFile(File(filePath))
                    onPickerListener?.onViewImg(imageUri.toString(), "")

                    IsLog(TAG, "imageUri======$imageUri")

                } catch (e: Exception) {

                    IsLog(TAG, "Exception ${e.message}")
                }
            }
        }

    fun interface OnPickerListener : Parcelable, Serializable {

        fun onViewImg(imageUri: String, opt: String)

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {}

        companion object CREATOR : Parcelable.Creator<OnPickerListener> {

            override fun createFromParcel(source: Parcel
            ): OnPickerListener {
                return OnPickerListener { _, _ -> }
            }

            override fun newArray(size: Int): Array<OnPickerListener?> {
                return arrayOfNulls(size)
            }
        }
    }
}
