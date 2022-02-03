package id.vouched.android.example;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import id.vouched.android.Step;
import id.vouched.android.VouchedCameraHelper;
import id.vouched.android.VouchedCameraHelperOptions;
import id.vouched.android.VouchedSession;
import id.vouched.android.VouchedSessionParameters;
import id.vouched.android.VouchedUtils;
import id.vouched.android.exception.VouchedAssetsMissingException;
import id.vouched.android.exception.VouchedCameraHelperException;
import id.vouched.android.model.Insight;
import id.vouched.android.model.Job;
import id.vouched.android.model.JobResponse;
import id.vouched.android.model.Params;

public class DetectorActivityWithHelper extends AppCompatActivity implements CardDetect.OnDetectResultListener, BarcodeDetect.OnBarcodeResultListener, VouchedSession.OnJobResponseListener {

    private static final int PERMISSION_REQUESTS = 1;

    private PreviewView previewView;

    private VouchedCameraHelper cameraHelper;
    private VouchedSession session;

    private boolean onBarcodeStep = false;
    private boolean includeBarcode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            includeBarcode = (boolean) bundle.get("includeBarcode");
        }

        setContentView(R.layout.activity_id_ex);
        previewView = findViewById(R.id.preview_view);

        session = new VouchedSession(BuildConfig.API_KEY, new VouchedSessionParameters.Builder().build());
        try {
            cameraHelper = new VouchedCameraHelper(this, this, ContextCompat.getMainExecutor(this), previewView, VouchedCameraHelper.Mode.ID, new VouchedCameraHelperOptions.Builder()
                    .withCardDetectOptions(new CardDetectOptions.Builder()
                            .withEnableDistanceCheck(false)
                            .withEnhanceInfoExtraction(false)
                            .build())
                    .withCardDetectResultListener(this)
                    .withBarcodeDetectResultListener(this)
                    .withCameraFlashDisabled(true)
                    .build());
        } catch (VouchedAssetsMissingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        } else {
            try {
                cameraHelper.onResume();
            } catch (VouchedCameraHelperException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        cameraHelper.onPause();
        super.onPause();
    }

    @Override
    public void onBarcodeResult(BarcodeResult barcodeResult) {
        if (barcodeResult != null) {
            onPause();
            session.postBackId(this, barcodeResult, null, this);
            setFeedbackText("Please wait. Processing image.");
        } else {
            setFeedbackText("Focus camera on barcode");
        }
    }

    @Override
    public void onCardDetectResult(CardDetectResult cardDetectResult) {
        updateText(cardDetectResult.getInstruction());
        if (Step.POSTABLE.equals(cardDetectResult.getStep())) {
            setFeedbackText("Please wait. Processing image.");
            onPause();

            String inputFirstName = null;
            String inputLastName = null;
            Intent i = getIntent();
            Bundle bundle = i.getExtras();

            if (bundle != null) {
                inputFirstName = bundle.get("firstName") + "";
                inputLastName = bundle.get("lastName") + "";
            }

            session.postFrontId(this, cardDetectResult, new Params.Builder().withFirstName(inputFirstName).withLastName(inputLastName), this);
        }
    }

    @Override
    public void onJobResponse(JobResponse response) {
        Runnable resumeCamera = new Runnable() {
            public void run() {
                onResume();
            }
        };

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
                try {
                    cameraHelper.switchMode(VouchedCameraHelper.Mode.BARCODE);
                } catch (VouchedAssetsMissingException e) {
                    e.printStackTrace();
                }
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(resumeCamera);
                    }
                }, 5000);
            } else {
                onPause();
                Intent i = new Intent(DetectorActivityWithHelper.this, FaceDetectorActivityWithHelper.class);
                i.putExtra("Session", (Serializable) session);
                startActivity(i);
            }
        }
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

    // -- Permission helpers
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

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            System.out.println("Permission granted: " + permission);
            return true;
        }
        System.out.println("Permission NOT granted: " + permission);
        return false;
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
        if (allPermissionsGranted()) {
            onResume();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
