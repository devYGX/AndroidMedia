package org.cameralib.engine;


public class CameraFactory {

    public static ICamera open(int cameraId){
        return new Camera1Wrapper(cameraId);
    }
}
