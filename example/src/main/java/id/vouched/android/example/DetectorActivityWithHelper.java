package id.vouched.android.example;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

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
import id.vouched.android.VouchedLogger;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_id_ex);
        previewView = findViewById(R.id.preview_view);

        session = new VouchedSession(BuildConfig.API_KEY, new VouchedSessionParameters.Builder().build());
        try {
            cameraHelper = new VouchedCameraHelper(this, this, ContextCompat.getMainExecutor(this), previewView, VouchedCameraHelper.Mode.ID, new VouchedCameraHelperOptions.Builder()
                    .withCardDetectOptions(new CardDetectOptions.Builder()
                            .withEnableDistanceCheck(false)
                            .withEnhanceInfoExtraction(true)
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
            setFeedbackText("Focus camera on back of ID");
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
            VouchedCameraHelper.Mode currentMode = cameraHelper.getCurrentMode();
            if(currentMode.equals(VouchedCameraHelper.Mode.ID)) {
                session.postFrontId(this, cardDetectResult, new Params.Builder().withFirstName(inputFirstName).withLastName(inputLastName), this);
            } else if(currentMode.equals(VouchedCameraHelper.Mode.ID_BACK)) {
                session.postBackId(this, cardDetectResult, null, this);
            }
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
            System.out.println("Error: " + response.getError().getMessage());
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
        VouchedLogger.getInstance().info(job.toJson());

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
            // determine if the job response requires other id processing
            // NOTE: This only processes the response if .withEnhanceInfoExtraction is set to true,
            // otherwise, it will always return a next state of COMPLETED
            cameraHelper.updateDetectionModes(job.getResult());
            // advance the mode to the next ID detection state.
            VouchedCameraHelper.Mode next = cameraHelper.getNextMode();
            if (!next.equals(VouchedCameraHelper.Mode.COMPLETED)) {
                try {
                    cameraHelper.switchMode(next);
                } catch (VouchedAssetsMissingException e) {
                    e.printStackTrace();
                }
                setFeedbackText("");
                showToastForMode(next);
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(resumeCamera);
                    }
                }, 2000);

            } else {
                onPause();
                Intent i = new Intent(DetectorActivityWithHelper.this, FaceDetectorActivityWithHelper.class);
                i.putExtra("Session", (Serializable) session);
                startActivity(i);
            }
        }
    }

    private void showToastForMode(VouchedCameraHelper.Mode next) {
        // in a real app you would craft a per mode dialog and message, so the user of
        // your app understands intent.
        if (next.equals(VouchedCameraHelper.Mode.ID_BACK) ||
        next.equals(VouchedCameraHelper.Mode.BARCODE)) {
            Toast.makeText(this, "Turn ID card over to backside and lay on surface", Toast.LENGTH_LONG).show();
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
            case ID_PHOTO:
                return "ID needs a valid photo";
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
