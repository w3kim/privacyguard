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

package com.PrivacyGuard.Application.Activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.security.KeyChain;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ToggleButton;

import com.PrivacyGuard.Application.Database.AppSummary;
import com.PrivacyGuard.Application.Database.DatabaseHandler;
import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.Network.FakeVPN.MyVpnService;
import com.PrivacyGuard.Application.Network.FakeVPN.MyVpnService.MyVpnServiceBinder;
import com.PrivacyGuard.Application.PrivacyGuard;
import com.PrivacyGuard.Utilities.CertificateManager;

import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.security.cert.CertificateEncodingException;

public class MainActivity extends Activity {

    //public static final boolean debug = false;
    private static String TAG = "MainActivity";
    private static final int REQUEST_VPN = 1;
    private ArrayList<HashMap<String, String>> list;

    private ToggleButton buttonConnect;
    private ListView listLeak;
    private MainListViewAdapter adapter;

    private boolean bounded = false;
    ServiceConnection mSc;
    MyVpnService mVPN;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonConnect = (ToggleButton) findViewById(R.id.connect_button);
        listLeak = (ListView) findViewById(R.id.leaksList);

        buttonConnect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Logger.d(TAG, "Connect toggled "+isChecked);
                if (isChecked && ! MyVpnService.isRunning()) {
                    Logger.d(TAG, "Connect toggled ON");
                    startVPN();
                } else if(!isChecked){
                    Logger.d(TAG, "Connect toggled OFF");
                    stopVPN();
                }
            }
        });


        /** use bound service here because stopservice() doesn't immediately trigger onDestroy of VPN service */
        mSc = new ServiceConnection(){

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Logger.d(TAG, "VPN Service connected");
                mVPN = ((MyVpnServiceBinder)service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Logger.d(TAG, "VPN Service disconnected");
            }
        };

        DatabaseHandler db = new DatabaseHandler(this);
        db.monthlyReset();
        installCertificate();
    }




    @Override
    protected void onStart() {
        super.onStart();
        if(!bounded){
            Intent service = new Intent(this,MyVpnService.class);
            this.bindService(service, mSc, Context.BIND_AUTO_CREATE);
            bounded = true;
        }
        buttonConnect.setChecked(MyVpnService.isRunning());


    }

    @Override
    protected void onResume() {
        super.onResume();
        populateLeakList();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if(bounded){//must unbind the service otherwise the ServiceConnection will be leaked.
            this.unbindService(mSc);
            bounded = false;
        }


    }



    /**
     *
     */
    public void populateLeakList() {
        // -----------------------------------------------------------------------
        // Database Fetch
        DatabaseHandler db = new DatabaseHandler(this);
        List<AppSummary> apps = db.getAllApps();
        db.close();

        if (apps == null) {
            return;
        }
        if (adapter == null) {
            adapter = new MainListViewAdapter(this, apps);
            listLeak.setAdapter(adapter);
            listLeak.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(MainActivity.this, AppSummaryActivity.class);

                    AppSummary app = (AppSummary) parent.getItemAtPosition(position);

                    intent.putExtra(PrivacyGuard.EXTRA_PACKAGE_NAME, app.packageName);
                    intent.putExtra(PrivacyGuard.EXTRA_APP_NAME, app.appName);
                    intent.putExtra(PrivacyGuard.EXTRA_IGNORE, app.ignore);

                    startActivity(intent);
                }
            });
        } else {
            adapter.updateData(apps);
        }
    }

    /**
     *
     */
    public void installCertificate() {
        String Dir = Logger.getDiskCacheDir().getAbsolutePath();
        try {
            if (CertificateManager.isCACertificateInstalled(Dir, MyVpnService.CAName, MyVpnService.KeyType, MyVpnService.Password))
                return;
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        CertificateManager.generateCACertificate(Dir, MyVpnService.CAName, MyVpnService.CertName, MyVpnService.KeyType, MyVpnService.Password.toCharArray());
        Intent intent = KeyChain.createInstallIntent();
        try {
            intent.putExtra(KeyChain.EXTRA_CERTIFICATE, CertificateManager.getCACertificate(Dir, MyVpnService.CAName).getEncoded());
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        }
        intent.putExtra(KeyChain.EXTRA_NAME, MyVpnService.CAName);
        startActivity(intent);
    }

    /** Gets called immediately before onResume() when activity is re-starting */
    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if(request == REQUEST_VPN){
            if (result == RESULT_OK) {
                Logger.d(TAG, "Starting VPN service");
                mVPN.startVPN(this);
            }else{
                buttonConnect.setChecked(false);    // update UI in case user doesn't give consent to VPN
            }
        }

    }



    private void startVPN() {
        if(!bounded){
            Intent service = new Intent(this,MyVpnService.class);
            this.bindService(service, mSc, Context.BIND_AUTO_CREATE);
            bounded = true;
        }
        /**
         * prepare() sometimes would misbehave:
         * https://code.google.com/p/android/issues/detail?id=80074
         *
         * if this affects our app, we can let vpnservice update main activity for status
         * http://stackoverflow.com/questions/4111398/notify-activity-from-service
         *
         */
        Intent intent = VpnService.prepare(this);
        Logger.d(TAG, "VPN prepare done");
        if (intent != null) {
            startActivityForResult(intent, REQUEST_VPN);
        } else {
            onActivityResult(REQUEST_VPN, RESULT_OK, null);
        }
    }

    private void stopVPN() {
        Logger.d(TAG, "Stopping VPN service");
        if(bounded){
            this.unbindService(mSc);
            bounded = false;
        }
        mVPN.stopVPN();
    }
}
