package org.cameralib.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * 相机帮助类
 *
 * @author YGX
 */

public class CameraHelper {
    private static final String TAG = "CameraHelper";

    public static int getCameraDisplayRotation(Context ctx, int cameraId) {
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);

        int rotation = wm.getDefaultDisplay().getRotation();

        int degree = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;
        }

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - ((cameraInfo.orientation + degree) % 360)) % 360;
        } else {
            return (cameraInfo.orientation - degree + 360) % 360;
        }
    }

    /**
     * @see #nv21ToJpgBitmap(byte[], int, int, int)
     */
    public static byte[] nv21ToJpgByteBuffer(byte[] nv21, int width, int height, int degree) {
        Log.e(TAG, "nv21ToJpgByteBuffer: " + degree);
        YuvImage image = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        image.compressToJpeg(new Rect(0, 0, width, height), 100, stream);
        byte[] byteArray = stream.toByteArray();

        if (degree == 0) return byteArray;

        Bitmap b = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        Matrix matrix = new Matrix();
        matrix.postRotate(-degree);
        Bitmap bitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, false);
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        boolean compress = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, arrayOutputStream);
        return arrayOutputStream.toByteArray();
    }

    /**
     * 将nv21 buffer数据转换为一张位图;
     * 如果degree的值不为0, 转换出来的位图, 宽和高可能会和参数传入的预览分辨率不一致;
     * 例如: one plus 6, width x height 为640 * 480, degree为 90;
     * 转换出来的位图, width x height 变为了 480 * 640;
     *
     * @param nv21   nv21格式的数据buffer
     * @param width  预览分辨率.宽
     * @param height 预览分辨率.高
     * @param degree 计算出来的相机显示角度
     * @param mirror 图片是否需要左右镜像
     * @return 位图
     */
    public static Bitmap nv21ToJpgBitmap(byte[] nv21, int width, int height, int degree, boolean mirror) {

        YuvImage image = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        image.compressToJpeg(new Rect(0, 0, width, height), 100, stream);
        byte[] byteArray = stream.toByteArray();

        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        if (degree == 0 && !mirror) return bitmap;
        Matrix matrix = new Matrix();
        int px = bitmap.getWidth() / 2;
        int py = bitmap.getHeight() / 2;
        if (mirror) {
            matrix.setScale(-1, 1, px, py);
            if (degree == 0)
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        } else if (degree == 90) {
            matrix.setScale(-1, 1, px, py);
        }

        matrix.postRotate(degree);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }

    public static Camera.Size getBestPreviewSize(Context context, Camera.Parameters parameters) {
        //预览大小设置
        Camera.Size previewSize = parameters.getPreviewSize();
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        if (supportedPreviewSizes != null && supportedPreviewSizes.size() > 0) {
            previewSize = getBestSupportedSize(supportedPreviewSizes, context.getResources().getDisplayMetrics());
        }
        return previewSize;
    }

    private static Camera.Size getBestSupportedSize(List<Camera.Size> sizes, DisplayMetrics metrics) {
        Camera.Size bestSize = sizes.get(0);
        float screenRatio = (float) metrics.widthPixels / (float) metrics.heightPixels;
        if (screenRatio > 1) {
            screenRatio = 1 / screenRatio;
        }

        for (Camera.Size s : sizes) {

            if (Math.abs((s.height / (float) s.width) - screenRatio) < Math.abs(bestSize.height / (float) bestSize.width - screenRatio)) {
                bestSize = s;
            }
            Log.e(TAG, "getBestSupportedSize: "+s.width+", "+s.height);
        }
        return bestSize;
    }
}
