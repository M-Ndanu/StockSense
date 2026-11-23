package com.example.stocksense;

/** One product in an owner's shop. Price is in whole shillings. */
public class Product {
    private long id;
    private long ownerId;
    private String name;
    private String category;
    private int price;
    private int stock;
    private int reorderLevel;

    // Empty constructor, used by the add-product form before values are set.
    public Product() { }

    // Full constructor, used when reading a row from the database.
    public Product(long id, long ownerId, String name, String category,
                   int price, int stock, int reorderLevel) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.reorderLevel = reorderLevel;
    }

    // Returns true when stock is at or below the reorder level.
    public boolean isLowStock() { return stock <= reorderLevel; }

    // Getter for the product id.
    public long getId() { return id; }
    // Setter for the product id.
    public void setId(long id) { this.id = id; }

    // Getter for the owner id.
    public long getOwnerId() { return ownerId; }
    // Setter for the owner id.
    public void setOwnerId(long ownerId) { this.ownerId = ownerId; }

    // Getter for the product name.
    public String getName() { return name; }
    // Setter for the product name.
    public void setName(String name) { this.name = name; }

    // Getter for the category (a plain text column).
    public String getCategory() { return category; }
    // Setter for the category.
    public void setCategory(String category) { this.category = category; }

    // Getter for the price in whole shillings.
    public int getPrice() { return price; }
    // Setter for the price in whole shillings.
    public void setPrice(int price) { this.price = price; }

    // Getter for the stock count.
    public int getStock() { return stock; }
    // Setter for the stock count.
    public void setStock(int stock) { this.stock = stock; }

    // Getter for the reorder level.
    public int getReorderLevel() { return reorderLevel; }
    // Setter for the reorder level.
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
}
