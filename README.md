# Vouched

[![GitHub release](https://img.shields.io/github/release/vouched/vouched-android.svg?maxAge=60)](https://github.com/vouched/vouched-android/releases)
[![License](https://img.shields.io/github/license/vouched/vouched-android)](https://github.com/vouched/vouched-android/blob/master/LICENSE)

#### Run the Example

1. Clone the repo
2. Setup the [environment variables](#environment-variables)
3. Setup the [assets and library files](#assets-and-library-files)
4. Run Example on a device with minSdkVersion 24

**1st Screen** - Name Input (Optional)
**2st Screen** - Card Detection
**3nd Screen** - Face Detection
**4th Screen** - ID Verification Results

#### Features displayed in Example

- ID Card and Passport Detection
- Face Detection (with liveness)
- ID Verification
- Name Verification

## How to use the Vouched Library

To use the library in your own project refer to the following code snippets:

**ID Card detection and submission**

```
import id.vouched.android.CardDetect;
import id.vouched.android.VouchedSession;

private CardDetect cardDetect = new CardDetect(getAssets(), new CardDetectOptions.Builder().withEnableDistanceCheck(true).build(), handleCardDetectResult());
protected VouchedSession session = new VouchedSession();

cardDetect.processImage(rgbFrameBitmap, tracker, trackingOverlay, handler);
// if don't need trackingOverlay, pass null for tracker and trackingOverlay
// cardDetect.processImage(rgbFrameBitmap, null, null, handler);

private Consumer<CardDetectResult> handleCardDetectResult() {
    return (result) -> {
        switch (result.getStep()) {
            case PRE_DETECTED:
            case DETECTED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // display instruction message
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
    };
}

protected void processResult(CardDetectResult cardDetectResult) {

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

    Consumer<JobResponse> callback = response -> {
        // After session call, clear/clean CardDetect state
        cardDetect.reset();

        if (response.getError() != null) {
            // display error message
            // resume camera
            return;
        }

        List<RetryableError> retryableErrors = VouchedUtils.extractRetryableIdErrors(response.getJob());

        if (retryableErrors.size() != 0) {
            // display error message
            // resume camera
        } else {
            if (oldCamera == null) {
                camera.onPause();
            } else {
                oldCamera.stopPreview();
            }
            Intent i = new Intent(DetectorActivity.this, FaceDetectorActivityV2.class);
            i.putExtra("Session", (Serializable) session);
            startActivity(i);
        }
    };

    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            // display processing message
            // create Params.Builder object w/ applicable data
            Params.Builder builder = new Params.Builder();
            session.postFrontId(DetectorActivity.this, cardDetectResult, builder, callback);
            if (oldCamera == null) {
                camera.onPause();
            } else {
                oldCamera.stopPreview();
            }
        }
    });

}

```

**Face(Selfie) detection and submission**
Face detect uses [CameraX API](https://developer.android.com/training/camerax)

```
import id.vouched.android.FaceDetect;
import id.vouched.android.VouchedSession;

private FaceDetect faceDetect = new FaceDetect(this, FaceDetectOptions.defaultOptions(), handleFaceDetectResult());

private Consumer<FaceDetectResult> handleFaceDetectResult() {
    return faceDetectResult -> {
        // display instruction message
        if (faceDetectResult.getStep() == Step.POSTABLE) {
            session.postFace(this, faceDetectResult, null, jobResponse -> {
                if (jobResponse.getError() != null) {
                    // display error message
                } else {
                    Job job = jobResponse.getJob();
                    List<RetryableError> retryableErrors = VouchedUtils.extractRetryableFaceErrors(job);
                    if (!retryableErrors.isEmpty()) {
                        // display error message
                        // resume camera and bindAnalysisUseCase
                    } else {
                        Intent i = new Intent(FaceDetectorActivityV2.this, ResultsActivity.class);
                        i.putExtra("Session", (Serializable) session);
                        startActivity(i);
                    }

                }
            });

            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
        }
    };
}

private void bindAnalysisUseCase() {
    ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
    analysisUseCase = builder.build();
    analysisUseCase.setAnalyzer(
        ContextCompat.getMainExecutor(this),
        imageProxy -> {
            // set image source info if necessary
            try {
                if (faceDetect != null) {
                    faceDetect.processImageProxy(imageProxy, graphicOverlay);
                }
            } catch (Exception e) {
                // show error message
            }
        });

    cameraProvider.bindToLifecycle(this, cameraSelector, analysisUseCase);
}
```

## Environment Variables

Add to _example/gradle.properties_

```
android.useAndroidX=true
API_URL="https://verify.vouched.id"
API_KEY="<API_PUBLIC_KEY>"
```

note: `"` is required for strings

## Assets and Library Files

Get assets and library files from your Vouched representative

- copy _libs/_ directory to _/example_
- copy _assets/_ and _jniLibs/_ directores to _/example/src/main/_

## License

Vouched is available under the Apache License 2.0 license. See the LICENSE file for more info.
