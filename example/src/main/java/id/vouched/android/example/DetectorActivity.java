/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package id.vouched.android.example;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.Size;
import android.util.TypedValue;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import id.vouched.android.CardDetect;
import id.vouched.android.CardDetectOptions;
import id.vouched.android.CardDetectResult;
import id.vouched.android.Instruction;
import id.vouched.android.VouchedSession;
import id.vouched.android.VouchedUtils;
import id.vouched.android.customview.OverlayView;
import id.vouched.android.customview.OverlayView.DrawCallback;
import id.vouched.android.env.BorderedText;
import id.vouched.android.env.ImageUtils;
import id.vouched.android.model.JobResponse;
import id.vouched.android.model.Params;
import id.vouched.android.model.RetryableError;
import id.vouched.android.tracking.MultiBoxTracker;

public class DetectorActivity extends AppCompatActivity implements OnImageAvailableListener,
        Camera.PreviewCallback,
        CompoundButton.OnCheckedChangeListener,
        CardDetect.OnDetectResultListener,
        VouchedSession.OnJobResponseListener {

    private static final Size DESIRED_PREVIEW_SIZE = new Size(960, 720);
    private static final float TEXT_SIZE_DIP = 10;

    private Integer sensorOrientation;
    private Bitmap rgbFrameBitmap = null;

    private boolean computingDetection = false;
    private MultiBoxTracker tracker;
    private OverlayView trackingOverlay;

    private BorderedText borderedText;

    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;

    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;

    private CardDetect cardDetect;
    protected VouchedSession session;

    private CameraConnectionFragment camera;
    private boolean posted = false;
    private boolean waitingOnVouched = false;
    private Camera oldCamera;

    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.tfe_od_activity_camera);

        session = new VouchedSession(BuildConfig.API_KEY, BuildConfig.API_URL);
        cardDetect = new CardDetect(getAssets(), new CardDetectOptions.Builder().withEnableDistanceCheck(true).build(), this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }
    }

    protected void setFragment() {
        String cameraId = chooseCamera();
        Fragment fragment;
        if (useCamera2API) {
            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            new CameraConnectionFragment.ConnectionCallback() {
                                @Override
                                public void onPreviewSizeChosen(final Size size, final int rotation) {
                                    DetectorActivity.this.onPreviewSizeChosen(size, rotation);
                                }
                            },
                            this,
                            getLayoutId(),
                            getDesiredPreviewFrameSize());
            camera = camera2Fragment;
            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;
        } else {
            fragment =
                    new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
        }
        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    //  @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    //  @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }

    @Override
    public synchronized void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
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
    public synchronized void onStop() {
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                        DetectorActivity.this,
                        "Camera permission is required for this demo",
                        Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    // Returns true if the device supports the required hardware level, or better.
    private boolean isHardwareLevelSupported(
            CameraCharacteristics characteristics, int requiredLevel) {
        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel;
    }

    private String chooseCamera() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                // Fallback to camera1 API for internal cameras that don't have full support.
                // This should help with legacy situations where using the camera2 API causes
                // distorted or otherwise broken previews.
                useCamera2API =
                        (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                                || isHardwareLevelSupported(
                                characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
                return cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    //  @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);


        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
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


    protected void processImage() {
        cardDetect.processImage(rgbFrameBitmap, tracker, trackingOverlay, handler);

        // Run CardDetect without tracker box
        // cardDetect.processImage(rgbFrameBitmap, null, null, handler);
        computingDetection = false;
    }

    /**
     * Callback for Camera2 API
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onImageAvailable(final ImageReader reader) {
        if (waitingOnVouched) return;

        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            onImageAvailableHelper(reader);
            processImage();
        } catch (final Exception e) {
            e.printStackTrace();
            Trace.endSection();
            return;
        }
        Trace.endSection();
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

        TextView textView = (TextView) findViewById(R.id.textViewCardInstruction);
        textView.setTextSize(20);
        textView.setText(s);
    }

    protected void updateText(RetryableError retryableErrors) {
        String s = "";

        switch (retryableErrors) {
            case InvalidIdPhotoError:
                s = "Invalid Photo ID";
                break;
            case InvalidUserPhotoError:
                s = "Invalid Selfie";
                break;
            case GlareIdPhotoError:
                s = "ID has glare";
                break;
            case BlurryIdPhotoError:
                s = "Blurry ID";
                break;
        }

        TextView textView = (TextView) findViewById(R.id.textViewCardInstruction);
        textView.setTextSize(20);
        textView.setText(s);
    }

    protected void processResult(CardDetectResult cardDetectResult) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) findViewById(R.id.textViewCardInstruction);
                textView.setTextSize(20);
                textView.setText("Processing");

                String inputFirstName = null;
                String inputLastName = null;
                Intent i = getIntent();
                Bundle bundle = i.getExtras();

                if (bundle != null) {
                    inputFirstName = bundle.get("firstName") + "";
                    inputLastName = bundle.get("lastName") + "";
                }

                System.out.println("Before - Get Job Back from postFrontId");
                waitingOnVouched = true;
                session.postFrontId(DetectorActivity.this, cardDetectResult, new Params.Builder().withFirstName(inputFirstName).withLastName(inputLastName), DetectorActivity.this);
                if (oldCamera == null) {
                    camera.onPause();
                } else {
                    oldCamera.stopPreview();
                }
            }
        });

    }

    @Override
    public void onJobResponse(JobResponse response) {
        // After session call, clear/clean CardDetect state
        cardDetect.reset();

        Runnable resumeCamera = new Runnable() {
            public void run() {
                waitingOnVouched = false;
                posted = false;
                if (oldCamera == null) {
                    camera.onResume();
                } else {
                    oldCamera.startPreview();
                }
            }
        };

        if (response.getError() != null) {
            System.out.println(response.getError().getClass().getName() + ": " + response.getError().getMessage());
            TextView textView = (TextView) findViewById(R.id.textViewCardInstruction);
            textView.setTextSize(20);
            textView.setText("An error occurred");
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(resumeCamera);
                }
            }, 2000);
            return;
        }

        System.out.println("Callback");
        System.out.println(response);
        System.out.println(response.getJob().toJson());
        List<RetryableError> retryableErrors = VouchedUtils.extractRetryableIdErrors(response.getJob());

        if (retryableErrors.size() != 0) {
            System.out.println("Inside OnError - " + retryableErrors.size());
            for (RetryableError retryableError : retryableErrors) {
                System.out.println(retryableError);
            }
            updateText(retryableErrors.get(0));

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(resumeCamera);
                }
            }, 5000);
        } else {
            if (oldCamera == null) {
                camera.onPause();
                Intent i = new Intent(DetectorActivity.this, FaceDetectorActivity.class);
                i.putExtra("Session", (Serializable) session);
                startActivity(i);
            } else {
                oldCamera.stopPreview();
                Intent i = new Intent(DetectorActivity.this, FaceDetectorActivity.class);
                i.putExtra("Session", (Serializable) session);
                startActivity(i);
            }
        }
    }

    protected void onImageAvailableHelper(ImageReader reader) {
        final Image image = reader.acquireLatestImage();

        if (image == null) {
            return;
        }

        if (isProcessingFrame) {
            image.close();
            return;
        }
        isProcessingFrame = true;
        Trace.beginSection("imageAvailable");
        final Plane[] planes = image.getPlanes();
        fillBytes(planes, yuvBytes);
        yRowStride = planes[0].getRowStride();
        final int uvRowStride = planes[1].getRowStride();
        final int uvPixelStride = planes[1].getPixelStride();

        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420ToARGB8888(
                                yuvBytes[0],
                                yuvBytes[1],
                                yuvBytes[2],
                                previewWidth,
                                previewHeight,
                                yRowStride,
                                uvRowStride,
                                uvPixelStride,
                                rgbBytes);
                    }
                };

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        image.close();
                        isProcessingFrame = false;
                    }
                };

        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();
    }

    /**
     * Callback for android.hardware.Camera API
     */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame || waitingOnVouched) {
            return;
        }
        oldCamera = camera;

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }

        isProcessingFrame = true;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
                    }
                };

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        camera.addCallbackBuffer(bytes);
                        isProcessingFrame = false;
                    }
                };
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        processImage();
    }

}
