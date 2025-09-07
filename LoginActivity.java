package com.example.iechat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    Button btn;
    EditText username;
    EditText pass;

    // Use a configurable server URL instead of hardcoded IP
    //private static final String SERVER_BASE_URL =
    // Or if you must use IP, at least make it configurable
     private static final String SERVER_BASE_URL = "http:// Enter url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btn = findViewById(R.id.signin);
        username = findViewById(R.id.username);
        pass = findViewById(R.id.password);
    }

    public void onClick(View view) {
        if (view.getId() == R.id.signin) {
            String usernameText = username.getText().toString().trim();
            String passwordText = pass.getText().toString().trim();

            if (usernameText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Run network request in a background thread
            new Thread(() -> loginUser(usernameText, passwordText)).start();
        }
    }

    private void loginUser(String username, String password) {
        try {
            URL url = new URL(SERVER_BASE_URL + "/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000); // 10 seconds timeout
            conn.setReadTimeout(10000); // 10 seconds timeout

            // JSON body
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("username", username);
            jsonBody.put("password", password);

            // Send JSON
            OutputStream os = conn.getOutputStream();
            os.write(jsonBody.toString().getBytes("UTF-8"));
            os.close();

            // Get response code
            int responseCode = conn.getResponseCode();

            // Read response (even for errors like 401)
            InputStream is = (responseCode >= 200 && responseCode < 400) ?
                    conn.getInputStream() : conn.getErrorStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            String response = sb.toString();
            JSONObject jsonResponse = new JSONObject(response);

            // Handle response on UI thread
            runOnUiThread(() -> {
                try {
                    String status = jsonResponse.getString("status");
                    if (status.equals("success")) {
                        // Save session
                        SessionManager session = new SessionManager(LoginActivity.this);
                        session.saveUser(username);

                        // Start key generation and upload in background
                        new Thread(() -> {
                            try {
                                String alias = "chat_key_" + username;

                                // Ensure we have a keypair (this will generate if needed)
                                CryptoBox.ensureKeyPair(alias);

                                // Get the public key in proper format
                                String pubKeyBase64 = CryptoBox.getPublicKeyBase64(alias);
                                Log.d("Public_key", "Public key for " + username + " : " + pubKeyBase64);

                                // Upload public key to server
                                URL keyUrl = new URL(SERVER_BASE_URL + "/keys/upload");
                                HttpURLConnection keyConn = (HttpURLConnection) keyUrl.openConnection();
                                keyConn.setRequestMethod("POST");
                                keyConn.setRequestProperty("Content-Type", "application/json");
                                keyConn.setDoOutput(true);
                                keyConn.setConnectTimeout(10000);
                                keyConn.setReadTimeout(10000);

                                JSONObject json = new JSONObject();
                                json.put("username", username);
                                json.put("pem", pubKeyBase64);

                                OutputStream os2 = keyConn.getOutputStream();
                                os2.write(json.toString().getBytes("UTF-8"));
                                os2.close();

                                // Check if upload was successful
                                int keyResponseCode = keyConn.getResponseCode();
                                if (keyResponseCode >= 200 && keyResponseCode < 300) {
                                    Log.d("KEY_UPLOAD", "Public key uploaded successfully");
                                } else {
                                    Log.e("KEY_UPLOAD", "Failed to upload public key, response code: " + keyResponseCode);
                                }

                                keyConn.disconnect();
                            } catch (Exception e) {
                                Log.e("KEY_UPLOAD", "Error uploading public key", e);
                            }
                        }).start();

                        // Redirect to MainActivity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(LoginActivity.this, "Unexpected error", Toast.LENGTH_SHORT).show();
                }
            });

            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Network error", Toast.LENGTH_SHORT).show());
        }
    }
}
