package com.example.feature_login.presentation;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.feature_login.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {
    ChipGroup chip_group_role;
    Chip chip_role_attendee;
    Chip chip_role_organizer;
    TextInputLayout full_name;
    TextInputLayout email;
    TextInputLayout phone_number;
    TextInputLayout password;
    Button btn_submit_register;
    String selectedRole = "";
    String fullNameStr;
    String emailStr;
    String phoneNumberStr;
    String passwordStr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        initViews();
        setChip_group_role();
        setBtn_submit_register();
    }

    private void initViews() { // TRÙNG ID VỚI LOGIN
        chip_group_role = findViewById(R.id.chip_group_role);
        chip_role_attendee = findViewById(R.id.chip_role_attendee);
        chip_role_organizer = findViewById(R.id.chip_role_organizer);
        full_name = findViewById(R.id.input_full_name);
        email = findViewById(R.id.input_email);
        phone_number = findViewById(R.id.input_phone_number);
        password = findViewById(R.id.input_password);
        btn_submit_register = findViewById(R.id.btn_submit_register);
    }

    private void setChip_group_role() {
        chip_group_role.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_role_attendee) {
                selectedRole = "attendee";
            } else if (checkedId == R.id.chip_role_organizer) {
                selectedRole = "organizer";
            }
        });
    }

    private void setBtn_submit_register() {
        btn_submit_register.setOnClickListener(view -> {
            fullNameStr = full_name.getEditText().getText().toString().trim();
            emailStr = email.getEditText().getText().toString().trim();
            phoneNumberStr = phone_number.getEditText().getText().toString().trim();
            passwordStr = password.getEditText().getText().toString().trim();

            if (fullNameStr.isEmpty() || emailStr.isEmpty() || phoneNumberStr.isEmpty() || passwordStr.isEmpty() || selectedRole.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                // TODO: Implement SignupUseCase logic here
                Toast.makeText(this, "Đăng ký thành công (placeholder)", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
