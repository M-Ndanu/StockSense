package com.example.stocksense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ProductDetailsActivity extends AppCompatActivity {

    private TextView tvDetailName;
    private TextView tvDetailCategory;
    private TextView tvDetailPrice;
    private TextView tvDetailStock;
    private TextView tvDetailReorder;
    private Button btnEditProduct;
    private Button btnDeleteProduct;

    private DatabaseHelper dbHelper;
    private long ownerId;
    private long productId = -1;

    // Initializes the Product Details screen and wires up Edit and Delete actions.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE);
        ownerId = prefs.getLong(LoginActivity.KEY_OWNER_ID, -1);

        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailCategory = findViewById(R.id.tvDetailCategory);
        tvDetailPrice = findViewById(R.id.tvDetailPrice);
        tvDetailStock = findViewById(R.id.tvDetailStock);
        tvDetailReorder = findViewById(R.id.tvDetailReorder);
        btnEditProduct = findViewById(R.id.btnEditProduct);
        btnDeleteProduct = findViewById(R.id.btnDeleteProduct);

        productId = getIntent().getLongExtra(StockFragment.EXTRA_PRODUCT_ID, -1);
        if (productId == -1) {
            Toast.makeText(this, R.string.err_product_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnEditProduct.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditProductActivity.class);
            intent.putExtra(StockFragment.EXTRA_PRODUCT_ID, productId);
            startActivity(intent);
        });

        btnDeleteProduct.setOnClickListener(v -> handleDeleteProduct());

        loadProductDetails();
    }

    // Refreshes product details whenever returning from AddEditProductActivity.
    @Override
    protected void onResume() {
        super.onResume();
        if (productId != -1) {
            loadProductDetails();
        }
    }

    // Loads the product from SQLite and populates all detail TextViews.
    private void loadProductDetails() {
        Product product = dbHelper.getProduct(ownerId, productId);
        if (product == null) {
            Toast.makeText(this, R.string.err_product_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvDetailName.setText(product.getName());
        tvDetailCategory.setText(getString(R.string.label_category_prefix) + product.getCategory());
        tvDetailPrice.setText(getString(R.string.label_price_prefix) + product.getPrice());
        tvDetailStock.setText(getString(R.string.label_stock_prefix) + product.getStock());
        tvDetailReorder.setText(getString(R.string.label_reorder_prefix) + product.getReorderLevel());

        if (product.isLowStock()) {
            tvDetailStock.setTextColor(ContextCompat.getColor(this, R.color.stocksense_alert));
        } else {
            tvDetailStock.setTextColor(ContextCompat.getColor(this, R.color.stocksense_text));
        }
    }

    // Attempts to delete the product and displays the appropriate feedback message.
    private void handleDeleteProduct() {
        int result = dbHelper.deleteProduct(ownerId, productId);
        if (result == DatabaseHelper.DELETE_OK) {
            Toast.makeText(this, R.string.msg_product_deleted, Toast.LENGTH_SHORT).show();
            finish();
        } else if (result == DatabaseHelper.DELETE_HAS_SALES) {
            Toast.makeText(this, R.string.err_delete_has_sales, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.err_product_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}