package com.example.stocksense;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class LowStockReceiver extends BroadcastReceiver {

    // Runs when the low-stock broadcast arrives. Shows a Toast with the product names.
    @Override
    public void onReceive(Context context, Intent intent) {
        String names = intent.getStringExtra(Constants.EXTRA_LOW_STOCK_NAMES);
        Toast.makeText(context,
                context.getString(R.string.sales_low_stock_alert, names),
                Toast.LENGTH_LONG).show();
    }
}
