package com.example.iechat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView userlist;
    ArrayList<ChatUser> usersList = new ArrayList<>();
    UserAdapter adapter;
    String currentUser; // Current logged-in username

    ImageView acc ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userlist = findViewById(R.id.list_view);

        // Get current logged-in user from SessionManager
        SessionManager session = new SessionManager(MainActivity.this);
        currentUser = session.getUser();

        // Setup custom adapter
        adapter = new UserAdapter(this, usersList);
        userlist.setAdapter(adapter);

        // Click on a user to open ChatActivity
        userlist.setOnItemClickListener((parent, view, position, id) -> {
            ChatUser receiver = usersList.get(position);
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("sender", currentUser);
            intent.putExtra("receiver", receiver.getUsername());
            startActivity(intent);
        });

        // Fetch users from server
        fetchUsers();


        acc = findViewById(R.id.account) ;

        acc.setOnClickListener(v -> {
            // Clear session

            session.clearUser(); // You need to implement this method in SessionManager

            // Redirect to LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

    }

    private void fetchUsers() {
        new Thread(() -> {
            try {
                URL url = new URL("http://10.109.128.99:5000/users/" + currentUser); // Flask endpoint
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONArray jsonArray = new JSONArray(sb.toString());
                ArrayList<String> fetchedUsers = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    fetchedUsers.add(jsonArray.getString(i));
                }

                // Update UI on main thread
                runOnUiThread(() -> {
                    usersList.clear();
                    for (String name : fetchedUsers) {
                        usersList.add(new ChatUser(name, "", R.drawable.account)); // Default last message empty
                    }
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to fetch users", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
