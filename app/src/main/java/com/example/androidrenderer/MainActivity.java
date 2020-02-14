package com.example.androidrenderer;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.view.TextureView;

import androidx.appcompat.app.AppCompatActivity;

import org.cameralib.engine.ActionCallback;
import org.cameralib.engine.ActionCarrier;
import org.cameralib.engine.CameraDisplayHandler;
import org.cameralib.engine.CameraManager;
import org.cameralib.engine.PreviewCallback;
import org.cameralib.engine.PreviewParameter;
import org.renderer.FrameRendererView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, PreviewCallback {
    private static final String TAG = "MainActivity";
    int[] rendererIdRes = new int[]{R.id.rendererView1, R.id.rendererView3, R.id.rendererView4};
    List<FrameRendererView> rendererViews = new ArrayList<>();
    private CameraDisplayHandler displayHandler;
    private ExecutorService threadPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CameraManager.get().initContext(getApplicationContext());

        TextureView textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(this);

        for (int idRe : rendererIdRes) {
            FrameRendererView view = findViewById(idRe);
            rendererViews.add(view);
        }

        threadPool = Executors.newFixedThreadPool(rendererViews.size());
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        CameraManager.get()
                .displayCamera(surface, new ActionCallback<CameraDisplayHandler>() {
                    @Override
                    public void onAction(ActionCarrier<CameraDisplayHandler> carrier) {
                        if (carrier.isSuccess()) {
                            displayHandler = carrier.getExtra();
                        } else {
                            carrier.getT().printStackTrace();
                        }
                    }
                }).registerPreviewCallback(this, new ActionCallback<PreviewParameter>() {
            @Override
            public void onAction(ActionCarrier<PreviewParameter> carrier) {
                if (carrier.isSuccess()) {
                    PreviewParameter previewParameter = carrier.getExtra();
                    Camera.Size previewSize = previewParameter.getPreviewSize();
                    for (FrameRendererView rendererView : rendererViews) {
                        try {
                            rendererView.setup(ImageFormat.NV21, previewSize.width, previewSize.height, previewParameter.getDegree());
                            rendererView.updateMatrix(true, FrameRendererView.SCALE_TYPE_CENTER_CROP);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    carrier.getT().printStackTrace();
                }
            }
        });
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onPreviewData(final byte[] buf, PreviewParameter paramter) {

        for (final FrameRendererView rendererView : rendererViews) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    long start = System.currentTimeMillis();

                    int i = rendererView.refreshFrame(buf);

                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraManager.get().unregisterPreviewCallback(this);
        if (displayHandler != null) {
            displayHandler.stopDisplay();
            displayHandler = null;
        }
        for (FrameRendererView rendererView : rendererViews) {
            rendererView.unSetup();
        }
        rendererViews.clear();

        threadPool.shutdown();
    }
}
