package com.rp.picture.pickerMultiple.imageCompression;

import java.util.List;

public interface ImageCompressionListener {
    void onStart();
    void onCompressed(String filePath);
    void onCompressed(List<String> filePath);
}
