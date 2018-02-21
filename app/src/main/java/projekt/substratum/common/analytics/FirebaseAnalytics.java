/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.common.analytics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import projekt.substratum.common.References;

import static projekt.substratum.common.Systems.checkPackageSupport;

public enum FirebaseAnalytics {
    ;

    public static final String NAMES_PREFS = "names";
    public static final String PACKAGES_PREFS = "prefs";
    private static FirebaseDatabase firebaseDatabase;

    public static boolean checkFirebaseAuthorized() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(
                    "https://console.firebase.google.com/").openConnection();
            connection.setRequestMethod("HEAD");
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception ignored) {
        }
        return false;
    }

    @SuppressLint("MissingFirebaseInstanceTokenRefresh")
    private static DatabaseReference getDatabaseReference() {
        if (firebaseDatabase == null) {
            firebaseDatabase = FirebaseDatabase.getInstance();
            firebaseDatabase.setPersistenceEnabled(true);
            String token = FirebaseInstanceId.getInstance().getToken();
            Log.d(References.SUBSTRATUM_LOG, "Firebase Registration Token: " + token);
        }
        return firebaseDatabase.getReference();
    }

    public static void withdrawBlacklistedPackages(Context context, Boolean firstStart) {
        DatabaseReference database = getDatabaseReference();
        database.child("patchers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                SharedPreferences.Editor editor = context
                        .getSharedPreferences(PACKAGES_PREFS, Context.MODE_PRIVATE).edit();
                editor.clear();
                Object dataValue = dataSnapshot.getValue();
                if (dataValue != null) {
                    String data = dataValue.toString();
                    String[] dataArr = data.substring(1, data.length() - 1).split(",");
                    Collection<String> listOfPackages = new ArrayList<>();
                    for (String aDataArr : dataArr) {
                        String entry = aDataArr.split("=")[1];
                        listOfPackages.add(entry);
                    }

                    Set<String> set = new HashSet<>(listOfPackages);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy", Locale.US);
                    editor.putStringSet(dateFormat.format(new Date()), set);
                    editor.apply();

                    if (firstStart) checkPackageSupport(context, true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    public static void withdrawAndromedaFingerprint(Context context,
                                                    int version) {
        SharedPreferences prefs = context
                .getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
        if (!prefs.contains("andromeda_exp_fp_" + version)) {
            SharedPreferences.Editor editor = prefs.edit();
            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                if (entry.getKey().startsWith("andromeda_fp_")) {
                    editor.remove(entry.getKey());
                }
            }
            DatabaseReference database = getDatabaseReference();
            String prefKey = "andromeda_exp_fp_" + version;
            database.child("andromeda-fp")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Object dataValue = dataSnapshot.child(String.valueOf(version))
                                    .getValue();
                            if (dataValue != null) {
                                editor.putString(prefKey, dataValue.toString()).apply();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
        }
    }

    public static void withdrawSungstratumFingerprint(Context context,
                                                      int version) {
        SharedPreferences prefs = context
                .getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
        if (!prefs.contains("sungstratum_exp_fp_" + version)) {
            SharedPreferences.Editor editor = prefs.edit();
            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                if (entry.getKey().startsWith("sungstratum_exp_fp_")) {
                    editor.remove(entry.getKey());
                }
            }
            DatabaseReference database = getDatabaseReference();
            String prefKey = "sungstratum_exp_fp_" + version;
            database.child("sungstratum-fp")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Object dataValue = dataSnapshot.child(String.valueOf(version))
                                    .getValue();
                            if (dataValue != null) {
                                editor.putString(prefKey, dataValue.toString()).apply();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
        }
    }
}