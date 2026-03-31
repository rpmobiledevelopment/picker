package com.rp.picture.onPermission

interface OnGlobalPermission {
    companion object {
        var onCameraPermission : Boolean? = true
        var onDialogShow : Boolean? = true
        var saveFolderName : String? = "saveImages"


        const val TAG_SELECT_SOURCE_ARA: String = "اختر مصدر"
        const val TAG_CAMERA_ARA: String = "كاميرة"
        const val TAG_GALLERY_ARA: String = "استيديو"
        const val TAG_CANCEL_ARA: String = "الغاء"


        const val TAG_SELECT_SOURCE_ENG: String = "Select Source"
        const val TAG_CAMERA_ENG: String = "Camera"
        const val TAG_GALLERY_ENG: String = "Gallery"
        const val TAG_CANCEL_ENG: String = "Cancel"
    }
}
