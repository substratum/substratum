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

package projekt.substratum.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebase.client.Firebase;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.HashSet;

@SuppressWarnings("AccessStaticViaInstance")
public class FirebaseAnalytics {

    private static FirebaseDatabase mDatabase;

    public static FirebaseDatabase getDatabase() {
        if (mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance();
            mDatabase.setPersistenceEnabled(true);
            printFCMtoken();
        }
        return mDatabase;
    }

    public static void printFCMtoken() {
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(References.SUBSTRATUM_LOG, "FCM Registration Token: " + token);
    }

    @SuppressWarnings("unchecked")
    static void withdrawBlacklistedPackages(Context context) {
        Firebase.setAndroidContext(context);
        DatabaseReference database = getDatabase().getInstance().getReference();
        database.child("patchers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                ArrayList<String> listOfPackages = new ArrayList<>();
                String data = dataSnapshot.getValue().toString();
                String[] dataArr = data.substring(1, data.length() - 1).split(",");
                for (String aDataArr : dataArr) {
                    String entry = aDataArr.split("=")[1];
                    listOfPackages.add(entry);
                }

                HashSet set = new HashSet();
                set.addAll(listOfPackages);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                prefs.edit().putStringSet("blacklisted_packages", set).apply();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @SuppressWarnings("unchecked")
    static void withdrawNames(Context context) {
        Firebase.setAndroidContext(context);
        DatabaseReference database = getDatabase().getInstance().getReference();
        database.child("blacklisted").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                ArrayList<String> listOfPackages = new ArrayList<>();
                String data = dataSnapshot.getValue().toString();
                String[] dataArr = data.substring(1, data.length() - 1).split(",");
                for (String aDataArr : dataArr) {
                    String entry = aDataArr.split("=")[1];
                    listOfPackages.add(entry);
                }

                HashSet set = new HashSet();
                set.addAll(listOfPackages);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                prefs.edit().putStringSet("blacklisted_names", set).apply();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
}