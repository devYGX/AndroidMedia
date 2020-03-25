package org.renderer;

public interface IRenderer {

    int FMT_NV21 = 0;
    int FMT_I420 = 1;

    void setupRenderer(int frameFormat, int frameWidth, int frameHeight, int degree, boolean isMirror) throws Exception;

    int refreshFrame(byte[] frame);

}
