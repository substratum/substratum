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

package projekt.substratum.tabs;


import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import projekt.substratum.BuildConfig;
import projekt.substratum.R;
import projekt.substratum.adapters.tabs.overlays.OverlaysAdapter;
import projekt.substratum.adapters.tabs.overlays.OverlaysItem;
import projekt.substratum.adapters.tabs.overlays.VariantAdapter;
import projekt.substratum.adapters.tabs.overlays.VariantItem;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.compilers.SubstratumBuilder;
import projekt.substratum.util.files.MapUtils;
import projekt.substratum.util.files.Root;
import projekt.substratum.util.views.SheetDialog;

import static android.content.Context.ACTIVITY_SERVICE;
import static projekt.substratum.InformationActivity.currentShownLunchBar;
import static projekt.substratum.common.References.DEFAULT_NOTIFICATION_CHANNEL_ID;
import static projekt.substratum.common.References.ENABLE_PACKAGE_LOGGING;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER;
import static projekt.substratum.common.References.metadataEmail;
import static projekt.substratum.common.References.metadataEncryption;
import static projekt.substratum.common.References.metadataEncryptionValue;
import static projekt.substratum.tabs.OverlayFunctions.Phase3_mainFunction;
import static projekt.substratum.tabs.OverlayFunctions.getThemeCache;

public class Overlays extends Fragment {

    public static final String overlaysDir = "overlays";
    public static final String TAG = SUBSTRATUM_BUILDER;
    public static final int THREAD_WAIT_DURATION = 500;
    public ImageView loader_image;
    public TextView loader_string;
    public SheetDialog mCompileDialog;
    public SubstratumBuilder sb;
    public List<OverlaysItem> overlaysLists, checkedOverlays;
    public RecyclerView.Adapter mAdapter;
    public String theme_name;
    public String theme_pid;
    public String versionName;
    public NotificationManager mNotifyManager;
    public NotificationCompat.Builder mBuilder;
    public boolean has_failed = false;
    public int fail_count;
    public StringBuilder failed_packages;
    public ArrayList<OverlaysItem> values2;
    public RecyclerView mRecyclerView;
    public Spinner base_spinner;
    public SharedPreferences prefs;
    public List<String> final_runner, late_install;
    public boolean mixAndMatchMode, enable_mode, disable_mode, compile_enable_mode,
            enable_disable_mode;
    public Switch toggle_all;
    public SwipeRefreshLayout swipeRefreshLayout;
    public ProgressBar progressBar;
    public Boolean is_overlay_active = false;
    public StringBuilder error_logs;
    public double current_amount = 0;
    public double total_amount = 0;
    public String current_dialog_overlay;
    public ProgressBar dialogProgress;
    public ArrayList<String> final_command;
    public AssetManager themeAssetManager;
    public Phase3_mainFunction phase3_mainFunction;
    public Boolean missingType3 = false;
    public JobReceiver jobReceiver;
    public LocalBroadcastManager localBroadcastManager;
    public String type1a = "";
    public String type1b = "";
    public String type1c = "";
    public String type2 = "";
    public String type3 = "";
    public String type4 = "";
    public Boolean encrypted = false;
    public Cipher cipher = null;
    public RefreshReceiver refreshReceiver;
    public ActivityManager am;
    public boolean decryptedAssetsExceptionReached;
    public int overlaysWaiting = 0;
    private int currentPosition;

    protected void logTypes() {
        if (ENABLE_PACKAGE_LOGGING) {
            Log.d("Theme Type1a Resource", this.type1a);
            Log.d("Theme Type1b Resource", this.type1b);
            Log.d("Theme Type1c Resource", this.type1c);
            Log.d("Theme Type2  Resource", this.type2);
            Log.d("Theme Type3  Resource", this.type3);
            Log.d("Theme Type4  Resource", this.type4);
        }
    }

    public View getActivityView() {
        return ((ViewGroup) getActivity().findViewById(android.R.id.content)).getChildAt(0);
    }

    public void startCompileEnableMode() {
        if (!this.is_overlay_active) {
            this.is_overlay_active = true;
            this.compile_enable_mode = true;
            this.enable_mode = false;
            this.disable_mode = false;
            this.enable_disable_mode = false;

            this.overlaysLists = ((OverlaysAdapter) this.mAdapter).getOverlayList();
            this.checkedOverlays = new ArrayList<>();

            for (int i = 0; i < this.overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = this.overlaysLists.get(i);
                if (currentOverlay.isSelected()) {
                    this.checkedOverlays.add(currentOverlay);
                }
            }
            if (!this.checkedOverlays.isEmpty()) {
                getThemeCache phase2 = new getThemeCache(this);
                if (this.base_spinner.getSelectedItemPosition() != 0 &&
                        this.base_spinner.getVisibility() == View.VISIBLE) {
                    phase2.execute(this.base_spinner.getSelectedItem().toString());
                } else {
                    phase2.execute("");
                }
                for (OverlaysItem overlay : this.checkedOverlays) {
                    Log.d("OverlayTargetPackageKiller", "Killing package : " + overlay
                            .getPackageName());
                    this.am.killBackgroundProcesses(overlay.getPackageName());
                }
            } else {
                if (this.toggle_all.isChecked()) this.toggle_all.setChecked(false);
                this.is_overlay_active = false;
                currentShownLunchBar = Lunchbar.make(
                        getActivityView(),
                        R.string.toast_disabled5,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }
    }

    public void startCompileUpdateMode() {
        if (!this.is_overlay_active) {
            this.is_overlay_active = true;
            this.compile_enable_mode = false;

            this.overlaysLists = ((OverlaysAdapter) this.mAdapter).getOverlayList();
            this.checkedOverlays = new ArrayList<>();

            for (int i = 0; i < this.overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = this.overlaysLists.get(i);
                if (currentOverlay.isSelected()) {
                    this.checkedOverlays.add(currentOverlay);
                }
            }

            if (!this.checkedOverlays.isEmpty()) {
                getThemeCache phase2 = new getThemeCache(this);
                if (this.base_spinner.getSelectedItemPosition() != 0 &&
                        this.base_spinner.getVisibility() == View.VISIBLE) {
                    phase2.execute(this.base_spinner.getSelectedItem().toString());
                } else {
                    phase2.execute("");
                }
                for (OverlaysItem overlay : this.checkedOverlays) {
                    Log.d("OverlayTargetPackageKiller", "Killing package : " + overlay
                            .getPackageName());
                    this.am.killBackgroundProcesses(overlay.getPackageName());
                }
            } else {
                if (this.toggle_all.isChecked()) this.toggle_all.setChecked(false);
                this.is_overlay_active = false;
                currentShownLunchBar = Lunchbar.make(
                        getActivityView(),
                        R.string.toast_disabled5,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }
    }

    public void startDisable() {
        if (!this.is_overlay_active) {
            this.is_overlay_active = true;

            this.overlaysLists = ((OverlaysAdapter) this.mAdapter).getOverlayList();
            this.checkedOverlays = new ArrayList<>();

            if (Systems.checkOMS(getContext())) {
                this.compile_enable_mode = false;
                this.enable_mode = false;
                this.disable_mode = true;
                this.enable_disable_mode = false;

                for (int i = 0; i < this.overlaysLists.size(); i++) {
                    OverlaysItem currentOverlay = this.overlaysLists.get(i);
                    if (currentOverlay.isSelected() && currentOverlay.isOverlayEnabled()) {
                        this.checkedOverlays.add(currentOverlay);
                    } else {
                        currentOverlay.setSelected(false);
                        this.mAdapter.notifyDataSetChanged();
                    }
                }
                if (!this.checkedOverlays.isEmpty()) {
                    getThemeCache phase2 = new getThemeCache(this);
                    if (this.base_spinner.getSelectedItemPosition() != 0 &&
                            this.base_spinner.getVisibility() == View.VISIBLE) {
                        phase2.execute(this.base_spinner.getSelectedItem().toString());
                    } else {
                        phase2.execute("");
                    }
                    for (OverlaysItem overlay : this.checkedOverlays) {
                        Log.d("OverlayTargetPackageKiller", "Killing package : " + overlay
                                .getPackageName());
                        this.am.killBackgroundProcesses(overlay.getPackageName());
                    }
                } else {
                    if (this.toggle_all.isChecked()) this.toggle_all.setChecked(false);
                    this.is_overlay_active = false;
                    currentShownLunchBar = Lunchbar.make(
                            getActivityView(),
                            R.string.toast_disabled5,
                            Lunchbar.LENGTH_LONG);
                    currentShownLunchBar.show();
                }
            } else {
                this.compile_enable_mode = false;
                this.enable_mode = false;
                this.disable_mode = true;
                this.enable_disable_mode = false;

                for (int i = 0; i < this.overlaysLists.size(); i++) {
                    OverlaysItem currentOverlay = this.overlaysLists.get(i);
                    if (currentOverlay.isSelected()) {
                        this.checkedOverlays.add(currentOverlay);
                    } else {
                        currentOverlay.setSelected(false);
                        this.mAdapter.notifyDataSetChanged();
                    }
                }

                String current_directory;
                if (projekt.substratum.common.Resources.inNexusFilter()) {
                    current_directory = PIXEL_NEXUS_DIR;
                } else {
                    current_directory = LEGACY_NEXUS_DIR;
                }

                if (!this.checkedOverlays.isEmpty()) {
                    if (Systems.isSamsung(getContext())) {
                        if (Root.checkRootAccess() && Root.requestRootAccess()) {
                            ArrayList<String> checked_overlays = new ArrayList<>();
                            for (int i = 0; i < this.checkedOverlays.size(); i++) {
                                checked_overlays.add(
                                        this.checkedOverlays.get(i).getFullOverlayParameters());
                            }
                            ThemeManager.uninstallOverlay(getContext(), checked_overlays);
                        } else {
                            for (int i = 0; i < this.checkedOverlays.size(); i++) {
                                Uri packageURI = Uri.parse("package:" +
                                        this.checkedOverlays.get(i).getFullOverlayParameters());
                                Intent uninstallIntent =
                                        new Intent(Intent.ACTION_DELETE, packageURI);

                                startActivity(uninstallIntent);
                            }
                        }
                    } else {
                        for (int i = 0; i < this.checkedOverlays.size(); i++) {
                            FileOperations.mountRW();
                            FileOperations.delete(getContext(), current_directory +
                                    this.checkedOverlays.get(i).getFullOverlayParameters() + ".apk");
                            this.mAdapter.notifyDataSetChanged();
                        }
                        // Untick all options in the adapter after compiling
                        this.toggle_all.setChecked(false);
                        this.overlaysLists = ((OverlaysAdapter) this.mAdapter).getOverlayList();
                        for (int i = 0; i < this.overlaysLists.size(); i++) {
                            OverlaysItem currentOverlay = this.overlaysLists.get(i);
                            if (currentOverlay.isSelected()) {
                                currentOverlay.setSelected(false);
                            }
                        }
                        Toast.makeText(getContext(),
                                getString(R.string.toast_disabled6),
                                Toast.LENGTH_SHORT).show();
                        AlertDialog.Builder alertDialogBuilder =
                                new AlertDialog.Builder(getContext());
                        alertDialogBuilder.setTitle(
                                getString(R.string.legacy_dialog_soft_reboot_title));
                        alertDialogBuilder.setMessage(
                                getString(R.string.legacy_dialog_soft_reboot_text));
                        alertDialogBuilder.setPositiveButton(
                                android.R.string.ok,
                                (dialog, id12) -> ElevatedCommands.reboot());
                        alertDialogBuilder.setNegativeButton(
                                R.string.remove_dialog_later, (dialog, id1) -> {
                                    this.progressBar.setVisibility(View.GONE);
                                    dialog.dismiss();
                                });
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                } else {
                    if (this.toggle_all.isChecked()) this.toggle_all.setChecked(false);
                    this.is_overlay_active = false;
                    currentShownLunchBar = Lunchbar.make(
                            getActivityView(),
                            R.string.toast_disabled5,
                            Lunchbar.LENGTH_LONG);
                    currentShownLunchBar.show();
                }
                this.is_overlay_active = false;
                this.disable_mode = false;
            }
        }
    }

    public void startEnable() {
        if (!this.is_overlay_active) {
            this.is_overlay_active = true;
            this.compile_enable_mode = false;
            this.enable_mode = true;
            this.disable_mode = false;
            this.enable_disable_mode = false;

            this.overlaysLists = ((OverlaysAdapter) this.mAdapter).getOverlayList();
            this.checkedOverlays = new ArrayList<>();

            for (int i = 0; i < this.overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = this.overlaysLists.get(i);
                if (currentOverlay.isSelected() && !currentOverlay.isOverlayEnabled()) {
                    this.checkedOverlays.add(currentOverlay);
                } else {
                    currentOverlay.setSelected(false);
                    this.mAdapter.notifyDataSetChanged();
                }
            }
            if (!this.checkedOverlays.isEmpty()) {
                getThemeCache phase2 = new getThemeCache(this);
                if (this.base_spinner.getSelectedItemPosition() != 0 &&
                        this.base_spinner.getVisibility() == View.VISIBLE) {
                    phase2.execute(this.base_spinner.getSelectedItem().toString());

                } else {
                    phase2.execute("");
                }
                for (OverlaysItem overlay : this.checkedOverlays) {
                    Log.d("OverlayTargetPackageKiller", "Killing package : " + overlay
                            .getPackageName());
                    this.am.killBackgroundProcesses(overlay.getPackageName());
                }
            } else {
                if (this.toggle_all.isChecked()) this.toggle_all.setChecked(false);
                this.is_overlay_active = false;
                currentShownLunchBar = Lunchbar.make(
                        getActivityView(),
                        R.string.toast_disabled5,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }
    }

    public void startEnableDisable() {
        if (!this.is_overlay_active) {
            this.is_overlay_active = true;
            this.compile_enable_mode = false;
            this.enable_mode = false;
            this.disable_mode = false;
            this.enable_disable_mode = true;

            this.overlaysLists = ((OverlaysAdapter) this.mAdapter).getOverlayList();
            this.checkedOverlays = new ArrayList<>();


            for (int i = 0; i < this.overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = this.overlaysLists.get(i);
                if (currentOverlay.isSelected()) this.checkedOverlays.add(currentOverlay);
                else {
                    currentOverlay.setSelected(false);
                    this.mAdapter.notifyDataSetChanged();
                }
            }
            if (!this.checkedOverlays.isEmpty()) {
                getThemeCache phase2 = new getThemeCache(this);
                if (this.base_spinner.getSelectedItemPosition() != 0 &&
                        this.base_spinner.getVisibility() == View.VISIBLE) {
                    phase2.execute(this.base_spinner.getSelectedItem().toString());

                } else {
                    phase2.execute("");
                }
                for (OverlaysItem overlay : this.checkedOverlays) {
                    Log.d("OverlayTargetPackageKiller", "Killing package : " + overlay
                            .getPackageName());
                    this.am.killBackgroundProcesses(overlay.getPackageName());
                }
            } else {
                if (this.toggle_all.isChecked()) this.toggle_all.setChecked(false);
                this.is_overlay_active = false;
                currentShownLunchBar = Lunchbar.make(
                        getActivityView(),
                        R.string.toast_disabled5,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }
    }

    public void setMixAndMatchMode(boolean newValue) {
        this.mixAndMatchMode = newValue;
        this.prefs.edit().putBoolean("enable_swapping_overlays", this.mixAndMatchMode).apply();
        updateEnabledOverlays();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.tab_overlays, container, false);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        this.am = (ActivityManager) getContext().getSystemService(ACTIVITY_SERVICE);

        // Register the theme install receiver to auto refresh the fragment
        this.refreshReceiver = new RefreshReceiver();
        this.localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        this.localBroadcastManager.registerReceiver(this.refreshReceiver,
                new IntentFilter("Overlay.REFRESH"));

        this.theme_name = getArguments().getString("theme_name");
        this.theme_pid = getArguments().getString("theme_pid");
        String encrypt_check =
                Packages.getOverlayMetadata(getContext(), this.theme_pid, metadataEncryption);

        if (encrypt_check != null && encrypt_check.equals(metadataEncryptionValue)) {
            byte[] encryption_key = getArguments().getByteArray("encryption_key");
            byte[] iv_encrypt_key = getArguments().getByteArray("iv_encrypt_key");
            try {
                this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                this.cipher.init(
                        Cipher.DECRYPT_MODE,
                        new SecretKeySpec(encryption_key, "AES"),
                        new IvParameterSpec(iv_encrypt_key)
                );
                Log.d(TAG, "Loading substratum theme in encrypted assets mode.");
                this.encrypted = true;
            } catch (Exception e) {
                Log.d(TAG,
                        "Loading substratum theme in decrypted assets mode due to an exception.");
                this.decryptedAssetsExceptionReached = true;
            }
        } else {
            Log.d(TAG, "Loading substratum theme in decrypted assets mode.");
        }

        if (this.decryptedAssetsExceptionReached) {
            currentShownLunchBar = Lunchbar.make(
                    getActivityView(),
                    R.string.error_loading_theme_close_text,
                    Lunchbar.LENGTH_INDEFINITE);
            currentShownLunchBar.setAction(getString(R.string.error_loading_theme_close), view -> {
                currentShownLunchBar.dismiss();
                getActivity().finish();
            });
            currentShownLunchBar.show();
        }

        this.mixAndMatchMode = this.prefs.getBoolean("enable_swapping_overlays", false);

        this.progressBar = root.findViewById(R.id.header_loading_bar);
        this.progressBar.setVisibility(View.GONE);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        this.mRecyclerView = root.findViewById(R.id.overlayRecyclerView);
        this.mRecyclerView.setHasFixedSize(true);
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayList<OverlaysItem> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new OverlaysAdapter(empty_array);
        this.mRecyclerView.setAdapter(empty_adapter);

        TextView toggle_all_overlays_text = root.findViewById(R.id.toggle_all_overlays_text);
        toggle_all_overlays_text.setVisibility(View.VISIBLE);

        File work_area = new File(EXTERNAL_STORAGE_CACHE);
        if (!work_area.exists() && work_area.mkdir()) {
            Log.d(TAG, "Updating the internal storage with proper file directories...");
        }

        // Adjust the behaviour of the mix and match toggle in the sheet
        this.toggle_all = root.findViewById(R.id.toggle_all_overlays);
        this.toggle_all.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    try {
                        this.overlaysLists = ((OverlaysAdapter) this.mAdapter).getOverlayList();
                        for (int i = 0; i < this.overlaysLists.size(); i++) {
                            OverlaysItem currentOverlay = this.overlaysLists.get(i);
                            currentOverlay.setSelected(isChecked);
                            this.mAdapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Window has lost connection with the host.");
                    }
                });

        // Allow the user to toggle the select all switch by clicking on the bar above
        RelativeLayout toggleZone = root.findViewById(R.id.toggle_zone);
        toggleZone.setOnClickListener(v -> {
            try {
                this.toggle_all.setChecked(!this.toggle_all.isChecked());
                this.overlaysLists = ((OverlaysAdapter) this.mAdapter).getOverlayList();
                for (int i = 0; i < this.overlaysLists.size(); i++) {
                    OverlaysItem currentOverlay = this.overlaysLists.get(i);
                    currentOverlay.setSelected(this.toggle_all.isChecked());
                    this.mAdapter.notifyDataSetChanged();
                }
            } catch (Exception e) {
                Log.e(TAG, "Window has lost connection with the host.");
            }
        });

        // Allow the user to swipe down to refresh the overlay list
        this.swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        this.swipeRefreshLayout.setOnRefreshListener(this::refreshList);

        /*
          PLUGIN TYPE 3: Parse each overlay folder to see if they have folder options
         */
        SharedPreferences prefs2 =
                getContext().getSharedPreferences("base_variant", Context.MODE_PRIVATE);
        this.base_spinner = root.findViewById(R.id.type3_spinner);
        this.base_spinner.post(() -> this.base_spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long id) {
                        prefs2.edit().putInt(Overlays.this.theme_pid, pos).apply();
                        refreshList();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                }));
        this.base_spinner.setEnabled(false);

        try {
            Resources themeResources = getContext().getPackageManager()
                    .getResourcesForApplication(this.theme_pid);
            this.themeAssetManager = themeResources.getAssets();

            ArrayList<VariantItem> type3 = new ArrayList<>();
            List<String> stringArray = new ArrayList<>();

            String[] listArray = this.themeAssetManager.list("overlays/android");
            Collections.addAll(stringArray, listArray);

            if (stringArray.contains("type3") || stringArray.contains("type3.enc")) {
                InputStream inputStream;
                if (this.encrypted) {
                    inputStream = FileOperations.getInputStream(
                            this.themeAssetManager,
                            "overlays/android/type3.enc",
                            this.cipher);
                } else {
                    inputStream = this.themeAssetManager.open("overlays/android/type3");
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream))) {
                    String formatter = String.format(
                            getString(R.string.overlays_variant_substitute), reader.readLine());
                    type3.add(new VariantItem(formatter, null));
                } catch (IOException e) {
                    Log.e(TAG, "There was an error parsing asset file!");
                    type3.add(new VariantItem(getString(R.string
                            .overlays_variant_default_3), null));
                }
                inputStream.close();
            } else {
                type3.add(new VariantItem(getString(R.string.overlays_variant_default_3), null));
            }

            if (stringArray.size() > 1) {
                for (int i = 0; i < stringArray.size(); i++) {
                    String current = stringArray.get(i);
                    if (!"res".equals(current) &&
                            !current.contains(".") &&
                            current.length() >= 6 &&
                            "type3_".equals(current.substring(0, 6))) {
                        type3.add(new VariantItem(current.substring(6), null));
                    }
                }
                SpinnerAdapter adapter1 = new VariantAdapter(getActivity(), type3);
                if (type3.size() > 1) {
                    toggle_all_overlays_text.setVisibility(View.GONE);
                    this.base_spinner.setVisibility(View.VISIBLE);
                    this.base_spinner.setAdapter(adapter1);
                    try {
                        Log.d(TAG,
                                "Assigning the spinner position: " + prefs2.getInt(this.theme_pid, 0));
                        this.base_spinner.setSelection(prefs2.getInt(this.theme_pid, 0));
                    } catch (Exception e) {
                        // Should be OutOfBounds, but let's catch everything
                        Log.d(TAG, "Falling back to default spinner position due to an error...");
                        prefs2.edit().putInt(this.theme_pid, 0).apply();
                        this.base_spinner.setSelection(0);
                    }
                } else {
                    toggle_all_overlays_text.setVisibility(View.VISIBLE);
                    this.base_spinner.setVisibility(View.INVISIBLE);
                    refreshList();
                }
            } else {
                toggle_all_overlays_text.setVisibility(View.VISIBLE);
                this.base_spinner.setVisibility(View.INVISIBLE);
                refreshList();
            }
        } catch (Exception e) {
            if (this.base_spinner.getVisibility() == View.VISIBLE) {
                toggle_all_overlays_text.setVisibility(View.VISIBLE);
                this.base_spinner.setVisibility(View.INVISIBLE);
            }
            e.printStackTrace();
            Log.e(TAG, "Could not parse list of base options for this theme!");
        }

        // Enable job listener
        this.jobReceiver = new JobReceiver();
        IntentFilter intentFilter = new IntentFilter("Overlays.START_JOB");
        this.localBroadcastManager.registerReceiver(this.jobReceiver, intentFilter);

        // Enable the instance to be retained for LogChar invoke after configuration change
        setRetainInstance(true);
        if (this.error_logs != null && this.error_logs.length() > 0) {
            invokeLogCharLunchBar(getContext());
        }
        return root;
    }

    protected List<String> updateEnabledOverlays() {
        return new ArrayList<>(ThemeManager.listOverlays(
                getContext(),
                ThemeManager.STATE_ENABLED
        ));
    }

    protected boolean checkActiveNotifications() {
        StatusBarNotification[] activeNotifications = this.mNotifyManager.getActiveNotifications();
        for (StatusBarNotification statusBarNotification : activeNotifications) {
            if (statusBarNotification.getPackageName().equals(getContext().getPackageName())) {
                return true;
            }
        }
        return false;
    }

    protected void failedFunction(Context context) {
        // Add dummy intent to be able to close the notification on click
        Intent notificationIntent = new Intent(context, this.getClass());
        notificationIntent.putExtra("theme_name", this.theme_name);
        notificationIntent.putExtra("theme_pid", this.theme_pid);
        notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

        // Closing off the persistent notification
        this.mNotifyManager.cancel(References.notification_id_compiler);
        this.mBuilder = new NotificationCompat.Builder(context, DEFAULT_NOTIFICATION_CHANNEL_ID);
        this.mBuilder.setAutoCancel(true);
        this.mBuilder.setProgress(0, 0, false);
        this.mBuilder.setOngoing(false);
        this.mBuilder.setContentIntent(intent);
        this.mBuilder.setSmallIcon(R.drawable.notification_warning_icon);
        this.mBuilder.setContentTitle(context.getString(R.string.notification_done_title));
        this.mBuilder.setContentText(context.getString(R.string.notification_some_errors_found));
        if (this.prefs.getBoolean("vibrate_on_compiled", false)) {
            this.mBuilder.setVibrate(new long[]{200, 400, 200, 1000});
        }
        this.mNotifyManager.notify(References.notification_id_compiler, this.mBuilder.build());

        Toast.makeText(
                context,
                context.getString(R.string.toast_compiled_updated_with_errors),
                Toast.LENGTH_LONG).show();

        if (this.prefs.getBoolean("autosave_logchar", true)) {
            new SendErrorReport(
                    context,
                    this.theme_pid,
                    this.error_logs.toString(),
                    this.failed_packages.toString(),
                    true
            ).execute();
        }

        invokeLogCharLunchBar(context);
    }

    @SuppressWarnings("unchecked")
    public void invokeLogCharLunchBar(Context context) {
        CharSequence errorLogCopy = new StringBuilder(this.error_logs);
        this.error_logs = new StringBuilder();
        currentShownLunchBar = Lunchbar.make(
                getActivityView(),
                R.string.logcat_snackbar_text,
                Lunchbar.LENGTH_INDEFINITE);
        currentShownLunchBar.setAction(getString(R.string.logcat_snackbar_button), view -> {
            currentShownLunchBar.dismiss();
            invokeLogCharDialog(context, errorLogCopy);
        });
        currentShownLunchBar.show();
    }

    public void invokeLogCharDialog(Context context, CharSequence logs) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context)
                .setTitle(R.string.logcat_dialog_title)
                .setMessage("\n" + logs)
                .setNeutralButton(R.string
                        .customactivityoncrash_error_activity_error_details_close, null)
                .setNegativeButton(R.string
                                .customactivityoncrash_error_activity_error_details_copy,
                        (dialog1, which) -> {
                            References.copyToClipboard(context,
                                    "substratum_log", logs.toString());
                            currentShownLunchBar = Lunchbar.make(
                                    getActivityView(),
                                    R.string.logcat_dialog_copy_success,
                                    Lunchbar.LENGTH_LONG);
                            currentShownLunchBar.show();
                        });

        if (Packages.getOverlayMetadata(context, this.theme_pid, metadataEmail) != null) {
            builder.setPositiveButton(getString(R.string.logcat_send), (dialogInterface, i) ->
                    new SendErrorReport(
                            context,
                            this.theme_pid,
                            logs.toString(),
                            this.failed_packages.toString(),
                            false
                    ).execute());
        }
        builder.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.phase3_mainFunction != null && this.mNotifyManager != null) {
            if (this.phase3_mainFunction.getStatus() == AsyncTask.Status.RUNNING) {
                this.mNotifyManager.cancel(References.notification_id_compiler);
            }
        }
        try {
            this.localBroadcastManager.unregisterReceiver(this.refreshReceiver);
        } catch (IllegalArgumentException e) {
            // Unregistered already
        }
        try {
            this.localBroadcastManager.unregisterReceiver(this.jobReceiver);
        } catch (IllegalArgumentException e) {
            // Unregistered already
        }
    }

    protected boolean needsRecreate(Context context) {
        for (OverlaysItem oi : this.checkedOverlays) {
            String packageName = oi.getPackageName();
            if ("android".equals(packageName) || "projekt.substratum".equals(packageName)) {
                if (!this.enable_mode && !this.disable_mode && !this.enable_disable_mode &&
                        ThemeManager.isOverlayEnabled(context, oi.getFullOverlayParameters())) {
                    return false;
                } else if (this.enable_mode || this.disable_mode || this.compile_enable_mode
                        || this.enable_disable_mode) {
                    return false;
                }
            }
        }
        return Systems.checkOMS(getContext()) && !this.has_failed;
    }

    public VariantItem setTypeOneSpinners(String package_identifier,
                                          String type) {
        InputStream inputStream = null;
        try {
            if (this.encrypted) {
                inputStream = FileOperations.getInputStream(
                        this.themeAssetManager,
                        overlaysDir + "/" + package_identifier + "/type1" + type + ".enc",
                        this.cipher);
            } else {
                inputStream = this.themeAssetManager.open(
                        overlaysDir + "/" + package_identifier + "/type1" + type);
            }
        } catch (IOException ioe) {
            // Suppress warning
        }

        // Parse current default types on type3 base resource folders
        String parsedVariant = "";
        try {
            if (this.base_spinner.getSelectedItemPosition() != 0) {
                parsedVariant = this.base_spinner.getSelectedItem().toString().replaceAll("\\s+", "");
            }
        } catch (NullPointerException npe) {
            // Suppress warning
        }
        String suffix = ((parsedVariant.length() != 0) ? "/type3_" + parsedVariant : "/res");

        // Type1 Spinner Text Adjustments
        assert inputStream != null;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream))) {
            // This adjusts it so that we have the spinner text set
            String formatter = String.format(
                    getString(R.string.overlays_variant_substitute),
                    reader.readLine());
            // This is the default type1 xml hex, if present
            String hex = null;

            if (this.encrypted) {
                try (InputStream name = FileOperations.getInputStream(
                        this.themeAssetManager,
                        overlaysDir + "/" + package_identifier + suffix +
                                "/values/type1" + type + ".xml.enc",
                        this.cipher)) {
                    hex = Packages.getOverlayResource(name);
                } catch (IOException e) {
                    // Suppress warning
                }
            } else {
                try (InputStream name = this.themeAssetManager.open(
                        overlaysDir + "/" + package_identifier + suffix +
                                "/values/type1" + type + ".xml")) {
                    hex = Packages.getOverlayResource(name);
                } catch (IOException e) {
                    // Suppress warning
                }
            }
            return new VariantItem(formatter, hex);
        } catch (Exception e) {
            // When erroring out, put the default spinner text
            Log.d(TAG, "Falling back to default base variant text...");
            String hex = null;
            if (this.encrypted) {
                try (InputStream input = FileOperations.getInputStream(
                        this.themeAssetManager,
                        overlaysDir + "/" + package_identifier +
                                suffix + "/values/type1" + type + ".xml.enc",
                        this.cipher)) {
                    hex = Packages.getOverlayResource(input);
                } catch (IOException ioe) {
                    // Suppress warning
                }
            } else {
                try (InputStream input = this.themeAssetManager.open(overlaysDir +
                        "/" + package_identifier + suffix + "/values/type1" + type + ".xml")) {
                    hex = Packages.getOverlayResource(input);
                } catch (IOException ioe) {
                    // Suppress warning
                }
            }
            switch (type) {
                case "a":
                    return new VariantItem(
                            getString(R.string.overlays_variant_default_1a), hex);
                case "b":
                    return new VariantItem(
                            getString(R.string.overlays_variant_default_1b), hex);
                case "c":
                    return new VariantItem(
                            getString(R.string.overlays_variant_default_1c), hex);
                default:
                    return null;
            }
        }
    }

    public VariantItem setTypeTwoFourSpinners(InputStreamReader inputStreamReader, Integer type) {
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            return new VariantItem(String.format(
                    getString(R.string.overlays_variant_substitute), reader.readLine()), null);
        } catch (Exception e) {
            Log.d(TAG, "Falling back to default base variant text...");
            switch (type) {
                case 2:
                    return new VariantItem(getString(R.string.overlays_variant_default_2), null);
                case 4:
                    return new VariantItem(getString(R.string.overlays_variant_default_4), null);
            }
        }
        return null;
    }

    public VariantItem setTypeOneHexAndSpinner(String current, String package_identifier) {
        if (this.encrypted) {
            try (InputStream inputStream = FileOperations.getInputStream(this.themeAssetManager,
                    "overlays/" + package_identifier + "/" + current, this.cipher)) {
                String hex = Packages.getOverlayResource(inputStream);

                return new VariantItem(
                        current.substring(7, current.length() - 8), hex);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (InputStream inputStream = this.themeAssetManager.open(
                    "overlays/" + package_identifier + "/" + current)) {
                String hex = Packages.getOverlayResource(inputStream);

                return new VariantItem(
                        current.substring(7, current.length() - 4), hex);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 2486:
                FileOperations.delete(getContext(),
                        new File(this.late_install.get(0)).getAbsolutePath());
                if (this.late_install != null && this.late_install.size() > 0) this.late_install.remove(0);
                if (this.late_install.size() > 0) {
                    installMultipleAPKs();
                }
        }
    }

    private void installMultipleAPKs() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(
                getContext(),
                getContext().getApplicationContext().getPackageName() + ".provider",
                new File(this.late_install.get(0)));
        intent.setDataAndType(
                uri,
                "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, 2486);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!this.toggle_all.isChecked()) refreshList();
    }

    private void refreshList() {
        this.currentPosition = ((LinearLayoutManager) this.mRecyclerView.getLayoutManager())
                .findFirstCompletelyVisibleItemPosition();
        this.toggle_all.setChecked(false);
        if (this.base_spinner != null && this.base_spinner.getSelectedItemPosition() > 0) {
            String[] commands = {
                    this.base_spinner.getSelectedItem().toString()
            };
            new LoadOverlays(this).execute(commands);
        } else {
            new LoadOverlays(this).execute("");
        }
    }

    private static class SendErrorReport extends AsyncTask<Void, Void, File> {
        private final WeakReference<Context> ref;
        private final String themePid;
        private final String errorLog;
        private final String themeName;
        private final String themeAuthor;
        private final String themeEmail;
        private final String emailSubject;
        private final String emailBody;
        private final String failedPackages;
        private ProgressDialog progressDialog;
        private final Boolean autosaveInstance;

        SendErrorReport(Context context,
                        String themePid,
                        String errorLog,
                        String failedPackages,
                        Boolean autosaveInstance) {
            super();
            this.ref = new WeakReference<>(context);
            this.themePid = themePid;
            this.errorLog = errorLog;
            this.failedPackages = failedPackages;
            this.autosaveInstance = autosaveInstance;

            this.themeName = Packages.getPackageName(context, themePid);
            this.themeAuthor = Packages.getOverlayMetadata(context, themePid, References
                    .metadataAuthor);
            this.themeEmail = Packages.getOverlayMetadata(context, themePid, References
                    .metadataEmail);

            this.emailSubject = String.format(
                    context.getString(R.string.logcat_email_subject), this.themeName);
            this.emailBody = String.format(
                    context.getString(R.string.logcat_email_body), this.themeAuthor, this.themeName);
        }

        @Override
        protected void onPreExecute() {
            Context context = this.ref.get();
            if (context != null && !this.autosaveInstance) {
                this.progressDialog = new ProgressDialog(context);
                this.progressDialog.setIndeterminate(true);
                this.progressDialog.setCancelable(false);
                this.progressDialog.setMessage(
                        context.getString(R.string.logcat_processing_dialog));
                this.progressDialog.show();
            }
        }

        @Override
        protected File doInBackground(Void... sUrl) {
            Context context = this.ref.get();
            if (context != null) {
                String rom = Systems.checkFirmwareSupport(context,
                        context.getString(R.string.supported_roms_url),
                        "supported_roms.xml");
                String theme_version = Packages.getAppVersion(context, this.themePid);
                String rom_version = Build.VERSION.RELEASE + " - " +
                        (!rom.isEmpty() ? rom : "Unknown");

                String device = Build.MODEL + " (" + Build.DEVICE + ") " +
                        "[" + Build.FINGERPRINT + "]";
                String xposed = References.checkXposedVersion();
                if (!xposed.isEmpty()) device += " {" + xposed + "}";

                String attachment = String.format(
                        context.getString(R.string.logcat_attachment_body),
                        this.themeName,
                        device,
                        rom_version,
                        String.valueOf(BuildConfig.VERSION_CODE),
                        theme_version,
                        this.failedPackages,
                        this.errorLog);

                File log = null;
                if (this.autosaveInstance) {
                    References.writeLogCharFile(this.themePid, attachment);
                } else {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HH:mm", Locale.US);
                    log = new File(EXTERNAL_STORAGE_CACHE +
                            "/theme_error-" + dateFormat.format(new Date()) + ".txt");
                    try (FileWriter fw = new FileWriter(log, false);
                         BufferedWriter out = new BufferedWriter(fw)) {
                        out.write(attachment);
                    } catch (IOException e) {
                        // Suppress exception
                    }
                }
                return log;
            }
            return null;
        }

        @Override
        protected void onPostExecute(File result) {
            Context context = this.ref.get();
            if (context != null) {
                if (!this.autosaveInstance && result != null) {
                    if (this.progressDialog != null) this.progressDialog.dismiss();

                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("message/rfc822");
                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{this.themeEmail});
                    i.putExtra(Intent.EXTRA_SUBJECT, this.emailSubject);
                    i.putExtra(Intent.EXTRA_TEXT, this.emailBody);
                    i.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                            this.ref.get(),
                            this.ref.get().getPackageName() + ".provider",
                            result));
                    try {
                        context.startActivity(Intent.createChooser(i,
                                context.getString(R.string.logcat_email_activity)));
                    } catch (ActivityNotFoundException ex) {
                        Toast.makeText(context,
                                R.string.logcat_email_activity_error,
                                Toast.LENGTH_LONG)
                                .show();
                    }
                }
            }
        }
    }

    private static class LoadOverlays extends AsyncTask<String, Integer, String> {
        private final WeakReference<Overlays> ref;

        LoadOverlays(Overlays fragment) {
            super();
            this.ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            Overlays fragment = this.ref.get();
            if (fragment != null) {
                fragment.swipeRefreshLayout.setRefreshing(true);
                fragment.mRecyclerView.setEnabled(false);
                fragment.toggle_all.setEnabled(false);
                fragment.toggle_all.setChecked(false);
                fragment.base_spinner.setEnabled(false);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Overlays fragment = this.ref.get();
            if (fragment != null) {
                fragment.swipeRefreshLayout.setRefreshing(false);
                fragment.mRecyclerView.setEnabled(true);
                fragment.toggle_all.setEnabled(true);
                fragment.base_spinner.setEnabled(true);
                fragment.mAdapter = new OverlaysAdapter(fragment.values2);
                fragment.mRecyclerView.setAdapter(fragment.mAdapter);
                fragment.mRecyclerView.getLayoutManager()
                        .scrollToPosition(fragment.currentPosition);
                fragment.mAdapter.notifyDataSetChanged();
                fragment.mRecyclerView.setVisibility(View.VISIBLE);
            }
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(String... sUrl) {
            Overlays fragment = this.ref.get();
            if (fragment != null) {
                Context context = fragment.getActivity();
                // Refresh asset manager
                try {
                    Resources themeResources = context.getPackageManager()
                            .getResourcesForApplication(fragment.theme_pid);
                    fragment.themeAssetManager = themeResources.getAssets();

                    // Get the current theme_pid's versionName so that we can version our overlays
                    fragment.versionName = Packages.getAppVersion(context, fragment.theme_pid);
                    List<String> state5overlays = fragment.updateEnabledOverlays();
                    String parse1_themeName = fragment.theme_name.replaceAll("\\s+", "");
                    String parse2_themeName = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");

                    List<String> values = new ArrayList<>();
                    fragment.values2 = new ArrayList<>();

                    // Buffer the initial values list so that we get the list of packages
                    // inside this theme

                    Collection<String> overlaysFolder = new ArrayList<>();
                    try {
                        String[] overlayList =
                                fragment.themeAssetManager.list(Overlays.overlaysDir);
                        Collections.addAll(overlaysFolder, overlayList);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }

                    SharedPreferences prefs =
                            PreferenceManager.getDefaultSharedPreferences(context);

                    Boolean showDangerous = !prefs.getBoolean("show_dangerous_samsung_overlays",
                            false);

                    values.addAll(overlaysFolder.stream().filter(package_name -> (Packages
                            .isPackageInstalled(context, package_name) ||
                            projekt.substratum.common.Resources.allowedSystemUIOverlay
                                    (package_name) ||
                            projekt.substratum.common.Resources.allowedSettingsOverlay
                                    (package_name) ||
                            projekt.substratum.common.Resources.allowedFrameworkOverlay
                                    (package_name)) &&
                            (!showDangerous || !ThemeManager.blacklisted(
                                    package_name,
                                    Systems.isSamsung(context) &&
                                            !Packages.isSamsungTheme(
                                                    context, fragment.theme_pid))))
                            .collect(Collectors.toList()));

                    // Create the map for {package name: package identifier}
                    Map<String, String> unsortedMap = new HashMap<>();

                    // Then let's convert all the package names to their app names
                    for (int i = 0; i < values.size(); i++) {
                        try {
                            if (projekt.substratum.common.Resources.allowedSystemUIOverlay(values
                                    .get(i))) {
                                String package_name = "";
                                switch (values.get(i)) {
                                    case "com.android.systemui.headers":
                                        package_name = context.getString(R.string.systemui_headers);
                                        break;
                                    case "com.android.systemui.navbars":
                                        package_name = context.getString(R.string
                                                .systemui_navigation);
                                        break;
                                    case "com.android.systemui.statusbars":
                                        package_name = context.getString(R.string
                                                .systemui_statusbar);
                                        break;
                                    case "com.android.systemui.tiles":
                                        package_name = context.getString(R.string
                                                .systemui_qs_tiles);
                                        break;
                                }
                                unsortedMap.put(values.get(i), package_name);
                            } else if (projekt.substratum.common.Resources.allowedSettingsOverlay
                                    (values.get(i))) {
                                String package_name = "";
                                switch (values.get(i)) {
                                    case "com.android.settings.icons":
                                        package_name = context.getString(R.string.settings_icons);
                                        break;
                                }
                                unsortedMap.put(values.get(i), package_name);
                            } else if (projekt.substratum.common.Resources.allowedFrameworkOverlay
                                    (values.get(i))) {
                                String package_name = "";
                                switch (values.get(i)) {
                                    case "fwk":
                                        package_name = context.getString(
                                                R.string.samsung_framework);
                                        break;
                                    case "commit":
                                        package_name = context.getString(
                                                R.string.lg_framework);
                                        break;
                                }
                                unsortedMap.put(values.get(i), package_name);
                            } else if (projekt.substratum.common.Resources.allowedAppOverlay
                                    (values.get(i))) {
                                ApplicationInfo applicationInfo = context.getPackageManager()
                                        .getApplicationInfo
                                                (values.get(i), 0);
                                String packageTitle = context.getPackageManager()
                                        .getApplicationLabel
                                                (applicationInfo).toString();
                                unsortedMap.put(values.get(i), packageTitle);
                            }
                        } catch (Exception e) {
                            // Exception
                        }
                    }

                    // Sort the values list
                    List<Pair<String, String>> sortedMap = MapUtils.sortMapByValues(unsortedMap);

                    // Now let's add the new information so that the adapter can recognize custom
                    // method
                    // calls
                    for (Pair<String, String> entry : sortedMap) {
                        String package_name = entry.second;
                        String package_identifier = entry.first;

                        try {
                            ArrayList<VariantItem> type1a = new ArrayList<>();
                            ArrayList<VariantItem> type1b = new ArrayList<>();
                            ArrayList<VariantItem> type1c = new ArrayList<>();
                            ArrayList<VariantItem> type2 = new ArrayList<>();
                            ArrayList<VariantItem> type4 = new ArrayList<>();
                            List<String> typeArray = new ArrayList<>();

                            Object typeArrayRaw = fragment.themeAssetManager.list(
                                    Overlays.overlaysDir + "/" + package_identifier);

                            // Sort the typeArray so that the types are asciibetical
                            Collections.addAll(typeArray, (String[]) typeArrayRaw);
                            Collections.sort(typeArray);

                            // Sort the typeArray so that the types are asciibetical
                            Collections.sort(typeArray);

                            // Let's start adding the type xmls to be parsed into the spinners
                            if (typeArray.contains("type1a") || typeArray.contains("type1a.enc")) {
                                type1a.add(fragment.setTypeOneSpinners(
                                        package_identifier, "a"));
                            }

                            if (typeArray.contains("type1b") || typeArray.contains("type1b.enc")) {
                                type1b.add(fragment.setTypeOneSpinners(
                                        package_identifier, "b"));
                            }

                            if (typeArray.contains("type1c") || typeArray.contains("type1c.enc")) {
                                type1c.add(fragment.setTypeOneSpinners(
                                        package_identifier, "c"));
                            }

                            boolean type2checker = false;
                            for (int i = 0; i < typeArray.size(); i++) {
                                if (typeArray.get(i).startsWith("type2_")) {
                                    type2checker = true;
                                    break;
                                }
                            }
                            if (typeArray.contains("type2") ||
                                    typeArray.contains("type2.enc") ||
                                    type2checker) {
                                InputStreamReader inputStreamReader = null;
                                try {
                                    inputStreamReader =
                                            new InputStreamReader(
                                                    FileOperations.getInputStream(
                                                            fragment.themeAssetManager,
                                                            Overlays.overlaysDir + "/" +
                                                                    package_identifier +
                                                                    (fragment.encrypted ?
                                                                            "/type2.enc" :
                                                                            "/type2"),
                                                            (fragment.encrypted ?
                                                                    fragment.cipher :
                                                                    null)));
                                } catch (Exception e) {
                                    // Suppress warning
                                }
                                type2.add(fragment.setTypeTwoFourSpinners(inputStreamReader, 2));
                            }

                            // Begin Type4 initialization
                            boolean type4checker = false;
                            for (int i = 0; i < typeArray.size(); i++) {
                                if (typeArray.get(i).startsWith("type4_")) {
                                    type4checker = true;
                                    break;
                                }
                            }
                            if (typeArray.contains("type4") ||
                                    typeArray.contains("type4.enc") ||
                                    type4checker) {
                                InputStreamReader inputStreamReader = null;
                                try {
                                    inputStreamReader =
                                            new InputStreamReader(
                                                    FileOperations.getInputStream(
                                                            fragment.themeAssetManager,
                                                            Overlays.overlaysDir + "/" +
                                                                    package_identifier +
                                                                    (fragment.encrypted ?
                                                                            "/type4.enc" :
                                                                            "/type4"),
                                                            (fragment.encrypted ?
                                                                    fragment.cipher :
                                                                    null)));
                                } catch (Exception e) {
                                    // Suppress warning
                                }
                                type4.add(fragment.setTypeTwoFourSpinners(inputStreamReader, 4));
                            }

                            if (typeArray.size() > 1) {
                                for (int i = 0; i < typeArray.size(); i++) {
                                    String current = typeArray.get(i);
                                    if (!"res".equals(current)) {
                                        if (current.contains(".xml")) {
                                            switch (current.substring(0, 7)) {
                                                case "type1a_":
                                                    type1a.add(
                                                            fragment.setTypeOneHexAndSpinner(
                                                                    current, package_identifier));
                                                    break;
                                                case "type1b_":
                                                    type1b.add(
                                                            fragment.setTypeOneHexAndSpinner(
                                                                    current, package_identifier));
                                                    break;
                                                case "type1c_":
                                                    type1c.add(
                                                            fragment.setTypeOneHexAndSpinner(
                                                                    current, package_identifier));
                                                    break;
                                            }
                                        } else if (!current.contains(".") && current.length() > 5) {
                                            if ("type2_".equals(current.substring(0, 6))) {
                                                type2.add(
                                                        new VariantItem(
                                                                current.substring(6), null));
                                            } else if ("type4_".equals(current.substring(0, 6))) {
                                                type4.add(
                                                        new VariantItem(
                                                                current.substring(6), null));
                                            }
                                        }
                                    }
                                }

                                VariantAdapter adapter1 = new VariantAdapter(context, type1a);
                                VariantAdapter adapter2 = new VariantAdapter(context, type1b);
                                VariantAdapter adapter3 = new VariantAdapter(context, type1c);
                                VariantAdapter adapter4 = new VariantAdapter(context, type2);
                                VariantAdapter adapter5 = new VariantAdapter(context, type4);

                                boolean adapterOneChecker = type1a.size() == 0;
                                boolean adapterTwoChecker = type1b.size() == 0;
                                boolean adapterThreeChecker = type1c.size() == 0;
                                boolean adapterFourChecker = type2.size() == 0;
                                boolean adapterFiveChecker = type4.size() == 0;

                                OverlaysItem overlaysItem =
                                        new OverlaysItem(
                                                parse2_themeName,
                                                package_name,
                                                package_identifier,
                                                false,
                                                (adapterOneChecker ? null : adapter1),
                                                (adapterTwoChecker ? null : adapter2),
                                                (adapterThreeChecker ? null : adapter3),
                                                (adapterFourChecker ? null : adapter4),
                                                (adapterFiveChecker ? null : adapter5),
                                                context,
                                                fragment.versionName,
                                                sUrl[0],
                                                state5overlays,
                                                Systems.checkOMS(context),
                                                fragment.getActivityView());
                                fragment.values2.add(overlaysItem);
                            } else {
                                // At this point, there is no spinner adapter, so it should be null
                                OverlaysItem overlaysItem =
                                        new OverlaysItem(
                                                parse2_themeName,
                                                package_name,
                                                package_identifier,
                                                false,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                context,
                                                fragment.versionName,
                                                sUrl[0],
                                                state5overlays,
                                                Systems.checkOMS(context),
                                                fragment.getActivityView());
                                fragment.values2.add(overlaysItem);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    // Consume window disconnection
                }
            }
            return null;
        }
    }

    class JobReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isAdded()) return;

            String command = intent.getStringExtra("command");
            switch (command) {
                case "CompileEnable":
                    if (Overlays.this.mAdapter != null) startCompileEnableMode();
                    break;
                case "CompileUpdate":
                    if (Overlays.this.mAdapter != null) startCompileUpdateMode();
                    break;
                case "Disable":
                    if (Overlays.this.mAdapter != null) startDisable();
                    break;
                case "Enable":
                    if (Overlays.this.mAdapter != null) startEnable();
                    break;
                case "EnableDisable":
                    if (Overlays.this.mAdapter != null) startEnableDisable();
                    break;
                case "MixAndMatchMode":
                    if (Overlays.this.mAdapter != null) {
                        boolean newValue = intent.getBooleanExtra("newValue", false);
                        setMixAndMatchMode(newValue);
                    }
                    break;
            }
        }
    }

    protected class RefreshReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Systems.isSamsung(context) && Root.checkRootAccess()) {
                if (Overlays.this.overlaysWaiting > 0) {
                    --Overlays.this.overlaysWaiting;
                } else {
                    Overlays.this.progressBar.setVisibility(View.GONE);
                }
            }
            refreshList();
        }
    }
}