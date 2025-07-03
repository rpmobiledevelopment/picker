package com.rp.picture.pickerMultiple.imageCompression;

import java.util.List;

public interface ImageCompressionListenerArray {
    void onStart();
    void onCompressed(List<String> filePaths);
}
