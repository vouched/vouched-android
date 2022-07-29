


### Overview of the Vouched SDK for Android

The Vouched SDK for Android assists in the process of verifying a user identity (referred to as IDV). Verification typically takes different pieces of information (for example, a photo ID and a selfie) as input, and then determines, through analysis of that information, if the user's identity can be verified from that information. 

Verification information flow is performed in *stages*, (front id -> rear id -> face detection -> confirmation) which are identified by the use of ```Stage``` enumeration in the SDK, which is passed as part of the data posted to the Vouched API service:
```
public enum Stage {
    id,
    face,
    confirm,
    backId
}
```

### VouchedSession

A session is responsible for the posting of information in each Stage to the Vouched API, and requires your account's public key to authenticate and identify itself to the API service. The session posts the information from each step of the verification flow until there is enough information to verify (confirm) the user's identity. Typical information posted includes images, additional job input parameters (user info such as first/last name, etc.), and in some cases data extracted from barcodes..

**Note:** There should only be one session object used during the entire verification flow.

###  Job

When submitting verification information via a session, the Vouched service will analyze that information, and will send back a Job response, which contains the job id, results, errors and signals that have been generated during that Stage of the verification flow. 

More information on the Job request and response parameters can be found in our [jobs documentation](https://docs.vouched.id/reference/findjobs)

#### JobResult

The JobResult object contains verification status, confidence ratings of the verification, user information, and details about the current Stage being examined. 

#### JobError

Any errors that occur while posting information to the API service are reported as errors in the Job. Current job errors can be found by expanding the 200 response section found in our [jobs documentation]( https://docs.vouched.id/reference/findjobs#:~:text=RESPONSES-,200,-Provide%20Results%20on. ), 

#### Confidence

Since the Vouched system is using machine learning and other artifical intelligence techniques, a probability is generated for the data items that are analyzed. These probabilites are reported as a confidence score, which can be found in the JobResult. You can create your own decisions comparing the scores to thresholds that are meaningful for you.

#### Insight

Insights categorize image analysis output signals for easier identification, which in many cases can provide a means of user guidance or insight with respect to image based issues. Insights are typically extracted from a Job though the use of ```VouchedUtils.extractInsights(Job job)```

```
public enum Insight {
    UNKNOWN,
    NON_GLARE,
    QUALITY,
    BRIGHTNESS,
    FACE,
    GLASSES,
    ID_PHOTO
}
```










