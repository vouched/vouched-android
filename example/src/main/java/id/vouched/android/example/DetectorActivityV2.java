package id.vouched.android.example;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import id.vouched.android.BarcodeDetect;
import id.vouched.android.BarcodeResult;
import id.vouched.android.CardDetect;
import id.vouched.android.CardDetectOptions;
import id.vouched.android.CardDetectResult;
import id.vouched.android.Instruction;
import id.vouched.android.VouchedSession;
import id.vouched.android.VouchedSessionParameters;
import id.vouched.android.VouchedUtils;
import id.vouched.android.model.Insight;
import id.vouched.android.model.Job;
import id.vouched.android.model.JobResponse;
import id.vouched.android.model.Params;

public class DetectorActivityV2 extends AppCompatActivity implements CardDetect.OnDetectResultListener, BarcodeDetect.OnBarcodeResultListener, VouchedSession.OnJobResponseListener {

    private static final int PERMISSION_REQUESTS = 1;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(720, 1280);
    private static final Size DESIRED_PREVIEW_BARCODE_SIZE = new Size(1080, 1920);

    private PreviewView previewView;

    @Nullable
    private ProcessCameraProvider cameraProvider;
    @Nullable
    private Preview previewUseCase;
    @Nullable
    private ImageAnalysis analysisUseCase;

    @Nullable
    private CardDetect cardDetect;
    @Nullable
    private BarcodeDetect barcodeDetect;

    private CameraSelector cameraSelector;
    private VouchedSession session;

    private Handler handler;
    private HandlerThread handlerThread;

    private boolean posted = false;
    private boolean onBarcodeStep = false;
    private boolean includeBarcode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            includeBarcode = (boolean) bundle.get("includeBarcode");
            if (includeBarcode) {
                barcodeDetect = new BarcodeDetect(this);
            }
        }

        session = new VouchedSession(BuildConfig.API_KEY, new VouchedSessionParameters.Builder().build());
        cardDetect = new CardDetect(getAssets(), new CardDetectOptions.Builder().withEnableDistanceCheck(false).build(), this);
        cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        setContentView(R.layout.activity_id_2);
        previewView = findViewById(R.id.preview_view);
        if (previewView == null) {
            System.out.println("previewView is null");
        }

        new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
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
    public synchronized void onResume() {
        super.onResume();
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        bindAllCameraUseCases();
    }

    @Override
    public synchronized void onPause() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        handlerThread = new HandlerThread("inference");
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        super.onPause();
    }

    @Override
    public void onCardDetectResult(CardDetectResult result) {
        switch (result.getStep()) {
            case PRE_DETECTED:
            case DETECTED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateText(result.getInstruction());
                    }
                });
                break;
            case POSTABLE:
                if (!posted) {
                    posted = true;
                    processResult(result);
                }
                break;
        }
    }

    @Override
    public void onBarcodeResult(BarcodeResult barcodeResult) {
        if (barcodeResult != null) {
            onPause();
            session.postBackId(this, barcodeResult, null, this);
            setFeedbackText("Processing");
        } else {
            setFeedbackText("Focus camera on barcode");
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
        builder.setTargetResolution(!onBarcodeStep ? DESIRED_PREVIEW_SIZE : DESIRED_PREVIEW_BARCODE_SIZE);
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
        builder.setTargetResolution(!onBarcodeStep ? DESIRED_PREVIEW_SIZE : DESIRED_PREVIEW_BARCODE_SIZE);
        analysisUseCase = builder.build();

        analysisUseCase.setAnalyzer(
                ContextCompat.getMainExecutor(this),
                imageProxy -> {
                    try {
                        if (onBarcodeStep) {
                            if (barcodeDetect != null)
                                barcodeDetect.findBarcode(imageProxy);
                        } else {
                            if (cardDetect != null)
                                cardDetect.processImageProxy(imageProxy, handler);
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to process image. Error: " + e.getLocalizedMessage());
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });

        cameraProvider.bindToLifecycle(this, cameraSelector, analysisUseCase);
    }

    protected void updateText(Instruction instruction) {
//        System.out.println(instruction);
        String s = "";

        switch (instruction) {
            case HOLD_STEADY:
                s = "Hold Steady";
                break;
            case MOVE_CLOSER:
                s = "Move Closer";
                break;
            case MOVE_AWAY:
                s = "Move Away";
                break;
            case ONLY_ONE:
                s = "Multiple IDs";
                break;
            case NO_CARD:
            default:
                s = "Show ID";
        }

        setFeedbackText(s);
    }


    protected String messageByInsight(Insight insight) {
        switch (insight) {
            case NON_GLARE:
                return "image has glare";
            case QUALITY:
                return "image is blurry";
            case BRIGHTNESS:
                return "image needs to be brighter";
            case FACE:
                return "image is missing required visual markers";
            case GLASSES:
                return "please take off your glasses";
            case UNKNOWN:
            default:
                return "Unknown Error";
        }
    }

    protected void setFeedbackText(@NonNull final String s) {
        TextView textView = (TextView) findViewById(R.id.textViewIdInstruction);
        textView.setTextSize(20);
        textView.setTextColor(Color.WHITE);
        textView.setText(s);
    }

    protected void processResult(CardDetectResult cardDetectResult) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onPause();
                setFeedbackText("Processing");

                String inputFirstName = null;
                String inputLastName = null;
                Intent i = getIntent();
                Bundle bundle = i.getExtras();

                if (bundle != null) {
                    inputFirstName = bundle.get("firstName") + "";
                    inputLastName = bundle.get("lastName") + "";
                }

                System.out.println("Before - Get Job Back from postFrontId");
                session.postFrontId(DetectorActivityV2.this, cardDetectResult, new Params.Builder().withFirstName(inputFirstName).withLastName(inputLastName), DetectorActivityV2.this);
            }
        });

    }

    @Override
    public void onJobResponse(JobResponse response) {
        Runnable resumeCamera = new Runnable() {
            public void run() {
                posted = false;
                onResume();
            }
        };

        // After session call, clear/clean CardDetect state
        cardDetect.reset();

        if (response.getError() != null) {
            System.out.println(response.getError().getMessage());
            setFeedbackText("An error occurred");
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(resumeCamera);
                }
            }, 2000);
            return;
        }

        Job job = response.getJob();
        System.out.println(job.toJson());

        List<Insight> insights = VouchedUtils.extractInsights(response.getJob());
        if (insights.size() != 0) {
            setFeedbackText(messageByInsight(insights.get(0)));

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(resumeCamera);
                }
            }, 5000);
        } else {
            if (includeBarcode && !onBarcodeStep) {
                onBarcodeStep = true;
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(resumeCamera);
                    }
                }, 5000);
            } else {
                onPause();
                Intent i = new Intent(DetectorActivityV2.this, FaceDetectorActivity.class);
                i.putExtra("Session", (Serializable) session);
                startActivity(i);
            }
        }

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
