package com.bingo.smsapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegistrationActivity extends AppCompatActivity {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("sms_relay", MODE_PRIVATE);
        if (prefs.getBoolean("registered", false)) {
            finish();
            return;
        }

        setContentView(R.layout.activity_registration);

        EditText deviceIdField = findViewById(R.id.deviceIdInput);
        EditText phoneField = findViewById(R.id.phoneInput);
        EditText codeField = findViewById(R.id.codeInput);
        Button registerButton = findViewById(R.id.registerButton);

        deviceIdField.setText(android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID));

        registerButton.setOnClickListener(v -> {
            String deviceId = deviceIdField.getText().toString().trim();
            String phoneNumber = phoneField.getText().toString().trim();
            String code = codeField.getText().toString().trim();

            if (deviceId.isEmpty() || phoneNumber.isEmpty() || code.isEmpty()) {
                Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            registerWithBackend(deviceId, phoneNumber, code);
        });
    }

    private void registerWithBackend(String deviceId, String phoneNumber, String code) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("deviceId", deviceId);
                payload.put("adminPhoneNumber", phoneNumber);
                payload.put("registrationCode", code);
                payload.put("appVersion", "1.0");

                Request request = new Request.Builder()
                        .url("http://192.168.1.15:8080/api/v1/sms-relay/register")
                        .header("Authorization", "Bearer demo-admin-token")
                        .post(RequestBody.create(payload.toString(), JSON))
                        .build();

                OkHttpClient client = new OkHttpClient();
                try (Response response = client.newCall(request).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            SharedPreferences prefs = getSharedPreferences("sms_relay", MODE_PRIVATE);
                            prefs.edit().putBoolean("registered", true).apply();
                            Toast.makeText(this, "Registration successful", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Registration failed: " + body, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
