package com.PrivacyGuard.Plugin;

import android.content.Context;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by frank on 23/07/14.
 */
public class ContactDetection implements IPlugin {
    //Note: this only works for north america?
    private final Pattern phone_pattern = Pattern.compile("(\\+?( |-|\\.)?\\d{1,2}( |-|\\.)?)?(\\(?\\d{3}\\)?|\\d{3})( |-|\\.)?(\\d{3}( |-|\\.)?\\d{4})");
    //Note: there is no good regex that matched all valid email, so use an simple check instead
    private final Pattern email_pattern = Pattern.compile("[\\S]+@[\\S]+");
    private final boolean DEBUG = false;

    @Override
    @Nullable
    public LeakReport handleRequest(String request) {
        ArrayList<LeakInstance> leaks = new ArrayList<>();

        //TODO: import contact
        //Matcher phone = phone_pattern.matcher(request);
        //while (phone.find()) {
        //    leaks.add(new LeakInstance("Phone Number", phone.group(0)));
        //}

       // Matcher email = email_pattern.matcher(request);
        //while (email.find()) {
        //    leaks.add(new LeakInstance("Email", email.group(0)));
        //}

        if(leaks.isEmpty()){
            return null;
        }
        LeakReport rpt = new LeakReport(LeakReport.LeakCategory.CONTACT);
        rpt.addLeaks(leaks);
        return rpt;
    }

    @Override
    public LeakReport handleResponse(String response) {
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

    }
}
