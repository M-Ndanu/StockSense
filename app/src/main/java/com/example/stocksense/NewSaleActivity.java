package com.example.stocksense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class NewSaleActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private long ownerId;
    private List<Product> products;
    private final List<SaleItem> cart = new ArrayList<>();
    private final List<String> cartLines = new ArrayList<>();
    private ArrayAdapter<String> cartAdapter;
    private Spinner spProducts;
    private EditText etQuantity;
    private TextView tvTotal;

    // Called when the screen is created. Loads the owner's products and sets up the buttons.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("lifecycle", "NewSaleActivity onCreate invoked");
        setContentView(R.layout.activity_new_sale);

        spProducts = findViewById(R.id.spProducts);
        etQuantity = findViewById(R.id.etQuantity);
        tvTotal = findViewById(R.id.tvTotal);
        Button btnAddItem = findViewById(R.id.btnAddItem);
        Button btnRecordSale = findViewById(R.id.btnRecordSale);
        ListView lvCart = findViewById(R.id.lvCart);

        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE);
        ownerId = prefs.getLong(LoginActivity.KEY_OWNER_ID, -1);
        dbHelper = new DatabaseHelper(this);
        products = dbHelper.getProducts(ownerId);

        if (products.isEmpty()) {
            Toast.makeText(this, R.string.sales_no_products, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        List<String> names = new ArrayList<>();
        for (Product p : products) {
            names.add(p.getName());
        }
        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProducts.setAdapter(spinnerAdapter);

        cartAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cartLines);
        lvCart.setAdapter(cartAdapter);
        refreshCart();

        btnAddItem.setOnClickListener(v -> addItemToCart());
        btnRecordSale.setOnClickListener(v -> recordSale());
    }

    // Adds the chosen product and quantity to the cart. Blocks quantities above the stock.
    private void addItemToCart() {
        String qtyText = etQuantity.getText().toString().trim();
        int quantity = 0;
        if (!qtyText.isEmpty()) {
            quantity = Integer.parseInt(qtyText);
        }
        if (quantity <= 0) {
            etQuantity.setError(getString(R.string.sales_err_quantity));
            return;
        }

        Product chosen = products.get(spProducts.getSelectedItemPosition());
        int alreadyInCart = 0;
        for (SaleItem item : cart) {
            if (item.getProductId() == chosen.getId()) {
                alreadyInCart = item.getQuantity();
            }
        }
        int left = chosen.getStock() - alreadyInCart;
        if (quantity > left) {
            Toast.makeText(this, getString(R.string.sales_err_stock, left), Toast.LENGTH_LONG).show();
            return;
        }

        for (int i = 0; i < cart.size(); i++) {
            if (cart.get(i).getProductId() == chosen.getId()) {
                cart.remove(i);
                break;
            }
        }
        cart.add(new SaleItem(chosen.getId(), alreadyInCart + quantity));
        etQuantity.setText("");
        refreshCart();
    }

    // Rebuilds the list lines and the total from the cart.
    private void refreshCart() {
        cartLines.clear();
        int total = 0;
        for (SaleItem item : cart) {
            Product p = findProduct(item.getProductId());
            int lineTotal = p.getPrice() * item.getQuantity();
            total += lineTotal;
            cartLines.add(getString(R.string.sales_item_line, p.getName(), item.getQuantity(), lineTotal));
        }
        cartAdapter.notifyDataSetChanged();
        tvTotal.setText(getString(R.string.sales_total, total));
    }

    // Finds a product in the loaded list by its id.
    private Product findProduct(long productId) {
        for (Product p : products) {
            if (p.getId() == productId) {
                return p;
            }
        }
        return null;
    }

    // Saves the sale. On success: sends the low-stock broadcast and opens the sale summary.
    private void recordSale() {
        if (cart.isEmpty()) {
            Toast.makeText(this, R.string.sales_err_empty_cart, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            long saleId = dbHelper.recordSale(ownerId, cart);
            sendLowStockBroadcast(saleId);
            Toast.makeText(this, R.string.sales_recorded, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, SaleSummaryActivity.class);
            intent.putExtra(Constants.EXTRA_SALE_ID, saleId);
            startActivity(intent);
            finish();
        } catch (SaleException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Shouts "low stock" to the app if any product in this sale is now at or below its reorder level.
    private void sendLowStockBroadcast(long saleId) {
        List<Product> low = dbHelper.getLowStockProductsInSale(ownerId, saleId);
        if (low.isEmpty()) {
            return;
        }
        StringBuilder names = new StringBuilder();
        for (Product p : low) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(p.getName());
        }
        Intent intent = new Intent(Constants.ACTION_LOW_STOCK);
        intent.setPackage(getPackageName());
        intent.putExtra(Constants.EXTRA_LOW_STOCK_NAMES, names.toString());
        sendBroadcast(intent);
    }

    // Lifecycle logging: writes a line to Logcat each time this screen changes state.
    @Override
    protected void onStart() {
        super.onStart();
        Log.d("lifecycle", "NewSaleActivity onStart invoked");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("lifecycle", "NewSaleActivity onResume invoked");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("lifecycle", "NewSaleActivity onPause invoked");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("lifecycle", "NewSaleActivity onStop invoked");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("lifecycle", "NewSaleActivity onRestart invoked");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("lifecycle", "NewSaleActivity onDestroy invoked");
    }
}
