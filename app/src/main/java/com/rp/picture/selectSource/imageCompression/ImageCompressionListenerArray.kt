package com.rp.picture.selectSource.imageCompression

interface ImageCompressionListenerArray {
    fun onStart()
    fun onCompressed(filePaths: MutableList<String>)
}
