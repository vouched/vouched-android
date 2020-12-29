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
* ID Card and Passport Detection
* Face Detection (with liveness)
* ID Verification
* Name Verification

## How to use the Vouched Library

To use the library in your own project refer to the following code snippets:

**ID Card detection and submission**

```
import id.vouched.android.CardDetect;
import id.vouched.android.VouchedSession;

private CardDetect cardDetect = new CardDetect();
protected VouchedSession session = new VouchedSession();

cardDetect.processImage(detector, croppedBitmap, MINIMUM_CONFIDENCE_TF_OD_API, cropToFrameTransform, tracker, trackingOverlay, handler, processImageCallback);

Consumer<CardDetect.Step> processImageCallback = (step) -> {
    res = step;
    switch (res) {
        case preDetected:
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // prompt user to move camera closer
                }
            });
            break;
        case detected:
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // prompt user to hold camera steady
                }
            });
            break;
        case postable:
            // the card/passport is detected, is close enough, and is ready to submit
            if (!posted) {
                processingImage(getEncodedString(rgbFrameBitmap), null);
            }
            posted = true;
            break;
    }
};

protected void processingImage(String image, Camera oldCamera) {
        Consumer<Job> callback = response -> {
        ArrayList<Job.JobError> retryableErrors = extractRetryableErrors(response);
        if (retryableErrors.size() != 0) {
            if(oldCamera == null){
                camera.onResume();
                posted = false;
            } else{
                oldCamera.startPreview();
                posted = false;
            }
        } else {
            if(oldCamera == null){
                camera.onPause();
                button.setVisibility(View.VISIBLE);
            } else{
                oldCamera.stopPreview();
                button.setVisibility(View.VISIBLE);
            }
        }
    };
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
             try {
                 session.postFrontId(image, mQueue, callback);            
             } catch (Exception e) {
                                            
             }    
        }
        if(oldCamera == null){
            camera.onPause();
        }else{
            oldCamera.stopPreview();
        }
    });        
}   

```

**Face(Selfie) detection and submission**

```
import id.vouched.android.FaceDetect;
import id.vouched.android.VouchedSession;

private FaceDetect faceDetect = new FaceDetect();
faceDetect.faceDet();
public SurfaceView previewDisplayView = new SurfaceView();
faceDetect.setupPreviewDisplayView(previewDisplayView);

faceDetect.processImage(multiFaceLandmarks,handler,detectCallback,retake);

Consumer<FaceDetect.ReturningValues> detectCallback = (res) -> {
switch (res.step) {
    case preDetected:
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
		
            }
        });
        break;
    case detected:
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // prompt user to open mouth for liveness check
            }
        });
        break;
    case liveness:
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // prompt user to close mouth (liveness check complete) and hold steady
            }
        });
        break;
    case postable:
	// Selfie image captured and is ready to submit
        if (!posted) {
            posted = true;
            retake = false;
            processingImage(res.encodedImage);
        }
        break;
}
};

protected void processingImage(String image) {
	Consumer<Job> callback = response -> {
	    ArrayList<Job.JobError> retryableErrors = extractRetryableErrors(response);

	    if (retryableErrors.size() != 0) {
		    posted = false;
		    retake = true;
	    } else {
    		// Send response from session to Results Activity
    		Intent i = new Intent(FaceDetectorActivity.this, ResultsActivity.class);
    		Gson gson = new Gson();
    		String myJson = gson.toJson(response);
    		i.putExtra("Detect", myJson);
    		startActivity(i);
	    }
	};
	runOnUiThread(new Runnable() {
	    @Override
	    public void run() {
    		try {
    		    session.postFace(image, mQueue, callback);
    
    		} catch (Exception e) {
    		    e.printStackTrace();
    		}
	    }
	});

}
```

## Environment Variables
Add to *example/gradle.properties*
```
android.useAndroidX=true
API_URL="https://verify.vouched.id"
API_KEY="<API_PUBLIC_KEY>"
```
note: `"` is required for strings

## Assets and Library Files
Get assets and library files from your Vouched representative
- copy *libs/* directory to */example*
- copy *assets/* and *jniLibs/* directores to */example/src/main/*

## License

Vouched is available under the Apache License 2.0 license. See the LICENSE file for more info.



