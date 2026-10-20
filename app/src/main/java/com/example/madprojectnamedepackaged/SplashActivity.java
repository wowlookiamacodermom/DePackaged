package com.example.madprojectnamedepackaged;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

/**
 * Shows the logo and tagline for 1.5 seconds, then proceeds to the Login / Setup screen.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500; // milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, UsernameActivity.class));
            finish(); // so Back does not return to the splash
        }, SPLASH_DELAY);
    }
}