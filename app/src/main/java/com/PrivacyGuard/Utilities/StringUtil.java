package com.PrivacyGuard.Utilities;

/**
 * Created by justinhu on 16-01-19.
 */
public class StringUtil {

    public static String EmptyString = "";

    public static String typeFromMsg(String msg) {
        msg = msg.replace("is leaking", "");
        int endIndex = msg.lastIndexOf(":");
        if (endIndex == -1) {
            return EmptyString;
        }
        msg = msg.substring(0, endIndex - 1);
        msg = msg.trim();
        return msg;
    }

    public static String locationFromMsg(String msg) {
        msg = msg.replace("is leaking", "");
        int endIndex = msg.lastIndexOf(":");
        if (endIndex == -1) {
            return EmptyString;
        }
        return msg.substring(endIndex + 1);
    }
}
