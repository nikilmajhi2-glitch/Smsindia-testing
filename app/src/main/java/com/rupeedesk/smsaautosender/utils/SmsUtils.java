package com.rupeedesk.smsaautosender;

import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;

public class SmsUtils {
    public static boolean sendSms(Context context, String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Log.d("SmsUtils", "Sent to " + phoneNumber);
            return true;
        } catch (Exception e) {
            Log.e("SmsUtils", "Failed to send SMS: " + e.getMessage());
            return false;
        }
    }
}