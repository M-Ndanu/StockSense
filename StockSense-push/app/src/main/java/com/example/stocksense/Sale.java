package com.example.stocksense;

import java.util.ArrayList;
import java.util.List;

/** One completed sale. saleTime is a timestamp in milliseconds; total is whole shillings. */
public class Sale {
    private long id;
    private long ownerId;
    private long saleTime;
    private int total;
    private List<SaleItem> items = new ArrayList<>(); // only filled by getSale(...)

    // Builds a sale from a database row.
    public Sale(long id, long ownerId, long saleTime, int total) {
        this.id = id;
        this.ownerId = ownerId;
        this.saleTime = saleTime;
        this.total = total;
    }

    // Getter for the sale id.
    public long getId() { return id; }
    // Getter for the owner id.
    public long getOwnerId() { return ownerId; }
    // Getter for the sale timestamp (milliseconds).
    public long getSaleTime() { return saleTime; }
    // Getter for the sale total in whole shillings.
    public int getTotal() { return total; }
    // Getter for the sale's items (empty unless loaded with getSale).
    public List<SaleItem> getItems() { return items; }
    // Setter for the sale's items.
    public void setItems(List<SaleItem> items) { this.items = items; }
}
