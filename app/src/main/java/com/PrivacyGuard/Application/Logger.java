package com.PrivacyGuard.Application;

import android.location.Location;
import android.os.Environment;
import android.util.Log;

import com.PrivacyGuard.Application.Activities.BuildConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by frank on 9/29/14.
 */
public class Logger {
    private static final String TIME_STAMP_FORMAT = "MM-dd HH:mm:ss.SSS";
    private static final SimpleDateFormat df = new SimpleDateFormat(TIME_STAMP_FORMAT, Locale.CANADA);//TODO: auto detect locale
    private static File logFile = new File(getDiskCacheDir(),"Log");
    private static File trafficFile = new File(getDiskFileDir(), "NetworkTraffic");


    /**
     * Returns a dir  for cache storage
     *
     * @return SD storage for cash or internal storage for cash
     */
    public static File getDiskCacheDir() {
        File cacheFile = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cacheFile = PrivacyGuard.getAppContext().getExternalCacheDir();
        }
        if (cacheFile == null) {
            if (BuildConfig.DEBUG) {
                Log.d("LoggerManager", "External Cache Directory not available.");
            }
            cacheFile = PrivacyGuard.getAppContext().getCacheDir();
        }
        return cacheFile;
    }

    public static File getDiskFileDir() {
        File file = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            file = PrivacyGuard.getAppContext().getExternalFilesDir(null);
        }
        if (file == null) {
            if (BuildConfig.DEBUG) {
                Log.d("LoggerManager", "External Cache Directory not available.");
            }
            file = PrivacyGuard.getAppContext().getFilesDir();
        }
        return file;
    }

    /**
     * @param tag
     * @param msg
     * @param locations
     */
    public static void logTraffic(String tag, String msg, ArrayList<Location> locations) {
        //log network traffic ONLY in debug build
        if (BuildConfig.DEBUG) {

            //out put to terminal first
            Log.v(tag, msg);
            Log.v(tag, locations.toString());

            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(trafficFile, true)));
                out.println("Time : " + df.format(new Date()));
                out.println(" [ " + tag + " ] ");
                out.println(msg);
                for (Location loc : locations) {
                    out.println(loc.getProvider() + " : lon = " + loc.getLongitude() + ", lat = " + loc.getLatitude());
                }
                out.println("");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void logToFile(String tag, String msg) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
            out.println("Time : " + df.format(new Date()));
            out.println(" [ " + tag + " ] ");
            out.println(msg);
            out.println("");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void i(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, msg);
        } else {
            logToFile(tag, msg);
        }
    }

    // ignore debug if release
    public static void d(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, msg);
        } else {
            logToFile(tag, msg);
        }
    }

    // ignore verbose if release
    public static void v(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, msg);
        } else {
            logToFile(tag, msg);
        }
    }

}
