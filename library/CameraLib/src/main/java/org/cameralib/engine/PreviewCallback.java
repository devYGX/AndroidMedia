package org.cameralib.engine;



public interface PreviewCallback {

    void onPreviewData(byte[] buf, PreviewParameter paramter);
}
