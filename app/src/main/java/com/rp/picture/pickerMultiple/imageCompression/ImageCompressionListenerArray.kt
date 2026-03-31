package com.rp.picture.pickerMultiple.imageCompression

interface ImageCompressionListenerArray {
    fun onStart()
    fun onCompressed(filePaths: MutableList<String>)
}
