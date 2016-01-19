/*
 * Main activity
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

package com.PrivacyGuard.UI;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.VpnService;
import android.os.Bundle;
import android.security.KeyChain;
import android.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.PrivacyGuard.Utilities.Certificate.CertificateManager;
import com.PrivacyGuard.Utilities.Database.DataLeak;
import com.PrivacyGuard.Utilities.Database.DatabaseHandler;
import com.PrivacyGuard.Utilities.Database.LocationLeak;
import com.PrivacyGuard.Utilities.Logger;
import com.PrivacyGuard.Utilities.MyVpnService;

import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.security.cert.CertificateEncodingException;

public class MainActivity extends Activity {
    public static final String FIRST_COLUMN="First";
    public static final String SECOND_COLUMN="Second";
    public static final String THIRD_COLUMN="Third";
    public static final String FOURTH_COLUMN="Fourth";
    public static final String FIFTH_COLUMN="Fifth";
    public final static String EXTRA_DATA = "com.y59song.UI.PrivacyGuard.DATA";
    public final static String EXTRA_APP = "com.y59song.UI.PrivacyGuard.APP";
    public final static String EXTRA_SIZE = "com.y59song.UI.PrivacyGuard.SIZE";
    public final static String EXTRA_DATE_FORMAT = "com.y59song.UI.PrivacyGuard.DATE";
    private static String TAG = MainActivity.class.getSimpleName();
    private Intent intent;
    private ArrayList<HashMap<String, String>> list;
    private Button buttonConnect;
    private ListView listLeak;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = new Intent(this, MyVpnService.class);

        DatabaseHandler db = new DatabaseHandler(this);
        db.monthlyReset();

        installCertificate();

        setContentView(R.layout.activity_main);
        buttonConnect = (Button) findViewById(R.id.connect_button);
        listLeak = (ListView)findViewById(R.id.leaksList);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!MyVpnService.isRunning()) {
                    startVPN();
                }
            }
        });

        Logger.logTraffic("test", "test", new ArrayList<Location>());
        Logger.logToFile("test","test");
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateDb();

        if(MyVpnService.isRunning()){
            buttonConnect.setText(R.string.connected);
            buttonConnect.setEnabled(false);

        }else{
            Logger.w(TAG, "VPN service has stopped");
            buttonConnect.setText(R.string.connect);
            buttonConnect.setEnabled(true);
        }
    }

    /**
     *
     */
    public void populateDb () {
        // -----------------------------------------------------------------------
        // Database Fetch
        DatabaseHandler db = new DatabaseHandler(this);
        List<DataLeak> leaks = db.getAllLeaks();
        // -----------------------------------------------------------------------
        list = new ArrayList<HashMap<String,String>>();

        HashMap<String,ArrayList<Integer>> groupedList = new HashMap<String, ArrayList<Integer>>();

        for (DataLeak l : leaks) {
            if (groupedList.containsKey(l.getAppName())){
                ArrayList<Integer> previousCount = groupedList.get(l.getAppName());
                if (l.getLeakType().replace("is leaking", "").equals("Android ID")){
                    previousCount.set(1,l.getFrequency());
                } else if (l.getLeakType().replace("is leaking", "").equals("IMEI")){
                    previousCount.set(0,l.getFrequency());
                } else if (l.getLeakType().replace("is leaking", "").equals("Location")){
                    previousCount.set(2,l.getFrequency());
                } else if (l.getLeakType().replace("is leaking", "").equals("phone_number") || l.getLeakType().replace("is leaking", "").equals("email_address")){
                    previousCount.set(3,l.getFrequency());
                }

                groupedList.put(l.getAppName(), previousCount);
            } else {
                ArrayList<Integer> addCount = new ArrayList<Integer>();
                addCount.add(0);
                addCount.add(0);
                addCount.add(0);
                if (l.getLeakType().replace("is leaking", "").equals("Android ID")){
                    addCount.set(1,l.getFrequency());
                } else if (l.getLeakType().replace("is leaking", "").equals("IMEI")){
                    addCount.set(0,l.getFrequency());
                } else if (l.getLeakType().replace("is leaking", "").equals("Location")){
                    addCount.set(2,l.getFrequency());
                } else if (l.getLeakType().replace("is leaking", "").equals("phone_number") || l.getLeakType().replace("is leaking", "").equals("email_address")){
                    addCount.set(3,l.getFrequency());
                }

                groupedList.put(l.getAppName(), addCount);
            }
        }

        HashMap<String,String> header = new HashMap<String, String>();
        header.put(FIRST_COLUMN, "App Name");
        header.put(SECOND_COLUMN, "IMEI");
        header.put(THIRD_COLUMN, "Android ID");
        header.put(FOURTH_COLUMN, "Location");
        header.put(FIFTH_COLUMN, "Contact");
        list.add(header);

        Iterator it = groupedList.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            ArrayList<Integer> counters = (ArrayList)  pair.getValue();
            HashMap<String, String> temp = new HashMap<String, String>();
            temp.put(FIRST_COLUMN, pair.getKey().toString());
            temp.put(SECOND_COLUMN, counters.get(0).toString());
            temp.put(THIRD_COLUMN, counters.get(1).toString());
            temp.put(FOURTH_COLUMN, counters.get(2).toString());
            list.add(temp);
            it.remove(); // avoids a ConcurrentModificationException
        }

        ListViewAdapter adapter=new ListViewAdapter(this, list);
        listLeak.setAdapter(adapter);

        listLeak.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                if (position != 0) {
                    Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
                    DatabaseHandler db = new DatabaseHandler(MainActivity.this);

                    String appName = list.get(position).get(FIRST_COLUMN);
                    List<LocationLeak> leakList = db.getLocationLeaks(appName);

                    intent.putExtra(EXTRA_APP, appName);
                    intent.putExtra(EXTRA_SIZE, String.valueOf(leakList.size()));

                    for (int i = 0; i < leakList.size(); i++) {
                        intent.putExtra(EXTRA_DATA + i, leakList.get(i).getLocation()); // to pass values between activities
                        intent.putExtra(EXTRA_DATE_FORMAT + i, leakList.get(i).getTimeStamp());
                    }
                    startActivity(intent);
                }

            }

        });

    }

    /**
     *
     */
    public void installCertificate() {
        String Dir = this.getCacheDir().getAbsolutePath();
        try {
            if(CertificateManager.isCACertificateInstalled(Dir, MyVpnService.CAName, MyVpnService.KeyType, MyVpnService.Password))
                return;
            else CertificateManager.generateCACertificate(Dir, MyVpnService.CAName, MyVpnService.CertName, MyVpnService.KeyType, MyVpnService.Password.toCharArray());
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        Intent intent = KeyChain.createInstallIntent();
        try {
            intent.putExtra(KeyChain.EXTRA_CERTIFICATE, CertificateManager.getCACertificate(Dir, MyVpnService.CAName).getEncoded());
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        intent.putExtra(KeyChain.EXTRA_NAME, MyVpnService.CAName);
        startActivity(intent);
    }



    @Override
    // Gets called immediately before onResume() when activity is re-starting
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            Logger.i(TAG, "Starting VPN service");
            ComponentName service = startService(intent);
            if (service == null) {
                Logger.w(TAG, "Failed to start VPN service");

                //TODO: or use AppCompat.Dialog?
                // 1. Instantiate an AlertDialog.Builder with its constructor
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                // 2. Chain together various setter methods to set the dialog characteristics
                builder.setMessage(R.string.mainActivity_warn_dialog_msg)
                        .setTitle(R.string.mainActivity_warn_dialog_title)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                // 3. Get the AlertDialog from create()
                AlertDialog warnDialog = builder.create();
                warnDialog.show();
            } else {
                buttonConnect.setText(R.string.connected);
                buttonConnect.setEnabled(false);
            }
        }
    }

    private void startVPN() {//TODO: why...need to call startActivityForResult() ?
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }

    }
}
