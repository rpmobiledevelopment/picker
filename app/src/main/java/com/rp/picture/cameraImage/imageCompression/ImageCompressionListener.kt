package com.rp.picture.cameraImage.imageCompression

interface ImageCompressionListener {
    fun onStart()
    fun onCompressed(filePath: String?)
}
