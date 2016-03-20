package com.PrivacyGuard.Application.Database;

import com.PrivacyGuard.Utilities.StringUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DataLeak {
    //private variables
    //int notifyId;
    //String packageName;
    //String appName;
    //String category;
    public String type;
    public String leakContent;
    public String timestamp;
    // Empty constructor
    public DataLeak(){
    }


    public DataLeak( String type, String content,String timestamp){
        this.type = type;
        this.leakContent = content;
        this.timestamp = timestamp;
    }


}
