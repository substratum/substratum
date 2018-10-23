/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.common.analytics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static projekt.substratum.common.Systems.checkPackageSupport;

public class FirebaseAnalytics {

    public static final String NAMES_PREFS = "names";
    public static final String PACKAGES_PREFS = "prefs";
    private static FirebaseDatabase firebaseDatabase;

    public static boolean checkFirebaseAuthorized() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(
                    "https://console.firebase.google.com/").openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            return connection.getResponseCode() == HttpsURLConnection.HTTP_OK;
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
            Substratum.log(References.SUBSTRATUM_LOG, "Firebase Registration Token: " + token);
        }
        return firebaseDatabase.getReference();
    }

    public static void withdrawBlacklistedPackages(Context context, Boolean firstStart) {
        DatabaseReference database = getDatabaseReference();
        database.child("patchers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
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
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}