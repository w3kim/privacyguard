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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.security.cert.Certificate;
import javax.security.cert.CertificateEncodingException;

public class MainActivity extends Activity {

    public static final boolean debug = false;
    public static final int REQUEST_CERT = 1;
    private static String TAG = "MainActivity";
    private ArrayList<HashMap<String, String>> list;
private boolean keyChainInstalled = false;

    private ToggleButton buttonConnect;
    private ListView listLeak;
    private MainListViewAdapter adapter;

    ServiceConnection mSc;
    MyVpnService mVPN;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonConnect = (ToggleButton) findViewById(R.id.connect_button);
        listLeak = (ListView) findViewById(R.id.leaksList);

        buttonConnect.setChecked(false);
        buttonConnect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Logger.d(TAG, "Connect toggled ON");
                    if(!keyChainInstalled){
                        installCertificate();
                    }else{
                        startVPN();
                    }
                } else {
                    Logger.d(TAG, "Connect toggled OFF");
                    stopVPN();
                }
            }
        });


        mSc = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Logger.d(TAG, "VPN Service connected");
                mVPN = ((MyVpnServiceBinder) service).getService();
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
        Logger.d(TAG, this.getApplicationContext().getPackageCodePath());
        Intent service = new Intent(this.getApplicationContext(), MyVpnService.class);
        this.bindService(service, mSc, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        populateLeakList();

    }

    @Override
    protected void onStop() {
        super.onStop();
        //must unbind the service otherwise the ServiceConnection will be leaked.
        this.unbindService(mSc);
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
        boolean certInstalled = CertificateManager.isCACertificateInstalled(MyVpnService.CADir, MyVpnService.CAName, MyVpnService.KeyType, MyVpnService.Password.toCharArray());
        if (keyChainInstalled && certInstalled)
            return;
        if(!certInstalled) {
            CertificateManager.initiateFactory(MyVpnService.CADir, MyVpnService.CAName, MyVpnService.CertName, MyVpnService.KeyType, MyVpnService.Password.toCharArray());
        }
        Intent intent = KeyChain.createInstallIntent();
        try {
            Certificate cert = CertificateManager.getCACertificate(MyVpnService.CADir, MyVpnService.CAName);
            if(cert != null){
                intent.putExtra(KeyChain.EXTRA_CERTIFICATE, cert.getEncoded());
                intent.putExtra(KeyChain.EXTRA_NAME, MyVpnService.CAName);
                startActivityForResult(intent, REQUEST_CERT);
            }
        } catch (CertificateEncodingException e) {
            Logger.e(TAG,"Certificate Encoding Error",e);
        }

    }

    /**
     * Gets called immediately before onResume() when activity is re-starting
     */
    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if(request == REQUEST_CERT){
            keyChainInstalled = result == RESULT_OK;
            if(keyChainInstalled){
                startVPN();
            }else{
                buttonConnect.setChecked(false);
            }
        }else if (result == RESULT_OK) {
            Logger.d(TAG, "Starting VPN service");
            mVPN.startVPN(this);
        }
    }


    private void startVPN() {
        /**
         * prepare() sometimes would misbehave:
         * https://code.google.com/p/android/issues/detail?id=80074
         *
         * need to inform user
         */
        Intent intent = VpnService.prepare(this);
        Logger.d(TAG, "VPN prepare done");
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    private void stopVPN() {
        Logger.d(TAG, "Stopping VPN service");
        mVPN.stopVPN();
    }
}
