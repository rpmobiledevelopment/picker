package com.rp.picture.pickerMultiple.imageCompression

interface ImageCompressionListener {
    fun onStart()
    fun onCompressed(filePath: String?)
}
