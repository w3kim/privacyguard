/*
 * Vpnservice, build the virtual network interface
 * Copyright (C) 2014  Yihang Song

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.PrivacyGuard.Application;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;

import com.PrivacyGuard.Application.Network.Forwader.ForwarderPools;
import com.PrivacyGuard.Application.Network.LocalServer;
import com.PrivacyGuard.Application.Network.Resolver.MyClientResolver;
import com.PrivacyGuard.Application.Network.Resolver.MyNetworkHostNameResolver;
import com.PrivacyGuard.Application.Network.TunReadThread;
import com.PrivacyGuard.Application.Network.TunWriteThread;
import com.PrivacyGuard.Plugin.ContactDetection;
import com.PrivacyGuard.Plugin.IPlugin;
import com.PrivacyGuard.Plugin.LocationDetection;
import com.PrivacyGuard.Plugin.PhoneStateDetection;
import com.PrivacyGuard.Application.Activities.DetailsActivity;
import com.PrivacyGuard.Application.Activities.R;
import com.PrivacyGuard.Utilities.CertificateManager;
import com.PrivacyGuard.Application.Database.DataLeak;
import com.PrivacyGuard.Application.Database.DatabaseHandler;
import com.PrivacyGuard.Application.Database.LocationLeak;
import com.PrivacyGuard.Utilities.StringUtil;

import org.sandrop.webscarab.plugin.proxy.SSLSocketFactoryFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


/**
 * Created by frank on 2014-03-26.
 */
public class MyVpnService extends VpnService implements Runnable {
    public static final String CAName = "PrivacyGuard_CA";
    public static final String CertName = "PrivacyGuard_Cert";
    public static final String KeyType = "PKCS12";
    public static final String Password = "";
    public final static String EXTRA_DATA = "com.y59song.UI.PrivacyGuard.DATA";
    public final static String EXTRA_APP = "com.y59song.UI.PrivacyGuard.APP";
    public final static String EXTRA_SIZE = "com.y59song.UI.PrivacyGuard.SIZE";
    private static final String TAG = MyVpnService.class.getSimpleName();
    private static final boolean DEBUG = true;
    private static int mId = 0;
    private static HashMap<String, Integer[]> notificationMap = new HashMap<String, Integer[]>();
    //The virtual network interface, get and return packets to it
    private ParcelFileDescriptor mInterface;
    private TunWriteThread writeThread;
    private TunReadThread readThread;
    private Thread uiThread;
    //Pools
    private ForwarderPools forwarderPools;
    //SSL stuff
    private SSLSocketFactoryFactory sslSocketFactoryFactory;
    //Network
    private MyNetworkHostNameResolver hostNameResolver;
    private MyClientResolver clientAppResolver;
    private LocalServer localServer;

    // Plugin
    private Class pluginClass[] = {LocationDetection.class, PhoneStateDetection.class, ContactDetection.class};

    // Other
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    public static boolean isRunning() {
        ActivityManager activityManager = (ActivityManager) PrivacyGuard.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(30);

        for (ActivityManager.RunningServiceInfo serviceInfo : serviceList) {
            if (serviceInfo.service.getClassName().equals(MyVpnService.class.getName()))
                return true;
        }

        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        uiThread = new Thread(this);
        uiThread.start();
        return 0;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mInterface == null) return;
        try {
            readThread.interrupt();
            writeThread.interrupt();
            localServer.interrupt();
            mInterface.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        setup_network();
        setup_workers();
        wait_to_close();
    }

    private void setup_network() {
        Builder b = new Builder();
        b.addAddress("10.8.0.1", 32);
        b.addDnsServer("8.8.8.8");
        b.addRoute("0.0.0.0", 0);
        b.setMtu(1500);
        mInterface = b.establish();
        forwarderPools = new ForwarderPools(this);
        sslSocketFactoryFactory = CertificateManager.generateCACertificate(this.getCacheDir().getAbsolutePath(), CAName,
                CertName, KeyType, Password.toCharArray());
    }

    private void setup_workers() {
        hostNameResolver = new MyNetworkHostNameResolver(this);
        clientAppResolver = new MyClientResolver(this);

        localServer = new LocalServer(this);
        localServer.start();
        readThread = new TunReadThread(mInterface.getFileDescriptor(), this);
        readThread.start();
        writeThread = new TunWriteThread(mInterface.getFileDescriptor(), this);
        writeThread.start();
    }

    private void wait_to_close() {
        // wait until all threads stop
        try {
            while (writeThread.isAlive())
                writeThread.join();

            while (readThread.isAlive())
                readThread.join();

            while (localServer.isAlive())
                localServer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void fetchResponse(byte[] response) {
        writeThread.write(response);
    }

    public SSLSocketFactoryFactory getSSlSocketFactoryFactory() {
        return sslSocketFactoryFactory;
    }

    public MyNetworkHostNameResolver getHostNameResolver() {
        return hostNameResolver;
    }

    public MyClientResolver getClientAppResolver() {
        return clientAppResolver;
    }

    public ForwarderPools getForwarderPools() {
        return forwarderPools;
    }

    public ArrayList<IPlugin> getNewPlugins() {
        ArrayList<IPlugin> ret = new ArrayList<IPlugin>();
        try {
            for (Class c : pluginClass) {
                IPlugin temp = (IPlugin) c.newInstance();
                temp.setContext(this);
                ret.add(temp);
            }
            return ret;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    ////////////////////////////////////////////////////
    // Notification Methods
    ///////////////////////////////////////////////////


    public boolean isLocation(String msg) {
        msg = StringUtil.typeFromMsg(msg);
        return msg.equals("Location");
    }

    public void notify(String appName, String msg) {
        //update database
        //if has notification, update
        //if no notification, build
        DatabaseHandler db = new DatabaseHandler(this);

        if (isLocation(msg)) {
            String location = StringUtil.locationFromMsg(msg);
            db.addLocationLeak(new LocationLeak(mId, appName, location, dateFormat.format(new Date())));
        }

        int notifyId = db.findNotificationId(appName, msg);
        if (notifyId >= 0) {     // already have this notification, update content
            int frequency = db.findNotificationCounter(appName, msg);
            db.addDataLeak(new DataLeak(notifyId, appName, msg, frequency, dateFormat.format(new Date())));
            db.close();
            updateNotification(appName, msg);
            return;
        }
        // else this is new, update database Entry
        db.addDataLeak(new DataLeak(mId, appName, msg, 1, dateFormat.format(new Date())));
        List<LocationLeak> leakList = db.getLocationLeaks(appName);


        // this is ignored, do not send notification
        if (db.isIgnored(appName, msg)) {
            return;
        }
        //Logger.d(TAG, msg);
        // build notification

        String content = msg;
        buildNotification(appName, msg, content, notifyId, db.findGeneralNotificationId(appName), leakList);
        mId++;
    }

    void buildNotification(String appName, String msg, String content, int notifyId, int generalNotifyId, List<LocationLeak> leakList) { // ignore action
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notify)
                        .setContentTitle(appName)
                        .setContentText(content)
                        .setTicker(appName + " " + msg)
                        .setAutoCancel(true);

        Intent ignoreIntent = new Intent(this, ActionReceiver.class);
        ignoreIntent.setAction("Ignore");
        ignoreIntent.putExtra("notificationId", notifyId);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), (int) System.currentTimeMillis(), ignoreIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.addAction(R.drawable.ignore, "Ignore this kind of leaks", pendingIntent);

        //TODO: Currently initiates a new activity instance each time, should recycle if already open
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, DetailsActivity.class);
        resultIntent.putExtra(EXTRA_APP, appName);
        resultIntent.putExtra(EXTRA_SIZE, String.valueOf(leakList.size()));
        for (int i = 0; i < leakList.size(); i++) {
            resultIntent.putExtra(EXTRA_DATA + i, leakList.get(i).getLocation()); // to pass values between activities
        }

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of home screen
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(DetailsActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        // builds the notification and sends it
        mNotificationManager.notify(generalNotifyId, mBuilder.build());

    }

    public void updateNotification(String appName, String msg) {
        DatabaseHandler db = new DatabaseHandler(this);
        int notifyId = db.findNotificationId(appName, msg);
        int generalNotifyId = db.findGeneralNotificationId(appName);
        List<LocationLeak> leakList = db.getLocationLeaks(appName);
        boolean ignored = db.isIgnored(appName, msg);

        if (generalNotifyId >= 0 && !ignored) {
            String content = "Number of leaks: " + db.findNotificationCounter(appName, msg);
            // Because the ID remains unchanged, the existing notification is updated.
            Logger.i(TAG, "NOTIFYID IS SUCCESSFULL" + notifyId);
            buildNotification(appName, msg, content, notifyId, db.findGeneralNotificationId(appName), leakList);
        } else {
            Logger.i(TAG, "NOTIFYID IS FAILING" + notifyId);
        }

    }

    public void deleteNotification(int id) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.cancel(id);
    }
}
