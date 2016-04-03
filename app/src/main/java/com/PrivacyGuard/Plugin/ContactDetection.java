package com.PrivacyGuard.Plugin;

import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.provider.ContactsContract;

import com.PrivacyGuard.Application.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by frank on 23/07/14.
 */
public class ContactDetection implements IPlugin {
    private final String TAG = "ContactDetection";
    //Note: this only works for north america?
    private final Pattern phone_pattern = Pattern.compile("(\\+?( |-|\\.)?\\d{1,2}( |-|\\.)?)?(\\(?\\d{3}\\)?|\\d{3})( |-|\\.)?(\\d{3}( |-|\\.)?\\d{4})");
    //Note: there is no good regex that matched all valid email, so use an simple check instead
    private final Pattern email_pattern = Pattern.compile("[\\S]+@[\\S]+");
    private final boolean DEBUG = false;

    private static final HashSet<String> emailList = new HashSet<>();
    private static final HashSet<String> phoneList = new HashSet<>();


    @Override
    @Nullable
    public LeakReport handleRequest(String request) {
        ArrayList<LeakInstance> leaks = new ArrayList<>();

        //TODO: import contact
        Matcher phone = phone_pattern.matcher(request);
        while (phone.find()) {
            String phoneNumber = phone.group(0).replaceAll("\\D+", "");
            if(phoneList.contains(phoneNumber)){
                leaks.add(new LeakInstance("Phone Number", phoneNumber));
            }
        }

        Matcher email = email_pattern.matcher(request);
        while (email.find()) {
            String emailAddress = email.group(0);
            if(emailList.contains(emailAddress)){
                leaks.add(new LeakInstance("Email",emailAddress ));
            }
        }

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
        getNumber(context.getContentResolver());
        getEmail(context.getContentResolver());
    }


    public void getNumber(ContentResolver cr)
    {
        Cursor phones = null;
        try {
            phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            int phoneNumberIdx = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            if(phones.moveToFirst()){
                do {
                    String phoneNumber = phones.getString(phoneNumberIdx);
                    phoneList.add(phoneNumber.replaceAll("\\D+",""));
                } while (phones.moveToNext());
            }
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
        } finally {
            if (phones != null) {
                phones.close();
            }
        }
    }
    public void getEmail(ContentResolver cr)
    {
        Cursor emails = null;
        try {
            emails = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null);
            int emailAddressIdx = emails.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);

            if(emails.moveToFirst()){
                do {
                    String phoneNumber = emails.getString(emailAddressIdx);
                    emailList.add(phoneNumber);
                } while (emails.moveToNext());
            }

        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
        } finally {
            if (emails != null) {
                emails.close();
            }
        }
    }

}
