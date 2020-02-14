package org.renderer;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import static org.renderer.RendererCode.RENDERER_INVALID;

public class FrameRendererView extends TextureView {
    private static final String TAG = "FrameRendererView";

    public static final int SCALE_TYPE_FIX_XY = 0;
    public static final int SCALE_TYPE_CENTER_CROP = 1;

    private SurfaceRenderer renderer;
    private byte[] frameBuf;
    private boolean lrMirror;
    private int mScaleType;
    private int mDisplayFrameWidth;
    private int mDisplayFrameHeight;
    private int mFrameFormat;
    private int mFrameWidth;
    private int mFrameHeight;

    public FrameRendererView(Context context) {
        super(context);
        init();
    }

    public FrameRendererView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FrameRendererView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    @Override
    public final void setSurfaceTextureListener(SurfaceTextureListener listener) {
        throw new RuntimeException("UnSupport Operation");
    }

    private Surface mSurface;
    private SurfaceTextureListener surfaceTextureListener = new SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurface = new Surface(surface);
            synchronized (FrameRendererView.this) {
                if (renderer != null) {
                    renderer.setSurface(mSurface);
                }
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            synchronized (FrameRendererView.this) {
                if (renderer != null) {
                    renderer.setSurface(null);
                }
            }
            mSurface = null;
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private void init() {
        super.setSurfaceTextureListener(surfaceTextureListener);
    }

    public synchronized final void setup(int frameFormat, int frameWidth, int frameHeight, int degree)
            throws Exception {
        if (renderer != null) {
            if (renderer.getFormat() == frameFormat
                    && renderer.getWidth() == frameWidth
                    && renderer.getHeight() == frameHeight) {
                Log.w(TAG, "setup repeat, format: " + frameFormat
                        + ", width: " + frameWidth
                        + ", height" + frameHeight);
                return;
            }
        }
        if (renderer != null) {
            renderer.release();
        }
        renderer = new SurfaceRenderer(frameWidth, frameHeight, frameFormat, degree);
        mFrameFormat = frameFormat;
        if (degree == 90 || degree == 270) {
            mFrameWidth = frameHeight;
            mFrameHeight = frameWidth;
        } else {
            mFrameWidth = frameWidth;
            mFrameHeight = frameHeight;
        }
        if (mSurface != null) {
            renderer.setSurface(mSurface);
        }
    }


    public void updateMatrix(boolean lrMirror, int scaleType) {
        this.mScaleType = scaleType;
        this.lrMirror = lrMirror;

        Matrix matrix = getTransform(new Matrix());
        int px = getWidth() / 2;
        int py = getHeight() / 2;
        switch (mScaleType) {
            case SCALE_TYPE_CENTER_CROP:
                int[] display = scaleCenterCrop(matrix);
                mDisplayFrameWidth = display[0];
                mDisplayFrameHeight = display[1];
                if (lrMirror) {
                    matrix.postScale(-1, 1, px, py);
                }
                break;
            case SCALE_TYPE_FIX_XY:
            default:
                // nothing to do
                mDisplayFrameWidth = getWidth();
                mDisplayFrameHeight = getHeight();
                if (lrMirror) {
                    matrix.setScale(-1, 1, px, py);
                }
                break;
        }

        setTransform(matrix);
    }

    private int[] scaleCenterCrop(Matrix matrix) {

        int previewWidth;
        int previewHeight;

        previewWidth = mFrameWidth;
        previewHeight = mFrameHeight;

        // 1. 分别求出填充屏幕时, 宽和高的缩放比例

        int width = getWidth();
        float ratioW = 1.0F * width / previewWidth;
        int height = getHeight();
        float ratioH = 1.0F * height / previewHeight;

        // 计算最小的缩放比例; 尽量用最小的缩放比例来缩放, 因为比例太大了可能会失帧, 画面不清晰;
        float min = Math.min(ratioH, ratioW);
        // 1.0979167, 1.0979167, 1.2, 768, 527, 640, 480

        // 如果最小的缩放比例是高;
        if (min == ratioH) {

            // 用高的缩放比, 求出宽度缩放后的宽度值;
            float scaleWidth = (previewWidth * ratioH);

            // 如果缩放后的控件宽度大与等于当前的控件宽度, 则按照当前的高的缩放比例进行缩放;
            if (scaleWidth >= width) {
                matrix.setScale(scaleWidth / width, 1, width / 2, height / 2);
                return new int[]{(int) scaleWidth, height};
            }

            // 如果按照高度的缩放比进行缩放后宽度小雨控件的实际宽度, 那么明显是不能以这个宽度进行缩放的;
            // 以宽的缩放比进行缩放;
            float scaleHeight = ratioW * previewHeight;
            matrix.setScale(1, scaleHeight / height, width / 2, height / 2);

            return new int[]{width, (int) scaleHeight};
        } else {
            // 如果最小的缩放比例是宽度的缩放比例;
            // 则按高度的缩放比例求出控件缩放后的宽度, 并与现在的宽度进行比较
            float scaleHeight = (previewHeight * ratioW);

            // 如果缩放后的控件高度大与等于当前的控件高度, 则按照当前的高的缩放比例进行缩放;
            if (scaleHeight >= height) {
                matrix.setScale(1, scaleHeight / height, width / 2, height / 2);
                return new int[]{width, (int) scaleHeight};
            }
            // 如果按照高度的缩放比进行缩放后宽度小于控件的实际宽度, 那么按照宽度的缩放比进行缩放
            float scaleWidth = ratioH * previewWidth;
            matrix.setScale(scaleWidth / width, 1, width / 2, height / 2);
            return new int[]{(int) scaleWidth, height};
        }
    }

    public synchronized final int refreshFrame(byte[] buffer) {
        if (renderer == null) {
            return RENDERER_INVALID;
        }
        if (frameBuf == null || frameBuf.length != buffer.length) {
            frameBuf = new byte[buffer.length];
        }
        System.arraycopy(buffer, 0, frameBuf, 0, buffer.length);
        return renderer.refreshFrame(buffer);
    }

    public synchronized void unSetup() {
        if (renderer != null) {
            renderer.release();
            renderer = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unSetup();
        Log.d(TAG, "onDetachedFromWindow: ");
    }
}
