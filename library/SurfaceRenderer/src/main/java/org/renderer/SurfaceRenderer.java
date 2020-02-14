package org.renderer;

import android.view.Surface;

public class SurfaceRenderer {

    static {
        System.loadLibrary("surface_renderer");
    }

    public static final int FMT_NV21 = 0;
    public static final int FMT_I420 = 1;


    private final int mDegree;

    private long mNativePtr;

    private final int mWidth;
    private final int mHeight;
    private final int mFormat;

    public SurfaceRenderer(int width, int height, int fmt, int degree) throws Exception {
        mNativePtr = nativeCreate(width, height, fmt, degree);
        this.mWidth = width;
        this.mHeight = height;
        this.mFormat = fmt;
        this.mDegree = degree;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getFormat() {
        return mFormat;
    }


    private native long nativeCreate(int width, int height, int fmt, int degree);

    private native int nativeSetSurface(long ptr, Surface surface);

    private native int nativeRefreshFrame(long ptr, byte[] buffer);

    private native void nativeRelease(long ptr);

    public int setSurface(Surface surface) {
        return nativeSetSurface(mNativePtr, surface);
    }

    /**
     * 刷新数据
     * @param buffer yuv frame
     * @return 0 is success; otherwise refresh error;see {@link RendererCode}
     */
    public int refreshFrame(byte[] buffer) {
        return nativeRefreshFrame(mNativePtr, buffer);
    }

    public void release() {
        nativeRelease(mNativePtr);
    }
}
