package com.example.stocksense;

/**
 * One line of a sale. To record a sale you only need to set productId and quantity;
 * the database fills in unitPrice and subtotal from the product's price at that moment.
 */
public class SaleItem {
    private long id;
    private long saleId;
    private long productId;
    private int quantity;
    private int unitPrice;   // price at the time of sale, whole shillings
    private int subtotal;    // quantity * unitPrice, whole shillings
    private String productName; // not stored in sale_items; filled in when reading, for display

    // Builds a line for a new sale: just the product and how many.
    public SaleItem(long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    // Full constructor, used when reading a row from the database.
    public SaleItem(long id, long saleId, long productId, int quantity,
                    int unitPrice, int subtotal, String productName) {
        this.id = id;
        this.saleId = saleId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
        this.productName = productName;
    }

    // Getter for the sale item id.
    public long getId() { return id; }
    // Getter for the id of the sale this line belongs to.
    public long getSaleId() { return saleId; }
    // Getter for the product id.
    public long getProductId() { return productId; }
    // Getter for the quantity sold.
    public int getQuantity() { return quantity; }
    // Getter for the price per item at the time of sale.
    public int getUnitPrice() { return unitPrice; }
    // Getter for quantity times unit price.
    public int getSubtotal() { return subtotal; }
    // Getter for the product name (for display only).
    public String getProductName() { return productName; }
}
