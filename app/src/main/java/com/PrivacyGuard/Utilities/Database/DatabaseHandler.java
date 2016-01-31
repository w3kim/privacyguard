package com.PrivacyGuard.Utilities.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import com.PrivacyGuard.Utilities.StringUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by MAK on 03/11/2015.
 */
public class DatabaseHandler extends SQLiteOpenHelper {
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "dataLeaksManager";

    // DataLeaks table name
    private static final String TABLE_DATA_LEAKS = "data_leaks";
    private static final String TABLE_LOCATION_LEAKS = "location_leaks";

    // DataLeaks Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "app_name";
    private static final String KEY_TYPE = "leak_type";
    private static final String KEY_FREQUENCY = "leak_frequency";
    private static final String KEY_IGNORE = "ignore";
    private static final String KEY_TIME_STAMP = "time_stamp";

    private static final String KEY_LOCATION = "location";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        //create table data_leaks
        String CREATE_DATA_LEAKS_TABLE = "CREATE TABLE " + TABLE_DATA_LEAKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_TYPE + " TEXT," + KEY_FREQUENCY + " INTEGER," + KEY_IGNORE
                + " INTEGER," + KEY_TIME_STAMP + " TEXT" + ")";
        db.execSQL(CREATE_DATA_LEAKS_TABLE);

        //create table location_leaks
        String CREATE_LOCATION_LEAKS_TABLE = "CREATE TABLE " + TABLE_LOCATION_LEAKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_LOCATION + " TEXT, " + KEY_TIME_STAMP + " TEXT" + ")";
        db.execSQL(CREATE_LOCATION_LEAKS_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA_LEAKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION_LEAKS);

        // Create tables again
        onCreate(db);
    }

    //TODO: I think this resets table as soon as it goes into next month, so on the first day of a month
    //      user would lose all data even they are from yesterday
    public void monthlyReset() {
        DateFormat dateFormat = new SimpleDateFormat("MM", Locale.getDefault());//TODO: get user locale
        Date date = new Date();
        String m = dateFormat.format(date);

        SQLiteDatabase db = this.getWritableDatabase();

        //reset table date_leak
        Cursor cursor = db.query(TABLE_DATA_LEAKS, new String[]{KEY_TIME_STAMP}, null,
                null, null, null, " date(" + KEY_TIME_STAMP + ") DESC", null);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            String dateString = cursor.getString(0).substring(3, 5);

            if (!dateString.equals(m)) {
                resetDataLeakTable();
                return;
            }
        }

        //reset table location_leak
        cursor = db.query(TABLE_LOCATION_LEAKS, new String[]{KEY_TIME_STAMP}, null,
                null, null, null, " date(" + KEY_TIME_STAMP + ") DESC", null);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            String dateString = cursor.getString(0).substring(3, 5);

            if (!dateString.equals(m)) {
                resetLocationLeakTable();
            }
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    void resetDataLeakTable() {
        SQLiteDatabase db = this.getWritableDatabase();

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA_LEAKS);

        String CREATE_DATA_LEAKS_TABLE = "CREATE TABLE " + TABLE_DATA_LEAKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_TYPE + " TEXT," + KEY_FREQUENCY + " INTEGER," + KEY_IGNORE
                + " INTEGER," + KEY_TIME_STAMP + " TEXT" + ")";
        db.execSQL(CREATE_DATA_LEAKS_TABLE);
    }

    void resetLocationLeakTable() {
        SQLiteDatabase db = this.getWritableDatabase();

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION_LEAKS);

        String CREATE_LOCATION_LEAKS_TABLE = "CREATE TABLE " + TABLE_LOCATION_LEAKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_LOCATION + " TEXT, " + KEY_TIME_STAMP + " TEXT" + ")";
        db.execSQL(CREATE_LOCATION_LEAKS_TABLE);
    }

    // Adding new data leak
    public void addDataLeak(DataLeak leak) {
        SQLiteDatabase db = this.getWritableDatabase();
//TODO: app name can be same, but package name is unique, consider change shcema
        String selection = KEY_NAME + "= '" + leak.getAppName() + "' AND " + KEY_TYPE + "= '" + leak.getLeakType() + "'";

        Cursor cursor = db.query(TABLE_DATA_LEAKS, new String[]{KEY_ID,
                        KEY_NAME, KEY_TYPE, KEY_FREQUENCY, KEY_TIME_STAMP}, selection,
                null, null, null, null);

        if (cursor != null) {
            // if entry for this app exists
            if (cursor.moveToFirst()) {
                ContentValues newValues = new ContentValues();
                int freq = (1 + cursor.getInt(3));
                newValues.put(KEY_FREQUENCY, freq);
                String[] args = new String[]{cursor.getString(0)};

                db.update(TABLE_DATA_LEAKS, newValues, KEY_ID + "=?", args);
            } else {
                // cursor is empty
                ContentValues values = new ContentValues();
                values.put(KEY_NAME, leak.getAppName()); // App Name
                values.put(KEY_TYPE, leak.getLeakType()); // Leak type
                values.put(KEY_FREQUENCY, leak.getFrequency()); // Leak counter/frequency
                values.put(KEY_IGNORE, leak.getIgnore()); // Ignore
                values.put(KEY_TIME_STAMP, leak.getTimeStamp()); // Leak time stamp

                // Inserting Row
                db.insert(TABLE_DATA_LEAKS, null, values);

            }
            cursor.close();
        }
        db.close(); // Closing database connection
    }

    // Adding new data leak
    public void addLocationLeak(LocationLeak leak) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, leak.getAppName()); // App Name
        values.put(KEY_LOCATION, leak.getLocation()); // Leaked location
        values.put(KEY_TIME_STAMP, leak.getTimeStamp()); // Leak time stamp

        // Inserting Row
        db.insert(TABLE_LOCATION_LEAKS, null, values);

        db.close(); // Closing database connection
    }

    // Getting single leak
    @Nullable
    public DataLeak getLeak(int id) {
        DataLeak leak = null;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_DATA_LEAKS, new String[]{KEY_ID,
                        KEY_NAME, KEY_TYPE, KEY_FREQUENCY, KEY_IGNORE, KEY_TIME_STAMP}, KEY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                leak = new DataLeak(cursor.getInt(0),
                        cursor.getString(1), cursor.getString(2), cursor.getInt(3), cursor.getString(5));
                leak.setIgnore(cursor.getInt(4));
            }
            cursor.close();
        }
        db.close();
        // return leak
        return leak;
    }

    // Getting All Data Leaks for Specific App
    public List<LocationLeak> getLocationLeaks(String appName) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<LocationLeak> leakList = new ArrayList<LocationLeak>();

        Cursor cursor = db.query(TABLE_LOCATION_LEAKS, new String[]{KEY_ID,
                        KEY_NAME, KEY_LOCATION, KEY_TIME_STAMP}, KEY_NAME + "=?",
                new String[]{appName}, null, null, null, null);

        if (cursor != null) {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    LocationLeak leak = new LocationLeak();
                    //leak.setID(Integer.parseInt(cursor.getString(0)));
                    leak.setAppName(cursor.getString(1));
                    leak.setLocation(cursor.getString(2));
                    leak.setTimeStamp(cursor.getString(3));
                    // Adding leak to list
                    leakList.add(leak);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        // return contact list
        return leakList;
    }

    // Getting All Data Leaks for Specific App
    public List<DataLeak> getAppLeaks(String appName) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<DataLeak> leakList = new ArrayList<DataLeak>();

        Cursor cursor = db.query(TABLE_DATA_LEAKS, new String[]{KEY_ID,
                        KEY_NAME, KEY_TYPE, KEY_FREQUENCY, KEY_IGNORE, KEY_TIME_STAMP}, KEY_NAME + "=?",
                new String[]{appName}, null, null, null, null);

        if (cursor != null) {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    DataLeak leak = new DataLeak();
                    leak.setID(cursor.getInt(0));
                    leak.setAppName(cursor.getString(1));
                    leak.setLeakType(cursor.getString(2));
                    leak.setFrequency(cursor.getInt(3));
                    leak.setIgnore(cursor.getInt(4));
                    // Adding leak to list
                    leakList.add(leak);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        db.close();
        // return contact list
        return leakList;
    }

    // Getting All Data Leaks
    public List<DataLeak> getAllLeaks() {
        List<DataLeak> leakList = new ArrayList<DataLeak>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_DATA_LEAKS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    DataLeak leak = new DataLeak();
                    leak.setID(cursor.getInt(0));
                    leak.setAppName(cursor.getString(1));
                    leak.setLeakType(cursor.getString(2));
                    leak.setFrequency(cursor.getInt(3));
                    leak.setIgnore(cursor.getInt(4));
                    leak.setTimeStamp(cursor.getString(5));
                    // Adding leak to list
                    leakList.add(leak);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        db.close();
        // return contact list
        return leakList;
    }

    // Updating single leak
    public int updateLeak(DataLeak leak) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, leak.getAppName());
        values.put(KEY_TYPE, leak.getLeakType());
        values.put(KEY_FREQUENCY, leak.getFrequency());
        values.put(KEY_IGNORE, leak.getIgnore());
        values.put(KEY_TIME_STAMP, leak.getTimeStamp());

        // updating row
        return db.update(TABLE_DATA_LEAKS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(leak.getID())});
    }

    // Deleting single leak
    public void deleteLeak(DataLeak leak) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DATA_LEAKS, KEY_ID + " = ?",
                new String[]{String.valueOf(leak.getID())});
        db.close();
    }


    // Getting data leaks Count
    public int getDataLeaksCount() {
        String countQuery = "SELECT  * FROM " + TABLE_DATA_LEAKS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();
        db.close();
        // return count
        return cursor.getCount();
    }

    public int findNotificationId(String appName, String msg) {
        List<DataLeak> leakList = getAppLeaks(appName);
        msg = StringUtil.typeFromMsg(msg);
        for (int i = 0; i < leakList.size(); i++) {
            if (leakList.get(i).getLeakType().equals(msg)) {
                return leakList.get(i).getID();
            }
        }
        return -1;
    }

    public boolean isIgnored(String appName, String msg) {

        List<DataLeak> leakList = getAppLeaks(appName);
        msg = StringUtil.typeFromMsg(msg);
        for (int i = 0; i < leakList.size(); i++) {
            if (leakList.get(i).getLeakType().equals(msg) && leakList.get(i).getIgnore() != 0) {
                return true;
            }
        }
        return false;
    }

    public int findNotificationCounter(String appName, String msg) {
        List<DataLeak> leakList = getAppLeaks(appName);
        msg = StringUtil.typeFromMsg(msg);
        for (int i = 0; i < leakList.size(); i++) {
            if (leakList.get(i).getLeakType().equals(msg)) {
                return leakList.get(i).getFrequency();
            }
        }
        return -1;
    }

    public int findGeneralNotificationId(String appName) {
        List<DataLeak> leakList = getAppLeaks(appName);
        for (int i = 0; i < leakList.size(); i++) {
            if (leakList.get(i) != null) {
                return leakList.get(i).getID();
            }
        }

        return -1;
    }
}
