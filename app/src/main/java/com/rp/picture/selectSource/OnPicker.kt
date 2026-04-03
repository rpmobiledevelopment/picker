package com.rp.picture.selectSource

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import bottomDlg.BtmSheetDlg
import com.rp.picture.R
import com.rp.picture.onPermission.OnGlobalPermission
import com.rp.picture.onPermission.OnGlobalPermission.Companion.onDialogShow

class OnPicker() : BtmDlg() {

    private val TAG = OnPicker::class.java.simpleName

    private var someActivityResultLauncher: ActivityResultLauncher<Intent?>? = null
    private var mActivity: Activity? = null
    private var selectLang: String? = null

    private lateinit var tv_header: TextView
    private lateinit var tv_camera: TextView
    private lateinit var tv_gallery: TextView
    private lateinit var tv_cancel: TextView

    constructor(mActivity: Activity?, selectLang: String,someActivityResultLauncher: ActivityResultLauncher<Intent?>) : this() {

        this.mActivity = mActivity
        this.selectLang = selectLang
        this.someActivityResultLauncher = someActivityResultLauncher

        if (onDialogShow == true) {
        } else {

            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(requireActivity(), SelectImageSourceAct::class.java)
                intent.putExtra(SelectImageSourceAct.FLAG_COMPRESS, true)
                intent.putExtra(SelectImageSourceAct.FLAG_CAMERA, false)
                intent.putExtra(SelectImageSourceAct.FLAG_GALLERY, true)
                intent.putExtra(SelectImageSourceAct.FLAG_GALLERY_LIMITED, 5)

                someActivityResultLauncher.launch(intent)
                dismiss()

            }, 200)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mActivity = activity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        if (onDialogShow == true) {
            val convertView = inflater.inflate(R.layout.dlg_pic_opt, container, false)

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

            tv_cancel.setOnClickListener { dismiss() }

            convertView.findViewById<View>(R.id.ll_gallery).setOnClickListener {

                val intent = Intent(requireActivity(), SelectImageSourceAct::class.java)
                intent.putExtra(SelectImageSourceAct.FLAG_COMPRESS, true)
                intent.putExtra(SelectImageSourceAct.FLAG_CAMERA, false)
                intent.putExtra(SelectImageSourceAct.FLAG_GALLERY, true)
                intent.putExtra(SelectImageSourceAct.FLAG_GALLERY_LIMITED, 5)

                someActivityResultLauncher?.launch(intent)
                dismiss()
            }

            convertView.findViewById<View>(R.id.ll_camera).setOnClickListener {

                val intent = Intent(requireActivity(), SelectImageSourceAct::class.java)
                intent.putExtra(SelectImageSourceAct.FLAG_COMPRESS, true)
                intent.putExtra(SelectImageSourceAct.FLAG_CAMERA, true)
                intent.putExtra(SelectImageSourceAct.FLAG_GALLERY, false)

                someActivityResultLauncher?.launch(intent)
                dismiss()
            }
            return convertView
        }else {
            return null
        }


    }
    override fun onStart() {
        super.onStart()

    }
}
