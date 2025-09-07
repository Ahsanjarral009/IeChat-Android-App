package com.example.iechat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;



@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500; // 1.5 seconds
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Use SplashTheme so window background shows immediately
        setTheme(R.style.SplashTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        session = new SessionManager(this);

        // Optional logo animation
        ImageView logo = findViewById(R.id.imgLogo);
        TextView appName = findViewById(R.id.tvFooter);
        try {
            Animation anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            anim.setDuration(700);
            logo.startAnimation(anim);
            appName.startAnimation(anim);
        } catch (Exception ignored) {}

        // Delay then check login state and move forward
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String loggedUser = session.getUser(); // returns username or null
            Intent intent;
            if (loggedUser != null) {
                // already logged in -> go to HomeActivity
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                // not logged in -> go to LoginActivity
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            // Clear splash from back stack
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MS);
    }
}
