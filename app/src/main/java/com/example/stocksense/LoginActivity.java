package com.example.stocksense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    public static final String PREF_NAME = "StockSensePrefs";
    public static final String KEY_OWNER_ID = "owner_id";

    private TextView tvLoginTitle;
    private TextView tvLoginSubtitle;
    private EditText etUsername;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnSubmitAuth;
    private TextView tvToggleAuthMode;

    private DatabaseHelper dbHelper;
    private boolean isSignUpMode = false;

    // Initializes the login/sign-up screen and checks if an owner is already logged in.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (prefs.getLong(KEY_OWNER_ID, -1) != -1) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);
        dbHelper = new DatabaseHelper(this);

        tvLoginTitle = findViewById(R.id.tvLoginTitle);
        tvLoginSubtitle = findViewById(R.id.tvLoginSubtitle);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmitAuth = findViewById(R.id.btnSubmitAuth);
        tvToggleAuthMode = findViewById(R.id.tvToggleAuthMode);

        tvToggleAuthMode.setOnClickListener(v -> toggleAuthMode());
        btnSubmitAuth.setOnClickListener(v -> handleSubmit());
    }

    // Switches the form between Login mode and Sign-Up mode.
    private void toggleAuthMode() {
        isSignUpMode = !isSignUpMode;
        clearErrors();

        if (isSignUpMode) {
            tvLoginTitle.setText(R.string.title_signup);
            tvLoginSubtitle.setVisibility(View.GONE);
            etConfirmPassword.setVisibility(View.VISIBLE);
            btnSubmitAuth.setText(R.string.btn_signup);
            tvToggleAuthMode.setText(R.string.toggle_to_login);
        } else {
            tvLoginTitle.setText(R.string.title_login);
            tvLoginSubtitle.setVisibility(View.VISIBLE);
            etConfirmPassword.setVisibility(View.GONE);
            btnSubmitAuth.setText(R.string.btn_login);
            tvToggleAuthMode.setText(R.string.toggle_to_signup);
        }
    }

    // Validates inputs and logs in or registers the owner using DatabaseHelper.
    private void handleSubmit() {
        clearErrors();

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        boolean isValid = true;

        if (username.isEmpty()) {
            etUsername.setError(getString(R.string.err_field_required));
            isValid = false;
        }
        if (password.isEmpty()) {
            etPassword.setError(getString(R.string.err_field_required));
            isValid = false;
        }
        if (isSignUpMode) {
            if (confirmPassword.isEmpty()) {
                etConfirmPassword.setError(getString(R.string.err_field_required));
                isValid = false;
            } else if (!password.equals(confirmPassword)) {
                etConfirmPassword.setError(getString(R.string.err_password_mismatch));
                isValid = false;
            }
        }

        if (!isValid) return;

        if (isSignUpMode) {
            if (dbHelper.isUsernameTaken(username)) {
                etUsername.setError(getString(R.string.err_username_taken));
                return;
            }
            long newOwnerId = dbHelper.registerUser(username, password);
            if (newOwnerId != -1) {
                saveSession(newOwnerId);
                Toast.makeText(this, R.string.msg_signup_success, Toast.LENGTH_SHORT).show();
                goToMain();
            } else {
                etUsername.setError(getString(R.string.err_username_taken));
            }
        } else {
            long ownerId = dbHelper.loginUser(username, password);
            if (ownerId != -1) {
                saveSession(ownerId);
                goToMain();
            } else {
                etPassword.setError(getString(R.string.err_invalid_credentials));
            }
        }
    }

    // Saves the logged-in owner's ID into SharedPreferences.
    private void saveSession(long ownerId) {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit()
                .putLong(KEY_OWNER_ID, ownerId)
                .apply();
    }

    // Navigates to MainActivity and closes LoginActivity.
    private void goToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    // Clears any validation error messages on the input fields.
    private void clearErrors() {
        etUsername.setError(null);
        etPassword.setError(null);
        etConfirmPassword.setError(null);
    }
}