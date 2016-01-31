package com.PrivacyGuard.Plugin;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by frank on 2014-06-23.
 */
public class LocationDetection implements IPlugin, LocationListener {
  private final static String TAG = LocationDetection.class.getSimpleName();
    private final boolean DEBUG = false;
  private LocationManager locationManager;
  private ArrayList<Double> latitudes = new ArrayList<Double>(), longitudes = new ArrayList<Double>();
    private HashMap<String, Location> locations = new HashMap<String, Location>();

    public HashMap<String, Location> getLocations() {
      List<String> providers = locationManager.getAllProviders();
      for(String provider : providers) {
          Location loc = locationManager.getLastKnownLocation(provider);
          if(loc == null) continue;
          locations.put(provider, loc);
          locationManager.requestLocationUpdates(provider, 0, 0, this);
      }
        return locations;
  }


  @Override
  public String handleRequest(String requestStr) {
    boolean ret = false;

      if (locations == null) getLocations();
      for (Location loc : locations.values()) {
          double latD = Math.round(loc.getLatitude() * 10) / 10.0, lonD = Math.round(loc.getLongitude() * 10) / 10.0;
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
      if (DEBUG && ret) Log.d(TAG + "request : " + ret + " : " + requestStr.length(), requestStr);
    return msg;
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
    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
  }


    // Location Listener
    @Override
    public void onLocationChanged(Location location) {
        locations.put(location.getProvider(), location);
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
