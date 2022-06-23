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
        SwitchMaterial idConfirmationSwitch = findViewById(R.id.id_confirmation);

        Button button = (Button) findViewById(R.id.button3);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(NameInputActivity.this, DetectorActivityWithHelper.class);

                i.putExtra("firstName", firstNameInput.getText());
                i.putExtra("lastName", lastNameInput.getText());
                i.putExtra("includeBarcode", barcodeSwitch.isChecked());
                i.putExtra("idConfirmationEnabled", idConfirmationSwitch.isChecked());
                startActivity(i);
            }
        });
    }
}
