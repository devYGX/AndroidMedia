package org.cameralib.strategy;

import android.hardware.Camera;

/**
 * 默认打开相机的策略
 *
 * @author YGX
 */

public class DefaultCameraStrategy implements ICameraStrategy {

    private static final int DEFAULT_CAMERA_ID = 1;
    private static final int DEFAULT_PREVIEW_WIDTH = 640;
    private static final int DEFAULT_PREVIEW_HEIGHT = 480;
    private static final int DEFAULT_KEEP_ALIVE = 0;

    @Override
    public int getCameraId() {
        return Camera.getNumberOfCameras() > 1 ? 1 : 0;
    }


    @Override
    public long getKeepAliveTimeMillis() {
        return DEFAULT_KEEP_ALIVE;
    }

    @Override
    public int[] getPreviewSize() {
        return new int[]{DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT};
        // return new int[]{1024,768};
    }
}
