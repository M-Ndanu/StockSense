package com.example.stocksense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private Button btnNavStock;
    private Button btnNavSales;

    // Initializes the main dashboard, checks login session, and loads StockFragment by default.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE);
        if (prefs.getLong(LoginActivity.KEY_OWNER_ID, -1) == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        btnNavStock = findViewById(R.id.btnNavStock);
        btnNavSales = findViewById(R.id.btnNavSales);

        btnNavStock.setOnClickListener(v -> showStockTab());
        btnNavSales.setOnClickListener(v -> showSalesTab());

        // Load StockFragment by default only on first launch
        if (savedInstanceState == null) {
            showStockTab();
        }
    }

    // Displays StockFragment and highlights the Stock bottom tab.
    private void showStockTab() {
        loadFragment(new StockFragment());
        btnNavStock.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.stocksense_primary));
        btnNavSales.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.stocksense_dark));
    }

    // Displays SalesFragment and highlights the Sales bottom tab.
    private void showSalesTab() {
        loadFragment(new SalesFragment());
        btnNavSales.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.stocksense_primary));
        btnNavStock.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.stocksense_dark));
    }

    // Replaces the fragment container with the selected fragment.
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}