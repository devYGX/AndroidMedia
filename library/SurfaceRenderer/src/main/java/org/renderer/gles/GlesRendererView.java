package org.renderer.gles;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import org.renderer.IRenderer;
import org.renderer.RendererCode;

public class GlesRendererView extends GLSurfaceView implements IRenderer {

    private YUVRender yuvRender;
    private int fmt;
    private int width;
    private int height;
    private int degree;
    private boolean isMirror;

    public GlesRendererView(Context context) {
        super(context);
        init();
    }

    public GlesRendererView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        yuvRender = new YUVRender();
        setRenderer(yuvRender);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void setupRenderer(int fmt, int width, int height, int degree, boolean isMirror) {

        if ((this.fmt == fmt)
                && (this.width == width)
                && (this.height == height)
                && (this.degree == degree)
                && (this.isMirror == isMirror)) {
            return;
        }
        this.fmt = fmt;
        this.width = width;
        this.height = height;
        this.degree = degree;
        this.isMirror = isMirror;
        yuvRender.setupRender(fmt, width, height, degree, isMirror);
    }


    @Override
    public int refreshFrame(byte[] frame) {
        int refreshFrame = yuvRender.refreshFrame(frame);
        if (refreshFrame == RendererCode.SUCCESS) {
            requestRender();
        }
        return refreshFrame;
    }
}
