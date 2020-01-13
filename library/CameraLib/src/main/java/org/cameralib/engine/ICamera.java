package org.cameralib.engine;


import android.graphics.SurfaceTexture;
import android.view.SurfaceHolder;

import java.io.IOException;

public interface ICamera<CameraConfig, PreviewCallback, ErrorCallback, PictureCallback> {

    int FORCE_RELEASE = 1;

    void initCamera(int degrees);

    CameraConfig getCameraConfig();

    void setCameraConfig(CameraConfig config);

    void setDisplaySurfaceView(SurfaceHolder displayer) throws IOException;

    void setDisplayTextureView(SurfaceTexture displayer) throws IOException;

    void setPreviewCallback(PreviewCallback pCallback);

    void setPreviewCallbackWithBuffer(PreviewCallback pCallback);

    void addCallbackBuffer(byte[] buffer);

    int getCameraId();

    void startPreview();

    void takePicture(PictureCallback picCallback);

    void stopPreview();

    void destoryCamera();

    void setErrorCallback(ErrorCallback eCallback);

}
