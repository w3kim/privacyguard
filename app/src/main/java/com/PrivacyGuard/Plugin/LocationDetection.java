package com.PrivacyGuard.Plugin;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import com.PrivacyGuard.UI.BuildConfig;
import com.PrivacyGuard.UI.MainActivity;
import com.PrivacyGuard.Utilities.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 2014-06-23.
 */
public class LocationDetection implements IPlugin {

  private final static String TAG = LocationDetection.class.getSimpleName();
  private LocationManager locationManager;
  private ArrayList<Double> latitudes = new ArrayList<Double>(), longitudes = new ArrayList<Double>();

  public ArrayList<Location> getLocations() {
      List<String> providers = locationManager.getAllProviders();
      ArrayList<Location> ret = new ArrayList<Location>();
      for(String provider : providers) {
          Location loc = locationManager.getLastKnownLocation(provider);
          if(loc == null) continue;
          ret.add(loc);
      }
      return ret;
  }


  @Override
  public String handleRequest(String requestStr) {
    boolean ret = false;
    String latS = "", lonS = "";
    ArrayList<Location> locations = getLocations();
    for(Location loc : locations) {
      double latD = Math.round(loc.getLatitude() * 100) / 100.0, lonD = Math.round(loc.getLongitude() * 100) / 100.0;
      latS = "" + latD;
      lonS = "" + lonD;
      Logger.d(TAG, "" + loc.getLatitude() + " " + loc.getLongitude() + " " + latS + " " + lonS);
      ret |= requestStr.contains(latS) && requestStr.contains(lonS);
      ret |= requestStr.contains(latS.replace(".", "")) && requestStr.contains(lonS.replace(".", ""));
      Logger.d(TAG, latS + " " + lonS);
    }

    String msg = ret ? "is leaking Location:" + latS + ";" + lonS : null;
    if(ret) Logger.d(TAG + "request : " + ret + " : " + requestStr.length(), requestStr);
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

}
