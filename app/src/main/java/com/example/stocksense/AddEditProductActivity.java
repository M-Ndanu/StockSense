package com.example.stocksense;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AddEditProductActivity extends AppCompatActivity {

    private TextView tvAddEditTitle;
    private EditText etProductName;
    private EditText etCategory;
    private EditText etPrice;
    private EditText etStock;
    private EditText etReorderLevel;
    private Button btnSaveProduct;

    private DatabaseHelper dbHelper;
    private long ownerId;
    private long editProductId = -1;

    // Initializes the Add/Edit Product screen and loads existing product data if in Edit mode.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_product);

        dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREF_NAME, MODE_PRIVATE);
        ownerId = prefs.getLong(LoginActivity.KEY_OWNER_ID, -1);

        tvAddEditTitle = findViewById(R.id.tvAddEditTitle);
        etProductName = findViewById(R.id.etProductName);
        etCategory = findViewById(R.id.etCategory);
        etPrice = findViewById(R.id.etPrice);
        etStock = findViewById(R.id.etStock);
        etReorderLevel = findViewById(R.id.etReorderLevel);
        btnSaveProduct = findViewById(R.id.btnSaveProduct);

        editProductId = getIntent().getLongExtra(StockFragment.EXTRA_PRODUCT_ID, -1);
        if (editProductId != -1) {
            tvAddEditTitle.setText(R.string.title_edit_product);
            loadExistingProduct();
        } else {
            tvAddEditTitle.setText(R.string.title_add_product);
        }

        btnSaveProduct.setOnClickListener(v -> saveProduct());
    }

    // Loads the existing product's values into the form fields when editing.
    private void loadExistingProduct() {
        Product existing = dbHelper.getProduct(ownerId, editProductId);
        if (existing == null) {
            Toast.makeText(this, R.string.err_product_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        etProductName.setText(existing.getName());
        etCategory.setText(existing.getCategory());
        etPrice.setText(String.valueOf(existing.getPrice()));
        etStock.setText(String.valueOf(existing.getStock()));
        etReorderLevel.setText(String.valueOf(existing.getReorderLevel()));
    }

    // Validates all form inputs and either inserts a new product or updates the existing one.
    private void saveProduct() {
        clearErrors();

        String name = etProductName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();
        String reorderStr = etReorderLevel.getText().toString().trim();

        boolean isValid = true;

        if (name.isEmpty()) {
            etProductName.setError(getString(R.string.err_field_required));
            isValid = false;
        }
        if (category.isEmpty()) {
            etCategory.setError(getString(R.string.err_field_required));
            isValid = false;
        }

        int price = 0;
        if (priceStr.isEmpty()) {
            etPrice.setError(getString(R.string.err_field_required));
            isValid = false;
        } else {
            try {
                price = Integer.parseInt(priceStr);
                if (price <= 0) {
                    etPrice.setError(getString(R.string.err_invalid_price));
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etPrice.setError(getString(R.string.err_invalid_price));
                isValid = false;
            }
        }

        int stock = 0;
        if (stockStr.isEmpty()) {
            etStock.setError(getString(R.string.err_field_required));
            isValid = false;
        } else {
            try {
                stock = Integer.parseInt(stockStr);
                if (stock < 0) {
                    etStock.setError(getString(R.string.err_invalid_stock));
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etStock.setError(getString(R.string.err_invalid_stock));
                isValid = false;
            }
        }

        int reorderLevel = 0;
        if (reorderStr.isEmpty()) {
            etReorderLevel.setError(getString(R.string.err_field_required));
            isValid = false;
        } else {
            try {
                reorderLevel = Integer.parseInt(reorderStr);
                if (reorderLevel < 0) {
                    etReorderLevel.setError(getString(R.string.err_invalid_reorder));
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etReorderLevel.setError(getString(R.string.err_invalid_reorder));
                isValid = false;
            }
        }

        if (!isValid) return;

        Product product = new Product();
        product.setOwnerId(ownerId);
        product.setName(name);
        product.setCategory(category);
        product.setPrice(price);
        product.setStock(stock);
        product.setReorderLevel(reorderLevel);

        if (editProductId != -1) {
            product.setId(editProductId);
            int rows = dbHelper.updateProduct(product);
            if (rows > 0) {
                Toast.makeText(this, R.string.msg_product_updated, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, R.string.err_product_save_failed, Toast.LENGTH_SHORT).show();
            }
        } else {
            long newId = dbHelper.addProduct(product);
            if (newId != -1) {
                Toast.makeText(this, R.string.msg_product_saved, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, R.string.err_product_save_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Clears any validation errors displayed on the form inputs.
    private void clearErrors() {
        etProductName.setError(null);
        etCategory.setError(null);
        etPrice.setError(null);
        etStock.setError(null);
        etReorderLevel.setError(null);
    }
}