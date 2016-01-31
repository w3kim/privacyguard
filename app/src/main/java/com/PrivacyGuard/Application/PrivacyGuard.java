package com.PrivacyGuard.Application;

import android.app.Application;
import android.content.Context;

/**
 * Created by justinhu on 16-01-15.
 */
public class PrivacyGuard extends Application {
    public final static String EXTRA_DATA = "PrivacyGuard.DATA";
    public final static String EXTRA_APP = "PrivacyGuard.APP";
    public final static String EXTRA_SIZE = "PrivacyGuard.SIZE";
    public final static String EXTRA_DATE_FORMAT = "PrivacyGuard.DATE";
    public static boolean doFilter = true;
    public static boolean asynchronous = false;
    public static int tcpForwarderWorkerRead = 0;
    public static int tcpForwarderWorkerWrite = 0;
    public static int socketForwarderWrite = 0;
    public static int socketForwarderRead = 0;

    private static Application sApplication;

    public static Application getApplication() {
        return sApplication;
    }

    public static Context getAppContext() {
        return getApplication().getApplicationContext();
    }//TODO:Nullable?

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
    }

    /*public static boolean checkLocationPermission(){
        return (ContextCompat.checkSelfPermission(getAppContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getAppContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }*/
}
