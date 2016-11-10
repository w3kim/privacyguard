package com.PrivacyGuard.Application.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Plugin.LeakInstance;
import com.PrivacyGuard.Plugin.LeakReport;

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
    private static final String TABLE_LEAK_SUMMARY = "leak_summary";

    // DataLeaks Table Columns names
    private static final String KEY_ID = "_id";
    private static final String KEY_NAME = "app_name";
    private static final String KEY_PACKAGE = "package_name";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_TYPE = "type";
    private static final String KEY_CONTENT = "content";
    private static final String KEY_TIME_STAMP = "time_stamp";

    private static final String KEY_FREQUENCY = "frequency";
    private static final String KEY_IGNORE = "ignore";

    private static final String CREATE_DATA_LEAKS_TABLE = "CREATE TABLE " + TABLE_DATA_LEAKS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_PACKAGE + " TEXT,"
            + KEY_NAME + " TEXT,"
            + KEY_CATEGORY + " TEXT,"
            + KEY_TYPE + " TEXT,"
            + KEY_CONTENT + " TEXT,"
            + KEY_TIME_STAMP + " TEXT" + ")";

    private static final String[] DATA_LEAK_TABLE_COLUMNS = new String[]{KEY_ID, KEY_PACKAGE, KEY_NAME, KEY_CATEGORY, KEY_TYPE, KEY_CONTENT, KEY_TIME_STAMP};

    private static final String CREATE_LEAK_SUMMARY_TABLE = "CREATE TABLE " + TABLE_LEAK_SUMMARY + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_PACKAGE + " TEXT,"
            + KEY_NAME + " TEXT,"
            + KEY_CATEGORY + " TEXT,"
            + KEY_FREQUENCY + " INTEGER,"
            + KEY_IGNORE + " INTEGER" + ")";

    private static final String[] LEAK_SUMMARY_TABLE_COLUMNS = new String[]{KEY_ID, KEY_PACKAGE, KEY_NAME,KEY_CATEGORY, KEY_FREQUENCY, KEY_IGNORE};


    private SQLiteDatabase mDB;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mDB = getReadableDatabase();
    }

    public String[] getTables() {
        return new String[] { TABLE_DATA_LEAKS, TABLE_LEAK_SUMMARY };
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        //create table data_leaks
        db.execSQL(CREATE_DATA_LEAKS_TABLE);
        db.execSQL(CREATE_LEAK_SUMMARY_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA_LEAKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEAK_SUMMARY);
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

        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    void resetDataLeakTable() {
        mDB.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA_LEAKS);
        mDB.execSQL(CREATE_DATA_LEAKS_TABLE);
    }

    void resetLeakSummaryTable() {
        mDB.execSQL("DROP TABLE IF EXISTS " + TABLE_LEAK_SUMMARY);
        mDB.execSQL(CREATE_LEAK_SUMMARY_TABLE);
    }

    // Adding new data leak
    private void addDataLeak(String packageName, String appName, String category, String type, String content) {
        ContentValues values = new ContentValues();
        values.put(KEY_PACKAGE, packageName); // App Name
        values.put(KEY_NAME, appName); // App Name
        values.put(KEY_CATEGORY, category);
        values.put(KEY_TYPE, type); // Leak type
        values.put(KEY_CONTENT, content);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        values.put(KEY_TIME_STAMP, dateFormat.format(new Date())); // Leak time stamp

        // Inserting Row
        mDB.insert(TABLE_DATA_LEAKS, null, values);
    }

    private void addLeakSummary(LeakReport rpt) {
        ContentValues values = new ContentValues();
        values.put(KEY_PACKAGE, rpt.packageName);
        values.put(KEY_NAME, rpt.appName);
        values.put(KEY_CATEGORY, rpt.category.name());
        values.put(KEY_FREQUENCY, 0);
        values.put(KEY_IGNORE, 0);
        mDB.insert(TABLE_LEAK_SUMMARY, null, values);
    }


    public List<AppSummary> getAllApps() {
        List<AppSummary> apps = new ArrayList<>();
        Cursor cursor = mDB.query(TABLE_LEAK_SUMMARY, new String[]{KEY_PACKAGE, KEY_NAME, "SUM(" + KEY_FREQUENCY + ")", "MIN(" + KEY_IGNORE + ")"}, null, null, KEY_PACKAGE + ", " + KEY_NAME, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    AppSummary app = new AppSummary();
                    app.packageName = cursor.getString(0);
                    app.appName = cursor.getString(1);
                    app.totalLeaks = cursor.getInt(2);
                    app.ignore = cursor.getInt(3);
                    apps.add(app);
                } while (cursor.moveToNext());

            }
            cursor.close();
        }
        return apps;
    }

    public List<CategorySummary> getAppDetail(String packageName) {
        List<CategorySummary> categories = new ArrayList<>();
        Cursor cursor = mDB.query(TABLE_LEAK_SUMMARY, new String[]{KEY_ID, KEY_CATEGORY, KEY_FREQUENCY, KEY_IGNORE}, KEY_PACKAGE + "=?", new String[]{packageName}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    int notifyId = cursor.getInt(0);
                    String category = cursor.getString(1);
                    int count = cursor.getInt(2);
                    int ignore = cursor.getInt(3);
                    categories.add(new CategorySummary(notifyId,category, count, ignore));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return categories;
    }


    public List<DataLeak> getAppLeaks(String packageName, String category) {
        List<DataLeak> leakList = new ArrayList<DataLeak>();
        Cursor cursor = mDB.query(TABLE_DATA_LEAKS, new String[]{KEY_TYPE, KEY_CONTENT, KEY_TIME_STAMP}, KEY_PACKAGE + "=? AND " + KEY_CATEGORY + "=?", new String[]{packageName, category}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    DataLeak leak = new DataLeak();
                    leak.type = cursor.getString(0);
                    leak.leakContent = cursor.getString(1);
                    leak.timestamp = cursor.getString(2);
                    leakList.add(leak);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        // return contact list
        return leakList;
    }


    public int findNotificationId(LeakReport rpt) {

        Cursor cursor = mDB.query(TABLE_LEAK_SUMMARY,
                new String[]{KEY_ID, KEY_FREQUENCY, KEY_IGNORE},
                KEY_PACKAGE + "=? AND " + KEY_CATEGORY + "=?",
                new String[]{rpt.packageName, rpt.category.name()}, null, null, null, null);

        if (cursor != null) {
            if (!cursor.moveToFirst()) { // this package(app) has no leak of this category previously
                addLeakSummary(rpt);
                cursor = mDB.query(TABLE_LEAK_SUMMARY,
                        new String[]{KEY_ID, KEY_FREQUENCY, KEY_IGNORE},
                        KEY_PACKAGE + "=? AND " + KEY_CATEGORY + "=?",
                        new String[]{rpt.packageName, rpt.category.name()}, null, null, null, null);
            }
            if (!cursor.moveToFirst()) {
                Logger.i("DBHandler", "fail to create summary table");
                cursor.close();
                return -1;
            }
            int notifyId = -1;
            int frequency = 0;
            int ignore = 0;

            notifyId = cursor.getInt(0);
            frequency = cursor.getInt(1);
            ignore = cursor.getInt(2);

            cursor.close();

            for (LeakInstance li : rpt.leaks) {
                addDataLeak(rpt.packageName, rpt.appName, rpt.category.name(), li.type, li.content);
            }
            //need to update frequency in summary table accordingly
            // Which row to update, based on the package and category
            ContentValues values = new ContentValues();
            values.put(KEY_FREQUENCY, frequency + rpt.leaks.size());

            String selection = KEY_ID + " =?";
            String[] selectionArgs = {String.valueOf(notifyId)};

            int count = mDB.update(
                    TABLE_LEAK_SUMMARY,
                    values,
                    selection,
                    selectionArgs);

            if (count == 0) {
                Logger.i("DBHandler", "fail to update summary table");
            }
            return ignore == 1 ? -1 : notifyId;
        }
        return -1;
    }


    public int findNotificationCounter(int id, String category) {
        Cursor cursor = mDB.query(TABLE_LEAK_SUMMARY,
                new String[]{KEY_ID, KEY_FREQUENCY},
                KEY_ID + "=? AND " + KEY_CATEGORY + "=?",
                new String[]{String.valueOf(id), category}, null, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int frequency = cursor.getInt(1);
                cursor.close();
                return frequency;
            }
            cursor.close();
        }

        return -1;
    }


    public void setIgnoreApp(String packageName, boolean ignore) {
        ContentValues values = new ContentValues();
        values.put(KEY_IGNORE, ignore ? 1 : 0);

        String selection = KEY_PACKAGE + " =?";
        String[] selectionArgs = {packageName};

        int count = mDB.update(
                TABLE_LEAK_SUMMARY,
                values,
                selection,
                selectionArgs);

        if (count == 0) {
            Logger.i("DBHandler", "fail to set ignore for " + packageName);
        }
    }

    public void setIgnoreAppCategory(int notifyId,boolean ignore) {
        ContentValues values = new ContentValues();
        values.put(KEY_IGNORE, ignore ? 1 : 0);

        String selection = KEY_ID + " =?";
        String[] selectionArgs = {String.valueOf(notifyId)};
        int count = mDB.update(
                TABLE_LEAK_SUMMARY,
                values,
                selection,
                selectionArgs);
        if (count == 0) {
            Logger.i("DBHandler", "fail to set ignore for " + notifyId);
        }
    }

}
