package com.PrivacyGuard.Application.Database;

public class AppSummary{
    public String packageName;
    public String appName;
    public int totalLeaks;
    public int ignore;

    public AppSummary(){

    }

    public AppSummary(String packageName,String appName,int totalLeaks){
        this.packageName = packageName;
        this.appName = appName;
        this.totalLeaks = totalLeaks;
    }

}
