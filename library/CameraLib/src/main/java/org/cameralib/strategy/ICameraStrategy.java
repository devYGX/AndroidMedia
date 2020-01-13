package org.cameralib.strategy;

/**
 * 相机策略;
 *
 * @author YGX
 */

public interface ICameraStrategy {

    int getCameraId();

    long getKeepAliveTimeMillis();

    /**
     * return camera preview size
     * @return int[]{width, height}
     */
    int[] getPreviewSize();
}
