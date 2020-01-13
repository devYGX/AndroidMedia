package org.cameralib.engine;


import android.graphics.Bitmap;
import android.hardware.Camera;

public class CameraPic {

    private int cameraId;
    private Camera.Parameters params;
    private Bitmap pic;
    private int cameraDegree;

    public CameraPic(int cameraId, Camera.Parameters params, Bitmap pic, int cameraDegree) {
        this.cameraId = cameraId;
        this.params = params;
        this.pic = pic;
        this.cameraDegree = cameraDegree;
    }

    public int getCameraId() {
        return cameraId;
    }

    public Camera.Parameters getParams() {
        return params;
    }

    public Bitmap getPic() {
        return pic;
    }

    public int getCameraDegree() {
        return cameraDegree;
    }
}

