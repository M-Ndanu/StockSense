package com.example.stocksense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private View navStock;
    private View navSales;
    private ImageView ivNavStock;
    private ImageView ivNavSales;
    private TextView tvNavStock;
    private TextView tvNavSales;

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

        navStock = findViewById(R.id.navStock);
        navSales = findViewById(R.id.navSales);
        ivNavStock = findViewById(R.id.ivNavStock);
        ivNavSales = findViewById(R.id.ivNavSales);
        tvNavStock = findViewById(R.id.tvNavStock);
        tvNavSales = findViewById(R.id.tvNavSales);

        navStock.setOnClickListener(v -> showStockTab());
        navSales.setOnClickListener(v -> showSalesTab());

        // Load StockFragment by default only on first launch
        if (savedInstanceState == null) {
            showStockTab();
        } else {
            highlightTab(true);
        }
    }

    // Displays StockFragment and highlights the Stock tab.
    private void showStockTab() {
        loadFragment(new StockFragment());
        highlightTab(true);
    }

    // Displays SalesFragment and highlights the Sales tab.
    private void showSalesTab() {
        loadFragment(new SalesFragment());
        highlightTab(false);
    }

    // Shows the selected tab as an orange pill with its label; the other tab shows only its icon.
    private void highlightTab(boolean stockSelected) {
        navStock.setBackgroundResource(stockSelected ? R.drawable.bg_nav_selected : android.R.color.transparent);
        navSales.setBackgroundResource(stockSelected ? android.R.color.transparent : R.drawable.bg_nav_selected);
        tvNavStock.setVisibility(stockSelected ? View.VISIBLE : View.GONE);
        tvNavSales.setVisibility(stockSelected ? View.GONE : View.VISIBLE);
        ivNavStock.setColorFilter(ContextCompat.getColor(this,
                stockSelected ? R.color.stocksense_text : R.color.white));
        ivNavSales.setColorFilter(ContextCompat.getColor(this,
                stockSelected ? R.color.white : R.color.stocksense_text));
    }

    // Replaces the fragment container with the selected fragment.
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
