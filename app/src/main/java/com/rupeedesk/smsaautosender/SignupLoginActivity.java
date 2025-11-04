package com.rupeedesk.smsaautosender;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class SignupLoginActivity extends AppCompatActivity {

    private EditText phoneInput, pinInput;
    private Button signupLoginButton;
    private FirebaseFirestore db;
    private String deviceId;
    private static final String TAG = "SignupLoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_login);

        db = FirebaseFirestore.getInstance();
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        phoneInput = findViewById(R.id.phoneInput);
        pinInput = findViewById(R.id.pinInput);
        signupLoginButton = findViewById(R.id.signupLoginButton);

        // Check if user already logged in
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String savedUserId = prefs.getString("userId", null);
        if (savedUserId != null) {
            openHomePage();
            return;
        }

        signupLoginButton.setOnClickListener(v -> {
            String phone = phoneInput.getText().toString().trim();
            String pin = pinInput.getText().toString().trim();

            if (!isValidPhone(phone) || !isValidPin(pin)) {
                Toast.makeText(this, "Invalid phone or PIN", Toast.LENGTH_SHORT).show();
                return;
            }

            authenticateUser(deviceId, phone, pin);
        });
    }

    private boolean isValidPhone(String phone) {
        return phone.length() >= 10 && phone.matches("\\d+");
    }

    private boolean isValidPin(String pin) {
        return pin.length() == 6 && pin.matches("\\d{6}");
    }

    private void authenticateUser(String deviceId, String phone, String pin) {
        db.collection("users")
          .whereEqualTo("deviceId", deviceId)
          .get()
          .addOnCompleteListener(task -> {
              if (task.isSuccessful()) {
                  if (!task.getResult().isEmpty()) {
                      // User exists, verify PIN
                      for (QueryDocumentSnapshot doc : task.getResult()) {
                          String storedPin = doc.getString("pin");
                          if (storedPin != null && storedPin.equals(pin)) {
                              saveUserLocally(doc.getId());
                              Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                              openHomePage();
                              return;
                          } else {
                              Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show();
                              return;
                          }
                      }
                  } else {
                      // New user, register
                      registerUser(deviceId, phone, pin);
                  }
              } else {
                  Log.e(TAG, "Error fetching user", task.getException());
                  Toast.makeText(this, "Error during authentication", Toast.LENGTH_SHORT).show();
              }
          });
    }

    private void registerUser(String deviceId, String phone, String pin) {
        Map<String, Object> user = new HashMap<>();
        user.put("deviceId", deviceId);
        user.put("phone", phone);
        user.put("pin", pin);  // For production, store hashed PIN securely
        user.put("balance", 0.0);
        user.put("bankDetails", null);

        db.collection("users")
          .add(user)
          .addOnSuccessListener(documentReference -> {
              saveUserLocally(documentReference.getId());
              Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show();
              openHomePage();
          })
          .addOnFailureListener(e -> {
              Log.e(TAG, "Signup failed", e);
              Toast.makeText(this, "Signup failed", Toast.LENGTH_SHORT).show();
          });
    }

    private void saveUserLocally(String userId) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putString("userId", userId).apply();
    }

    private void openHomePage() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}