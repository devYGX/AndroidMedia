package org.cameralib.engine;


import android.hardware.Camera;

/**
 * 相机PreviewCallback观察者，
 * <p>
 * 当调用{@link CameraManager#registerPreviewCallback(PreviewCallback, ActionCallback)}
 * 方法时，如果{@link ActionCallback#onAction(ActionCarrier)}方法返回的{@link ActionCarrier#isSuccess()}
 * 为true, 则{@link ActionCarrier#getExtra()}即为一个{@link CameraPreviewHandler} 对象，可以调用
 * CameraPreviewHandler的{@link CameraPreviewHandler#setCameraPreviewObserver(CameraPreviewObserver)}方法
 * 设置回调；
 * </P>
 *
 * @author YGX
 */
public interface CameraPreviewObserver {
    /**
     * 分发切换相机事件
     *
     * @param tokenId      切换相机的tokenId,
     * @param nextCameraId 即将切换的相机ID
     * @return true, 执行切换；false, 不切换
     */
    void dispatchSwitchCamera(long tokenId, int nextCameraId);

    /**
     * @param tokenId      切换相机的tokenId
     * @param nextCameraId 下一个相机ID
     */
    void onSwitchCamera(long tokenId, int nextCameraId);

    /**
     * 切换生效后
     *
     * @param tokenId 切换相机的tokenId
     */
    void onSwitchAvailable(long tokenId);

    /**
     * 相机错误
     *
     * @param err 错误码
     * @see Camera.ErrorCallback
     */
    void onCameraError(int err);
}
