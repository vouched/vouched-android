package id.vouched.android.example;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class NameInputActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name_input);
        TextInputEditText firstNameInput = (TextInputEditText) findViewById(R.id.textInputFirstName);
        TextInputEditText lastNameInput = (TextInputEditText) findViewById(R.id.textInputLastName);
        SwitchMaterial barcodeSwitch = (SwitchMaterial) findViewById(R.id.barcodeSwitch);

        Button button = (Button) findViewById(R.id.button3);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(NameInputActivity.this, DetectorActivityV2.class);

                i.putExtra("firstName", firstNameInput.getText());
                i.putExtra("lastName", lastNameInput.getText());
                i.putExtra("includeBarcode", barcodeSwitch.isChecked());
                startActivity(i);
            }
        });
    }
}
