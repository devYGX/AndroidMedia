package org.cameralib.engine;


import android.hardware.Camera;
import android.util.Log;

public class CameraDisplayHandler {

    private ICamera<Camera.Parameters, Camera.PreviewCallback, Camera.ErrorCallback, Camera.PictureCallback> mCamera;
    private Observer observer;
    private CameraDisplayObserver mPreviewObserver;
    private int mDegree;

    public CameraDisplayHandler(ICamera<Camera.Parameters,Camera.PreviewCallback, Camera.ErrorCallback, Camera.PictureCallback> camera,int degree,
                         Observer observer){
        this.mCamera = camera;
        this.observer = observer;
        this.mDegree = degree;
    }


    void onSwitchCamera(long token, int cur, int nextId){
        if (mPreviewObserver != null) {
            mPreviewObserver.onSwitchCamera(cur, nextId);
        }
    }


    public void onCameraError(int err){
        if (mPreviewObserver != null) {
            mPreviewObserver.onCameraError(err);
        }
    }

    public int getDegree(){
        return mDegree;
    }

    public Camera.Parameters getParameters(){
        return this.mCamera.getCameraConfig();
    }

    public void setParameters(Camera.Parameters p){
        this.mCamera.setCameraConfig(p);
    }

    public int getCameraId(){
        return mCamera.getCameraId();
    }

    public void setCameraPreviewObserver(CameraDisplayObserver observer){
        mPreviewObserver = observer;
    }

    public void stopDisplay(){
        Log.e("CameraManager", "stopDisplay: ");

        if (observer != null) {
            observer.onStopDisplay();
        }

        observer = null;


        mCamera = null;

    }

    public static interface Observer{

        void onStopDisplay();
    }
}
