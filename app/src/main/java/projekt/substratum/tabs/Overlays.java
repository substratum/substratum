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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.BuildConfig;
import projekt.substratum.R;
import projekt.substratum.adapters.tabs.overlays.OverlaysAdapter;
import projekt.substratum.adapters.tabs.overlays.OverlaysItem;
import projekt.substratum.adapters.tabs.overlays.VariantAdapter;
import projekt.substratum.adapters.tabs.overlays.VariantItem;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.compilers.SubstratumBuilder;
import projekt.substratum.util.files.MapUtils;
import projekt.substratum.util.files.Root;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.CLIPBOARD_SERVICE;
import static projekt.substratum.InformationActivity.currentShownLunchBar;
import static projekt.substratum.common.References.DEFAULT_NOTIFICATION_CHANNEL_ID;
import static projekt.substratum.common.References.ENABLE_PACKAGE_LOGGING;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;
import static projekt.substratum.common.References.metadataEmail;
import static projekt.substratum.common.References.metadataEncryption;
import static projekt.substratum.common.References.metadataEncryptionValue;
import static projekt.substratum.tabs.OverlayFunctions.Phase3_mainFunction;
import static projekt.substratum.tabs.OverlayFunctions.getThemeCache;

public class Overlays extends Fragment {

    public static final String overlaysDir = "overlays";
    public static final String TAG = SUBSTRATUM_BUILDER;
    public static final int THREAD_WAIT_DURATION = 500;
    public TextView loader_string;
    public ProgressDialog mProgressDialog;
    public SubstratumBuilder sb;
    public List<OverlaysItem> overlaysLists, checkedOverlays;
    public RecyclerView.Adapter mAdapter;
    public String theme_name;
    public String theme_pid;
    public String versionName;
    public NotificationManager mNotifyManager;
    public NotificationCompat.Builder mBuilder;
    public boolean has_initialized_cache = false;
    public boolean has_failed = false;
    public int fail_count;
    public StringBuilder failed_packages;
    public int id = References.notification_id;
    public ArrayList<OverlaysItem> values2;
    public RecyclerView mRecyclerView;
    public Spinner base_spinner;
    public SharedPreferences prefs;
    public ArrayList<String> final_runner, late_install;
    public boolean mixAndMatchMode, enable_mode, disable_mode, compile_enable_mode, enable_disable_mode;
    public ArrayList<String> all_installed_overlays;
    public Switch toggle_all;
    public SwipeRefreshLayout swipeRefreshLayout;
    public ProgressBar progressBar;
    public Boolean is_overlay_active = false;
    public StringBuilder error_logs;
    public MaterialProgressBar materialProgressBar;
    public double current_amount = 0;
    public double total_amount = 0;
    public String current_dialog_overlay;
    public ProgressBar dialogProgress;
    public ArrayList<String> final_command;
    public AssetManager themeAssetManager;
    public Phase3_mainFunction phase3_mainFunction;
    public Boolean missingType3 = false;
    public JobReceiver jobReceiver;
    public LocalBroadcastManager localBroadcastManager, localBroadcastManager2;
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

    public Overlays() {
    }

    protected void logTypes() {
        if (ENABLE_PACKAGE_LOGGING) {
            Log.d("Theme Type1a Resource", type1a);
            Log.d("Theme Type1b Resource", type1b);
            Log.d("Theme Type1c Resource", type1c);
            Log.d("Theme Type2  Resource", type2);
            Log.d("Theme Type3  Resource", type3);
            Log.d("Theme Type4  Resource", type4);
        }
    }

    public View getActivityView() {
        return ((ViewGroup) getActivity().findViewById(android.R.id.content)).getChildAt(0);
    }

    public void startCompileEnableMode() {
        if (!is_overlay_active) {
            is_overlay_active = true;
            compile_enable_mode = true;
            enable_mode = false;
            disable_mode = false;
            enable_disable_mode = false;

            overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
            checkedOverlays = new ArrayList<>();

            for (int i = 0; i < overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = overlaysLists.get(i);
                if (currentOverlay.isSelected()) {
                    checkedOverlays.add(currentOverlay);
                }
            }
            if (!checkedOverlays.isEmpty()) {
                getThemeCache phase2 = new getThemeCache(this);
                if (base_spinner.getSelectedItemPosition() != 0 &&
                        base_spinner.getVisibility() == View.VISIBLE) {
                    phase2.execute(base_spinner.getSelectedItem().toString());
                } else {
                    phase2.execute("");
                }
                for (OverlaysItem overlay : checkedOverlays) {
                    Log.d("OverlayTargetPackageKiller", "Killing package : " + overlay
                            .getPackageName());
                    am.killBackgroundProcesses(overlay.getPackageName());
                }
            } else {
                if (toggle_all.isChecked()) toggle_all.setChecked(false);
                is_overlay_active = false;
                currentShownLunchBar = Lunchbar.make(
                        getActivityView(),
                        R.string.toast_disabled5,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }
    }

    public void startCompileUpdateMode() {
        if (!is_overlay_active) {
            is_overlay_active = true;
            compile_enable_mode = false;

            overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
            checkedOverlays = new ArrayList<>();

            for (int i = 0; i < overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = overlaysLists.get(i);
                if (currentOverlay.isSelected()) {
                    checkedOverlays.add(currentOverlay);
                }
            }

            if (!checkedOverlays.isEmpty()) {
                getThemeCache phase2 = new getThemeCache(this);
                if (base_spinner.getSelectedItemPosition() != 0 &&
                        base_spinner.getVisibility() == View.VISIBLE) {
                    phase2.execute(base_spinner.getSelectedItem().toString());
                } else {
                    phase2.execute("");
                }
                for (OverlaysItem overlay : checkedOverlays) {
                    Log.d("OverlayTargetPackageKiller", "Killing package : " + overlay
                            .getPackageName());
                    am.killBackgroundProcesses(overlay.getPackageName());
                }
            } else {
                if (toggle_all.isChecked()) toggle_all.setChecked(false);
                is_overlay_active = false;
                currentShownLunchBar = Lunchbar.make(
                        getActivityView(),
                        R.string.toast_disabled5,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }
    }

    public void startDisable() {
        if (!is_overlay_active) {
            is_overlay_active = true;

            overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
            checkedOverlays = new ArrayList<>();

            if (References.checkOMS(getContext())) {
                compile_enable_mode = false;
                enable_mode = false;
                disable_mode = true;
                enable_disable_mode = false;

                for (int i = 0; i < overlaysLists.size(); i++) {
                    OverlaysItem currentOverlay = overlaysLists.get(i);
                    if (currentOverlay.isSelected() && currentOverlay.isOverlayEnabled()) {
                        checkedOverlays.add(currentOverlay);
                    } else {
                        currentOverlay.setSelected(false);
                        mAdapter.notifyDataSetChanged();
                    }
                }
                if (!checkedOverlays.isEmpty()) {
                    getThemeCache phase2 = new getThemeCache(this);
                    if (base_spinner.getSelectedItemPosition() != 0 &&
                            base_spinner.getVisibility() == View.VISIBLE) {
                        phase2.execute(base_spinner.getSelectedItem().toString());
                    } else {
                        phase2.execute("");
                    }
                    for (OverlaysItem overlay : checkedOverlays) {
                        Log.d("OverlayTargetPackageKiller", "Killing package : " + overlay
                                .getPackageName());
                        am.killBackgroundProcesses(overlay.getPackageName());
                    }
                } else {
                    if (toggle_all.isChecked()) toggle_all.setChecked(false);
                    is_overlay_active = false;
                    currentShownLunchBar = Lunchbar.make(
                            getActivityView(),
                            R.string.toast_disabled5,
                            Lunchbar.LENGTH_LONG);
                    currentShownLunchBar.show();
                }
            } else {
                compile_enable_mode = false;
                enable_mode = false;
                disable_mode = true;
                enable_disable_mode = false;

                for (int i = 0; i < overlaysLists.size(); i++) {
                    OverlaysItem currentOverlay = overlaysLists.get(i);
                    if (currentOverlay.isSelected()) {
                        checkedOverlays.add(currentOverlay);
                    } else {
                        currentOverlay.setSelected(false);
                        mAdapter.notifyDataSetChanged();
                    }
                }

                String current_directory;
                if (References.inNexusFilter()) {
                    current_directory = PIXEL_NEXUS_DIR;
                } else {
                    current_directory = LEGACY_NEXUS_DIR;
                }

                if (!checkedOverlays.isEmpty()) {
                    if (References.isSamsung(getContext())) {
                        if (Root.checkRootAccess() && Root.requestRootAccess()) {
                            ArrayList<String> checked_overlays = new ArrayList<>();
                            for (int i = 0; i < checkedOverlays.size(); i++) {
                                checked_overlays.add(
                                        checkedOverlays.get(i).getFullOverlayParameters());
                            }
                            ThemeManager.uninstallOverlay(getContext(), checked_overlays);
                        } else {
                            for (int i = 0; i < checkedOverlays.size(); i++) {
                                Uri packageURI = Uri.parse("package:" +
                                        checkedOverlays.get(i).getFullOverlayParameters());
                                Intent uninstallIntent =
                                        new Intent(Intent.ACTION_DELETE, packageURI);

                                startActivity(uninstallIntent);
                            }
                        }
                    } else {
                        for (int i = 0; i < checkedOverlays.size(); i++) {
                            FileOperations.mountRW();
                            FileOperations.delete(getContext(), current_directory +
                                    checkedOverlays.get(i).getFullOverlayParameters() + ".apk");
                            mAdapter.notifyDataSetChanged();
                        }
                        // Untick all options in the adapter after compiling
                        toggle_all.setChecked(false);
                        overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                        for (int i = 0; i < overlaysLists.size(); i++) {
                            OverlaysItem currentOverlay = overlaysLists.get(i);
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
                                    progressBar.setVisibility(View.GONE);
                                    dialog.dismiss();
                                });
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                } else {
                    if (toggle_all.isChecked()) toggle_all.setChecked(false);
                    is_overlay_active = false;
                    currentShownLunchBar = Lunchbar.make(
                            getActivityView(),
                            R.string.toast_disabled5,
                            Lunchbar.LENGTH_LONG);
                    currentShownLunchBar.show();
                }
                is_overlay_active = false;
                disable_mode = false;
            }
        }
    }

    public void startEnable() {
        if (!is_overlay_active) {
            is_overlay_active = true;
            compile_enable_mode = false;
            enable_mode = true;
            disable_mode = false;
            enable_disable_mode = false;

            overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
            checkedOverlays = new ArrayList<>();

            for (int i = 0; i < overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = overlaysLists.get(i);
                if (currentOverlay.isSelected() && !currentOverlay.isOverlayEnabled()) {
                    checkedOverlays.add(currentOverlay);
                } else {
                    currentOverlay.setSelected(false);
                    mAdapter.notifyDataSetChanged();
                }
            }
            if (!checkedOverlays.isEmpty()) {
                getThemeCache phase2 = new getThemeCache(this);
                if (base_spinner.getSelectedItemPosition() != 0 &&
                        base_spinner.getVisibility() == View.VISIBLE) {
                    phase2.execute(base_spinner.getSelectedItem().toString());

                } else {
                    phase2.execute("");
                }
                for (OverlaysItem overlay : checkedOverlays) {
                    Log.d("OverlayTargetPackageKiller", "Killing package : " + overlay
                            .getPackageName());
                    am.killBackgroundProcesses(overlay.getPackageName());
                }
            } else {
                if (toggle_all.isChecked()) toggle_all.setChecked(false);
                is_overlay_active = false;
                currentShownLunchBar = Lunchbar.make(
                        getActivityView(),
                        R.string.toast_disabled5,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }
    }

    public void startEnableDisable() {
        if (!is_overlay_active) {
            is_overlay_active = true;
            compile_enable_mode = false;
            enable_mode = false;
            disable_mode = false;
            enable_disable_mode = true;

            overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
            checkedOverlays = new ArrayList<>();


            for (int i = 0; i < overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = overlaysLists.get(i);
                if (currentOverlay.isSelected()) checkedOverlays.add(currentOverlay);
                else {
                    currentOverlay.setSelected(false);
                    mAdapter.notifyDataSetChanged();
                }
            }
            if (!checkedOverlays.isEmpty()) {
                getThemeCache phase2 = new getThemeCache(this);
                if (base_spinner.getSelectedItemPosition() != 0 &&
                        base_spinner.getVisibility() == View.VISIBLE) {
                    phase2.execute(base_spinner.getSelectedItem().toString());

                } else {
                    phase2.execute("");
                }
                for (OverlaysItem overlay : checkedOverlays) {
                    Log.d("OverlayTargetPackageKiller", "Killing package : " + overlay
                            .getPackageName());
                    am.killBackgroundProcesses(overlay.getPackageName());
                }
            } else {
                if (toggle_all.isChecked()) toggle_all.setChecked(false);
                is_overlay_active = false;
                currentShownLunchBar = Lunchbar.make(
                        getActivityView(),
                        R.string.toast_disabled5,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }
    }

    public void setMixAndMatchMode(boolean newValue) {
        mixAndMatchMode = newValue;
        prefs.edit().putBoolean("enable_swapping_overlays", mixAndMatchMode).apply();
        updateEnabledOverlays();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.tab_overlays, container, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        am = (ActivityManager) getContext().getSystemService(ACTIVITY_SERVICE);

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new RefreshReceiver();
        IntentFilter filter = new IntentFilter("Overlay.REFRESH");
        localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        localBroadcastManager.registerReceiver(refreshReceiver, filter);

        theme_name = getArguments().getString("theme_name");
        theme_pid = getArguments().getString("theme_pid");
        String encrypt_check =
                References.getOverlayMetadata(getContext(), theme_pid, metadataEncryption);

        if (encrypt_check != null && encrypt_check.equals(metadataEncryptionValue)) {
            byte[] encryption_key = getArguments().getByteArray("encryption_key");
            byte[] iv_encrypt_key = getArguments().getByteArray("iv_encrypt_key");
            try {
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(
                        Cipher.DECRYPT_MODE,
                        new SecretKeySpec(encryption_key, "AES"),
                        new IvParameterSpec(iv_encrypt_key)
                );
                Log.d(TAG, "Loading substratum theme in encrypted assets mode.");
                encrypted = true;
            } catch (Exception e) {
                Log.d(TAG,
                        "Loading substratum theme in decrypted assets mode due to an exception.");
                decryptedAssetsExceptionReached = true;
            }
        } else {
            Log.d(TAG, "Loading substratum theme in decrypted assets mode.");
        }

        if (decryptedAssetsExceptionReached) {
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

        mixAndMatchMode = prefs.getBoolean("enable_swapping_overlays", false);

        progressBar = root.findViewById(R.id.header_loading_bar);
        progressBar.setVisibility(View.GONE);

        materialProgressBar = root.findViewById(R.id.progress_bar_loader);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        mRecyclerView = root.findViewById(R.id.overlayRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayList<OverlaysItem> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new OverlaysAdapter(empty_array);
        mRecyclerView.setAdapter(empty_adapter);

        TextView toggle_all_overlays_text = root.findViewById(R.id.toggle_all_overlays_text);
        toggle_all_overlays_text.setVisibility(View.VISIBLE);

        File work_area = new File(EXTERNAL_STORAGE_CACHE);
        if (!work_area.exists() && work_area.mkdir()) {
            Log.d(TAG, "Updating the internal storage with proper file directories...");
        }

        // Adjust the behaviour of the mix and match toggle in the sheet
        toggle_all = root.findViewById(R.id.toggle_all_overlays);
        toggle_all.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    try {
                        overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                        for (int i = 0; i < overlaysLists.size(); i++) {
                            OverlaysItem currentOverlay = overlaysLists.get(i);
                            currentOverlay.setSelected(isChecked);
                            mAdapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Window has lost connection with the host.");
                    }
                });

        // Allow the user to toggle the select all switch by clicking on the bar above
        RelativeLayout toggleZone = root.findViewById(R.id.toggle_zone);
        toggleZone.setOnClickListener(v -> {
            try {
                toggle_all.setChecked(!toggle_all.isChecked());
                overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                for (int i = 0; i < overlaysLists.size(); i++) {
                    OverlaysItem currentOverlay = overlaysLists.get(i);
                    currentOverlay.setSelected(toggle_all.isChecked());
                    mAdapter.notifyDataSetChanged();
                }
            } catch (Exception e) {
                Log.e(TAG, "Window has lost connection with the host.");
            }
        });

        // Allow the user to swipe down to refresh the overlay list
        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
            for (int i = 0; i < overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = overlaysLists.get(i);
                currentOverlay.setSelected(false);
                currentOverlay.updateEnabledOverlays(updateEnabledOverlays());
                mAdapter.notifyDataSetChanged();
            }
            toggle_all.setChecked(false);
            swipeRefreshLayout.setRefreshing(false);
        });
        swipeRefreshLayout.setVisibility(View.GONE);

        /*
          PLUGIN TYPE 3: Parse each overlay folder to see if they have folder options
         */
        base_spinner = root.findViewById(R.id.type3_spinner);
        Overlays overlays = this;
        base_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long id) {
                if (pos == 0) {
                    toggle_all.setChecked(false);
                    new LoadOverlays(overlays).execute("");
                } else {
                    toggle_all.setChecked(false);
                    String[] commands = {arg0.getSelectedItem().toString()};
                    new LoadOverlays(overlays).execute(commands);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        base_spinner.setEnabled(false);

        try {
            Resources themeResources = getContext().getPackageManager()
                    .getResourcesForApplication(theme_pid);
            themeAssetManager = themeResources.getAssets();

            ArrayList<VariantItem> type3 = new ArrayList<>();
            ArrayList<String> stringArray = new ArrayList<>();

            File f = new File(getContext().getCacheDir().getAbsoluteFile() +
                    SUBSTRATUM_BUILDER_CACHE +
                    theme_pid + "/assets/overlays/android/");
            if (!References.checkOMS(getContext())) {
                File check_file = new File(getContext().getCacheDir().getAbsoluteFile() +
                        SUBSTRATUM_BUILDER_CACHE + theme_pid + "/assets/overlays_legacy/android/");
                if (check_file.exists() && check_file.isDirectory()) {
                    f = new File(check_file.getAbsolutePath());
                }
            }

            if (!References.isCachingEnabled(getContext())) {
                String[] listArray = themeAssetManager.list("overlays/android");
                Collections.addAll(stringArray, listArray);
            } else {
                File[] fileArray = f.listFiles();
                if (fileArray != null && fileArray.length > 0) {
                    for (File file : fileArray) {
                        stringArray.add(file.getName());
                    }
                }
            }

            if (stringArray.contains("type3") || stringArray.contains("type3.enc")) {
                InputStream inputStream;
                if (encrypted) {
                    inputStream = FileOperations.getInputStream(
                            themeAssetManager,
                            "overlays/android/type3.enc",
                            cipher);
                } else if (!References.isCachingEnabled(getContext())) {
                    inputStream = themeAssetManager.open("overlays/android/type3");
                } else {
                    inputStream = new FileInputStream(new File(f.getAbsolutePath() + "/type3"));
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
                    if (!current.equals("res") &&
                            !current.contains(".") &&
                            current.length() >= 6 &&
                            current.substring(0, 6).equals("type3_")) {
                        type3.add(new VariantItem(current.substring(6), null));
                    }
                }
                VariantAdapter adapter1 = new VariantAdapter(getActivity(), type3);
                if (type3.size() > 1) {
                    toggle_all_overlays_text.setVisibility(View.GONE);
                    base_spinner.setVisibility(View.VISIBLE);
                    base_spinner.setAdapter(adapter1);
                } else {
                    toggle_all_overlays_text.setVisibility(View.VISIBLE);
                    base_spinner.setVisibility(View.INVISIBLE);
                    new LoadOverlays(this).execute("");
                }
            } else {
                toggle_all_overlays_text.setVisibility(View.VISIBLE);
                base_spinner.setVisibility(View.INVISIBLE);
                new LoadOverlays(this).execute("");
            }
        } catch (Exception e) {
            if (base_spinner.getVisibility() == View.VISIBLE) {
                toggle_all_overlays_text.setVisibility(View.VISIBLE);
                base_spinner.setVisibility(View.INVISIBLE);
            }
            e.printStackTrace();
            Log.e(TAG, "Could not parse list of base options for this theme!");
        }

        // Enable job listener
        jobReceiver = new JobReceiver();
        IntentFilter intentFilter = new IntentFilter("Overlays.START_JOB");
        localBroadcastManager2 = LocalBroadcastManager.getInstance(getContext());
        localBroadcastManager2.registerReceiver(jobReceiver, intentFilter);
        return root;
    }

    protected List<String> updateEnabledOverlays() {
        List<String> state5 = ThemeManager.listOverlays(getContext(), ThemeManager.STATE_ENABLED);
        ArrayList<String> all = new ArrayList<>(state5);

        all_installed_overlays = new ArrayList<>();

        // ValidatorFilter out icon pack overlays from all overlays
        for (int i = 0; i < all.size(); i++) {
            if (!all.get(i).endsWith(".icon")) {
                all_installed_overlays.add(all.get(i));
            }
        }
        return new ArrayList<>(all_installed_overlays);
    }

    protected boolean checkActiveNotifications() {
        StatusBarNotification[] activeNotifications = mNotifyManager.getActiveNotifications();
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
        notificationIntent.putExtra("theme_name", theme_name);
        notificationIntent.putExtra("theme_pid", theme_pid);
        notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

        // Closing off the persistent notification
        if (checkActiveNotifications()) {
            mNotifyManager.cancel(id);
            mBuilder = new NotificationCompat.Builder(context, DEFAULT_NOTIFICATION_CHANNEL_ID);
            mBuilder.setAutoCancel(true);
            mBuilder.setProgress(0, 0, false);
            mBuilder.setOngoing(false);
            mBuilder.setContentIntent(intent);
            mBuilder.setSmallIcon(R.drawable.notification_warning_icon);
            mBuilder.setContentTitle(context.getString(R.string.notification_done_title));
            mBuilder.setContentText(context.getString(R.string.notification_some_errors_found));
            if (prefs.getBoolean("vibrate_on_compiled", false)) {
                mBuilder.setVibrate(new long[]{100, 200, 100, 500});
            }
            mNotifyManager.notify(id, mBuilder.build());
        }

        Toast.makeText(
                context,
                context.getString(R.string.toast_compiled_updated_with_errors),
                Toast.LENGTH_LONG).show();

        boolean save_logs = prefs.getBoolean("autosave_logchar", true);
        if (save_logs) {
            new SendErrorReport(
                    context,
                    theme_pid,
                    error_logs.toString(),
                    failed_packages.toString(),
                    true
            ).execute();
        }

        currentShownLunchBar = Lunchbar.make(
                getActivityView(),
                R.string.logcat_snackbar_text,
                Lunchbar.LENGTH_INDEFINITE);
        currentShownLunchBar.setAction(getString(R.string.logcat_snackbar_button), view -> {
            currentShownLunchBar.dismiss();
            invokeLogCharDialog(context);
        });
        currentShownLunchBar.show();
    }

    public void invokeLogCharDialog(Context context) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context)
                .setTitle(R.string.logcat_dialog_title)
                .setMessage("\n" + error_logs)
                .setNeutralButton(R.string
                        .customactivityoncrash_error_activity_error_details_close, null)
                .setNegativeButton(R.string
                                .customactivityoncrash_error_activity_error_details_copy,
                        (dialog1, which) -> {
                            ClipboardManager clipboard =
                                    (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("substratum_log", error_logs);
                            if (clipboard != null) {
                                clipboard.setPrimaryClip(clip);
                            }
                            currentShownLunchBar = Lunchbar.make(
                                    getActivityView(),
                                    R.string.logcat_dialog_copy_success,
                                    Lunchbar.LENGTH_LONG);
                            currentShownLunchBar.show();
                        });

        if (References.getOverlayMetadata(context, theme_pid, metadataEmail) != null) {
            builder.setPositiveButton("Send", (dialogInterface, i) ->
                    new SendErrorReport(
                            context,
                            theme_pid,
                            error_logs.toString(),
                            failed_packages.toString(),
                            false
                    ).execute());
        }
        builder.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (phase3_mainFunction != null && mNotifyManager != null) {
            if (phase3_mainFunction.getStatus() == AsyncTask.Status.RUNNING) {
                mNotifyManager.cancel(id);
            }
        }
        try {
            localBroadcastManager.unregisterReceiver(refreshReceiver);
        } catch (IllegalArgumentException e) {
            // Unregistered already
        }
        try {
            localBroadcastManager2.unregisterReceiver(jobReceiver);
        } catch (IllegalArgumentException e) {
            // Unregistered already
        }
    }

    protected boolean needsRecreate(Context context) {
        for (OverlaysItem oi : checkedOverlays) {
            String packageName = oi.getPackageName();
            if (packageName.equals("android") || packageName.equals("projekt.substratum")) {
                if (!enable_mode && !disable_mode && !enable_disable_mode &&
                        ThemeManager.isOverlayEnabled(context, oi.getFullOverlayParameters())) {
                    return false;
                } else if (enable_mode || disable_mode || compile_enable_mode
                        || enable_disable_mode) {
                    return false;
                }
            }
        }
        return References.checkOMS(getContext()) && !has_failed;
    }

    public VariantItem setTypeOneSpinners(Object typeArrayRaw,
                                          String package_identifier,
                                          String type) {
        InputStream inputStream = null;
        try {
            // Bypasses the caching mode, since if it's encrypted, caching mode is useless anyways
            if (encrypted) {
                inputStream = FileOperations.getInputStream(
                        themeAssetManager,
                        overlaysDir + "/" + package_identifier + "/type1" + type + ".enc",
                        cipher);
            } else if (References.isCachingEnabled(getContext())) {
                inputStream = new FileInputStream(
                        new File(((File) typeArrayRaw).getAbsolutePath() + "/type1" + type));
            } else {
                inputStream = themeAssetManager.open(
                        overlaysDir + "/" + package_identifier + "/type1" + type);
            }
        } catch (IOException ioe) {
            // Suppress warning
        }

        // Parse current default types on type3 base resource folders
        String parsedVariant = "";
        try {
            if (base_spinner.getSelectedItemPosition() != 0) {
                parsedVariant = base_spinner.getSelectedItem().toString().replaceAll("\\s+", "");
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

            if (encrypted) {
                try (InputStream name = FileOperations.getInputStream(
                        themeAssetManager,
                        overlaysDir + "/" + package_identifier + suffix +
                                "/values/type1" + type + ".xml.enc",
                        cipher)) {
                    hex = References.getOverlayResource(name);
                } catch (IOException e) {
                    // Suppress warning
                }
            } else {
                try (InputStream name = themeAssetManager.open(
                        overlaysDir + "/" + package_identifier + suffix +
                                "/values/type1" + type + ".xml")) {
                    hex = References.getOverlayResource(name);
                } catch (IOException e) {
                    // Suppress warning
                }
            }
            return new VariantItem(formatter, hex);
        } catch (Exception e) {
            // When erroring out, put the default spinner text
            Log.d(TAG, "Falling back to default base variant text...");
            String hex = null;
            if (encrypted) {
                try (InputStream input = FileOperations.getInputStream(
                        themeAssetManager,
                        overlaysDir + "/" + package_identifier +
                                suffix + "/values/type1" + type + ".xml.enc",
                        cipher)) {
                    hex = References.getOverlayResource(input);
                } catch (IOException ioe) {
                    // Suppress warning
                }
            } else {
                try (InputStream input = themeAssetManager.open(overlaysDir +
                        "/" + package_identifier + suffix + "/values/type1" + type + ".xml")) {
                    hex = References.getOverlayResource(input);
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
        if (encrypted) {
            try (InputStream inputStream = FileOperations.getInputStream(themeAssetManager,
                    "overlays/" + package_identifier + "/" + current, cipher)) {
                String hex = References.getOverlayResource(inputStream);

                return new VariantItem(
                        current.substring(7, current.length() - 8), hex);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (InputStream inputStream = themeAssetManager.open(
                    "overlays/" + package_identifier + "/" + current)) {
                String hex = References.getOverlayResource(inputStream);

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
                refreshList();
                FileOperations.delete(getContext(),
                        new File(late_install.get(0)).getAbsolutePath());
                if (late_install != null && late_install.size() > 0) late_install.remove(0);
                if (late_install.size() > 0) {
                    uninstallMultipleAPKs();
                }
        }
    }

    private void uninstallMultipleAPKs() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(
                getContext(),
                getContext().getApplicationContext().getPackageName() + ".provider",
                new File(late_install.get(0)));
        intent.setDataAndType(
                uri,
                "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, 2486);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!toggle_all.isChecked()) refreshList();
    }

    private void refreshList() {
        if (mAdapter != null) mAdapter.notifyDataSetChanged();

        if (overlaysLists != null) {
            for (int i = 0; i < overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = overlaysLists.get(i);
                if (currentOverlay.isSelected()) {
                    currentOverlay.setSelected(false);
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    private static class SendErrorReport extends AsyncTask<Void, Void, File> {
        private WeakReference<Context> ref;
        private String themePid;
        private String errorLog;
        private String themeName, themeAuthor, themeEmail;
        private String emailSubject, emailBody;
        private String failedPackages;
        private ProgressDialog progressDialog;
        private Boolean autosaveInstance;

        SendErrorReport(Context context,
                        String themePid,
                        String errorLog,
                        String failedPackages,
                        Boolean autosaveInstance) {
            ref = new WeakReference<>(context);
            this.themePid = themePid;
            this.errorLog = errorLog;
            this.failedPackages = failedPackages;
            this.autosaveInstance = autosaveInstance;

            themeName = References.grabPackageName(context, themePid);
            themeAuthor = References.getOverlayMetadata(context, themePid, References
                    .metadataAuthor);
            themeEmail = References.getOverlayMetadata(context, themePid, References
                    .metadataEmail);

            emailSubject = String.format(
                    context.getString(R.string.logcat_email_subject), themeName);
            emailBody = String.format(
                    context.getString(R.string.logcat_email_body), themeAuthor, themeName);
        }

        @Override
        protected void onPreExecute() {
            Context context = ref.get();
            if (context != null && !autosaveInstance) {
                progressDialog = new ProgressDialog(context);
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(false);
                progressDialog.setMessage(
                        context.getString(R.string.logcat_processing_dialog));
                progressDialog.show();
            }
        }

        @Override
        protected File doInBackground(Void... sUrl) {
            Context context = ref.get();
            if (context != null) {
                String rom = References.checkFirmwareSupport(context,
                        context.getString(R.string.supported_roms_url),
                        "supported_roms.xml");
                String theme_version = References.grabAppVersion(context, themePid);
                String rom_version = Build.VERSION.RELEASE + " - " +
                        (!rom.isEmpty() ? rom : "Unknown");

                String device = Build.MODEL + " (" + Build.DEVICE + ") " +
                        "[" + Build.FINGERPRINT + "]";
                String xposed = References.checkXposedVersion();
                if (!xposed.isEmpty()) device += " {" + xposed + "}";

                String attachment = String.format(
                        context.getString(R.string.logcat_attachment_body),
                        themeName,
                        device,
                        rom_version,
                        String.valueOf(BuildConfig.VERSION_CODE),
                        theme_version,
                        failedPackages,
                        errorLog);

                File log = null;
                if (autosaveInstance) {
                    References.writeLogCharFile(themePid, attachment);
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
            Context context = ref.get();
            if (context != null) {
                if (!autosaveInstance && result != null) {
                    if (progressDialog != null) progressDialog.dismiss();

                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("message/rfc822");
                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{themeEmail});
                    i.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
                    i.putExtra(Intent.EXTRA_TEXT, emailBody);
                    i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(result));
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
        private WeakReference<Overlays> ref;

        LoadOverlays(Overlays fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            Overlays fragment = ref.get();
            if (fragment != null) {
                if (fragment.materialProgressBar != null) {
                    fragment.materialProgressBar.setVisibility(View.VISIBLE);
                }
                fragment.mRecyclerView.setVisibility(View.INVISIBLE);
                fragment.swipeRefreshLayout.setVisibility(View.GONE);
                fragment.toggle_all.setEnabled(false);
                fragment.base_spinner.setEnabled(false);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Overlays fragment = ref.get();
            if (fragment != null) {
                if (fragment.materialProgressBar != null) {
                    fragment.materialProgressBar.setVisibility(View.GONE);
                }
                fragment.toggle_all.setEnabled(true);
                fragment.base_spinner.setEnabled(true);
                fragment.mAdapter = new OverlaysAdapter(fragment.values2);
                fragment.mRecyclerView.setAdapter(fragment.mAdapter);
                fragment.mAdapter.notifyDataSetChanged();
                fragment.mRecyclerView.setVisibility(View.VISIBLE);
                fragment.swipeRefreshLayout.setVisibility(View.VISIBLE);
            }
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(String... sUrl) {
            Overlays fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.getActivity();
                // Refresh asset manager
                try {
                    if (!References.isCachingEnabled(context)) {
                        try {
                            Resources themeResources = context.getPackageManager()
                                    .getResourcesForApplication(fragment.theme_pid);
                            fragment.themeAssetManager = themeResources.getAssets();
                        } catch (PackageManager.NameNotFoundException e) {
                            // Suppress exception
                        }
                    }

                    // Grab the current theme_pid's versionName so that we can version our overlays
                    fragment.versionName = References.grabAppVersion(context, fragment.theme_pid);
                    List<String> state5overlays = fragment.updateEnabledOverlays();
                    String parse1_themeName = fragment.theme_name.replaceAll("\\s+", "");
                    String parse2_themeName = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");

                    ArrayList<String> values = new ArrayList<>();
                    fragment.values2 = new ArrayList<>();

                    // Buffer the initial values list so that we get the list of packages
                    // inside this theme

                    ArrayList<String> overlaysFolder = new ArrayList<>();
                    if (References.isCachingEnabled(context)) {
                        File overlaysDirectory = new File(context.getCacheDir().getAbsoluteFile() +
                                References.SUBSTRATUM_BUILDER_CACHE + fragment.theme_pid +
                                "/assets/overlays/");

                        if (!References.checkOMS(context)) {
                            File check_file = new File(context.getCacheDir().getAbsoluteFile() +
                                    References.SUBSTRATUM_BUILDER_CACHE + fragment.theme_pid +
                                    "/assets/overlays_legacy/");
                            if (check_file.exists() && check_file.isDirectory()) {
                                overlaysDirectory = new File(check_file.getAbsolutePath());
                            }
                        }

                        File[] fileArray = overlaysDirectory.listFiles();
                        if (fileArray != null && fileArray.length > 0) {
                            for (File file : fileArray) {
                                overlaysFolder.add(file.getName());
                            }
                        }
                    } else {
                        try {
                            String[] overlayList = fragment.themeAssetManager.list(Overlays
                                    .overlaysDir);
                            Collections.addAll(overlaysFolder, overlayList);
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }

                    SharedPreferences prefs =
                            PreferenceManager.getDefaultSharedPreferences(context);

                    Boolean showDangerous = !prefs.getBoolean("show_dangerous_samsung_overlays",
                            false);

                    values.addAll(overlaysFolder.stream().filter(package_name -> (References
                            .isPackageInstalled(context, package_name) ||
                            References.allowedSystemUIOverlay(package_name) ||
                            References.allowedSettingsOverlay(package_name)) &&
                            (!showDangerous || !ThemeManager.blacklisted(
                                    package_name,
                                    References.isSamsung(context) &&
                                            !References.isSamsungTheme(
                                                    context, fragment.theme_pid))))
                            .collect(Collectors.toList()));

                    // Create the map for {package name: package identifier}
                    HashMap<String, String> unsortedMap = new HashMap<>();

                    // Then let's convert all the package names to their app names
                    for (int i = 0; i < values.size(); i++) {
                        try {
                            if (References.allowedSystemUIOverlay(values.get(i))) {
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
                            } else if (References.allowedSettingsOverlay(values.get(i))) {
                                String package_name = "";
                                switch (values.get(i)) {
                                    case "com.android.settings.icons":
                                        package_name = context.getString(R.string.settings_icons);
                                        break;
                                }
                                unsortedMap.put(values.get(i), package_name);
                            } else if (References.allowedAppOverlay(values.get(i))) {
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
                            ArrayList<String> typeArray = new ArrayList<>();

                            Object typeArrayRaw;
                            if (References.isCachingEnabled(context)) {
                                typeArrayRaw = new File(context.getCacheDir().getAbsoluteFile() +
                                        References.SUBSTRATUM_BUILDER_CACHE + fragment.theme_pid
                                        + "/assets/overlays/" + package_identifier);
                            } else {
                                // Begin the no caching algorithm
                                typeArrayRaw = fragment.themeAssetManager.list(
                                        Overlays.overlaysDir + "/" + package_identifier);

                                // Sort the typeArray so that the types are asciibetical
                                Collections.addAll(typeArray, (String[]) typeArrayRaw);
                                Collections.sort(typeArray);
                            }

                            if (!References.checkOMS(context)) {
                                File check_file = new File(
                                        context.getCacheDir().getAbsoluteFile() +
                                                References.SUBSTRATUM_BUILDER_CACHE + fragment
                                                .theme_pid
                                                + "/assets/overlays_legacy/" + package_identifier +
                                                "/");
                                if (check_file.exists() && check_file.isDirectory()) {
                                    typeArrayRaw = new File(check_file.getAbsolutePath());
                                }
                            }

                            File[] fileArray;
                            if (References.isCachingEnabled(context)) {
                                fileArray = ((File) typeArrayRaw).listFiles();
                                if (fileArray != null && fileArray.length > 0) {
                                    for (File file : fileArray) {
                                        typeArray.add(file.getName());
                                    }
                                }
                            }

                            // Sort the typeArray so that the types are asciibetical
                            Collections.sort(typeArray);

                            // Let's start adding the type xmls to be parsed into the spinners
                            if (typeArray.contains("type1a") || typeArray.contains("type1a.enc")) {
                                type1a.add(fragment.setTypeOneSpinners(
                                        typeArrayRaw, package_identifier, "a"));
                            }

                            if (typeArray.contains("type1b") || typeArray.contains("type1b.enc")) {
                                type1b.add(fragment.setTypeOneSpinners(
                                        typeArrayRaw, package_identifier, "b"));
                            }

                            if (typeArray.contains("type1c") || typeArray.contains("type1c.enc")) {
                                type1c.add(fragment.setTypeOneSpinners(
                                        typeArrayRaw, package_identifier, "c"));
                            }

                            boolean type2checker = false;
                            for (int i = 0; i < typeArray.size(); i++) {
                                if (typeArray.get(i).startsWith("type2_")) {
                                    type2checker = true;
                                    break;
                                }
                            }
                            if (References.isCachingEnabled(fragment.getContext()) &&
                                    (typeArray.contains("type2") ||
                                            typeArray.contains("type2.enc"))) {
                                type2.add(fragment.setTypeTwoFourSpinners(
                                        new InputStreamReader(new FileInputStream(
                                                new File(((File) typeArrayRaw).getAbsolutePath() +
                                                        "/type2"))), 2));
                            } else if (typeArray.contains("type2") ||
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
                            if (References.isCachingEnabled(fragment.getContext()) &&
                                    (typeArray.contains("type4") ||
                                            typeArray.contains("type4.enc"))) {
                                type4.add(fragment.setTypeTwoFourSpinners(
                                        new InputStreamReader(new FileInputStream(
                                                new File(((File) typeArrayRaw).getAbsolutePath() +
                                                        "/type4"))), 4));
                            } else if (typeArray.contains("type4") ||
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
                                    if (!current.equals("res")) {
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
                                            if (current.substring(0, 6).equals("type2_")) {
                                                type2.add(
                                                        new VariantItem(
                                                                current.substring(6), null));
                                            } else if (current.substring(0, 6).equals("type4_")) {
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
                                                References.checkOMS(context));
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
                                                References.checkOMS(context));
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
                    if (mAdapter != null) startCompileEnableMode();
                    break;
                case "CompileUpdate":
                    if (mAdapter != null) startCompileUpdateMode();
                    break;
                case "Disable":
                    if (mAdapter != null) startDisable();
                    break;
                case "Enable":
                    if (mAdapter != null) startEnable();
                    break;
                case "EnableDisable":
                    if (mAdapter != null) startEnableDisable();
                    break;
                case "MixAndMatchMode":
                    if (mAdapter != null) {
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
            if (References.isSamsung(context) && Root.checkRootAccess()) {
                if (overlaysWaiting > 0) {
                    --overlaysWaiting;
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
            refreshList();
        }
    }
}