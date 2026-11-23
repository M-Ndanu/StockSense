package com.example.stocksense;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalesFragment extends Fragment {

    public static final String EXTRA_SALE_ID = "extra_sale_id";

    private TextView tvEmptySales;
    private ListView lvSales;

    private DatabaseHelper dbHelper;
    private long ownerId;
    private final List<Sale> salesList = new ArrayList<>();
    private SalesAdapter adapter;

    // Inflates the Sales fragment layout and sets up the past sales ListView.
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        SharedPreferences prefs = requireContext().getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        ownerId = prefs.getLong(LoginActivity.KEY_OWNER_ID, -1);

        tvEmptySales = view.findViewById(R.id.tvEmptySales);
        lvSales = view.findViewById(R.id.lvSales);

        adapter = new SalesAdapter();
        lvSales.setAdapter(adapter);

        lvSales.setOnItemClickListener((parent, itemView, position, id) -> {
            Sale selectedSale = salesList.get(position);
            Intent intent = new Intent(requireContext(), SaleSummaryActivity.class);
            intent.putExtra(EXTRA_SALE_ID, selectedSale.getId());
            startActivity(intent);
        });

        loadSales();
        return view;
    }

    // Reloads past sales whenever returning to this fragment.
    @Override
    public void onResume() {
        super.onResume();
        loadSales();
    }

    // Loads all past sales for the logged-in owner from SQLite, newest first.
    private void loadSales() {
        salesList.clear();
        salesList.addAll(dbHelper.getSales(ownerId));

        if (salesList.isEmpty()) {
            tvEmptySales.setVisibility(View.VISIBLE);
            lvSales.setVisibility(View.GONE);
        } else {
            tvEmptySales.setVisibility(View.GONE);
            lvSales.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    // Custom adapter binding Sale objects to item_sale.xml.
    private class SalesAdapter extends BaseAdapter {

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

        // Returns the number of past sales.
        @Override
        public int getCount() {
            return salesList.size();
        }

        // Returns the Sale at the given list position.
        @Override
        public Object getItem(int position) {
            return salesList.get(position);
        }

        // Returns the database ID of the Sale at the given position.
        @Override
        public long getItemId(int position) {
            return salesList.get(position).getId();
        }

        // Populates each sale row with its sale ID, formatted date, and total amount.
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext()).inflate(R.layout.item_sale, parent, false);
            }

            Sale sale = salesList.get(position);

            TextView tvSaleId = convertView.findViewById(R.id.tvSaleId);
            TextView tvSaleDate = convertView.findViewById(R.id.tvSaleDate);
            TextView tvSaleTotal = convertView.findViewById(R.id.tvSaleTotal);

            tvSaleId.setText(getString(R.string.label_sale_prefix) + sale.getId());
            tvSaleDate.setText(dateFormat.format(new Date(sale.getSaleTime())));
            tvSaleTotal.setText(getString(R.string.label_currency_prefix) + sale.getTotal());

            return convertView;
        }
    }
}