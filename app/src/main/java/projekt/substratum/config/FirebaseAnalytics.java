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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.HashSet;

@SuppressWarnings("AccessStaticViaInstance")
class FirebaseAnalytics {

    private static FirebaseDatabase mDatabase;
    private static SharedPreferences mPrefs;

    private static DatabaseReference getDatabaseReference(Context context) {
        if (mDatabase == null) {
            Firebase.setAndroidContext(context);
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            mDatabase = FirebaseDatabase.getInstance();
            mDatabase.setPersistenceEnabled(true);
            String token = FirebaseInstanceId.getInstance().getToken();
            Log.d(References.SUBSTRATUM_LOG, "Firebase Registration Token: " + token);
        }
        return mDatabase.getReference();
    }

    @SuppressWarnings("unchecked")
    static void withdrawBlacklistedPackages(Context context) {
        DatabaseReference database = getDatabaseReference(context);
        database.child("patchers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mPrefs.edit().remove("blacklisted_packages").apply();
                ArrayList<String> listOfPackages = new ArrayList<>();
                String data = dataSnapshot.getValue().toString();
                String[] dataArr = data.substring(1, data.length() - 1).split(",");
                for (String aDataArr : dataArr) {
                    String entry = aDataArr.split("=")[1];
                    listOfPackages.add(entry);
                }

                HashSet set = new HashSet();
                set.addAll(listOfPackages);
                mPrefs.edit().putStringSet("blacklisted_packages", set).apply();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @SuppressWarnings("unchecked")
    static void withdrawNames(Context context) {
        Firebase.setAndroidContext(context);
        DatabaseReference database = getDatabaseReference(context);
        database.child("blacklisted").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mPrefs.edit().remove("blacklisted_names").apply();
                ArrayList<String> listOfPackages = new ArrayList<>();
                String data = dataSnapshot.getValue().toString();
                String[] dataArr = data.substring(1, data.length() - 1).split(",");
                for (String aDataArr : dataArr) {
                    String entry = aDataArr.split("=")[1];
                    listOfPackages.add(entry);
                }

                HashSet set = new HashSet();
                set.addAll(listOfPackages);
                mPrefs.edit().putStringSet("blacklisted_names", set).apply();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
}