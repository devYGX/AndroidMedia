package org.renderer.gles;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import org.renderer.IRenderer;
import org.renderer.RendererCode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class YUVRender implements GLSurfaceView.Renderer {

    private static final String TAG = "YUVRender";

    private static final float[] OS_MATRIX = new float[]{
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };

    // 默认的片元着色器代码
    private final String DEFAULT_FRAG_SHADER_CODE =
            "precision mediump float;\n"
                    + "varying vec2 tc;\n"
                    + "uniform sampler2D ySampler;\n"
                    + "uniform sampler2D uSampler;\n"
                    + "uniform sampler2D vSampler;\n"
                    + "const mat3 convertMat = mat3(1.0,1.0,1.0,0,-0.344,1.77,1.403,-0.714,0);\n"
                    + "void main(){\n"
                    + "     vec3 yuv;\n"
                    + "     yuv.x = texture2D(ySampler,tc).r;\n"
                    + "     yuv.y = texture2D(uSampler,tc).r - 0.5;\n"
                    + "     yuv.z = texture2D(vSampler,tc).r - 0.5;\n"
                    + "     vec4 color = vec4(convertMat * yuv,1.0);\n"
                    // + "     color.r = min(color.r + 0.2, 1.0);\n"
                    // + "     color.g = min(color.g + 0.2, 1.0);\n"
                    // + "     color.b = max(color.b - 0.2, 0.0);\n"
                    + "     gl_FragColor = color;\n"
                    + "}";

    private final String VERTEX_SHADER_CODE =
            "attribute vec4 attr_position;\n"
                    + "attribute vec2 attr_tc;\n"
                    + "varying vec2 tc;\n"
                    + "uniform mat4 vPMatrix;"
                    + "void main(){\n"
                    + "gl_Position = vPMatrix*attr_position;\n"
                    + "tc = attr_tc;\n"
                    + "}";

    /**
     * 0度未镜像时的顶点坐标
     * <p>
     * 0,1 **** 1,1
     * *        *
     * *        *
     * *        *
     * 0,0 **** 1,0
     * 在数组中坐标的排列方式：left-top, right-top, left-bottom, right-bottom
     */
    private static final float[] COOR_VERTICES_0 = new float[]{
            0, 1,
            1, 1,
            0, 0,
            1, 0
    };

    /**
     * 0度镜像时的顶点坐标
     * <p>
     * 1,1 **** 0,1
     * *        *
     * *        *
     * *        *
     * 1,0 **** 0,0
     * 在数组中坐标的排列方式：left-top, right-top, left-bottom, right-bottom
     */
    private static final float[] COOR_VERTICES_0_MIRROR = new float[]{
            1, 1,
            0, 1,
            1, 0,
            0, 0
    };

    /**
     * 90度未镜像时的顶点坐标
     * <p>
     * 1,1 **** 1,0
     * *        *
     * *        *
     * *        *
     * 0,1 **** 0,0
     * 在数组中坐标的排列方式：left-top, right-top, left-bottom, right-bottom
     */
    private static final float[] COOR_VERTICES_90 = new float[]{
            1, 1,
            1, 0,
            0, 1,
            0, 0
    };

    /**
     * 先镜像再旋转90度时的顶点坐标
     * <p>
     * 0,1 **** 0,0
     * *        *
     * *        *
     * *        *
     * 1,1 **** 1,0
     * 在数组中坐标的排列方式：left-top, right-top, left-bottom, right-bottom
     */
    private static final float[] COOR_VERTICES_90_MIRROR = new float[]{
            0.0F, 1.0F,
            0.0F, 0.0F,
            1.0F, 1.0F,
            1.0F, 0.0F
    };

    /**
     * 180度未镜像时的顶点坐标
     * <p>
     * 1,0 **** 0,0
     * *        *
     * *        *
     * *        *
     * 1,1 **** 0,1
     * 在数组中坐标的排列方式：left-top, right-top, left-bottom, right-bottom
     */
    private static final float[] COOR_VERTICES_180 = new float[]{
            1, 0,
            0, 0,
            1, 1,
            0, 1
    };
    /**
     * 先镜像再旋转180度时的顶点坐标
     * <p>
     * 0,0 **** 1,0
     * *        *
     * *        *
     * *        *
     * 0,1 **** 1,1
     * 在数组中坐标的排列方式：left-top, right-top, left-bottom, right-bottom
     */
    private static final float[] COOR_VERTICES_180_MIRROR = new float[]{
            0, 0,
            1, 0,
            0, 1,
            1, 1
    };

    /**
     * 270度未镜像时的顶点坐标
     * <p>
     * 0,0 **** 0,1
     * *        *
     * *        *
     * *        *
     * 1,0 **** 1,1
     * 在数组中坐标的排列方式：left-top, right-top, left-bottom, right-bottom
     */
    private static final float[] COOR_VERTICES_270 = new float[]{
            0, 0,
            0, 1,
            1, 0,
            1, 1
    };

    /**
     * 先镜像再旋转270度时的顶点坐标
     * <p>
     * 1,0 **** 1,1
     * *        *
     * *        *
     * *        *
     * 0,0 **** 0,1
     * 在数组中坐标的排列方式：left-top, right-top, left-bottom, right-bottom
     */
    private static final float[] COOR_VERTICES_270_MIRROR = new float[]{
            1, 0,
            1, 1,
            0, 0,
            0, 1
    };

    // 显示的范围顶点坐标
    private final float[] SQUARE_VERTICES = new float[]{
            -1, -1,
            1, -1,
            -1, 1,
            1, 1
    };

    private int glProgram;
    private int fmt;
    private int setupWidth;
    private int setupHeight;
    private int degree;
    private boolean isMirror;

    private ByteBuffer yBuffer;
    private ByteBuffer uBuffer;
    private ByteBuffer vBuffer;
    private FloatBuffer squareFloatBuffer;
    private FloatBuffer coorFloatBuffer;

    private byte[] yBuf;
    private byte[] uBuf;
    private byte[] vBuf;

    private int[] yTexture = new int[1];
    private int[] uTexture = new int[1];
    private int[] vTexture = new int[1];

    private volatile boolean surfaceCreated;
    private volatile boolean setuped;
    private volatile boolean input;
    private boolean needSetupProgram;
    private int vWidth;
    private int vHeight;

    private float[] mMatrixValue;
    private int vPMatrix;
    private int attr_position;
    private int attr_tc;

    public YUVRender() {
    }

    public boolean isSurfaceCreated() {
        return surfaceCreated;
    }

    public boolean isSetuped() {
        return setuped;
    }

    /*
    GLES20的api方法只能在gl线程调用, 否则提示call to OpenGL ES API with no current context
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initRenderer();
        surfaceCreated = true;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        vWidth = width;
        vHeight = height;
        mMatrixValue = getInitMatrix();
    }

    private float[] getInitMatrix() {
        if (this.setupWidth != 0 && this.setupHeight != 0 && this.vWidth != 0 && this.vHeight != 0) {
            float[] mm = new float[16];
            float[] projection = new float[16];
            float[] lookAtMatrix = new float[16];
            float rateImg = 1.0F * setupWidth / vHeight;
            float rateView = 1.0F * vWidth / vHeight;

            // 如果图像的宽高比大于控件的宽高比例; 说明需要按照top bottom为1来填充
            if (rateImg > rateView) {
                Matrix.orthoM(projection, 0, -rateView / rateImg, rateView / rateImg, -1, 1, 1, 3);
            } else {
                Matrix.orthoM(projection, 0, -1, 1, -rateImg / rateView, rateImg / rateView, 1, 3);
            }

            Matrix.setLookAtM(lookAtMatrix, 0, 0, 0, 2, 0, 0, 0, 0, 1, 0);
            Matrix.multiplyMM(mm, 0, projection, 0, lookAtMatrix, 0);

            // 2. 镜像
            if (isMirror) {
                Matrix.scaleM(mm, 0, -1, 1, 1);
            }

            // 1. 先旋转
            Matrix.rotateM(mm, 0, degree, 0, 0, 1);


            return mm;
        }
        return null;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        float[] matrixValue = this.mMatrixValue;
        if (matrixValue == null) return;
        if (needSetupProgram) {
            setupProgram();
            needSetupProgram = false;
        }

        if (!input) return;
        synchronized (this) {

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yTexture[0]);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D,
                    0, 0, 0,
                    setupWidth, setupHeight, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, uTexture[0]);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D,
                    0, 0, 0,
                    setupWidth >> 1, setupHeight >> 1, GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE, uBuffer);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, vTexture[0]);
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D,
                    0, 0, 0,
                    setupWidth >> 1, setupHeight >> 1,
                    GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, vBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        }
        input = false;
    }

    private void initRenderer() {
        //1. 加载顶点着色器
        int vertexShader = GLESUtils.loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);

        //2. 加载片元着色器
        int fragShader = GLESUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, DEFAULT_FRAG_SHADER_CODE);

        //3. 创建并链接程式
        glProgram = GLES20.glCreateProgram();

        GLES20.glAttachShader(glProgram, vertexShader);
        GLES20.glAttachShader(glProgram, fragShader);
        GLES20.glLinkProgram(glProgram);


        GLES20.glUseProgram(glProgram);
        int ySampler = GLES20.glGetUniformLocation(glProgram, "ySampler");
        int uSampler = GLES20.glGetUniformLocation(glProgram, "uSampler");
        int vSampler = GLES20.glGetUniformLocation(glProgram, "vSampler");
        vPMatrix = GLES20.glGetUniformLocation(glProgram, "vPMatrix");
        attr_position = GLES20.glGetAttribLocation(glProgram, "attr_position");
        attr_tc = GLES20.glGetAttribLocation(glProgram, "attr_tc");
        GLES20.glUniform1i(ySampler, 0);
        GLES20.glUniform1i(uSampler, 1);
        GLES20.glUniform1i(vSampler, 2);

    }


    public void setupRender(int fmt, int width, int height, int degree, boolean isMirror) {

        if ((this.fmt == fmt)
                && (this.setupWidth == width)
                && (this.setupHeight == height)
                && (this.degree == degree)
                && (this.isMirror == isMirror)) {
            return;
        }
        this.fmt = fmt;
        this.setupWidth = width;
        this.setupHeight = height;
        this.degree = degree;
        this.isMirror = isMirror;


        yBuffer = (ByteBuffer) ByteBuffer.allocateDirect(width * height)
                .order(ByteOrder.nativeOrder())
                .position(0);

        uBuffer = (ByteBuffer) ByteBuffer.allocateDirect((width >> 1) * (height >> 1))
                .order(ByteOrder.nativeOrder())
                .position(0);

        vBuffer = (ByteBuffer) ByteBuffer.allocateDirect((width >> 1) * (height >> 1))
                .order(ByteOrder.nativeOrder())
                .position(0);

        yBuf = new byte[width * height];
        uBuf = new byte[width * height / 4];
        vBuf = new byte[width * height / 4];
        setuped = true;
        needSetupProgram = true;
        mMatrixValue = getInitMatrix();
    }

    private void createTexture(int width, int height, int internalFmt, int[] texutreId) {
        GLES20.glGenTextures(1, texutreId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texutreId[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, internalFmt, width, height, 0, internalFmt, GLES20.GL_UNSIGNED_BYTE, null);
    }

    private void setupProgram() {
        int degree = this.degree;
        // boolean isMirror = this.isMirror;

        float[] coorVertices;
        switch (degree) {
            case 90:
                coorVertices = /*isMirror ? COOR_VERTICES_90_MIRROR : */COOR_VERTICES_90;
                break;
            case 180:
                coorVertices = /*isMirror ? COOR_VERTICES_180_MIRROR : */COOR_VERTICES_180;
                break;
            case 270:
                coorVertices = /*isMirror ? COOR_VERTICES_270_MIRROR : */COOR_VERTICES_270;
                break;
            case 0:
            default:
                coorVertices = /*isMirror ? COOR_VERTICES_0_MIRROR :*/ COOR_VERTICES_0;
                break;
        }

        coorFloatBuffer = (FloatBuffer) ByteBuffer.allocateDirect(coorVertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(coorVertices)
                .position(0);

        squareFloatBuffer = (FloatBuffer) ByteBuffer.allocateDirect(SQUARE_VERTICES.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(SQUARE_VERTICES)
                .position(0);

        GLES20.glUseProgram(glProgram);

        GLES20.glEnableVertexAttribArray(attr_position);
        GLES20.glEnableVertexAttribArray(attr_tc);
        GLES20.glVertexAttribPointer(attr_tc, 2, GLES20.GL_FLOAT, false, 8, coorFloatBuffer);
        GLES20.glVertexAttribPointer(attr_position, 2, GLES20.GL_FLOAT, false, 8, squareFloatBuffer);
        GLES20.glUniformMatrix4fv(vPMatrix, 1, false, this.mMatrixValue == null ? OS_MATRIX : mMatrixValue,0);

        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        createTexture(setupWidth, setupHeight, GLES20.GL_LUMINANCE, yTexture);
        createTexture(setupWidth >> 1, setupHeight >> 1, GLES20.GL_LUMINANCE, uTexture);
        createTexture(setupWidth >> 1, setupHeight >> 1, GLES20.GL_LUMINANCE, vTexture);
    }

    public int refreshFrame(byte[] frame) {
        if (!setuped) return RendererCode.RENDERER_INVALID;

        if (frame == null || frame.length != setupWidth * setupHeight * 3 / 2)
            return RendererCode.BAD_FRAME_SIZE;

        if (!supportFmt()) {
            return RendererCode.UNSUPPORT_FMT;
        }

        extraFrame(frame);
        input = true;

        return RendererCode.SUCCESS;
    }

    private void extraFrame(byte[] frame) {
        switch (fmt) {
            case IRenderer.FMT_I420:
                extraI420(frame);
                break;
            case IRenderer.FMT_NV21:
                extraNv21(frame);
                break;
        }
    }

    private void extraI420(byte[] frame) {
        System.arraycopy(frame, 0, yBuf, 0, setupWidth * setupHeight);

        System.arraycopy(frame, setupWidth * setupHeight, uBuf, 0, (setupWidth >> 1) * (setupHeight >> 1));
        System.arraycopy(frame, setupWidth * setupHeight + (setupWidth >> 1) * (setupHeight >> 1), vBuf, 0, (setupWidth >> 1) * (setupHeight >> 1));

        synchronized (this) {
            yBuffer.put(yBuf).position(0);
            uBuffer.put(uBuf).position(0);
            vBuffer.put(vBuf).position(0);
        }
    }

    private void extraNv21(byte[] frame) {
        System.arraycopy(frame, 0, yBuf, 0, setupWidth * setupHeight);
        int position = setupWidth * setupHeight;
        int index = 0;
        while (position < frame.length) {
            vBuf[index] = frame[position++];
            uBuf[index] = frame[position++];
            index++;
        }

        synchronized (this) {
            yBuffer.put(yBuf).position(0);
            uBuffer.put(uBuf).position(0);
            vBuffer.put(vBuf).position(0);
        }
    }

    private boolean supportFmt() {
        return this.fmt == IRenderer.FMT_NV21 || this.fmt == IRenderer.FMT_I420;
    }
}
