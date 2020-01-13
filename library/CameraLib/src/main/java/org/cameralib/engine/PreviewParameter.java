package org.cameralib.engine;


import android.hardware.Camera;

public class PreviewParameter {

    private Camera.Size previewSize;
    private int id;
    private int degree;
    private Camera.Parameters cameraParameters;

    public PreviewParameter(Camera.Size previewSize, int id, int degree, Camera.Parameters parameters) {
        this.previewSize = previewSize;
        this.id = id;
        this.degree = degree;
        this.cameraParameters = parameters;
    }

    public Camera.Size getPreviewSize() {
        return previewSize;
    }

    public void setPreviewSize(Camera.Size previewSize) {
        this.previewSize = previewSize;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDegree() {
        return degree;
    }

    public void setDegree(int degree) {
        this.degree = degree;
    }

    public Camera.Parameters getCameraParameters() {
        return cameraParameters;
    }

    @Override
    public String toString() {
        return "PreviewParameter{" +
                "previewSize=[width:" + previewSize.width + ", height: " + previewSize.height + "]" +
                ", id=" + id +
                ", degree=" + degree +
                '}';
    }
}
