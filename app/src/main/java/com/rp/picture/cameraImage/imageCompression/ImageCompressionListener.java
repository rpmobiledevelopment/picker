package com.rp.picture.cameraImage.imageCompression;

public interface ImageCompressionListener {
    void onStart();
    void onCompressed(String filePath);
}
