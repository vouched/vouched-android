package id.vouched.android.example;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import id.vouched.android.FaceDetect;
import id.vouched.android.OnFaceDetectListener;
import id.vouched.android.VouchedSession;
import id.vouched.android.VouchedUtils;
import id.vouched.android.model.Job;
import id.vouched.android.model.JobError;
import id.vouched.android.model.Params;
import id.vouched.android.model.RetryableError;


public class FaceDetectorActivity extends AppCompatActivity implements OnFaceDetectListener {
    private Handler handler;
    private HandlerThread handlerThread;
    private FaceDetect faceDetect;
    public SurfaceView previewDisplayView;
    private RequestQueue mQueue;
    public VouchedSession session;
    private boolean posted = false;
    private boolean retake = false;
    private boolean waitingOnVouched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face);
        mQueue = Volley.newRequestQueue(this);
        previewDisplayView = new SurfaceView(this);
        previewDisplayView.setVisibility(View.GONE);
        ViewGroup viewGroup = findViewById(R.id.preview_display_layout);
        viewGroup.addView(previewDisplayView);
        faceDetect = new FaceDetect(this);
        faceDetect.configure(this);
        faceDetect.setupPreviewDisplayView(previewDisplayView);
    }


    @Override
    protected void onResume() {
        super.onResume();
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        Intent i = getIntent();
        session = (VouchedSession) i.getSerializableExtra("Session");
        if (session != null) {
            System.out.println("BREAK");
            startCamera();
        }
    }

    @Override
    protected void onPause() {
        //handlerThread = new HandlerThread("inference");
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

    public void startCamera() {
        faceDetect.startCamera(previewDisplayView);
    }

    public void stopCamera() {
        faceDetect.stopCamera();
    }

    @Override
    public void onFaceDetected(List multiFaceLandmarks) {
        if (multiFaceLandmarks.isEmpty() || waitingOnVouched) {
            return;
        }
        System.out.println("Number of faces detected: " + multiFaceLandmarks.size() + "\n");
        try {
            faceDetect.processImage(multiFaceLandmarks, handler, retake, (res) -> {
                switch (res.step) {
                    case preDetected:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView textView = (TextView) findViewById(R.id.FaceDetInstructions);
                                textView.setVisibility(View.VISIBLE);
                                textView.setTextSize(20);
                                textView.setText("Hold Steady");
                            }
                        });
                        break;
                    case detected:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView textView = (TextView) findViewById(R.id.FaceDetInstructions);
                                textView.setVisibility(View.VISIBLE);
                                textView.setTextSize(20);
                                textView.setText("Open Mouth");
                            }
                        });
                        break;
                    case liveness:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("onImageAvailable - liveness");
                                TextView textView = (TextView) findViewById(R.id.FaceDetInstructions);
                                textView.setVisibility(View.VISIBLE);
                                textView.setTextSize(20);
                                textView.setText("Close Mouth and Hold Steady");
                            }
                        });
                        break;
                    case postable:
                        if (!posted) {
                            System.out.println("Postable");
                            posted = true;
                            retake = false;
                            Toast.makeText(this, "Capturing Selfie", Toast.LENGTH_LONG);
                            processingImage(res.encodedImage);
                        }
                        break;
                }
            });
        } catch (final Exception e) {
            e.printStackTrace();
            Trace.endSection();
        }
        Trace.endSection();
    }


    protected void processingImage(String image) {
        System.out.println("processingImage");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) findViewById(R.id.FaceDetInstructions);
                textView.setText("PROCESSING IMAGE");
                try {
                    waitingOnVouched = true;
                    session.postFace(FaceDetectorActivity.this, image, new Params.Builder(), (response) -> {
                        // After session call, clear/clean FaceDetect state
                        faceDetect.reset();

                        if (response.getError() != null) {
                            System.out.println(response.getError().getClass().getName() + ": " + response.getError().getMessage());
                            textView.setTextSize(20);
                            textView.setText("ERROR PROCESSING");

                            Timer timer = new Timer();
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            posted = false;
                                            retake = true;
                                            waitingOnVouched = false;
                                            startCamera();
                                        }
                                    });
                                }
                            }, 2000);

                            return;
                        }

                        System.out.println("Callback");
                        System.out.println(response);
                        List<RetryableError> retryableErrors = VouchedUtils.extractRetryableFaceErrors(response.getJob());

                        if (retryableErrors.size() != 0) {
                            System.out.println("Inside OnError - " + retryableErrors.size());
                            System.out.println("Inside OnError");
                            posted = false;
                            retake = true;
                        } else {
                            System.out.println(response);
                            Intent i = new Intent(FaceDetectorActivity.this, ResultsActivity.class);
                            i.putExtra("Session", (Serializable) session);
                            startActivity(i);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

}
