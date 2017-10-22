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
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import projekt.substratum.common.References;

public enum FirebaseAnalytics {
    ;

    public static final String NAMES_PREFS = "names";
    public static final String PACKAGES_PREFS = "prefs";
    private static FirebaseDatabase mDatabase;

    @SuppressLint("MissingFirebaseInstanceTokenRefresh")
    private static DatabaseReference getDatabaseReference() {
        if (mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance();
            mDatabase.setPersistenceEnabled(true);
            final String token = FirebaseInstanceId.getInstance().getToken();
            Log.d(References.SUBSTRATUM_LOG, "Firebase Registration Token: " + token);
        }
        return mDatabase.getReference();
    }

    @SuppressWarnings("unchecked")
    public static void withdrawBlacklistedPackages(final Context context) {
        final DatabaseReference database = getDatabaseReference();
        database.child("patchers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                final SharedPreferences.Editor editor = context
                        .getSharedPreferences(PACKAGES_PREFS, Context.MODE_PRIVATE).edit();
                editor.clear();
                final Object dataValue = dataSnapshot.getValue();
                if (dataValue != null) {
                    final String data = dataValue.toString();
                    final String[] dataArr = data.substring(1, data.length() - 1).split(",");
                    final Collection<String> listOfPackages = new ArrayList<>();
                    for (final String aDataArr : dataArr) {
                        final String entry = aDataArr.split("=")[1];
                        listOfPackages.add(entry);
                    }

                    final Set set = new HashSet();
                    set.addAll(listOfPackages);
                    final SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy", Locale.US);
                    editor.putStringSet(dateFormat.format(new Date()), set);
                    editor.apply();
                }
            }

            @Override
            public void onCancelled(final DatabaseError databaseError) {
            }
        });
    }

    public static void withdrawAndromedaFingerprint(final Context context, final int version) {
        final SharedPreferences prefs = context
                .getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
        if (!prefs.contains("andromeda_exp_fp_" + version)) {
            final SharedPreferences.Editor editor = prefs.edit();
            for (final Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                if (entry.getKey().startsWith("andromeda_fp_")) {
                    editor.remove(entry.getKey());
                }
            }
            final DatabaseReference database = getDatabaseReference();
            final String prefKey = "andromeda_exp_fp_" + version;
            database.child("andromeda-fp")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(final DataSnapshot dataSnapshot) {
                            final Object dataValue = dataSnapshot.child(String.valueOf(version))
                                    .getValue();
                            if (dataValue != null) {
                                final String hash = dataValue.toString();
                                editor.putString(prefKey, hash).apply();
                            }
                        }

                        @Override
                        public void onCancelled(final DatabaseError databaseError) {
                        }
                    });
        }
    }

    public static void withdrawSungstratumFingerprint(final Context context, final int version) {
        final SharedPreferences prefs = context
                .getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
        if (!prefs.contains("sungstratum_exp_fp_" + version)) {
            final SharedPreferences.Editor editor = prefs.edit();
            for (final Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                if (entry.getKey().startsWith("sungstratum_exp_fp_")) {
                    editor.remove(entry.getKey());
                }
            }
            final DatabaseReference database = getDatabaseReference();
            final String prefKey = "sungstratum_exp_fp_" + version;
            database.child("sungstratum-fp")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(final DataSnapshot dataSnapshot) {
                            final Object dataValue = dataSnapshot.child(String.valueOf(version))
                                    .getValue();
                            if (dataValue != null) {
                                final String hash = dataValue.toString();
                                editor.putString(prefKey, hash).apply();
                            }
                        }

                        @Override
                        public void onCancelled(final DatabaseError databaseError) {
                        }
                    });
        }
    }
}