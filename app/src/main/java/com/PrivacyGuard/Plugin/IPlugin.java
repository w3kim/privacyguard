package com.PrivacyGuard.Plugin;

import android.content.Context;

/**
 * Created by frank on 2014-06-23.
 */
public interface IPlugin {
  // May modify the content of the request and response
  public LeakReport handleRequest(String request);
  public LeakReport handleResponse(String response);
  public String modifyRequest(String request);
  public String modifyResponse(String response);
  // TODO: make this method static and initialize filter only once; provide an update routine for filters like location
  public void setContext(Context context);
}
