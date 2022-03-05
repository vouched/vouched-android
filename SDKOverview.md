### Overview of the Vouched SDK for Android

The Vouched SDK for Android assists in the process of verifying a user identity (referred to as IDV). Verification typically takes different pieces of information (for example, a photo ID and a selfie) as input, and then determines, through analysis of that information, if the user can be verified. (Note that the information extracted from the ID photo may include ID text, barcoded information and id photo images, depending on the ID used).

Verification flow is taken in steps, (front id -> rear id -> face detection -> confirmation) which are identified by the use of ```Stage``` enumeration in the SDK, which is passed as part of the data posted:
```
public enum Stage {
    id,
    face,
    confirm,
    backId
}
```

### VouchedSession

A session is responsible for the posting of information in each Stage to the Vouched API, and requires your account's public key to authenticate and identify itself to the API service. The session posts the information from each step of the verification flow until there is enough information to verify (confirm) the user's identity. 
**Note:** There should only be one session object used during the entire verification flow.

###  Job

When submitting verification information via a session, the Vouched service will analyze that information, and will send back a Job response, which contains the job id, results, errors and signals that have been generated during that Stage of the verification flow. 

More information on the Job response can be found in our [jobs documentation](https://docs.vouched.id/reference/findjobs)

#### JobResult

The JobResult object contains verification status, confidence ratings of the verification, user information, and details about the current Stage being examined. 

#### JobError

Any errors that occur while posting information to the API service are reported as errors in the Job. Current job errors can be found by expanding the 200 response section found in our [jobs documentation]( https://docs.vouched.id/reference/findjobs#:~:text=RESPONSES-,200,-Provide%20Results%20on. ), 

#### Confidence

Since the Vouched system is using machine learning and other artifical intelligence techniques, a probability is generated for the data items that are analyzed. These probabilites are reported as a confidence score, which can be found in the JobResult. You can create your own decisions comparing the scores to thresholds that are meaningful for you.

#### Insight

Insights categorize image analysis output signals for easier identification, which in many cases can provide a means of user guidance or insight with respect to image based issues:

```
public enum Insight {
    UNKNOWN,
    NON_GLARE,
    QUALITY,
    BRIGHTNESS,
    FACE,
    GLASSES
}
```
