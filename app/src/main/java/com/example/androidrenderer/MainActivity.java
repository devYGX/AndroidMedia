package com.example.androidrenderer;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.TextureView;

import androidx.appcompat.app.AppCompatActivity;

import org.cameralib.engine.ActionCallback;
import org.cameralib.engine.ActionCarrier;
import org.cameralib.engine.CameraDisplayHandler;
import org.cameralib.engine.CameraManager;
import org.cameralib.engine.PreviewCallback;
import org.cameralib.engine.PreviewParameter;
import org.renderer.IRenderer;
import org.renderer.SurfaceRendererView;
import org.renderer.gles.GlesRendererView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, PreviewCallback {
    int[] surfaceRendererIdRes = new int[]{R.id.rendererView1, R.id.rendererView2};
    int[] glesRendererIdRes = new int[]{R.id.glesRenderView0, R.id.glesRenderView1};
    List<IRenderer> iRendererList = new ArrayList<>();
    private CameraDisplayHandler displayHandler;
    private ExecutorService threadPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CameraManager.get().initContext(getApplicationContext());

        TextureView textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(this);
        for (int idRe : surfaceRendererIdRes) {
            SurfaceRendererView view = findViewById(idRe);
            iRendererList.add(view);
        }
        for (int idRe : glesRendererIdRes) {
            GlesRendererView view = findViewById(idRe);
            iRendererList.add(view);
        }

        threadPool = Executors.newFixedThreadPool(iRendererList.size() == 0 ? 1 : iRendererList.size());
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
                    for (IRenderer rendererView : iRendererList) {
                        try {
                            rendererView.setupRenderer(IRenderer.FMT_NV21, previewSize.width, previewSize.height, previewParameter.getDegree(), true);
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
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPreviewData(final byte[] buf, PreviewParameter paramter) {

        for (final IRenderer rendererView : iRendererList) {
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

        threadPool.shutdown();
    }
}
