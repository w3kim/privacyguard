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
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Environment;
import android.security.KeyChain;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ToggleButton;

import com.PrivacyGuard.Application.Database.AppSummary;
import com.PrivacyGuard.Application.Database.DatabaseHandler;
import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Application.MyVpnService;
import com.PrivacyGuard.Application.PrivacyGuard;
import com.PrivacyGuard.Plugin.KeywordDetection;
import com.PrivacyGuard.Utilities.CertificateManager;
import com.PrivacyGuard.Utilities.FileChooser;
import com.PrivacyGuard.Utilities.FileUtils;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import javax.security.cert.CertificateEncodingException;

public class MainActivity extends Activity {

    public static final boolean debug = false;
    private static String TAG = "MainActivity";
    private Intent intent;
    private ArrayList<HashMap<String, String>> list;

    private ToggleButton buttonConnect;
    //private Switch asyncSwitch;
    private ListView listLeak;
    private MainListViewAdapter adapter;
    private DatabaseHandler mDbHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = new Intent(this, MyVpnService.class);

        if (!MyVpnService.isRunning()) {
            startVPN();
        }


        mDbHandler = new DatabaseHandler(this);
        mDbHandler.monthlyReset();

        installCertificate();

        setContentView(R.layout.activity_main);
        buttonConnect = (ToggleButton) findViewById(R.id.connect_button);
        listLeak = (ListView) findViewById(R.id.leaksList);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (PrivacyGuard.doFilter) {
            buttonConnect.setChecked(true);
        }
        populateLeakList();
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

    @Override
    // Gets called immediately before onResume() when activity is re-starting
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            Logger.d(TAG, "Starting VPN service");
            ComponentName service = startService(intent);
            if (service == null) {
                Logger.w(TAG, "Failed to start VPN service");
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.mainActivity_warn_dialog_msg)
                        .setTitle(R.string.mainActivity_warn_dialog_title)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog warnDialog = builder.create();
                warnDialog.show();
            } else {
                Logger.d(TAG, "VPN service started");
            }
        }
    }

    private void startVPN() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    private void stopVPN() {
        // w3kim: MyVpnService does not properly respond to stop signal
        stopService(new Intent(this, MyVpnService.class));
    }

    public void updateFilterKeywords(View view) {
        new FileChooser(this).setFileListener(new FileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(final File file) {
                Log.e(TAG, file.getAbsolutePath());
                File keywords = new File(getFilesDir().getAbsolutePath() + "/keywords.txt");
                if (keywords.exists()) {
                    keywords.delete();
                }
                FileUtils.copyFile(file, keywords.getAbsolutePath());
                KeywordDetection.invalidate();
            }
        }).showDialog();
    }

    /**
     * [w3kim@uwaterloo.ca]
     * Toggle the VPN
     *
     * Note that this method does not function as expected since `MyVpnService` does not correc
     *
     * @param view
     */
    public void toggleVPN(View view) {
        ToggleButton toggle = (ToggleButton) view;
        String value = toggle.getText().toString();
        if (value.equalsIgnoreCase("on")) {
            Log.d(TAG, "on");
            if (!MyVpnService.isRunning()) startVPN();
            PrivacyGuard.doFilter = true;
        } else {
            Log.d(TAG, "off");
            if (MyVpnService.isRunning()) stopVPN();
            PrivacyGuard.doFilter = false;
        }
    }

    /**
     * [w3kim@uwaterloo.ca]
     * Export DB contents to CSV files
     *
     * @param view UI view triggering this method
     */
    public void exportData(View view) {
        File exportDir = new File(Environment.getExternalStorageDirectory(), "privacyguard");
        if (!exportDir.exists()) {
            if (!exportDir.mkdirs()) {
                Log.e(TAG, "cannot create directories: " + exportDir.getAbsolutePath());
            }
        }

        long timestamp = System.currentTimeMillis();
        for (String table : mDbHandler.getTables()) {
            File file = new File(exportDir,
                    String.format("pg-export-%s-%s.csv", timestamp, table));
            try {
                file.createNewFile();
                CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
                SQLiteDatabase db = mDbHandler.getReadableDatabase();
                Cursor curCSV = db.rawQuery("SELECT * FROM " + table, null);
                csvWrite.writeNext(curCSV.getColumnNames());
                while (curCSV.moveToNext()) {
                    //Which column you want to exprort
                    int numColumns = curCSV.getColumnCount();
                    String[] arrStr = new String[numColumns];
                    for (int i = 0; i < numColumns; i++) {
                        arrStr[i] = curCSV.getString(i);
                    }
                    csvWrite.writeNext(arrStr);
                }
                csvWrite.close();
                curCSV.close();

                Log.d(TAG, String.format("table '%s' has been exported to '%s'", table, file.getAbsolutePath()));
            } catch (Exception sqlEx) {
                Log.e(TAG, sqlEx.getMessage(), sqlEx);
            }
        }
    }
}
