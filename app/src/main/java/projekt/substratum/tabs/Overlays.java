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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.InformationActivity;
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

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.om.OverlayInfo.STATE_APPROVED_ENABLED;
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
    public int id = References.notification_id;
    public ArrayList<OverlaysItem> values2;
    public RecyclerView mRecyclerView;
    public Spinner base_spinner;
    public SharedPreferences prefs;
    public ArrayList<String> final_runner, late_install;
    public boolean mixAndMatchMode, enable_mode, disable_mode, compile_enable_mode;
    public ArrayList<String> all_installed_overlays;
    public Switch toggle_all;
    public SwipeRefreshLayout swipeRefreshLayout;
    public ProgressBar progressBar;
    public Boolean is_active = false;
    public StringBuilder error_logs;
    public MaterialProgressBar materialProgressBar;
    public double current_amount = 0;
    public double total_amount = 0;
    public String current_dialog_overlay;
    public ProgressBar dialogProgress;
    public FinishReceiver finishReceiver;
    public ArrayList<String> final_command;
    public boolean isWaiting;
    public AssetManager themeAssetManager;
    public Boolean missingType3 = false;
    public JobReceiver jobReceiver;
    public LocalBroadcastManager localBroadcastManager, localBroadcastManager2;
    public String type1a = "";
    public String type1b = "";
    public String type1c = "";
    public String type2 = "";
    public String type3 = "";
    public OverlayFunctions.Phase3_mainFunction phase3_mainFunction;
    public Boolean encrypted = false;
    public Cipher cipher = null;
    public RefreshReceiver refreshReceiver;
    public ActivityManager am;

    protected void logTypes() {
        if (ENABLE_PACKAGE_LOGGING) {
            Log.d("Theme Type1a Resource", type1a);
            Log.d("Theme Type1b Resource", type1b);
            Log.d("Theme Type1c Resource", type1c);
            Log.d("Theme Type2  Resource", type2);
            Log.d("Theme Type3  Resource", type3);
        }
    }

    protected View getActivityView() {
        return ((ViewGroup) getActivity().findViewById(android.R.id.content)).getChildAt(0);
    }

    public void startCompileEnableMode() {
        if (!is_active) {
            is_active = true;
            compile_enable_mode = true;
            enable_mode = false;
            disable_mode = false;

            overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
            checkedOverlays = new ArrayList<>();

            for (int i = 0; i < overlaysLists.size(); i++) {
                OverlaysItem currentOverlay = overlaysLists.get(i);
                if (currentOverlay.isSelected()) {
                    checkedOverlays.add(currentOverlay);
                }
            }
            if (!checkedOverlays.isEmpty()) {
                OverlayFunctions.Phase2_InitializeCache phase2 = new OverlayFunctions
                        .Phase2_InitializeCache(this);
                if (base_spinner.getSelectedItemPosition() != 0 &&
                        base_spinner.getVisibility() == View.VISIBLE) {
                    phase2.execute(base_spinner.getSelectedItem().toString());
                } else {
                    phase2.execute("");
                }
            } else {
                if (toggle_all.isChecked()) toggle_all.setChecked(false);
                is_active = false;
                Lunchbar.make(
                        getActivityView(),
                        R.string.toast_disabled5,
                        Lunchbar.LENGTH_LONG)
                        .show();
            }
        }
    }

    public void startCompileUpdateMode() {
        if (!is_active) {
            is_active = true;
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
                OverlayFunctions.Phase2_InitializeCache phase2 = new OverlayFunctions
                        .Phase2_InitializeCache(this);
                if (base_spinner.getSelectedItemPosition() != 0 &&
                        base_spinner.getVisibility() == View.VISIBLE) {
                    phase2.execute(base_spinner.getSelectedItem().toString());
                } else {
                    phase2.execute("");
                }
            } else {
                if (toggle_all.isChecked()) toggle_all.setChecked(false);
                is_active = false;
                Lunchbar.make(
                        getActivityView(),
                        R.string.toast_disabled5,
                        Lunchbar.LENGTH_LONG)
                        .show();
            }
        }
    }

    public void startDisable() {
        if (!is_active) {
            is_active = true;

            overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
            checkedOverlays = new ArrayList<>();

            if (References.checkOMS(getContext())) {
                compile_enable_mode = false;
                enable_mode = false;
                disable_mode = true;

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
                    OverlayFunctions.Phase2_InitializeCache phase2 = new OverlayFunctions
                            .Phase2_InitializeCache(this);
                    if (base_spinner.getSelectedItemPosition() != 0 &&
                            base_spinner.getVisibility() == View.VISIBLE) {
                        phase2.execute(base_spinner.getSelectedItem().toString());
                    } else {
                        phase2.execute("");
                    }
                } else {
                    if (toggle_all.isChecked()) toggle_all.setChecked(false);
                    is_active = false;
                    Lunchbar.make(
                            getActivityView(),
                            R.string.toast_disabled5,
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            } else {
                compile_enable_mode = false;
                enable_mode = false;
                disable_mode = true;

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
                        for (int i = 0; i < checkedOverlays.size(); i++) {
                            Uri packageURI = Uri.parse("package:" +
                                    checkedOverlays.get(i).getFullOverlayParameters());
                            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                            startActivity(uninstallIntent);
                        }
                    } else {
                        for (int i = 0; i < checkedOverlays.size(); i++) {
                            FileOperations.mountRW();
                            FileOperations.delete(getContext(), current_directory +
                                    checkedOverlays.get(i).getPackageName() + "." +
                                    checkedOverlays.get(i).getThemeName() + ".apk");
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
                    is_active = false;
                    Lunchbar.make(
                            getActivityView(),
                            R.string.toast_disabled5,
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
                is_active = false;
                disable_mode = false;
            }
        }
    }

    public void startEnable() {
        if (!is_active) {
            is_active = true;
            compile_enable_mode = false;
            enable_mode = true;
            disable_mode = false;

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
                OverlayFunctions.Phase2_InitializeCache phase2 = new OverlayFunctions
                        .Phase2_InitializeCache(this);
                if (base_spinner.getSelectedItemPosition() != 0 &&
                        base_spinner.getVisibility() == View.VISIBLE) {
                    phase2.execute(base_spinner.getSelectedItem().toString());

                } else {
                    phase2.execute("");
                }
            } else {
                if (toggle_all.isChecked()) toggle_all.setChecked(false);
                is_active = false;
                Lunchbar.make(
                        getActivityView(),
                        R.string.toast_disabled5,
                        Lunchbar.LENGTH_LONG)
                        .show();
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

        am = (ActivityManager) getContext().getSystemService(Activity.ACTIVITY_SERVICE);

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new RefreshReceiver();
        IntentFilter filter = new IntentFilter("Overlay.REFRESH");
        localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        localBroadcastManager.registerReceiver(refreshReceiver, filter);

        theme_name = InformationActivity.getThemeName();
        theme_pid = InformationActivity.getThemePID();
        String encrypt_check =
                References.getOverlayMetadata(getContext(), theme_pid, metadataEncryption);

        if (encrypt_check != null && encrypt_check.equals(metadataEncryptionValue)) {
            byte[] encryption_key = InformationActivity.getEncryptionKey();
            byte[] iv_encrypt_key = InformationActivity.getIVEncryptKey();
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
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Loading substratum theme in decrypted assets mode.");
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

        File work_area = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                EXTERNAL_STORAGE_CACHE);
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
                    new OverlayFunctions.LoadOverlays(overlays).execute("");
                } else {
                    toggle_all.setChecked(false);
                    String[] commands = {arg0.getSelectedItem().toString()};
                    new OverlayFunctions.LoadOverlays(overlays).execute(commands);
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
                    new OverlayFunctions.LoadOverlays(this).execute("");
                }
            } else {
                toggle_all_overlays_text.setVisibility(View.VISIBLE);
                base_spinner.setVisibility(View.INVISIBLE);
                new OverlayFunctions.LoadOverlays(this).execute("");
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
        List<String> state5 = ThemeManager.listOverlays(getContext(), STATE_APPROVED_ENABLED);
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

        final Dialog dialog = new Dialog(context, android.R.style.Theme_DeviceDefault_Dialog);
        dialog.setContentView(R.layout.logcat_dialog);
        dialog.setTitle(R.string.logcat_dialog_title);
        if (dialog.getWindow() != null)
            dialog.getWindow().setLayout(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT);

        TextView text = dialog.findViewById(R.id.textField);
        text.setText(error_logs);
        ImageButton confirm = dialog.findViewById(R.id.confirm);
        confirm.setOnClickListener(view -> dialog.dismiss());

        ImageButton copy_clipboard = dialog.findViewById(R.id.copy_clipboard);
        copy_clipboard.setOnClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("substratum_log", error_logs);
            clipboard.setPrimaryClip(clip);
            Lunchbar.make(
                    getActivityView(),
                    R.string.logcat_dialog_copy_success,
                    Lunchbar.LENGTH_LONG)
                    .show();
            dialog.dismiss();
        });

        ImageButton send = dialog.findViewById(R.id.send);
        send.setVisibility(View.GONE);
        if (References.getOverlayMetadata(context, theme_pid, metadataEmail) != null) {
            send.setVisibility(View.VISIBLE);
            send.setOnClickListener(v -> {
                dialog.dismiss();
                new OverlayFunctions.SendErrorReport(context, theme_pid, error_logs.toString())
                        .execute();
            });
        }
        dialog.show();
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
            getContext().unregisterReceiver(finishReceiver);
            localBroadcastManager2.unregisterReceiver(jobReceiver);
        } catch (IllegalArgumentException e) {
            // Unregistered already
        }
    }

    protected boolean needsRecreate(Context context) {
        for (OverlaysItem oi : checkedOverlays) {
            String packageName = oi.getPackageName();
            if (packageName.equals("android") || packageName.equals("projekt.substratum")) {
                if (!enable_mode && !disable_mode &&
                        ThemeManager.isOverlayEnabled(context, oi.getFullOverlayParameters())) {
                    return false;
                } else if (enable_mode || disable_mode || compile_enable_mode) {
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
                    e.printStackTrace();
                }
            } else {
                try (InputStream name = themeAssetManager.open(
                        overlaysDir + "/" + package_identifier + suffix +
                                "/values/type1" + type + ".xml")) {
                    hex = References.getOverlayResource(name);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return new VariantItem(formatter, hex);
        } catch (IOException e) {
            e.printStackTrace();
            // When erroring out, put the default spinner text
            Log.e(TAG, "There was an error parsing asset file!");
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

    public VariantItem setTypeTwoSpinners(InputStreamReader inputStreamReader) {
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            return new VariantItem(String.format(getString(R.string
                    .overlays_variant_substitute), reader.readLine()), null);
        } catch (IOException e) {
            Log.e(TAG, "There was an error parsing asset file!");
            return new VariantItem(getString(R.string.overlays_variant_default_2), null);
        }
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

    private void refreshList() {
        if (mAdapter != null) mAdapter.notifyDataSetChanged();

        for (int i = 0; i < overlaysLists.size(); i++) {
            OverlaysItem currentOverlay = overlaysLists.get(i);
            if (currentOverlay.isSelected()) {
                currentOverlay.setSelected(false);
            }
            am.killBackgroundProcesses(currentOverlay.getPackageName());

            mAdapter.notifyDataSetChanged();
        }
    }

    protected static class FinishReceiver extends BroadcastReceiver {
        private WeakReference<Overlays> ref;

        FinishReceiver(Overlays fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String PRIMARY_COMMAND_KEY = "primary_command_key";
            String COMMAND_VALUE_JOB_COMPLETE = "job_complete";
            String command = intent.getStringExtra(PRIMARY_COMMAND_KEY);

            if (command.equals(COMMAND_VALUE_JOB_COMPLETE)) {
                Log.d(TAG,
                        "Substratum is now refreshing its resources after " +
                                "successful job completion!");
                ref.get().isWaiting = false;
            }
        }
    }

    protected class JobReceiver extends BroadcastReceiver {
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
            refreshList();
        }
    }
}