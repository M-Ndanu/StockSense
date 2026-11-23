package com.example.stocksense;

/** One shop owner account. The password hash is never exposed to the rest of the app. */
public class User {
    private long id;
    private String username;

    // Builds a user with an id and a username.
    public User(long id, String username) {
        this.id = id;
        this.username = username;
    }

    // Returns the user's database id (this is the "owner id" used everywhere).
    public long getId() { return id; }

    // Returns the username.
    public String getUsername() { return username; }
}
