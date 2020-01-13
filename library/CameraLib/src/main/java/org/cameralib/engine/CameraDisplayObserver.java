package org.cameralib.engine;

import android.hardware.Camera;

/**
 * 相机预览观察者
 *
 * @author YGX
 */

public interface CameraDisplayObserver {


    /**
     *
     * @param curCameraId 当前ID
     * @param nextCameraId 下一个相机ID
     */
    void onSwitchCamera(int curCameraId, int nextCameraId);



    /**
     * 相机错误
     *
     * @see Camera.ErrorCallback
     * @param err 错误码
     */
    void onCameraError(int err);
}
