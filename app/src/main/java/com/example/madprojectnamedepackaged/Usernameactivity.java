package com.example.madprojectnamedepackaged;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/** Asks for username and password to log in or create an account. */
public class UsernameActivity extends AppCompatActivity {

    /** Set to true when opened from the menu to change user/password. */
    public static final String EXTRA_CHANGE = "change";

    private boolean isLoginMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_username);

        TextView tvAsk = findViewById(R.id.tvAsk);
        EditText etUsername = findViewById(R.id.etUsername);
        EditText etPassword = findViewById(R.id.etPassword);
        TextView tvNote = findViewById(R.id.tvNote);
        Button btnContinue = findViewById(R.id.btnContinue);
        TextView tvSwitchMode = findViewById(R.id.tvSwitchMode);

        boolean isChange = getIntent().getBooleanExtra(EXTRA_CHANGE, false);
        boolean hasUser = UserStore.hasUser(this);

        if (hasUser && !isChange) {
            // User exists and opening app: Login Mode
            isLoginMode = true;
            tvAsk.setText("Welcome Back!");
            String savedName = UserStore.getName(this);
            etUsername.setText(savedName);
            etUsername.setEnabled(false);
            etPassword.setHint("Enter password");
            tvNote.setText("Enter your password to unlock Depackaged.");
            btnContinue.setText("Log In");
            tvSwitchMode.setText("Reset or switch account");
            tvSwitchMode.setVisibility(View.VISIBLE);
        } else if (isChange) {
            // Changing details from menu
            isLoginMode = false;
            tvAsk.setText("Update Account Details");
            String savedName = UserStore.getName(this);
            if (savedName != null) {
                etUsername.setText(savedName);
            }
            etPassword.setHint("Set new password");
            tvNote.setText("Update your username or password.");
            btnContinue.setText("Save Changes");
            tvSwitchMode.setVisibility(View.GONE);
        } else {
            // New user setup
            isLoginMode = false;
            tvAsk.setText("Create Your Account");
            tvNote.setText("Set a username and password to start using the app.");
            btnContinue.setText("Create Account");
            tvSwitchMode.setVisibility(View.GONE);
        }

        tvSwitchMode.setOnClickListener(v -> {
            // Toggle between login mode and register/edit mode
            isLoginMode = !isLoginMode;
            if (isLoginMode) {
                tvAsk.setText("Welcome Back!");
                etUsername.setEnabled(false);
                btnContinue.setText("Log In");
                tvSwitchMode.setText("Reset or switch account");
            } else {
                tvAsk.setText("Create New Account");
                etUsername.setEnabled(true);
                etUsername.setText("");
                etPassword.setText("");
                btnContinue.setText("Create Account");
                tvSwitchMode.setText("Already have an account? Log in");
            }
        });

        btnContinue.setOnClickListener(v -> {
            String name = etUsername.getText().toString().trim().replaceAll("\\s+", " ");
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty()) {
                etUsername.setError("Please enter a username");
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                etPassword.setError("Please enter a password");
                Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isLoginMode) {
                if (UserStore.checkPassword(this, password)) {
                    Toast.makeText(this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
                    openHome();
                } else {
                    etPassword.setError("Incorrect password");
                    Toast.makeText(this, "Incorrect password. Try again.", Toast.LENGTH_SHORT).show();
                }
            } else {
                UserStore.saveUser(this, name, password);
                Toast.makeText(this, "Account saved successfully!", Toast.LENGTH_SHORT).show();
                openHome();
            }
        });
    }

    private void openHome() {
        Intent home = new Intent(this, MainActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(home);
        finish();
    }
}
