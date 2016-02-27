package com.PrivacyGuard.Plugin;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.PrivacyGuard.Application.Logger;

import java.util.HashMap;
import java.util.List;

/**
 * Created by frank on 2014-06-23.
 */
public class LocationDetection implements IPlugin {
    private final static String TAG = LocationDetection.class.getSimpleName();
    private final static boolean DEBUG = true;

    private static long MIN_TIME_INTERVAL_PASSIVE = 60000; //one minute
    private static long MIN_TIME_INTERVAL_ACTIVE = 300000; //five minute
    private static float MIN_DISTANCE_INTERVAL = 10; // 10 meters

    private static LocationManager mLocationManager;
    private static HashMap<String, Location> mLocations = new HashMap<String, Location>();
    private static long mLastUpdate;

    @Override
    public String handleRequest(String requestStr) {
        boolean ret = false;
        updateLocations();
        for (Location loc : mLocations.values()) {
            double latD = Math.round(loc.getLatitude() * 10) / 10.0;
            double lonD = Math.round(loc.getLongitude() * 10) / 10.0;
            String latS = "" + latD, lonS = "" + lonD;
            ret |= requestStr.contains(latS) && requestStr.contains(lonS);
            ret |= requestStr.contains(latS.replace(".", "")) && requestStr.contains(lonS.replace(".", ""));

            latD = ((int) (loc.getLatitude() * 10)) / 10.0;
            lonD = ((int) (loc.getLongitude() * 10)) / 10.0;
            latS = "" + latD;
            lonS = "" + lonD;
            ret |= requestStr.contains(latS) && requestStr.contains(lonS);
            ret |= requestStr.contains(latS.replace(".", "")) && requestStr.contains(lonS.replace(".", ""));
        }

        String msg = ret ? "is leaking location" : null;
        if (DEBUG & ret) Log.d(TAG + "request : " + ret + " : " + requestStr.length(), requestStr);
        return msg;
    }

    private void updateLocations() {
        long now = System.currentTimeMillis();
        if ((now - mLastUpdate) > MIN_TIME_INTERVAL_ACTIVE) {    // more than 5 min since last known location
            updateLastLocations();
            mLastUpdate = now;
            Logger.logLastLocations(mLocations, false);
        }
    }

    @Override
    public String handleResponse(String responseStr) {
        return null;
    }

    @Override
    public String modifyRequest(String request) {
        return request;
    }

    @Override
    public String modifyResponse(String response) {
        return response;
    }

    @Override
    public void setContext(Context context) {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, MIN_TIME_INTERVAL_PASSIVE, MIN_DISTANCE_INTERVAL, new LocationUpdateListener(), Looper.getMainLooper());
            updateLastLocations();
            Logger.logLastLocations(mLocations, true);
            mLastUpdate = System.currentTimeMillis();
        }
    }

    public void updateLastLocations() {
        List<String> providers = mLocationManager.getAllProviders();
        for (String provider : providers) {
            Location loc = mLocationManager.getLastKnownLocation(provider);
            if (loc == null) continue;
            mLocations.put(loc.getProvider(), loc);
        }
    }


    class LocationUpdateListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            mLocations.put(loc.getProvider(), loc);
            mLastUpdate = System.currentTimeMillis();
            Logger.logLastLocation(loc);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

}
