package id.vouched.android.example;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import id.vouched.android.FaceDetect;
import id.vouched.android.FaceDetectOptions;
import id.vouched.android.FaceDetectResult;
import id.vouched.android.Instruction;
import id.vouched.android.Step;
import id.vouched.android.VouchedSession;
import id.vouched.android.VouchedUtils;
import id.vouched.android.liveness.LivenessMode;
import id.vouched.android.mlkit.GraphicOverlay;
import id.vouched.android.model.Job;
import id.vouched.android.model.JobResponse;
import id.vouched.android.model.RetryableError;


@RequiresApi(VERSION_CODES.LOLLIPOP)
public final class FaceDetectorActivity extends AppCompatActivity implements FaceDetect.OnDetectResultListener, VouchedSession.OnJobResponseListener {
    private static final int PERMISSION_REQUESTS = 1;

    private PreviewView previewView;
    private GraphicOverlay graphicOverlay;

    @Nullable
    private ProcessCameraProvider cameraProvider;
    @Nullable
    private Preview previewUseCase;
    @Nullable
    private ImageAnalysis analysisUseCase;

    @Nullable
    private FaceDetect faceDetect;

    private boolean needUpdateGraphicOverlayImageSourceInfo;

    private CameraSelector cameraSelector;
    private VouchedSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        faceDetect = new FaceDetect(this, new FaceDetectOptions.Builder().withLivenessMode(LivenessMode.MOUTH_MOVEMENT).build(), this);
        cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

        setContentView(R.layout.activity_face_2);
        previewView = findViewById(R.id.preview_view);
        if (previewView == null) {
            System.out.println("previewView is null");
        }
        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            System.out.println("graphicOverlay is null");
        }


        new ViewModelProvider(this, AndroidViewModelFactory.getInstance(getApplication()))
                .get(CameraXViewModel.class)
                .getProcessCameraProvider()
                .observe(
                        this,
                        provider -> {
                            cameraProvider = provider;
                            if (allPermissionsGranted()) {
                                bindAllCameraUseCases();
                            }
                        });
        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (session == null) {
            Intent i = getIntent();
            session = (VouchedSession) i.getSerializableExtra("Session");
        }
        if (faceDetect != null) {
            faceDetect.resume();
        }
        bindAllCameraUseCases();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (faceDetect != null) {
            faceDetect.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (faceDetect != null) {
            faceDetect.stop();
        }
    }

    @Override
    public void onFaceDetectResult(FaceDetectResult faceDetectResult) {
        TextView textView = (TextView) findViewById(R.id.textViewFaceInstruction);
        textView.setTextSize(20);
        textView.setTextColor(Color.WHITE);
        textView.setText(getFeedbackLabel(faceDetectResult.getInstruction()));

        if (faceDetectResult.getStep() == Step.POSTABLE) {
            session.postFace(this, faceDetectResult, null, this);

            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
        }
    }

    private String getFeedbackLabel(Instruction instruction) {
        switch (instruction) {
            case BLINK_EYES:
                return "Slowly Blink";
            case MOVE_AWAY:
                return "Move Away";
            case MOVE_CLOSER:
                return "Come Closer to Camera";
            case LOOK_FORWARD:
                return "Look Forward";
            case CLOSE_MOUTH:
                return "Close Mouth";
            case OPEN_MOUTH:
                return "Open Mouth";
            case HOLD_STEADY:
                return "Hold Steady";
            case NO_FACE:
                return "Show Face";
            case NONE:
            default:
                return "";
        }
    }

    @Override
    public void onJobResponse(JobResponse response) {
        faceDetect.reset();

        if (response.getError() != null) {
            System.out.println(response.getError().getMessage());
        } else {
            Job job = response.getJob();
            System.out.println(job.toJson());
            List<RetryableError> retryableErrors = VouchedUtils.extractRetryableFaceErrors(job);
            if (!retryableErrors.isEmpty()) {
                retryableErrors.forEach(System.out::println);

                Timer timer = new Timer();
                Runnable resume = new Runnable() {
                    public void run() {
                        if (faceDetect != null) {
                            faceDetect.resume();
                        }
                        bindAllCameraUseCases();
                    }
                };
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(resume);
                    }
                }, 5000);
            } else {
                Intent i = new Intent(FaceDetectorActivity.this, ResultsActivity.class);
                i.putExtra("Session", (Serializable) session);
                startActivity(i);
            }

        }
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        Preview.Builder builder = new Preview.Builder();
        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase);
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private void bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }

        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        analysisUseCase = builder.build();

        needUpdateGraphicOverlayImageSourceInfo = true;
        analysisUseCase.setAnalyzer(
                ContextCompat.getMainExecutor(this),
                imageProxy -> {
                    if (needUpdateGraphicOverlayImageSourceInfo) {
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        if (graphicOverlay != null) {
                            if (rotationDegrees == 0 || rotationDegrees == 180) {
                                graphicOverlay.setImageSourceInfo(
                                        imageProxy.getWidth(), imageProxy.getHeight(), true);
                            } else {
                                graphicOverlay.setImageSourceInfo(
                                        imageProxy.getHeight(), imageProxy.getWidth(), true);
                            }
                        }
                        needUpdateGraphicOverlayImageSourceInfo = false;
                    }
                    try {
                        if (faceDetect != null) {
                            faceDetect.processImageProxy(imageProxy, graphicOverlay);
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to process image. Error: " + e.getLocalizedMessage());
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });

        cameraProvider.bindToLifecycle(this, cameraSelector, analysisUseCase);
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        System.out.println("Permission granted!");
        if (allPermissionsGranted()) {
            bindAllCameraUseCases();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            System.out.println("Permission granted: " + permission);
            return true;
        }
        System.out.println("Permission NOT granted: " + permission);
        return false;
    }

}
