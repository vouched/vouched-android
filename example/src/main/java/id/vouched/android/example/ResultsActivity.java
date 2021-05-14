package id.vouched.android.example;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import id.vouched.android.VouchedSession;
import id.vouched.android.model.Job;
import id.vouched.android.model.JobResult;

public class ResultsActivity extends AppCompatActivity {
    String resultName = "";
    boolean resultSuccess = false;
    float resultId = 0f;
    float resultSelfie = 0f;
    float resultFaceMatch = 0f;
    float resultNameMatch = 0f;
    ArrayList<String> arr = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        Intent i = getIntent();
        VouchedSession session = (VouchedSession) i.getSerializableExtra("Session");
        if (session != null) {
            session.confirm(this, null, jobResponse -> {
                Job job = jobResponse.getJob();
                if (job != null) {
                    populateJob(job);
                } else {
                    System.out.println(jobResponse.getError().getMessage());
                }
            });
        }
    }

    protected void populateJob(Job job) {
        JobResult jobResult = job.getResult();
        resultSuccess = jobResult.getSuccess();

        if (jobResult.getFirstName() != null && jobResult.getLastName() != null)
            resultName = jobResult.getFirstName() + " " + jobResult.getLastName();

        resultId = jobResult.getConfidences().getId();
        resultSelfie = jobResult.getConfidences().getSelfie();
        resultFaceMatch = jobResult.getConfidences().getFaceMatch();
        resultNameMatch = jobResult.getConfidences().getNameMatch();
        populateArray();
    }

    protected void populateArray() {
        arr.add(resultId < 0.9 ? "false" : "true");
        arr.add(resultSelfie < 0.9 ? "false" : "true");
        arr.add(resultSuccess ? "true" : "false");
        arr.add(resultName);
        arr.add(resultFaceMatch < 0.9 ? "false" : "true");

        populateTable();
    }

    protected void populateTable() {
        TextView textView_1 = (TextView) findViewById(R.id.textView_1);
        ImageView imageView_1 = (ImageView) findViewById(R.id.imageView_1);
        String output_1 = "Valid ID - " + arr.get(0);
        textView_1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textView_1.setText(output_1);

        if (Boolean.parseBoolean(arr.get(0))) {
            imageView_1.setImageResource(R.drawable.check);
        } else {
            imageView_1.setImageResource(R.drawable.x);
        }

        TextView textView_2 = (TextView) findViewById(R.id.textView_2);
        ImageView imageView_2 = (ImageView) findViewById(R.id.imageView_2);
        String output_2 = "Valid Selfie - " + arr.get(1);
        textView_2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textView_2.setText(output_2);

        if (Boolean.parseBoolean(arr.get(1))) {
            imageView_2.setImageResource(R.drawable.check);
        } else {
            imageView_2.setImageResource(R.drawable.x);
        }

        TextView textView_3 = (TextView) findViewById(R.id.textView_3);
        ImageView imageView_3 = (ImageView) findViewById(R.id.imageView_3);
        String output_3 = "Valid Match - " + arr.get(2);
        textView_3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textView_3.setText(output_3);

        if (Boolean.parseBoolean(arr.get(2))) {
            imageView_3.setImageResource(R.drawable.check);
        } else {
            imageView_3.setImageResource(R.drawable.x);
        }

        TextView textView_4 = (TextView) findViewById(R.id.textView_4);
        ImageView imageView_4 = (ImageView) findViewById(R.id.imageView_4);
        String output_4 = "Name - " + arr.get(3);
        textView_4.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textView_4.setText(output_4);

        if (resultNameMatch >= 0.9) {
            imageView_4.setImageResource(R.drawable.check);
        } else {
            imageView_4.setImageResource(R.drawable.x);
        }

        TextView textView_5 = (TextView) findViewById(R.id.textView_5);
        ImageView imageView_5 = (ImageView) findViewById(R.id.imageView_5);
        String output_5 = "Face Match - " + arr.get(4);
        textView_5.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textView_5.setText(output_5);

        if (resultFaceMatch >= 0.9) {
            imageView_5.setImageResource(R.drawable.check);
        } else {
            imageView_5.setImageResource(R.drawable.x);
        }
    }

}