package com.example.stocksense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SaleSummaryActivity extends AppCompatActivity {

    private String reportText = "";

    // Called when the screen is created. Loads one sale and shows its items and total.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("lifecycle", "SaleSummaryActivity onCreate invoked");
        setContentView(R.layout.activity_sale_summary);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvSaleInfo = findViewById(R.id.tvSaleInfo);
        ListView lvSummaryItems = findViewById(R.id.lvSummaryItems);
        TextView tvSummaryTotal = findViewById(R.id.tvSummaryTotal);
        Button btnShareReport = findViewById(R.id.btnShareReport);

        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE);
        long ownerId = prefs.getLong(LoginActivity.KEY_OWNER_ID, -1);
        long saleId = getIntent().getLongExtra(Constants.EXTRA_SALE_ID, -1);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        Sale sale = dbHelper.getSale(ownerId, saleId);
        if (sale == null) {
            Toast.makeText(this, R.string.sales_err_not_found, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date(sale.getSaleTime()));
        tvSaleInfo.setText(getString(R.string.sales_summary_header, sale.getId(), date));

        List<String> lines = new ArrayList<>();
        for (SaleItem item : sale.getItems()) {
            lines.add(getString(R.string.sales_item_line,
                    item.getProductName(), item.getQuantity(), item.getSubtotal()));
        }
        lvSummaryItems.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lines));
        tvSummaryTotal.setText(getString(R.string.sales_total, sale.getTotal()));

        reportText = buildReport(dbHelper, ownerId, sale, date, lines);
        btnShareReport.setOnClickListener(v -> shareReport());
    }

    // Builds the plain text report: owner, date, number of sales, products, total, low stock.
    private String buildReport(DatabaseHelper dbHelper, long ownerId, Sale sale,
                               String date, List<String> lines) {
        User owner = dbHelper.getUser(ownerId);
        String ownerName = owner == null ? "" : owner.getUsername();

        StringBuilder report = new StringBuilder();
        report.append(getString(R.string.sales_report_title)).append("\n");
        report.append(getString(R.string.sales_report_owner, ownerName)).append("\n");
        report.append(getString(R.string.sales_report_date, date)).append("\n");
        report.append(getString(R.string.sales_report_count, 1)).append("\n");
        report.append(getString(R.string.sales_report_items)).append("\n");
        for (String line : lines) {
            report.append(line).append("\n");
        }
        report.append(getString(R.string.sales_report_total, sale.getTotal())).append("\n");

        StringBuilder lowNames = new StringBuilder();
        for (Product p : dbHelper.getLowStockProducts(ownerId)) {
            if (lowNames.length() > 0) {
                lowNames.append(", ");
            }
            lowNames.append(p.getName());
        }
        if (lowNames.length() == 0) {
            report.append(getString(R.string.sales_report_low_none));
        } else {
            report.append(getString(R.string.sales_report_low_stock, lowNames.toString()));
        }
        return report.toString();
    }

    // Opens the phone's share sheet with the report as plain text (implicit intent).
    private void shareReport() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, reportText);
        startActivity(Intent.createChooser(intent, getString(R.string.sales_share_title)));
    }

    // Lifecycle logging: writes a line to Logcat each time this screen changes state.
    @Override
    protected void onStart() {
        super.onStart();
        Log.d("lifecycle", "SaleSummaryActivity onStart invoked");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("lifecycle", "SaleSummaryActivity onResume invoked");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("lifecycle", "SaleSummaryActivity onPause invoked");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("lifecycle", "SaleSummaryActivity onStop invoked");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("lifecycle", "SaleSummaryActivity onRestart invoked");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("lifecycle", "SaleSummaryActivity onDestroy invoked");
    }
}
