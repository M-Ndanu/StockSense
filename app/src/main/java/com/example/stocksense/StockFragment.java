package com.example.stocksense;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class StockFragment extends Fragment {

    public static final String EXTRA_PRODUCT_ID = Constants.EXTRA_PRODUCT_ID;

    private Toolbar toolbarStock;
    private TextView tvEmptyStock;
    private ListView lvProducts;

    private DatabaseHelper dbHelper;
    private long ownerId;
    private final List<Product> displayedProducts = new ArrayList<>();
    private StockAdapter adapter;
    private boolean showLowStockOnly = false;

    // Inflates the Stock fragment layout and wires up buttons, menus, and database loading.
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stock, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        SharedPreferences prefs = requireContext().getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        ownerId = prefs.getLong(LoginActivity.KEY_OWNER_ID, -1);

        toolbarStock = view.findViewById(R.id.toolbarStock);
        Button btnAddProduct = view.findViewById(R.id.btnAddProduct);
        Button btnGoNewSale = view.findViewById(R.id.btnGoNewSale);
        tvEmptyStock = view.findViewById(R.id.tvEmptyStock);
        lvProducts = view.findViewById(R.id.lvProducts);

        setupOptionsMenu();

        adapter = new StockAdapter();
        lvProducts.setAdapter(adapter);

        btnAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditProductActivity.class);
            startActivity(intent);
        });

        btnGoNewSale.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), NewSaleActivity.class);
            startActivity(intent);
        });

        lvProducts.setOnItemClickListener((parent, itemView, position, id) -> {
            Product selected = displayedProducts.get(position);
            showProductPopupMenu(itemView, selected);
        });

        loadProducts();
        return view;
    }

    // Reloads the product list whenever returning to this fragment.
    @Override
    public void onResume() {
        super.onResume();
        loadProducts();
    }

    // Configures the Options Menu for filtering low-stock items and logging out.
    private void setupOptionsMenu() {
        toolbarStock.getMenu().clear();
        MenuItem filterItem = toolbarStock.getMenu().add(0, 1, 0,
                showLowStockOnly ? R.string.menu_show_all_stock : R.string.menu_filter_low_stock);
        filterItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        MenuItem logoutItem = toolbarStock.getMenu().add(0, 2, 1, R.string.menu_logout);
        logoutItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        toolbarStock.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    showLowStockOnly = !showLowStockOnly;
                    setupOptionsMenu();
                    loadProducts();
                    return true;
                case 2:
                    requireContext().getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .remove(LoginActivity.KEY_OWNER_ID)
                            .apply();
                    startActivity(new Intent(requireContext(), LoginActivity.class));
                    requireActivity().finish();
                    return true;
                default:
                    return false;
            }
        });
    }

    // Picks a picture for a product by looking for a keyword in its name. Falls back to a default picture.
    private int imageForProduct(String productName) {
        String name = productName.toLowerCase();
        if (name.contains("sugar")) return R.drawable.img_sugar;
        if (name.contains("oil")) return R.drawable.img_cooking_oil;
        if (name.contains("flour")) return R.drawable.img_wheat_flour;
        if (name.contains("rice")) return R.drawable.img_rice;
        if (name.contains("milk")) return R.drawable.img_milk;
        if (name.contains("bread")) return R.drawable.img_bread;
        if (name.contains("soap")) return R.drawable.img_bar_soap;
        if (name.contains("toothpaste")) return R.drawable.img_toothpaste;
        if (name.contains("egg")) return R.drawable.img_eggs;
        if (name.contains("tea")) return R.drawable.img_tea_leaves;
        if (name.contains("salt")) return R.drawable.img_salt;
        if (name.contains("soda") || name.contains("drink") || name.contains("cola") || name.contains("juice")) return R.drawable.img_soft_drink;
        if (name.contains("margarine")) return R.drawable.img_margarine;
        return R.drawable.img_default;
    }

    // Displays a Popup Menu on a tapped product with View Details, Edit, and Delete actions.
    private void showProductPopupMenu(View anchorView, Product product) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenu().add(0, 1, 0, R.string.menu_view_details);
        popup.getMenu().add(0, 2, 1, R.string.menu_edit);
        popup.getMenu().add(0, 3, 2, R.string.menu_delete);

        popup.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case 1: {
                    Intent intent = new Intent(requireContext(), ProductDetailsActivity.class);
                    intent.putExtra(EXTRA_PRODUCT_ID, product.getId());
                    startActivity(intent);
                    return true;
                }
                case 2: {
                    Intent intent = new Intent(requireContext(), AddEditProductActivity.class);
                    intent.putExtra(EXTRA_PRODUCT_ID, product.getId());
                    startActivity(intent);
                    return true;
                }
                case 3: {
                    int result = dbHelper.deleteProduct(ownerId, product.getId());
                    switch (result) {
                        case DatabaseHelper.DELETE_OK:
                            Toast.makeText(requireContext(), R.string.msg_product_deleted, Toast.LENGTH_SHORT).show();
                            loadProducts();
                            break;
                        case DatabaseHelper.DELETE_HAS_SALES:
                            Toast.makeText(requireContext(), R.string.err_delete_has_sales, Toast.LENGTH_LONG).show();
                            break;
                        default:
                            Toast.makeText(requireContext(), R.string.err_product_not_found, Toast.LENGTH_SHORT).show();
                            break;
                    }
                    return true;
                }
                default:
                    return false;
            }
        });
        popup.show();
    }

    // Loads all products or low-stock products for the current owner from SQLite.
    private void loadProducts() {
        displayedProducts.clear();
        List<Product> products = showLowStockOnly
                ? dbHelper.getLowStockProducts(ownerId)
                : dbHelper.getProducts(ownerId);
        displayedProducts.addAll(products);

        if (displayedProducts.isEmpty()) {
            tvEmptyStock.setVisibility(View.VISIBLE);
            lvProducts.setVisibility(View.GONE);
        } else {
            tvEmptyStock.setVisibility(View.GONE);
            lvProducts.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    // Custom adapter binding Product objects to item_product.xml with low-stock highlighting.
    private class StockAdapter extends BaseAdapter {

        // Returns the number of displayed products.
        // Returns the number of displayed products.
        @Override
        public int getCount() {
            return displayedProducts.size();
        }

        // Returns the Product at the given list position.
        @Override
        public Object getItem(int position) {
            return displayedProducts.get(position);
        }

        // Returns the database ID of the Product at the given position.
        @Override
        public long getItemId(int position) {
            return displayedProducts.get(position).getId();
        }

        // Populates each product row view and shows the LOW STOCK badge if isLowStock() is true.
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = (convertView != null) ? convertView : LayoutInflater.from(requireContext()).inflate(R.layout.item_product, parent, false);

            Product item = displayedProducts.get(position);

            TextView tvName = itemView.findViewById(R.id.tvItemProductName);
            TextView tvCategory = itemView.findViewById(R.id.tvItemCategory);
            TextView tvPrice = itemView.findViewById(R.id.tvItemPrice);
            TextView tvStock = itemView.findViewById(R.id.tvItemStock);
            TextView tvBadge = itemView.findViewById(R.id.tvLowStockBadge);
            ImageButton btnMore = itemView.findViewById(R.id.btnItemMore);
            btnMore.setOnClickListener(v -> showProductPopupMenu(v, item));
            ImageView ivImage = itemView.findViewById(R.id.ivItemImage);
            ivImage.setImageResource(imageForProduct(item.getName()));

            tvName.setText(item.getName());
            tvCategory.setText(item.getCategory());
            tvPrice.setText(getString(R.string.label_currency_prefix) + item.getPrice());
            tvStock.setText(getString(R.string.label_in_stock) + item.getStock());

            if (item.isLowStock()) {
                tvBadge.setVisibility(View.VISIBLE);
                tvStock.setTextColor(ContextCompat.getColor(requireContext(), R.color.stocksense_alert));
            } else {
                tvBadge.setVisibility(View.GONE);
                tvStock.setTextColor(ContextCompat.getColor(requireContext(), R.color.stocksense_text));
            }

            return itemView;
        }
    }
}
