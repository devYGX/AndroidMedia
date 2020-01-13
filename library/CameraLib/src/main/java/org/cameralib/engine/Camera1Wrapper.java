package org.cameralib.engine;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;


public class Camera1Wrapper implements ICamera<Camera.Parameters,
        Camera.PreviewCallback, Camera.ErrorCallback, Camera.PictureCallback> {
    private static final String TAG = "Camera1Wrapper";
    private final int mCameraId;
    private Camera mCamera;

    private boolean isPreviewing;

    public Camera1Wrapper(int cameraId) {
        this.mCameraId = cameraId;
    }


    @Override
    public void initCamera(int degrees) {
        Log.e(TAG, "initCamera: "+degrees+", "+mCameraId);
        mCamera = Camera.open(mCameraId);
        mCamera.setDisplayOrientation(degrees);
    }

    @Override
    public Camera.Parameters getCameraConfig() {
        if (mCamera != null) {
            return mCamera.getParameters();
        }
        return null;
    }

    @Override
    public void setCameraConfig(Camera.Parameters parameters) {
        if (mCamera != null) {
            mCamera.setParameters(parameters);
        }
    }

    @Override
    public void setDisplaySurfaceView(SurfaceHolder displayer) throws IOException {
        if (mCamera != null) {
            mCamera.setPreviewDisplay(displayer);
        }
    }

    @Override
    public void setDisplayTextureView(SurfaceTexture displayer) throws IOException {
        if (mCamera != null) {
            mCamera.setPreviewTexture(displayer);
        }
    }

    @Override
    public void setPreviewCallback(Camera.PreviewCallback pCallback) {
        if (mCamera != null) {
            mCamera.setPreviewCallback(pCallback);

        }
    }

    @Override
    public void setPreviewCallbackWithBuffer(Camera.PreviewCallback pCallback) {
        if (mCamera != null) {
            mCamera.setPreviewCallbackWithBuffer(pCallback);
        }
    }

    @Override
    public void addCallbackBuffer(byte[] buffer) {
        if (mCamera != null) {
            mCamera.addCallbackBuffer(buffer);
        }
    }


    @Override
    public int getCameraId() {
        return mCameraId;
    }

    @Override
    public void startPreview() {
        if (mCamera != null) {
            if (isPreviewing) return;

            isPreviewing = true;
            mCamera.startPreview();
        }
    }

    @Override
    public void takePicture(Camera.PictureCallback picCallback) {
        mCamera.takePicture(null, null, picCallback);
    }

    @Override
    public void stopPreview() {
        if (mCamera != null) {

            if (!isPreviewing) return;

            mCamera.stopPreview();
            isPreviewing = false;
        }
    }

    @Override
    public void destoryCamera() {
        try {
            mCamera.stopPreview();
        } catch (Exception e1) {
        }

        try {
            mCamera.release();
        } catch (Exception e1) {
        }

        mCamera = null;
    }

    @Override
    public void setErrorCallback(Camera.ErrorCallback eCallback) {
        if (mCamera != null) {
            mCamera.setErrorCallback(eCallback);
        }
    }


}
