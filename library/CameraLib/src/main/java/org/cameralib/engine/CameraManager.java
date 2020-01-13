package org.cameralib.engine;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

import org.cameralib.strategy.DefaultCameraStrategy;
import org.cameralib.strategy.ICameraStrategy;
import org.cameralib.utils.CameraHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public final class CameraManager implements CameraDisplayHandler.Observer,
        Camera.ErrorCallback {
    private static final String TAG = "CameraManager";
    private static CameraManager sCameraManager;
    private GuardHandler mGuardHandler;

    private CameraHandler mCameraHandler;
    private Context mCtx;
    private volatile boolean isHoldingDisplay;
    private boolean isHoldingPreviewCallback;
    private boolean isHoldingTakePic;
    private boolean live;
    private long mDelayMillis = 2 * 1000;
    private boolean guard;
    private byte[] callbackBuffer;

    @SuppressLint("UseSparseArrays")
    private Map<PreviewCallback, Boolean> mPreviewCallbacks2 = new HashMap<>();
    private Map<CameraErrorCallback, Boolean> mErrorCallback = new HashMap<>();

    private static final int TO_MAIN = 1;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TO_MAIN:
                    ActionCarrier obj = (ActionCarrier) msg.obj;
                    if (obj != null) obj.postToTarget();
                    break;
            }
        }
    };
    private int mCurCameraDegree;
    private SurfaceTexture mDisplaySurfaceTexture;
    private SurfaceHolder mDisplayHolder;
    private ICameraStrategy mCameraStrategy;
    private Camera.Parameters cameraParameters;

    @Override
    public void onStopDisplay() {
        mPreviewHandler = null;
        stopCurDisplay();

    }

    private void stopCurDisplay() {
        Message msg = Message.obtain(mCameraHandler);
        msg.what = CameraHandler.STOP_CUR_DISPLAY;
        msg.setTarget(mCameraHandler);
        msg.sendToTarget();
    }

    @Override
    public void onError(int error, Camera camera) {
        mGuardHandler.removeMessages(CameraHandler.DAEMON);
        onCameraError(error);
    }


    private PreviewParameter previewParamter;
    private Camera.Size mPreviewSize;

    private PreviewParameter getPreviewParameter() {
        if (previewParamter == null) {
            previewParamter = new PreviewParameter(mPreviewSize, mCurCameraId, mCurCameraDegree, cameraParameters);
        }
        return previewParamter;
    }

    private Random r = new Random();

    private void unregisterPreviewCallbackInQueue(PreviewCallback previewCallback) {
        mPreviewCallbacks2.remove(previewCallback);
        Log.e(TAG, "unregisterPreviewCallbackInQueue: " + previewCallback + ", " + mPreviewCallbacks2.size());
        if (mPreviewCallbacks2.size() == 0) {
            isHoldingPreviewCallback = false;
            delayReleaseCamera();
        }
    }

    void ungisterPreviewCallback(long id) {

        mCameraHandler.sendMessage(Message.obtain(mCameraHandler, CameraHandler.UNREG_PRE_CB, id));
    }

    void switchCamera(long token, int newCameraId) {
        Message msg = Message.obtain(mCameraHandler);
        msg.what = CameraHandler.SWITCH_CAMERA;
        msg.arg1 = newCameraId;
        msg.obj = token;
        msg.setTarget(mCameraHandler);
        msg.sendToTarget();
    }

    Camera.Parameters getParameters() {
        return mCameraWrapper == null ? null : mCameraWrapper.getCameraConfig();
    }

    int getCameraFacing() {
        return mCameraWrapper == null ? -1 : mCurCameraId;
    }

    final class GuardHandler extends Handler {
        final static int GUARD = -1;
        final static int FREEZE_GUARD = -2;

        GuardHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.e(TAG, "handleMessage: blocking" + msg.what);
            switch (msg.what) {
                case GUARD:
                    // CameraReboot.rebootCAM(mCtx);
                    guard = true;
                    break;
                case FREEZE_GUARD:
                    removeMessages(FREEZE_GUARD);
                    // Log.e(TAG, "handleMessage: " + live);
                    if (live) {
                        sendEmptyMessageDelayed(FREEZE_GUARD, mDelayMillis);
                        live = false;
                    } else {
                        onCameraErrorInQueue(-1);
                    }
                    break;
            }
        }
    }

    private void onCameraErrorInQueue(int err) {
        Message msg = Message.obtain(mCameraHandler);
        msg.obj = err;
        msg.what = CameraHandler.FREEZE;
        msg.setTarget(mCameraHandler);
        msg.sendToTarget();
    }

    final class CameraHandler extends Handler {

        final static int DISPLAY = 1;
        final static int TAKE_PIC = 2;
        final static int REG_PRE_CB = 3;
        final static int REG_PRE_CB_2 = 12;
        final static int UNREG_PRE_CB = 4;
        final static int FORCE_RELEASE = 5;
        final static int DELAY_RELEASE = 6;
        final static int REG_STRATEGY = 7;
        final static int SWITCH_CAMERA = 8;
        final static int STOP_CUR_DISPLAY = 9;
        final static int DAEMON = 10;
        final static int UNREG_CB_BY_ID = 11;
        final static int UNREG_CB_2 = 13;
        final static int REG_ERROR_CB = 14;
        final static int UNREG_ERROR_CB = 15;

        final static int ARG1_DISPLAY_SURFACETEXTURE = 1;
        final static int ARG1_DISPLAY_SURFACEHOLDER = 2;
        public static final int FREEZE = 16;


        CameraHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DISPLAY:
                    display(msg);
                    break;
                case TAKE_PIC:
                    takePic(msg);
                    break;
                case REG_PRE_CB:
                    registerPreviewCallbackInQueue(msg);
                    break;
                case UNREG_CB_2:
                    unregisterPreviewCallbackInQueue((PreviewCallback) msg.obj);
                    break;
                case FORCE_RELEASE:
                    releaseAll(msg);
                    break;
                case DELAY_RELEASE:
                    releaseAll(null);
                    break;
                case STOP_CUR_DISPLAY:
                    stopCurDisplayInHandlerQueue();
                    break;
                case UNREG_ERROR_CB:
                    mErrorCallback.remove((CameraErrorCallback) msg.obj);
                    break;
                case REG_ERROR_CB:
                    mErrorCallback.put((CameraErrorCallback) msg.obj, true);
                    break;
                case FREEZE:
                    onCameraError((Integer) msg.obj);
                    break;
            }
            mGuardHandler.removeMessages(GuardHandler.GUARD);
        }
    }

    @SuppressWarnings("unchecked")
    private void registerPreviewCallbackInQueue(Message msg) {
        List list = (List) msg.obj;
        PreviewCallback pCallback = (PreviewCallback) list.get(0);
        ActionCallback<PreviewParameter> aCallback = (ActionCallback<PreviewParameter>) list.get(1);
        ICameraStrategy strategy = (ICameraStrategy) list.get(2);
        if (mCameraStrategy == null) {
            mCameraStrategy = strategy;
        }
        try {
            initCamera();

            mPreviewCallbacks2.put(pCallback, true);

            if (mPreviewCallbacks2.size() > 0) {
                isHoldingPreviewCallback = true;
            }
            ActionCarrier<PreviewParameter> carrier = new ActionCarrier<>(true,
                    aCallback.getId(),
                    getPreviewParameter(), null, aCallback);
            mHandler.sendMessage(Message.obtain(mHandler, TO_MAIN, carrier));
        } catch (Exception e) {
            releaseCamera();
            ActionCarrier<PreviewParameter> carrier = new ActionCarrier<>(false,
                    aCallback.getId(),
                    null, e, aCallback);
            mHandler.sendMessage(Message.obtain(mHandler, TO_MAIN, carrier));
        }
    }

    private void stopCurDisplayInHandlerQueue() {
        mDisplayHolder = null;
        mDisplaySurfaceTexture = null;
        isHoldingDisplay = false;
        delayReleaseCamera();
    }

    private void releaseCamera() {
        mGuardHandler.removeMessages(CameraHandler.DAEMON);
        mCameraHandler.removeMessages(CameraHandler.DELAY_RELEASE);
        if (mCameraWrapper != null) {
            try {
                mCameraWrapper.setPreviewCallback(null);
            } catch (Exception e) {
            }
            try {
                mCameraWrapper.setDisplayTextureView(null);
            } catch (IOException e) {
            }
            try {
                mCameraWrapper.setDisplaySurfaceView(null);
            } catch (IOException e) {
            }
            mCameraWrapper.destoryCamera();
            mCameraWrapper = null;
        }
    }

    private void onCameraError(final int err) {
        releaseAll(null);
        if (mErrorCallback.size() > 0) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Set<Map.Entry<CameraErrorCallback, Boolean>> set = mErrorCallback.entrySet();
                    Iterator<Map.Entry<CameraErrorCallback, Boolean>> iterator = set.iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<CameraErrorCallback, Boolean> entry = iterator.next();
                        Boolean value = entry.getValue();
                        if (value != null && value) entry.getKey().onErr(err);
                    }
                }
            });
        }
    }

    private void releaseAll(Message msg) {
        ActionCallback aCallback = null;
        if (msg != null && msg.obj != null && (msg.obj instanceof ActionCallback)) {
            aCallback = ((ActionCallback) msg.obj);
        }
        if (mCameraWrapper == null) {
            if (aCallback != null) {
                ActionCarrier<Void> carrier = new ActionCarrier<>(true,
                        aCallback.getId(),
                        null, null, aCallback);
                mHandler.sendMessage(Message.obtain(mHandler, TO_MAIN, carrier));
            }
            return;
        }
        if (mPreviewHandler != null) {
            mPreviewHandler.onCameraError(ICamera.FORCE_RELEASE);
            mPreviewHandler = null;
        }
        releaseCamera();
        isHoldingDisplay = false;
        isHoldingPreviewCallback = false;
        if (aCallback != null) {
            ActionCarrier<Void> carrier = new ActionCarrier<>(true,
                    aCallback.getId(),
                    null, null, aCallback);
            mHandler.sendMessage(Message.obtain(mHandler, TO_MAIN, carrier));
        }
        Log.e(TAG, "releaseAll: " + aCallback + mCameraWrapper);
    }


    @SuppressWarnings("unchecked")
    private void takePic(Message msg) {
        List list = (List) msg.obj;
        final ActionCallback aCallback = (ActionCallback) list.get(0);
        final Handler handler = (Handler) list.get(1);
        try {
            initCamera();
            isHoldingTakePic = true;
            if (mPreviewFrame != null && (System.currentTimeMillis() - mPreviewFrameMillis) < 1000) {
                byte[] cap = mPreviewFrame.clone();
                Camera.Parameters parameters = mCameraWrapper.getCameraConfig();
                Camera.Size previewSize = parameters.getPreviewSize();
                Bitmap jpegBuffer = CameraHelper.nv21ToJpgBitmap(
                        cap,
                        previewSize.width,
                        previewSize.height,
                        mCurCameraDegree, mCurCameraId == Camera.CameraInfo.CAMERA_FACING_BACK);
                final ActionCarrier<CameraPic> carrier = new ActionCarrier<>(true,
                        aCallback.getId(),
                        new CameraPic(mCurCameraId, parameters, jpegBuffer, mCurCameraDegree),
                        null,
                        aCallback);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        aCallback.onAction(carrier);
                    }
                });
            } else {
                final ActionCarrier<CameraPic> carrier = new ActionCarrier<>(false,
                        aCallback.getId(),
                        null, new RuntimeException("no camera preview frame data"), aCallback);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        aCallback.onAction(carrier);
                    }
                });
            }

        } catch (Exception e) {
            final ActionCarrier<CameraPic> carrier = new ActionCarrier<>(false,
                    aCallback.getId(),
                    null, e, aCallback);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    aCallback.onAction(carrier);
                }
            });
            isHoldingTakePic = false;
        }
    }

    private void delayReleaseCamera() {
        Log.e(TAG, "delayReleaseCamera: " + isHoldingDisplay + ", " + isHoldingPreviewCallback);
        if (isHoldingDisplay || isHoldingPreviewCallback) return;

        Log.e(TAG, "delayReleaseCamera: ");
        mCameraHandler.removeMessages(CameraHandler.DELAY_RELEASE);
        mCameraHandler.sendEmptyMessageDelayed(CameraHandler.DELAY_RELEASE, getStragety().getKeepAliveTimeMillis());
    }

    private ICameraStrategy getStragety() {
        return mCameraStrategy;
    }


    @SuppressWarnings("unchecked")
    private void display(Message msg) {
        int arg1 = msg.arg1;
        List obj = (List) msg.obj;
        ActionCallback aCallback = (ActionCallback) obj.get(1);

        if (mCameraStrategy == null) {
            mCameraStrategy = (ICameraStrategy) obj.get(2);
        }
        try {
            initCamera();
            switch (arg1) {
                case CameraHandler.ARG1_DISPLAY_SURFACETEXTURE:
                    mDisplaySurfaceTexture = (SurfaceTexture) obj.get(0);
                    mCameraWrapper.setDisplayTextureView(mDisplaySurfaceTexture);
                    break;
                case CameraHandler.ARG1_DISPLAY_SURFACEHOLDER:
                    mDisplayHolder = (SurfaceHolder) obj.get(0);
                    mCameraWrapper.setDisplaySurfaceView(mDisplayHolder);
                    break;
            }
            isHoldingDisplay = true;
            mPreviewHandler = new CameraDisplayHandler(mCameraWrapper, mCurCameraDegree, this);
            if (aCallback != null) {
                ActionCarrier<CameraDisplayHandler> carrier = new ActionCarrier<>(true,
                        aCallback.getId(),
                        mPreviewHandler,
                        null,
                        aCallback);
                mHandler.sendMessage(Message.obtain(mHandler, TO_MAIN, carrier));
            }
        } catch (Exception e) {
            if (aCallback != null) {
                ActionCarrier<CameraDisplayHandler> carrier = new ActionCarrier<>(
                        false,
                        aCallback.getId(),
                        null,
                        e,
                        aCallback);
                mHandler.sendMessage(Message.obtain(mHandler, TO_MAIN, carrier));
            }
        }
    }

    private void initCamera() {
        try {
            mCameraHandler.removeMessages(CameraHandler.DELAY_RELEASE);
            if (mCameraWrapper == null) {
                ICameraStrategy stragety = getStragety();
                mCurCameraId = stragety.getCameraId();
                Camera1Wrapper wrapper = new Camera1Wrapper(mCurCameraId);
                mCurCameraDegree = CameraHelper.getCameraDisplayRotation(mCtx, mCurCameraId);
                wrapper.initCamera(mCurCameraDegree);
                Camera.Parameters parameters = wrapper.getCameraConfig();
                parameters.setPreviewFormat(ImageFormat.NV21);
                int[] previewSize = stragety.getPreviewSize();
                if (previewSize != null) {
                    parameters.setPreviewSize(previewSize[0], previewSize[1]);
                } else {
                    Camera.Size bestPreviewSize = CameraHelper.getBestPreviewSize(mCtx, parameters);
                    parameters.setPreviewFormat(ImageFormat.NV21);
                    parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
                }
                mPreviewSize = parameters.getPreviewSize();
                Log.e(TAG, "initCamera: " + mPreviewSize.width + ", " + mPreviewSize.height);
                wrapper.setCameraConfig(parameters);
                cameraParameters = parameters;
                wrapper.startPreview();
                wrapper.setErrorCallback(eCallback);
                wrapper.setPreviewCallbackWithBuffer(pCallback);
                wrapper.addCallbackBuffer(callbackBuffer = new byte[mPreviewSize.width * mPreviewSize.height * 3 / 2]);
                mCameraWrapper = wrapper;
            }
        } catch (Exception e) {
            releaseCamera();
            throw e;
        }
    }

    private Camera.ErrorCallback eCallback = new Camera.ErrorCallback() {
        @Override
        public void onError(int error, Camera camera) {
            onCameraError(error);
        }
    };

    private byte[] mPreviewFrame;
    private long mPreviewFrameMillis;
    private Camera.PreviewCallback pCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            mPreviewFrame = data;
            mPreviewFrameMillis = System.currentTimeMillis();
            if (mPreviewCallbacks2.size() > 0) {
                Set<Map.Entry<PreviewCallback, Boolean>> set = mPreviewCallbacks2.entrySet();
                for (Map.Entry<PreviewCallback, Boolean> entry : set) {
                    Boolean value = entry.getValue();
                    if (value != null && value) {
                        entry.getKey().onPreviewData(data, getPreviewParameter());
                    }
                }
            }
            mCameraWrapper.addCallbackBuffer(callbackBuffer);
        }
    };


    private ICamera<Camera.Parameters, Camera.PreviewCallback, Camera.ErrorCallback, Camera.PictureCallback> mCameraWrapper;
    private int mCurCameraId = 1;
    private CameraDisplayHandler mPreviewHandler;

    private CameraManager() {

        HandlerThread camera = new HandlerThread("camera");
        camera.start();
        mCameraHandler = new CameraHandler(camera.getLooper());

        HandlerThread ht = new HandlerThread("GuardHandler");
        ht.start();
        mGuardHandler = new GuardHandler(ht.getLooper());
    }

    public static CameraManager get() {
        if (sCameraManager == null) {
            synchronized (CameraManager.class) {
                if (sCameraManager == null) sCameraManager = new CameraManager();
            }
        }
        return sCameraManager;
    }

    public void initContext(Context ctx) {
        mCtx = ctx.getApplicationContext();
    }

    /**
     * 展示相机
     *
     * @param displayer 展示器
     * @param aCallback 回调
     */
    public CameraManager displayCamera(SurfaceTexture displayer, ActionCallback<CameraDisplayHandler> aCallback) {
        displayCamera(null, displayer, aCallback);
        return this;
    }

    /**
     * 展示相机
     *
     * @param displayer 展示器
     * @param aCallback 回调
     */
    public CameraManager displayCamera(ICameraStrategy strategy, SurfaceTexture displayer, ActionCallback<CameraDisplayHandler> aCallback) {
        Message msg = Message.obtain(mCameraHandler);
        msg.obj = Arrays.asList(displayer, aCallback, strategy == null ? new DefaultCameraStrategy() : strategy);
        msg.what = CameraHandler.DISPLAY;
        msg.arg1 = CameraHandler.ARG1_DISPLAY_SURFACETEXTURE;
        msg.setTarget(mCameraHandler);
        msg.sendToTarget();
        return this;
    }

    /**
     * 展示相机
     *
     * @param displayer 展示器
     * @param aCallback 回调
     */
    public CameraManager displayCamera(SurfaceHolder displayer, ActionCallback<CameraDisplayHandler> aCallback) {
        displayCamera(null, displayer, aCallback);
        return this;
    }

    /**
     * 展示相机
     *
     * @param displayer 展示器
     * @param aCallback 回调
     */
    public CameraManager displayCamera(ICameraStrategy strategy, SurfaceHolder displayer, ActionCallback<CameraDisplayHandler> aCallback) {
        Message msg = Message.obtain(mCameraHandler);
        msg.obj = Arrays.asList(displayer, aCallback, strategy == null ? new DefaultCameraStrategy() : strategy);
        msg.what = CameraHandler.DISPLAY;
        msg.arg1 = CameraHandler.ARG1_DISPLAY_SURFACEHOLDER;
        msg.setTarget(mCameraHandler);
        msg.sendToTarget();
        return this;
    }

    /**
     * 拍照
     *
     * @param aCallback 回调
     */
    public void takePicture(Handler subscriber, ActionCallback<CameraPic> aCallback) {
        Message msg = Message.obtain(mCameraHandler);
        if (subscriber == null) {
            subscriber = mHandler;
        }
        msg.obj = Arrays.asList(aCallback, subscriber);
        msg.what = CameraHandler.TAKE_PIC;
        msg.setTarget(mCameraHandler);
        msg.sendToTarget();
    }

    /**
     * 拍照
     *
     * @param aCallback 回调
     */
    public void takePicture(ActionCallback<CameraPic> aCallback) {
        takePicture(null, aCallback);
    }


    /**
     * 注册预览回调， 如果
     *
     * @param pCallback 相机的Preview
     */
    public void registerPreviewCallback(ICameraStrategy strategy,
                                        PreviewCallback pCallback,
                                        ActionCallback<PreviewParameter> aCallback) {
        Message msg = Message.obtain(mCameraHandler);
        msg.obj = Arrays.asList(pCallback, aCallback,
                strategy == null ? new DefaultCameraStrategy() : strategy);
        msg.what = CameraHandler.REG_PRE_CB;
        msg.setTarget(mCameraHandler);
        msg.sendToTarget();
    }

    /**
     * 注册预览回调， 如果
     *
     * @param pCallback 相机的Preview
     */
    public void registerPreviewCallback(PreviewCallback pCallback, ActionCallback<PreviewParameter> aCallback) {
        registerPreviewCallback(null, pCallback, aCallback);
    }


    /**
     * 强制停止; 在呼叫前需要释放
     */
    public void forceRelease(ActionCallback<Void> aCallback) {
        Message msg = Message.obtain(mCameraHandler);
        msg.what = CameraHandler.FORCE_RELEASE;
        msg.obj = aCallback;
        msg.setTarget(mCameraHandler);
        msg.sendToTarget();
    }

    public void unregisterPreviewCallback(PreviewCallback pCallback) {
        if (pCallback == null) return;
        if (mPreviewCallbacks2.containsKey(pCallback)) {
            mPreviewCallbacks2.put(pCallback, false);
        } else {
            return;
        }
        Message msg = Message.obtain(mCameraHandler);
        msg.obj = pCallback;
        msg.what = CameraHandler.UNREG_CB_2;
        msg.setTarget(mCameraHandler);
        msg.sendToTarget();
    }

    public void registerErrorCallback(CameraErrorCallback eCallback) {
        Message msg = Message.obtain(mCameraHandler);
        msg.obj = eCallback;
        msg.what = CameraHandler.REG_ERROR_CB;
        msg.setTarget(mCameraHandler);
        msg.sendToTarget();
    }

    public void unregisterErrorCallback(CameraErrorCallback eCallback) {
        if (eCallback == null) return;
        if (mErrorCallback.containsKey(eCallback)) {
            mErrorCallback.put(eCallback, false);
        }
        Message msg = Message.obtain(mCameraHandler);
        msg.obj = eCallback;
        msg.what = CameraHandler.UNREG_ERROR_CB;
        msg.setTarget(mCameraHandler);
        msg.sendToTarget();
    }

    /**
     * 是否持有相机
     *
     * @return true， 是; false 否
     */
    public boolean isHoldingCamera() {
        return mCameraWrapper != null;
    }

}
