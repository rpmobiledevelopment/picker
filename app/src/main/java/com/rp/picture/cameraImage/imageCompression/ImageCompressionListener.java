package com.rp.picture.cameraImage.imageCompression;

import java.util.List;

public interface ImageCompressionListener {
    void onStart();
    void onCompressed(String filePaths);
    void onCompressed(List<String> filePaths);
}
