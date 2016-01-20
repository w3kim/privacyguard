package com.PrivacyGuard;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.PrivacyGuard.Utilities.Database.DataLeak;
import com.PrivacyGuard.Utilities.Database.DatabaseHandler;

/**
 * Created by MAK on 20/10/2015.
 */
public class ActionReceiver extends BroadcastReceiver {
    public ActionReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int notifyId = intent.getIntExtra("notificationId",0);

        // cancel notification
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // somehow get an instance of the calling MyVpnService, and invoke the setIgnored method
        manager.cancel(notifyId);

        DatabaseHandler db = new DatabaseHandler(context);
        DataLeak leak = db.getLeak(notifyId);
        leak.setIgnore(1);
        db.updateLeak(leak);
    }
}
