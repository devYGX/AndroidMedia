package org.cameralib.engine;


import android.hardware.Camera;

/**
 * 相机预览句柄
 *
 * <p>
 *     设计的思路是在设置PreviewCallback后，可以拿到CameraPreviewHandler对象;
 *     可以通过这个对象获取和设置相机的ID，参数等信息;
 *      @see CameraPreviewHandler#getParameters()
 *      @see CameraPreviewHandler#setParameters(Camera.Parameters)
 *      @see CameraPreviewHandler#getCameraId()
 *
 *     也可以给这个对象设置观察者，关注相机的切换，异常等事件
 *     @see CameraPreviewHandler#setCameraPreviewObserver(CameraPreviewObserver)
 *     @see CameraPreviewObserver
 *
 *     当需要释放相机时，调用取消注册方法
 *     @see CameraPreviewHandler#ungisterPreviewCallback()
 *     调用该方法后，CameraPreviewHandler不再有效，通过它获取或设置参数值都不再有意义
 *     (设置无效果或获取的值未null或-1），对应的PreviewCallback也不再接收到数据;
 * </p>
 *
 * @author YGX
 */
public class CameraPreviewHandler {

    private final long id;
    private final PreviewCallback pCallback;
    private final int degree;
    private ICamera<Camera.Parameters, Camera.PreviewCallback, Camera.ErrorCallback, Camera.PictureCallback> mCamera;
    private CameraManager observer;
    private CameraPreviewObserver mPreviewObserver;

    private boolean active;

    CameraPreviewHandler(ICamera<Camera.Parameters, Camera.PreviewCallback, Camera.ErrorCallback, Camera.PictureCallback> camera,
                         CameraManager observer, PreviewCallback pCallback,
                         long id, int degree) {
        this.mCamera = camera;
        this.observer = observer;
        this.pCallback = pCallback;
        this.id = id;
        this.degree = degree;
        active = true;
    }

    private long mSwitchTokenId;

    void dispatchSwitchCamera(int nextId) {
        if (mPreviewObserver != null) {
            mPreviewObserver.dispatchSwitchCamera(
                    mSwitchTokenId = System.currentTimeMillis(),
                    nextId);
        }

    }

    PreviewCallback getPreviewCallback() {
        return pCallback;
    }

    void onSwitchCamera(int nextId) {
        if (mPreviewObserver != null) {
            mPreviewObserver.onSwitchCamera(mSwitchTokenId, nextId);
        }
    }

    void onSwitchAvailable() {
        if (mPreviewObserver != null) {
            mPreviewObserver.onSwitchAvailable(mSwitchTokenId);
        }
    }

    void onCameraError(int err) {
        if (mPreviewObserver != null) {
            mPreviewObserver.onCameraError(err);
        }
    }

    long getPreviewCallbackId() {
        if(mCamera == null) return -1;
        return id;
    }

    public Camera.Parameters getParameters() {
        try {
            if (mCamera != null)
                return this.mCamera.getCameraConfig();
        }catch (Exception e){}
        return null;
    }

    public int getDegree(){
        return degree;
    }

    public void setParameters(Camera.Parameters p) {
        if (mCamera != null)
            this.mCamera.setCameraConfig(p);
    }

    public int getCameraId() {
        if (mCamera == null) return -1;

        return mCamera.getCameraId();
    }

    public void setCameraPreviewObserver(CameraPreviewObserver observer) {
        mPreviewObserver = observer;
    }

    boolean isActive(){
        return active;
    }

    public void ungisterPreviewCallback() {

        if (observer != null) {
            observer.ungisterPreviewCallback(id);
        }

        active = false;

        observer = null;

        mCamera = null;

    }

    public void switchCamera(int cameraId){
        if (observer != null) {
            observer.switchCamera(System.currentTimeMillis(),cameraId);
        }
    }

    interface Observer {

        void ungisterPreviewCallback(long id);

        void switchCamera(int cameraId);
    }
}
