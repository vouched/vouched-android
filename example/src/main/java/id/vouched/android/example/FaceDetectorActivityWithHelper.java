package id.vouched.android.example;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import java.io.Serializable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import id.vouched.android.FaceDetect;
import id.vouched.android.FaceDetectOptions;
import id.vouched.android.FaceDetectResult;
import id.vouched.android.Instruction;
import id.vouched.android.Step;
import id.vouched.android.VouchedCameraHelper;
import id.vouched.android.VouchedCameraHelperOptions;
import id.vouched.android.VouchedSession;
import id.vouched.android.VouchedUtils;
import id.vouched.android.exception.VouchedCameraHelperException;
import id.vouched.android.liveness.LivenessMode;
import id.vouched.android.model.Insight;
import id.vouched.android.model.Job;
import id.vouched.android.model.JobResponse;

public class FaceDetectorActivityWithHelper extends AppCompatActivity implements FaceDetect.OnDetectResultListener, VouchedSession.OnJobResponseListener {

    private PreviewView previewView;

    private VouchedCameraHelper cameraHelper;
    private VouchedSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_face_2);
        previewView = findViewById(R.id.preview_view);

        cameraHelper = new VouchedCameraHelper(this, this, ContextCompat.getMainExecutor(this), previewView, VouchedCameraHelper.Mode.FACE, new VouchedCameraHelperOptions.Builder()
                .withFaceDetectOptions(new FaceDetectOptions.Builder()
                        .withLivenessMode(LivenessMode.MOUTH_MOVEMENT)
                        .build())
                .withFaceDetectResultListener(this)
                .build());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (session == null) {
            Intent i = getIntent();
            session = (VouchedSession) i.getSerializableExtra("Session");
        }
        try {
            cameraHelper.onResume();
        } catch (VouchedCameraHelperException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onPause() {
        cameraHelper.onPause();
        super.onPause();
    }


    @Override
    public void onFaceDetectResult(FaceDetectResult faceDetectResult) {
        setFeedbackText(getFeedbackLabel(faceDetectResult.getInstruction()));
        if (Step.POSTABLE.equals(faceDetectResult.getStep())) {
            setFeedbackText("Please wait. Processing image.");
            onPause();
            session.postFace(this, faceDetectResult, null, this);
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
        } else {
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
                Intent i = new Intent(FaceDetectorActivityWithHelper.this, ResultsActivity.class);
                i.putExtra("Session", (Serializable) session);
                startActivity(i);
            }

        }
    }

    protected void setFeedbackText(@NonNull final String s) {
        TextView textView = (TextView) findViewById(R.id.textViewFaceInstruction);
        textView.setTextSize(20);
        textView.setTextColor(Color.WHITE);
        textView.setText(s);
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
}
