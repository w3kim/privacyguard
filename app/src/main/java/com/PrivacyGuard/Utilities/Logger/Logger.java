package com.PrivacyGuard.Utilities.Logger;

import android.location.Location;
import android.util.Log;

import com.PrivacyGuard.UI.BuildConfig;

import java.io.*;
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

    private String packageName;
    private File file;

    /**
     * @param packageName
     * @param dir
     */
    public Logger(String packageName, String dir) {
        this.packageName = packageName;
        this.file = new File(dir + File.separator + packageName);
    }

    public void logToFile(String tag, String msg) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            out.println("Time : " + df.format(new Date()));
            out.println(" [ " + tag + " ] ");
            out.println(msg);
            out.println("");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logToFile(String tag, String msg, ArrayList<Location> locations) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
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

    public void i(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, msg);
        } else {
            logToFile(tag, msg);
        }
    }

    // ignore debug if release
    public void d(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg);
        }
    }

    public void e(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, msg);
        } else {
            logToFile(tag, msg);
        }
    }

    // ignore verbose if release
    public void v(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, msg);
        }
    }

    public void w(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, msg);
        } else {
            logToFile(tag, msg);
        }
    }

    public String getPackageName() {
        return this.packageName;
    }
}
