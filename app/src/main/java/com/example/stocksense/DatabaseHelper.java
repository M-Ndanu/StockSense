package com.example.stocksense;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "stocksense.db";
    public static final int DATABASE_VERSION = 1;
    // Sets up the helper with the database name and version.
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    // Person 1 fills this in: create the tables here.
    @Override
    public void onCreate(SQLiteDatabase db) { }
    // Person 1 fills this in: handle database upgrades here.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
}
