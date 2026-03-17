package com.luxpro.max;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends BaseActivity {
    private FirebaseAuth mAuth;
    private EditText regEmail, regPass, regConfirmPass;
    private Button btnDoRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        regEmail = findViewById(R.id.edRegEmail);
        regPass = findViewById(R.id.edRegPass);
        regConfirmPass = findViewById(R.id.edRegConfirmPass);
        btnDoRegister = findViewById(R.id.btnDoRegister);

        NeonTouchListener neonPulsar = new NeonTouchListener(this);
        btnDoRegister.setOnTouchListener(neonPulsar);

        findViewById(R.id.txtLoginLink).setOnClickListener(v -> {
            finish();
            applyGlitchTransition();
        });

        btnDoRegister.setOnClickListener(v -> {
            String email = regEmail.getText().toString().trim();
            String password = regPass.getText().toString().trim();
            String confirmPass = regConfirmPass.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields_error), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPass)) {
                Toast.makeText(this, getString(R.string.passwords_no_match), Toast.LENGTH_SHORT).show();
                return;
            }

            btnDoRegister.setEnabled(false); // Prevent double-tap / duplicate accounts

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            if (mAuth.getCurrentUser() == null) {
                                btnDoRegister.setEnabled(true);
                                return;
                            }
                            String userId = mAuth.getCurrentUser().getUid();
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("email", email);
                            userMap.put("active", false);
                            userMap.put("hwid", null); // null (not string "null") for proper HWID check
                            userMap.put("expiry_date", 0);

                            FirebaseDatabase.getInstance().getReference("Users")
                                    .child(userId).setValue(userMap)
                                    .addOnCompleteListener(dbTask -> {
                                        Toast.makeText(RegisterActivity.this, getString(R.string.reg_success_msg),
                                                Toast.LENGTH_SHORT).show();
                                        finish();
                                        applyGlitchTransition();
                                    })
                                    .addOnFailureListener(e -> btnDoRegister.setEnabled(true));
                        } else {
                            btnDoRegister.setEnabled(true);
                            Exception ex = task.getException();
                            String errMsg = ex != null ? ex.getMessage() : getString(R.string.error);
                            Toast.makeText(RegisterActivity.this,
                                    getString(R.string.reg_failed_error, errMsg), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}