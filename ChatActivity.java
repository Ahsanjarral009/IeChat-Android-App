package com.example.iechat;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    RecyclerView recyclerChat;
    EditText editMessage;
    ImageButton btnSend;
    ChatAdapter adapter;
    ArrayList<ChatMessage> chatList = new ArrayList<>();

    String currentUser;
    String chatWith;
    private String recipientPublicKeyBase64;

    Handler handler = new Handler();
    Runnable pollRunnable;
    private final int POLL_INTERVAL = 2000;

    // Use a configurable server URL
    //private static final String SERVER_BASE_URL = "http://your-server-domain.com:5000";
     private static final String SERVER_BASE_URL = "http://10.109.128.99:5000";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ImageView imgProfile = findViewById(R.id.imgProfile);
        TextView txtUserName = findViewById(R.id.txtUser);
        recyclerChat = findViewById(R.id.recChat);
        editMessage = findViewById(R.id.editMesage);
        btnSend = findViewById(R.id.btn_Send);

        currentUser = getIntent().getStringExtra("sender");
        chatWith = getIntent().getStringExtra("receiver");
        txtUserName.setText(chatWith);

        adapter = new ChatAdapter(chatList);
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(adapter);

        // Ensure keypair exists locally
        try {
            String alias = "chat_key_" + currentUser;
            CryptoBox.ensureKeyPair(alias);
            Log.d("CryptoBox", "Key pair ensured for: " + alias);
        } catch (Exception e) {
            Log.e("CryptoBox", "Failed to ensure key pair", e);
            Toast.makeText(this, "Error initializing encryption", Toast.LENGTH_SHORT).show();
        }

        fetchRecipientPublicKey();

        btnSend.setOnClickListener(v -> {
            String msg = editMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                if (recipientPublicKeyBase64 != null) {
                    sendMessageToServer(msg);
                    editMessage.setText("");
                } else {
                    Toast.makeText(this, "Waiting for recipient's public key...", Toast.LENGTH_SHORT).show();
                    fetchRecipientPublicKey(); // Retry fetching the key
                }
            }
        });
    }

    private void fetchRecipientPublicKey() {
        new Thread(() -> {
            int retries = 0;
            while (recipientPublicKeyBase64 == null && retries < 10) {
                try {
                    URL url = new URL(SERVER_BASE_URL + "/keys/" + chatWith);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();

                        JSONObject json = new JSONObject(sb.toString());
                        if (json.getString("status").equals("success")) {
                            recipientPublicKeyBase64 = json.getString("pem");

                            runOnUiThread(() ->
                                    Toast.makeText(ChatActivity.this,
                                            "Recipient's public key retrieved successfully!",
                                            Toast.LENGTH_SHORT).show()
                            );
                            break;
                        }
                    }

                    Thread.sleep(2000);
                    retries++;

                } catch (Exception e) {
                    Log.e("KeyFetch", "Error fetching public key", e);
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    retries++;
                }
            }

            if (recipientPublicKeyBase64 == null) {
                runOnUiThread(() ->
                        Toast.makeText(ChatActivity.this,
                                "Failed to get recipient's public key after multiple attempts",
                                Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Fetch messages immediately when resuming
        fetchMessagesFromServer();

        pollRunnable = new Runnable() {
            @Override
            public void run() {
                fetchMessagesFromServer();
                handler.postDelayed(this, POLL_INTERVAL);
            }
        };
        handler.postDelayed(pollRunnable, POLL_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(pollRunnable);
    }

    private void sendMessageToServer(String msg) {
        String clientTimestamp = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        ChatMessage newMessage = new ChatMessage(msg, true, clientTimestamp);

        // Add to UI immediately for better user experience
        runOnUiThread(() -> {
            chatList.add(newMessage);
            adapter.notifyItemInserted(chatList.size() - 1);
            recyclerChat.scrollToPosition(chatList.size() - 1);
        });

        new Thread(() -> {
            try {
                // Encrypt message
                CryptoBox.EncryptedMessage em = CryptoBox.encryptForRecipient(recipientPublicKeyBase64, msg);

                JSONObject json = new JSONObject();
                json.put("sender", currentUser);
                json.put("receiver", chatWith);
                json.put("ciphertext", em.ciphertext);
                json.put("nonce", em.nonce);
                json.put("ek", em.ek);
                Log.d("ENC_DEBUG", "ciphertext(len)=" + em.ciphertext.length() +
                        " nonce(len)=" + em.nonce.length() +
                        " ek(len)=" + em.ek.length());

                URL url = new URL(SERVER_BASE_URL + "/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    Log.d("SendMessage", "Message sent successfully");

                    // Read server response to get the server's timestamp
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();


                } else {
                    Log.e("SendMessage", "Failed to send message, response code: " + responseCode);
                    // Remove the message from local list if sending failed

                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e("SendMessage", "Error sending message", e);
                // Remove the message from local list if sending failed

            }
        }).start();
    }

    private void fetchMessagesFromServer() {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_BASE_URL + "/messages/" + currentUser + "/" + chatWith);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONArray jsonArray = new JSONArray(sb.toString());
                ArrayList<ChatMessage> newMessages = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject m = jsonArray.getJSONObject(i);
                    boolean isSent = m.getString("sender").equals(currentUser);

                    String text = "";
                    if (!isSent && m.has("ciphertext") && m.has("nonce") && m.has("ek")) {
                        try {
                            String alias = "chat_key_" + currentUser;

                            Log.d("DECRYPT_DEBUG", "Incoming message raw JSON: " + m.toString());
                            Log.d("DECRYPT_DEBUG", "alias=" + alias);
                            Log.d("DECRYPT_DEBUG", "ek(len)=" + m.getString("ek").length());
                            Log.d("DECRYPT_DEBUG", "nonce(len)=" + m.getString("nonce").length());
                            Log.d("DECRYPT_DEBUG", "ciphertext(len)=" + m.getString("ciphertext").length());

                            text = CryptoBox.decryptMessage(
                                    alias,
                                    m.getString("ek"),
                                    m.getString("nonce"),
                                    m.getString("ciphertext")
                            );

                            Log.d("DECRYPT_DEBUG", "Decrypted text=" + text);

                        } catch (Exception e) {
                            Log.e("DECRYPT_ERROR", "Failed to decrypt message", e);
                            text = "[Failed to decrypt message]";
                        }
                    } else if (!isSent && m.has("content")) {
                        // Received plaintext fallback
                        text = m.getString("content");
                    }

                    // Only create ChatMessage for received messages
                    if (!isSent && !text.isEmpty()) {
                        String timestamp = m.optString("timestamp",
                                new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()));

                        ChatMessage chatMessage = new ChatMessage(text, false, timestamp);

                        // Check if message already exists
                        boolean exists = false;
                        for (ChatMessage existing : chatList) {
                            if (existing.getMessage().equals(chatMessage.getMessage()) &&
                                    existing.isSent() == chatMessage.isSent() &&
                                    existing.getTime().equals(chatMessage.getTime())) {
                                exists = true;
                                break;
                            }
                        }

                        if (!exists) {
                            newMessages.add(chatMessage);
                        }
                    }
                }

                // Update UI if we have new messages
                if (!newMessages.isEmpty()) {
                    runOnUiThread(() -> {
                        chatList.addAll(newMessages);
                        adapter.notifyDataSetChanged();
                        recyclerChat.scrollToPosition(chatList.size() - 1);
                    });
                }

            } catch (Exception e) {
                Log.e("FETCH_ERROR", "Error fetching messages", e);
            }
        }).start();
    }

}