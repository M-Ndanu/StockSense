package com.example.stocksense;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * All database work for the app lives here. Other screens only call these methods.
 * Every method that touches owner data takes an ownerId and filters by it,
 * so one owner can never see or change another owner's products or sales.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Database name and version.
    public static final String DATABASE_NAME = "stocksense.db";
    public static final int DATABASE_VERSION = 1;

    // Results of deleteProduct, so the screen knows what to tell the owner.
    public static final int DELETE_OK = 0;
    public static final int DELETE_HAS_SALES = 1;
    public static final int DELETE_NOT_FOUND = 2;

    // Table names.
    private static final String T_USERS = "users";
    private static final String T_PRODUCTS = "products";
    private static final String T_SALES = "sales";
    private static final String T_ITEMS = "sale_items";

    // Demo account details (shown in the demo, so keep them simple).
    public static final String DEMO_USERNAME = "demo";
    public static final String DEMO_PASSWORD = "demo123";

    // Kept so error messages can come from the strings file instead of being typed in code.
    private final Context appContext;

    // Creates the helper. Use one instance per screen: new DatabaseHelper(this).
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.appContext = context.getApplicationContext();
    }

    // Turns on foreign key checking, which SQLite leaves off by default.
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // Creates the 4 tables the first time the app runs, then loads the demo account.
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_USERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT NOT NULL UNIQUE COLLATE NOCASE, "
                + "password_hash TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + T_PRODUCTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "owner_id INTEGER NOT NULL, "
                + "name TEXT NOT NULL CHECK (length(trim(name)) > 0), "
                + "category TEXT NOT NULL CHECK (length(trim(category)) > 0), "
                + "price INTEGER NOT NULL CHECK (price > 0), "
                + "stock INTEGER NOT NULL CHECK (stock >= 0), "
                + "reorder_level INTEGER NOT NULL CHECK (reorder_level >= 0), "
                + "FOREIGN KEY (owner_id) REFERENCES " + T_USERS + "(id) ON DELETE RESTRICT)");

        db.execSQL("CREATE TABLE " + T_SALES + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "owner_id INTEGER NOT NULL, "
                + "sale_time INTEGER NOT NULL, "
                + "total INTEGER NOT NULL, "
                + "FOREIGN KEY (owner_id) REFERENCES " + T_USERS + "(id) ON DELETE RESTRICT)");

        db.execSQL("CREATE TABLE " + T_ITEMS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "sale_id INTEGER NOT NULL, "
                + "product_id INTEGER NOT NULL, "
                + "quantity INTEGER NOT NULL CHECK (quantity > 0), "
                + "unit_price INTEGER NOT NULL, "
                + "subtotal INTEGER NOT NULL, "
                + "FOREIGN KEY (sale_id) REFERENCES " + T_SALES + "(id) ON DELETE RESTRICT, "
                + "FOREIGN KEY (product_id) REFERENCES " + T_PRODUCTS + "(id) ON DELETE RESTRICT)");

        seedDemoData(db);
    }

    // Runs when DATABASE_VERSION goes up. For this project it simply wipes and rebuilds everything.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + T_SALES);
        db.execSQL("DROP TABLE IF EXISTS " + T_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + T_USERS);
        onCreate(db);
    }

    // ------------------------------------------------------------------
    // Password hashing (salted SHA-256). Stored as "salt:hash" in password_hash.
    // ------------------------------------------------------------------

    // Turns a password into the text we store: a random salt, a colon, then the hash.
    private static String makeStoredHash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltHex = toHex(salt);
        return saltHex + ":" + hashWithSalt(password, saltHex);
    }

    // Hashes the password together with the given salt and returns the result as hex text.
    private static String hashWithSalt(String password, String saltHex) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(saltHex.getBytes(StandardCharsets.UTF_8));
            return toHex(sha.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // Checks a typed password against a stored "salt:hash" value.
    private static boolean passwordMatches(String password, String stored) {
        int colon = stored.indexOf(':');
        if (colon < 0) return false;
        String saltHex = stored.substring(0, colon);
        String expected = stored.substring(colon + 1);
        String actual = hashWithSalt(password, saltHex);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    // Converts bytes to a lowercase hex string.
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Users: sign-up and login
    // ------------------------------------------------------------------

    // Creates an account. Returns the new owner id, or -1 if the username is taken or input is empty.
    public long registerUser(String username, String password) {
        if (username == null || password == null) return -1;
        username = username.trim();
        if (username.isEmpty() || password.isEmpty()) return -1;

        ContentValues v = new ContentValues();
        v.put("username", username);
        v.put("password_hash", makeStoredHash(password));
        return getWritableDatabase().insert(T_USERS, null, v); // -1 if username already exists
    }

    // Checks username and password. Returns the owner id on success, or -1 on failure.
    public long loginUser(String username, String password) {
        if (username == null || password == null) return -1;
        Cursor c = getReadableDatabase().query(T_USERS,
                new String[]{"id", "password_hash"},
                "username = ?", new String[]{username.trim()}, null, null, null);
        try {
            if (c.moveToFirst() && passwordMatches(password, c.getString(1))) {
                return c.getLong(0);
            }
            return -1;
        } finally {
            c.close();
        }
    }

    // Returns true if a username already exists (for live validation on the sign-up form).
    public boolean isUsernameTaken(String username) {
        if (username == null) return false;
        Cursor c = getReadableDatabase().query(T_USERS, new String[]{"id"},
                "username = ?", new String[]{username.trim()}, null, null, null);
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    // Returns the user for an owner id, or null if not found (used by the share report).
    public User getUser(long ownerId) {
        Cursor c = getReadableDatabase().query(T_USERS, new String[]{"id", "username"},
                "id = ?", new String[]{String.valueOf(ownerId)}, null, null, null);
        try {
            return c.moveToFirst() ? new User(c.getLong(0), c.getString(1)) : null;
        } finally {
            c.close();
        }
    }

    // ------------------------------------------------------------------
    // Products: add, read, update, delete, low-stock
    // ------------------------------------------------------------------

    // Adds a product for the owner set on the Product. Returns the new id, or -1 if the values are invalid.
    public long addProduct(Product p) {
        return getWritableDatabase().insert(T_PRODUCTS, null, productValues(p));
    }

    // Updates a product. Only works if it belongs to p.getOwnerId(). Returns rows changed (0 or 1).
    public int updateProduct(Product p) {
        return getWritableDatabase().update(T_PRODUCTS, productValues(p),
                "id = ? AND owner_id = ?",
                new String[]{String.valueOf(p.getId()), String.valueOf(p.getOwnerId())});
    }

    // Deletes a product unless it has past sales. Returns DELETE_OK, DELETE_HAS_SALES or DELETE_NOT_FOUND.
    public int deleteProduct(long ownerId, long productId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + T_ITEMS + " si JOIN " + T_PRODUCTS
                        + " p ON p.id = si.product_id WHERE p.id = ? AND p.owner_id = ?",
                new String[]{String.valueOf(productId), String.valueOf(ownerId)});
        int sold = 0;
        try {
            if (c.moveToFirst()) sold = c.getInt(0);
        } finally {
            c.close();
        }
        if (sold > 0) return DELETE_HAS_SALES;

        int rows = db.delete(T_PRODUCTS, "id = ? AND owner_id = ?",
                new String[]{String.valueOf(productId), String.valueOf(ownerId)});
        return rows > 0 ? DELETE_OK : DELETE_NOT_FOUND;
    }

    // Returns all of one owner's products, sorted by name.
    public List<Product> getProducts(long ownerId) {
        return queryProducts("owner_id = ?", new String[]{String.valueOf(ownerId)});
    }

    // Returns one product if it belongs to the owner, otherwise null.
    public Product getProduct(long ownerId, long productId) {
        List<Product> list = queryProducts("owner_id = ? AND id = ?",
                new String[]{String.valueOf(ownerId), String.valueOf(productId)});
        return list.isEmpty() ? null : list.get(0);
    }

    // Returns the owner's products whose stock is at or below their reorder level.
    public List<Product> getLowStockProducts(long ownerId) {
        return queryProducts("owner_id = ? AND stock <= reorder_level",
                new String[]{String.valueOf(ownerId)});
    }

    // Returns the products in one sale that are now low on stock (for the low-stock alert after a sale).
    public List<Product> getLowStockProductsInSale(long ownerId, long saleId) {
        List<Product> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT p.* FROM " + T_PRODUCTS + " p "
                        + "JOIN " + T_ITEMS + " si ON si.product_id = p.id "
                        + "JOIN " + T_SALES + " s ON s.id = si.sale_id "
                        + "WHERE s.id = ? AND s.owner_id = ? AND p.owner_id = s.owner_id "
                        + "AND p.stock <= p.reorder_level ORDER BY p.name",
                new String[]{String.valueOf(saleId), String.valueOf(ownerId)});
        try {
            while (c.moveToNext()) result.add(cursorToProduct(c));
        } finally {
            c.close();
        }
        return result;
    }

    // Runs a product query with a WHERE clause and returns the matching products sorted by name.
    private List<Product> queryProducts(String where, String[] args) {
        List<Product> result = new ArrayList<>();
        Cursor c = getReadableDatabase().query(T_PRODUCTS, null, where, args,
                null, null, "name COLLATE NOCASE ASC");
        try {
            while (c.moveToNext()) result.add(cursorToProduct(c));
        } finally {
            c.close();
        }
        return result;
    }

    // Copies a Product's fields into ContentValues for insert or update.
    private ContentValues productValues(Product p) {
        ContentValues v = new ContentValues();
        v.put("owner_id", p.getOwnerId());
        v.put("name", p.getName());
        v.put("category", p.getCategory());
        v.put("price", p.getPrice());
        v.put("stock", p.getStock());
        v.put("reorder_level", p.getReorderLevel());
        return v;
    }

    // Builds a Product from the cursor's current row.
    private Product cursorToProduct(Cursor c) {
        return new Product(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getLong(c.getColumnIndexOrThrow("owner_id")),
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getString(c.getColumnIndexOrThrow("category")),
                c.getInt(c.getColumnIndexOrThrow("price")),
                c.getInt(c.getColumnIndexOrThrow("stock")),
                c.getInt(c.getColumnIndexOrThrow("reorder_level")));
    }

    // ------------------------------------------------------------------
    // Sales: record, read, update, delete
    // ------------------------------------------------------------------

    // Records a sale. Each SaleItem needs only productId and quantity. Stock drops, prices are saved.
    // Everything happens in one transaction: if any line fails, nothing is saved.
    // Returns the new sale id, or throws SaleException with a message fit for a Toast.
    public long recordSale(long ownerId, List<SaleItem> items) throws SaleException {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long saleId = insertSale(db, ownerId, items, System.currentTimeMillis());
            db.setTransactionSuccessful();
            return saleId;
        } finally {
            db.endTransaction();
        }
    }

    // Replaces the items of an existing sale (keeps its time). Old stock is returned first,
    // then the new items are checked and sold. All or nothing.
    public void updateSale(long ownerId, long saleId, List<SaleItem> newItems) throws SaleException {
        if (newItems == null || newItems.isEmpty()) {
            throw new SaleException(appContext.getString(R.string.data_err_empty_sale));
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (!saleBelongsToOwner(db, ownerId, saleId)) {
                throw new SaleException(appContext.getString(R.string.data_err_sale_not_found));
            }
            returnStockAndRemoveItems(db, saleId);
            int total = addItemsToSale(db, ownerId, saleId, newItems);
            setSaleTotal(db, saleId, total);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Deletes a sale and puts its items back into stock. Returns true if the sale was deleted.
    public boolean deleteSale(long ownerId, long saleId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (!saleBelongsToOwner(db, ownerId, saleId)) return false;
            returnStockAndRemoveItems(db, saleId);
            db.delete(T_SALES, "id = ? AND owner_id = ?",
                    new String[]{String.valueOf(saleId), String.valueOf(ownerId)});
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    // Returns all of one owner's sales, newest first (items are not loaded).
    public List<Sale> getSales(long ownerId) {
        return querySales("owner_id = ?", new String[]{String.valueOf(ownerId)});
    }

    // Returns the owner's sales between two timestamps (inclusive), newest first. For the share report.
    public List<Sale> getSalesBetween(long ownerId, long fromTime, long toTime) {
        return querySales("owner_id = ? AND sale_time >= ? AND sale_time <= ?",
                new String[]{String.valueOf(ownerId), String.valueOf(fromTime), String.valueOf(toTime)});
    }

    // Returns one sale with its items loaded, or null if it does not belong to the owner.
    public Sale getSale(long ownerId, long saleId) {
        List<Sale> list = querySales("owner_id = ? AND id = ?",
                new String[]{String.valueOf(ownerId), String.valueOf(saleId)});
        if (list.isEmpty()) return null;
        Sale sale = list.get(0);
        sale.setItems(getSaleItems(ownerId, saleId));
        return sale;
    }

    // Returns the lines of one sale (with product names), only if the sale belongs to the owner.
    public List<SaleItem> getSaleItems(long ownerId, long saleId) {
        List<SaleItem> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT si.id, si.sale_id, si.product_id, si.quantity, si.unit_price, si.subtotal, "
                        + "p.name AS product_name FROM " + T_ITEMS + " si "
                        + "JOIN " + T_SALES + " s ON s.id = si.sale_id "
                        + "JOIN " + T_PRODUCTS + " p ON p.id = si.product_id "
                        + "WHERE si.sale_id = ? AND s.owner_id = ? ORDER BY si.id",
                new String[]{String.valueOf(saleId), String.valueOf(ownerId)});
        try {
            while (c.moveToNext()) {
                result.add(new SaleItem(c.getLong(0), c.getLong(1), c.getLong(2),
                        c.getInt(3), c.getInt(4), c.getInt(5), c.getString(6)));
            }
        } finally {
            c.close();
        }
        return result;
    }

    // Runs a sales query with a WHERE clause and returns the sales, newest first.
    private List<Sale> querySales(String where, String[] args) {
        List<Sale> result = new ArrayList<>();
        Cursor c = getReadableDatabase().query(T_SALES, null, where, args,
                null, null, "sale_time DESC");
        try {
            while (c.moveToNext()) {
                result.add(new Sale(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getLong(c.getColumnIndexOrThrow("owner_id")),
                        c.getLong(c.getColumnIndexOrThrow("sale_time")),
                        c.getInt(c.getColumnIndexOrThrow("total"))));
            }
        } finally {
            c.close();
        }
        return result;
    }

    // Inserts the sale row and its items inside an open transaction. Returns the new sale id.
    private long insertSale(SQLiteDatabase db, long ownerId, List<SaleItem> items, long saleTime)
            throws SaleException {
        if (items == null || items.isEmpty()) {
            throw new SaleException(appContext.getString(R.string.data_err_empty_sale));
        }
        ContentValues v = new ContentValues();
        v.put("owner_id", ownerId);
        v.put("sale_time", saleTime);
        v.put("total", 0); // real total is set once the items are added
        long saleId = db.insert(T_SALES, null, v);
        if (saleId == -1) throw new SaleException(appContext.getString(R.string.data_err_save_failed));

        int total = addItemsToSale(db, ownerId, saleId, items);
        setSaleTotal(db, saleId, total);
        return saleId;
    }

    // Checks each line, lowers stock, saves the items with the current price, returns the sale total.
    private int addItemsToSale(SQLiteDatabase db, long ownerId, long saleId, List<SaleItem> items)
            throws SaleException {
        int total = 0;
        for (SaleItem line : items) {
            if (line.getQuantity() <= 0) {
                throw new SaleException(appContext.getString(R.string.data_err_quantity));
            }
            Product p = loadProduct(db, ownerId, line.getProductId());
            if (p == null) {
                throw new SaleException(appContext.getString(R.string.data_err_product_missing));
            }
            if (line.getQuantity() > p.getStock()) {
                throw new SaleException(appContext.getString(
                        R.string.data_err_not_enough_stock, p.getName(), p.getStock()));
            }
            int subtotal = line.getQuantity() * p.getPrice();

            db.execSQL("UPDATE " + T_PRODUCTS + " SET stock = stock - ? WHERE id = ?",
                    new Object[]{line.getQuantity(), p.getId()});

            ContentValues iv = new ContentValues();
            iv.put("sale_id", saleId);
            iv.put("product_id", p.getId());
            iv.put("quantity", line.getQuantity());
            iv.put("unit_price", p.getPrice());
            iv.put("subtotal", subtotal);
            if (db.insert(T_ITEMS, null, iv) == -1) {
                throw new SaleException(appContext.getString(R.string.data_err_save_failed));
            }
            total += subtotal;
        }
        return total;
    }

    // Reads one product inside a transaction, only if it belongs to the owner.
    private Product loadProduct(SQLiteDatabase db, long ownerId, long productId) {
        Cursor c = db.query(T_PRODUCTS, null, "id = ? AND owner_id = ?",
                new String[]{String.valueOf(productId), String.valueOf(ownerId)},
                null, null, null);
        try {
            return c.moveToFirst() ? cursorToProduct(c) : null;
        } finally {
            c.close();
        }
    }

    // Writes the final total onto a sale row.
    private void setSaleTotal(SQLiteDatabase db, long saleId, int total) {
        db.execSQL("UPDATE " + T_SALES + " SET total = ? WHERE id = ?",
                new Object[]{total, saleId});
    }

    // Returns true if the sale exists and belongs to the owner.
    private boolean saleBelongsToOwner(SQLiteDatabase db, long ownerId, long saleId) {
        Cursor c = db.query(T_SALES, new String[]{"id"}, "id = ? AND owner_id = ?",
                new String[]{String.valueOf(saleId), String.valueOf(ownerId)}, null, null, null);
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    // Adds each item's quantity back to its product's stock, then deletes the sale's items.
    private void returnStockAndRemoveItems(SQLiteDatabase db, long saleId) {
        Cursor c = db.query(T_ITEMS, new String[]{"product_id", "quantity"}, "sale_id = ?",
                new String[]{String.valueOf(saleId)}, null, null, null);
        try {
            while (c.moveToNext()) {
                db.execSQL("UPDATE " + T_PRODUCTS + " SET stock = stock + ? WHERE id = ?",
                        new Object[]{c.getInt(1), c.getLong(0)});
            }
        } finally {
            c.close();
        }
        db.delete(T_ITEMS, "sale_id = ?", new String[]{String.valueOf(saleId)});
    }

    // ------------------------------------------------------------------
    // Demo account: created and filled the first time the database is made
    // ------------------------------------------------------------------

    // Creates the demo owner with sample products and a few past sales. Runs once, from onCreate.
    private void seedDemoData(SQLiteDatabase db) {
        ContentValues u = new ContentValues();
        u.put("username", DEMO_USERNAME);
        u.put("password_hash", makeStoredHash(DEMO_PASSWORD));
        long owner = db.insert(T_USERS, null, u);

        long sugar = seedProduct(db, owner, "Sugar 2kg", "Groceries", 280, 40, 10);
        long oil = seedProduct(db, owner, "Cooking Oil 1L", "Groceries", 350, 25, 8);
        long flour = seedProduct(db, owner, "Wheat Flour 2kg", "Groceries", 190, 30, 10);
        long rice = seedProduct(db, owner, "Rice 1kg", "Groceries", 180, 14, 10);
        long milk = seedProduct(db, owner, "Milk 500ml", "Dairy", 60, 9, 6);
        long bread = seedProduct(db, owner, "Bread", "Bakery", 60, 12, 6);
        long soap = seedProduct(db, owner, "Bar Soap", "Household", 90, 50, 15);
        seedProduct(db, owner, "Toothpaste", "Personal Care", 150, 18, 5);

        long day = 24L * 60 * 60 * 1000;
        long now = System.currentTimeMillis();
        try {
            seedSale(db, owner, now - 3 * day, line(sugar, 2), line(flour, 1));
            seedSale(db, owner, now - 2 * day, line(oil, 1), line(soap, 3), line(bread, 2));
            seedSale(db, owner, now - day, line(milk, 4), line(sugar, 1));
            seedSale(db, owner, now - 2 * 60 * 60 * 1000, line(rice, 5), line(bread, 2));
        } catch (SaleException e) {
            // Sample data is fixed and fits the stock, so this should never happen.
            throw new IllegalStateException("Demo data failed: " + e.getMessage(), e);
        }
        // Result: Rice and Milk end up at or below their reorder level, so the demo shows low stock.
    }

    // Inserts one sample product and returns its id.
    private long seedProduct(SQLiteDatabase db, long owner, String name, String category,
                             int price, int stock, int reorder) {
        ContentValues v = new ContentValues();
        v.put("owner_id", owner);
        v.put("name", name);
        v.put("category", category);
        v.put("price", price);
        v.put("stock", stock);
        v.put("reorder_level", reorder);
        return db.insert(T_PRODUCTS, null, v);
    }

    // Makes a sale line from a product id and a quantity (shorthand for the sample data).
    private SaleItem line(long productId, int quantity) {
        return new SaleItem(productId, quantity);
    }

    // Records one sample sale at a given time, using the normal sale logic.
    private void seedSale(SQLiteDatabase db, long owner, long time, SaleItem... lines)
            throws SaleException {
        List<SaleItem> items = new ArrayList<>();
        for (SaleItem l : lines) items.add(l);
        insertSale(db, owner, items, time);
    }
}
