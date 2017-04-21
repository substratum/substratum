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

import android.app.Dialog;
import android.app.Notification;
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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.BuildConfig;
import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.adapters.tabs.overlays.OverlaysAdapter;
import projekt.substratum.adapters.tabs.overlays.OverlaysItem;
import projekt.substratum.adapters.tabs.overlays.VariantAdapter;
import projekt.substratum.adapters.tabs.overlays.VariantItem;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.MasqueradeService;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.services.notification.NotificationButtonReceiver;
import projekt.substratum.util.compilers.CacheCreator;
import projekt.substratum.util.compilers.SubstratumBuilder;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.om.OverlayInfo.STATE_APPROVED_ENABLED;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.MASQUERADE_PACKAGE;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.REFRESH_WINDOW_DELAY;
import static projekt.substratum.common.References.STATUS_CHANGED;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.References.checkThemeInterfacer;
import static projekt.substratum.common.References.isPackageInstalled;
import static projekt.substratum.util.files.MapUtils.sortMapByValues;

public class Overlays extends Fragment {

    private static final String overlaysDir = "overlays";
    private static final String TAG = SUBSTRATUM_BUILDER;
    private static final int THREAD_WAIT_DURATION = 500;
    private TextView loader_string;
    private ProgressDialog mProgressDialog;
    private SubstratumBuilder sb;
    private List<OverlaysItem> overlaysLists, checkedOverlays;
    private RecyclerView.Adapter mAdapter;
    private String theme_name, theme_pid, versionName;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private boolean has_initialized_cache = false;
    private boolean has_failed = false;
    private int fail_count;
    private int id = References.notification_id;
    private ArrayList<OverlaysItem> values2;
    private RecyclerView mRecyclerView;
    private Spinner base_spinner;
    private SharedPreferences prefs;
    private ArrayList<String> final_runner, late_install;
    private boolean mixAndMatchMode, enable_mode, disable_mode, compile_enable_mode;
    private ArrayList<String> all_installed_overlays;
    private Context mContext;
    private Switch toggle_all;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private Boolean is_active = false;
    private String error_logs = "";
    private String themer_email, theme_author;
    private MaterialProgressBar materialProgressBar;
    private double current_amount = 0;
    private double total_amount = 0;
    private String current_dialog_overlay;
    private ProgressBar dialogProgress;
    private FinishReceiver finishReceiver;
    private ArrayList<String> final_command;
    private boolean isWaiting;
    private AssetManager themeAssetManager;
    private Boolean missingType3 = false;
    private JobReceiver jobReceiver;
    private LocalBroadcastManager localBroadcastManager;

    private View getActivityView() {
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
                if (base_spinner.getSelectedItemPosition() != 0 &&
                        base_spinner.getVisibility() == View.VISIBLE) {
                    Phase2_InitializeCache phase2_initializeCache = new Phase2_InitializeCache();
                    phase2_initializeCache.execute(base_spinner.getSelectedItem().toString());
                } else {
                    Phase2_InitializeCache phase2_initializeCache = new Phase2_InitializeCache();
                    phase2_initializeCache.execute("");
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
                if (base_spinner.getSelectedItemPosition() != 0 &&
                        base_spinner.getVisibility() == View.VISIBLE) {
                    Phase2_InitializeCache phase2_initializeCache = new Phase2_InitializeCache();
                    phase2_initializeCache.execute(base_spinner.getSelectedItem().toString());
                } else {
                    Phase2_InitializeCache phase2_initializeCache = new Phase2_InitializeCache();
                    phase2_initializeCache.execute("");
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

            if (References.checkOMS(mContext)) {
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
                    if (base_spinner.getSelectedItemPosition() != 0 &&
                            base_spinner.getVisibility() == View.VISIBLE) {
                        Phase2_InitializeCache phase2_initializeCache =
                                new Phase2_InitializeCache();
                        phase2_initializeCache.execute(base_spinner.getSelectedItem().toString());
                    } else {
                        Phase2_InitializeCache phase2_initializeCache =
                                new Phase2_InitializeCache();
                        phase2_initializeCache.execute("");
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
                    for (int i = 0; i < checkedOverlays.size(); i++) {
                        FileOperations.mountRW();
                        FileOperations.delete(mContext, current_directory +
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
                    Toast.makeText(mContext,
                            getString(R.string.toast_disabled6),
                            Toast.LENGTH_SHORT).show();
                    AlertDialog.Builder alertDialogBuilder =
                            new AlertDialog.Builder(mContext);
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
                if (base_spinner.getSelectedItemPosition() != 0 &&
                        base_spinner.getVisibility() == View.VISIBLE) {
                    Phase2_InitializeCache phase2_initializeCache = new Phase2_InitializeCache();
                    phase2_initializeCache.execute(base_spinner.getSelectedItem().toString());

                } else {
                    Phase2_InitializeCache phase2_initializeCache = new Phase2_InitializeCache();
                    phase2_initializeCache.execute("");
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
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_1, container, false);
        mContext = getContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        theme_name = InformationActivity.getThemeName();
        theme_pid = InformationActivity.getThemePID();

        mixAndMatchMode = prefs.getBoolean("enable_swapping_overlays", false);

        progressBar = (ProgressBar) root.findViewById(R.id.header_loading_bar);
        progressBar.setVisibility(View.GONE);

        materialProgressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        mRecyclerView = (RecyclerView) root.findViewById(R.id.overlayRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        ArrayList<OverlaysItem> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new OverlaysAdapter(empty_array);
        mRecyclerView.setAdapter(empty_adapter);

        TextView toggle_all_overlays_text =
                (TextView) root.findViewById(R.id.toggle_all_overlays_text);
        toggle_all_overlays_text.setVisibility(View.VISIBLE);

        File work_area = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                EXTERNAL_STORAGE_CACHE);
        if (!work_area.exists() && work_area.mkdir()) {
            Log.d(TAG, "Updating the internal storage with proper file directories...");
        }

        // Adjust the behaviour of the mix and match toggle in the sheet
        toggle_all = (Switch) root.findViewById(R.id.toggle_all_overlays);
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
        RelativeLayout toggleZone = (RelativeLayout) root.findViewById(R.id.toggle_zone);
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
        swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipeRefreshLayout);
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
        base_spinner = (Spinner) root.findViewById(R.id.type3_spinner);
        base_spinner.setOnItemSelectedListener(new AdapterView
                .OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int pos, long id) {
                if (pos == 0) {
                    toggle_all.setChecked(false);
                    new LoadOverlays().execute("");
                } else {
                    toggle_all.setChecked(false);
                    String[] commands = {arg0.getSelectedItem().toString()};
                    new LoadOverlays().execute(commands);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        base_spinner.setEnabled(false);

        try {
            Resources themeResources = mContext.getPackageManager().getResourcesForApplication
                    (theme_pid);
            themeAssetManager = themeResources.getAssets();

            ArrayList<VariantItem> type3 = new ArrayList<>();
            ArrayList<String> stringArray = new ArrayList<>();

            File f = new File(mContext.getCacheDir().getAbsoluteFile() + SUBSTRATUM_BUILDER_CACHE +
                    theme_pid + "/assets/overlays/android/");
            if (!References.checkOMS(mContext)) {
                File check_file = new File(mContext.getCacheDir().getAbsoluteFile() +
                        SUBSTRATUM_BUILDER_CACHE + theme_pid + "/assets/overlays_legacy/android/");
                if (check_file.exists() && check_file.isDirectory()) {
                    f = new File(check_file.getAbsolutePath());
                }
            }

            if (!References.isCachingEnabled(mContext)) {
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

            if (stringArray.contains("type3")) {
                InputStream inputStream;
                if (!References.isCachingEnabled(mContext)) {
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
                    new LoadOverlays().execute("");
                }
            } else {
                toggle_all_overlays_text.setVisibility(View.VISIBLE);
                base_spinner.setVisibility(View.INVISIBLE);
                new LoadOverlays().execute("");
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
        localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.registerReceiver(jobReceiver, intentFilter);
        return root;
    }

    private List<String> updateEnabledOverlays() {
        List<String> state5 = ThemeManager.listOverlays(STATE_APPROVED_ENABLED);
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

    private boolean checkActiveNotifications() {
        StatusBarNotification[] activeNotifications = mNotifyManager.getActiveNotifications();
        for (StatusBarNotification statusBarNotification : activeNotifications) {
            if (statusBarNotification.getPackageName().equals(getContext().getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private void finishFunction(Context context) {
        mProgressDialog.dismiss();

        // Add dummy intent to be able to close the notification on click
        Intent notificationIntent = new Intent(context, InformationActivity.class);
        notificationIntent.putExtra("theme_name", theme_name);
        notificationIntent.putExtra("theme_pid", theme_pid);
        notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

        if (!has_failed) {
            // Closing off the persistent notification
            if (checkActiveNotifications()) {
                mNotifyManager.cancel(id);
                mBuilder = new NotificationCompat.Builder(context);
                mBuilder.setAutoCancel(true);
                mBuilder.setProgress(0, 0, false);
                mBuilder.setOngoing(false);
                mBuilder.setContentIntent(intent);
                mBuilder.setSmallIcon(R.drawable.notification_success_icon);
                mBuilder.setContentTitle(context.getString(R.string.notification_done_title));
                mBuilder.setContentText(context.getString(R.string.notification_no_errors_found));
                if (prefs.getBoolean("vibrate_on_compiled", false)) {
                    mBuilder.setVibrate(new long[]{100, 200, 100, 500});
                }
                mNotifyManager.notify(id, mBuilder.build());
            }

            if (missingType3) {
                Lunchbar.make(
                        getActivityView(),
                        R.string.toast_compiled_missing,
                        Lunchbar.LENGTH_LONG)
                        .show();
            } else {
                Lunchbar.make(
                        getActivityView(),
                        R.string.toast_compiled_updated,
                        Lunchbar.LENGTH_LONG)
                        .show();
            }
        }

        if (!has_failed || final_runner.size() > fail_count) {
            String final_commands = "";
            if (compile_enable_mode && mixAndMatchMode) {
                // Buffer the disableBeforeEnabling String
                ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                for (int i = 0; i < all_installed_overlays.size(); i++) {
                    if (!java.util.Objects.equals(
                            References.grabOverlayParent(
                                    getContext(),
                                    all_installed_overlays.get(i)),
                            theme_pid)) {
                        disableBeforeEnabling.add(all_installed_overlays.get(i));
                    }
                }
                if (checkThemeInterfacer(context)) {
                    ThemeManager.disableOverlay(context, disableBeforeEnabling);
                } else {
                    final_commands = ThemeManager.disableOverlay;
                    for (int i = 0; i < disableBeforeEnabling.size(); i++) {
                        final_commands += " " + disableBeforeEnabling.get(i) + " ";
                    }
                    Log.d(TAG, final_commands);
                }
            }

            if (compile_enable_mode) {
                if (checkThemeInterfacer(context)) {
                    ThemeManager.enableOverlay(context, final_command);
                } else {
                    final_commands += ThemeManager.enableOverlay;
                    for (int i = 0; i < final_command.size(); i++) {
                        final_commands += " " + final_command.get(i);
                        // Wait for the install to be finished on the rooted set up
                        while (!isPackageInstalled(context, final_command.get(i))) {
                            try {
                                Log.d(TAG,
                                        "Waiting for \'" + final_command.get(i) +
                                                "\' to finish installing...");
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (isPackageInstalled(context, final_command.get(i)))
                            Log.d(TAG, final_command.get(i) + " successfully installed silently.");
                    }
                }
            }

            if (!checkThemeInterfacer(context) && isPackageInstalled(context, MASQUERADE_PACKAGE)) {
                Log.d(TAG, "Using Masquerade as the fallback system...");
                Intent runCommand = MasqueradeService.getMasquerade(getContext());
                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                runCommand.setAction("masquerade.substratum.COMMANDS");
                runCommand.putExtra("om-commands", final_commands);
                getContext().sendBroadcast(runCommand);
            }

            if (final_runner.size() == 0) {
                if (base_spinner.getSelectedItemPosition() == 0) {
                    mAdapter.notifyDataSetChanged();
                } else {
                    mAdapter.notifyDataSetChanged();
                }
            } else {
                progressBar.setVisibility(View.VISIBLE);
                if (toggle_all.isChecked()) toggle_all.setChecked(false);
                mAdapter.notifyDataSetChanged();
            }

            progressBar.setVisibility(View.GONE);
            if (needsRecreate()) {
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    // OMS may not have written all the changes so quickly just yet
                    // so we may need to have a small delay
                    try {
                        overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                        for (int i = 0; i < overlaysLists.size(); i++) {
                            OverlaysItem currentOverlay = overlaysLists.get(i);
                            currentOverlay.setSelected(false);
                            currentOverlay.updateEnabledOverlays(updateEnabledOverlays());
                            mAdapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        // Consume window refresh
                    }
                }, REFRESH_WINDOW_DELAY);
            }

            if (!late_install.isEmpty()) {
                // Install remaining overlays
                ThemeManager.installOverlay(context, late_install);
            }
        }
    }

    private void failedFunction(Context context) {
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
            mBuilder = new NotificationCompat.Builder(context);
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

        final Dialog dialog = new Dialog(context, android.R.style
                .Theme_DeviceDefault_Dialog);
        dialog.setContentView(R.layout.logcat_dialog);
        dialog.setTitle(R.string.logcat_dialog_title);
        if (dialog.getWindow() != null)
            dialog.getWindow().setLayout(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT);

        TextView text = (TextView) dialog.findViewById(R.id.textField);
        text.setText(error_logs);
        ImageButton confirm = (ImageButton) dialog.findViewById(R.id.confirm);
        confirm.setOnClickListener(view -> dialog.dismiss());

        ImageButton copy_clipboard = (ImageButton) dialog.findViewById(R.id.copy_clipboard);
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

        ImageButton send = (ImageButton) dialog.findViewById(
                R.id.send);
        send.setVisibility(View.GONE);

        theme_author = "";
        themer_email = "";
        try {
            ApplicationInfo appInfo =
                    context.getPackageManager().getApplicationInfo(
                            theme_pid,
                            PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString("Substratum_Author") != null) {
                    theme_author = appInfo.metaData.getString("Substratum_Author");
                }
                if (appInfo.metaData.getString("Substratum_Email") != null) {
                    themer_email = appInfo.metaData.getString("Substratum_Email");
                }
            }
        } catch (Exception e) {
            // NameNotFound
        }

        if (themer_email.length() > 0) {
            send.setVisibility(View.VISIBLE);
            send.setOnClickListener(v -> {
                String device = " " + Build.MODEL + " (" + Build.DEVICE + ") " +
                        "[" + Build.FINGERPRINT + "]";
                String theme_version = References.grabAppVersion(getContext(), theme_pid);
                String email_subject =
                        String.format(context.getString(R.string.logcat_email_subject), theme_name);
                String xposed = References.checkXposedVersion();
                if (xposed.length() > 0) {
                    device += " {" + xposed + "}";
                }
                String email_body = String.format(context.getString(R.string.logcat_email_body),
                        theme_author, theme_name);
                File log = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/theme_error.log");
                try (FileWriter fw = new FileWriter(log, false);
                     BufferedWriter out = new BufferedWriter(fw)) {
                    String attachment_body =
                            String.format(context.getString(R.string.logcat_attachment_body),
                                    device, error_logs, BuildConfig.VERSION_CODE, theme_version);
                    out.write(attachment_body);
                } catch (IOException e) {
                    // Suppress exception
                }
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{themer_email});
                i.putExtra(Intent.EXTRA_SUBJECT, email_subject);
                i.putExtra(Intent.EXTRA_TEXT, email_body);
                i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(log));
                try {
                    startActivity(Intent.createChooser(
                            i, context.getString(R.string.logcat_email_activity)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Lunchbar.make(
                            getActivityView(),
                            R.string.logcat_email_activity_error,
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
                dialog.dismiss();
            });
        }
        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mContext.unregisterReceiver(finishReceiver);
            localBroadcastManager.unregisterReceiver(jobReceiver);
        } catch (IllegalArgumentException e) {
            // Unregistered already
        }
    }

    private boolean needsRecreate() {
        for (OverlaysItem oi : checkedOverlays) {
            String packageName = oi.getPackageName();
            if (packageName.equals("android") || packageName.equals("projekt.substratum")) {
                if (!enable_mode && !disable_mode &&
                        ThemeManager.isOverlayEnabled(oi.getFullOverlayParameters())) {
                    return false;
                } else if (enable_mode || disable_mode || compile_enable_mode) {
                    return false;
                }
            }
        }
        return References.checkOMS(mContext) && !has_failed;
    }

    public VariantItem setTypeOneSpinners(Object typeArrayRaw, String package_identifier,
                                          String type) {
        InputStream inputStream = null;
        try {
            if (References.isCachingEnabled(mContext)) {
                inputStream = new FileInputStream(
                        new File(((File) typeArrayRaw).getAbsolutePath() + "/type1" + type));
            } else {
                inputStream = themeAssetManager.open(
                        overlaysDir + "/" + package_identifier + "/type1" + type);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // Parse current default types on type3 base resource folders
        String parsedVariant = "";
        try {
            parsedVariant = base_spinner.getSelectedItem().toString().replaceAll("\\s+", "");
        } catch (NullPointerException npe) {
            // Suppress warning
        }
        String suffix = ((parsedVariant.length() != 0) ? "/type3_" + parsedVariant : "/res");

        // Type1 Spinner Text Adjustments
        assert inputStream != null;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream))) {
            // This adjusts it so that we have the spinner text set
            String formatter = String.format(getString(R.string
                    .overlays_variant_substitute), reader.readLine());
            // This is the default type1 xml hex, if present
            String hex = null;
            try (InputStream name = themeAssetManager.open(
                    overlaysDir + "/" + package_identifier + suffix +
                            "/values/type1" + type + ".xml")) {
                hex = References.getOverlayResource(name);
            } catch (IOException e) {
                // Suppress warning
            }
            return new VariantItem(formatter, hex);
        } catch (IOException e) {
            e.printStackTrace();
            // When erroring out, put the default spinner text
            Log.e(TAG, "There was an error parsing asset file!");
            String hex = null;
            try (InputStream input = themeAssetManager.open(overlaysDir +
                    "/" + package_identifier + suffix + "/values/type1" + type + ".xml")) {
                hex = References.getOverlayResource(input);
            } catch (IOException ioe) {
                // Suppress warning
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

    public VariantItem setTypeTwoSpinners(Object typeArrayRaw, InputStreamReader
            inputStreamReader) {
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            return new VariantItem(String.format(getString(R.string
                    .overlays_variant_substitute), reader.readLine()), null);
        } catch (IOException e) {
            Log.e(TAG, "There was an error parsing asset file!");
            return new VariantItem(getString(R.string.overlays_variant_default_2), null);
        }
    }

    public VariantItem setTypeOneHexAndSpinner(String current, String package_identifier) {
        try (InputStream inputStream = themeAssetManager.open(
                "overlays/" + package_identifier + "/" + current)) {
            String hex = References.getOverlayResource(inputStream);
            return new VariantItem(current.substring(7, current.length() - 4), hex);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class LoadOverlays extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            if (materialProgressBar != null) materialProgressBar.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.INVISIBLE);
            swipeRefreshLayout.setVisibility(View.GONE);
            toggle_all.setEnabled(false);
            base_spinner.setEnabled(false);
        }

        @Override
        protected void onPostExecute(String result) {
            if (materialProgressBar != null) materialProgressBar.setVisibility(View.GONE);
            toggle_all.setEnabled(true);
            base_spinner.setEnabled(true);
            mAdapter = new OverlaysAdapter(values2);
            mRecyclerView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
            mRecyclerView.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setVisibility(View.VISIBLE);
            super.onPostExecute(result);
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(String... sUrl) {
            // Refresh asset manager
            if (!References.isCachingEnabled(mContext)) {
                try {
                    Resources themeResources = mContext.getPackageManager()
                            .getResourcesForApplication(theme_pid);
                    themeAssetManager = themeResources.getAssets();
                } catch (PackageManager.NameNotFoundException e) {
                    // Suppress exception
                }
            }

            // Grab the current theme_pid's versionName so that we can version our overlays
            versionName = References.grabAppVersion(mContext, theme_pid);
            List<String> state5overlays = updateEnabledOverlays();
            String parse1_themeName = theme_name.replaceAll("\\s+", "");
            String parse2_themeName = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");

            ArrayList<String> values = new ArrayList<>();
            values2 = new ArrayList<>();

            // Buffer the initial values list so that we get the list of packages inside this theme
            ArrayList<String> overlaysFolder = new ArrayList<>();
            if (References.isCachingEnabled(mContext)) {
                File overlaysDirectory = new File(mContext.getCacheDir().getAbsoluteFile() +
                        SUBSTRATUM_BUILDER_CACHE + theme_pid + "/assets/overlays/");

                if (!References.checkOMS(mContext)) {
                    File check_file = new File(mContext.getCacheDir().getAbsoluteFile() +
                            SUBSTRATUM_BUILDER_CACHE + theme_pid + "/assets/overlays_legacy/");
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
                    String[] overlayList = themeAssetManager.list(overlaysDir);
                    Collections.addAll(overlaysFolder, overlayList);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }

            values.addAll(overlaysFolder.stream().filter(package_name -> (References
                    .isPackageInstalled(mContext, package_name) ||
                    References.allowedSystemUIOverlay(package_name) ||
                    References.allowedSettingsOverlay(package_name)) &&
                    (!ThemeManager.blacklisted(package_name))).collect(Collectors.toList()));

            // Create the map for {package name: package identifier}
            HashMap<String, String> unsortedMap = new HashMap<>();

            // Then let's convert all the package names to their app names
            for (int i = 0; i < values.size(); i++) {
                try {
                    if (References.allowedSystemUIOverlay(values.get(i))) {
                        String package_name = "";
                        switch (values.get(i)) {
                            case "com.android.systemui.headers":
                                package_name = getString(R.string.systemui_headers);
                                break;
                            case "com.android.systemui.navbars":
                                package_name = getString(R.string.systemui_navigation);
                                break;
                            case "com.android.systemui.statusbars":
                                package_name = getString(R.string.systemui_statusbar);
                                break;
                            case "com.android.systemui.tiles":
                                package_name = getString(R.string.systemui_qs_tiles);
                                break;
                        }
                        unsortedMap.put(values.get(i), package_name);
                    } else {
                        if (References.allowedSettingsOverlay(values.get(i))) {
                            String package_name = "";
                            switch (values.get(i)) {
                                case "com.android.settings.icons":
                                    package_name = getString(R.string.settings_icons);
                                    break;
                            }
                            unsortedMap.put(values.get(i), package_name);
                        } else {
                            ApplicationInfo applicationInfo = mContext.getPackageManager()
                                    .getApplicationInfo
                                            (values.get(i), 0);
                            String packageTitle = mContext.getPackageManager()
                                    .getApplicationLabel
                                            (applicationInfo).toString();
                            unsortedMap.put(values.get(i), packageTitle);
                        }
                    }
                } catch (Exception e) {
                    // Exception
                }
            }

            // Sort the values list
            List<Pair<String, String>> sortedMap = sortMapByValues(unsortedMap);

            // Now let's add the new information so that the adapter can recognize custom method
            // calls
            for (Pair<String, String> entry : sortedMap) {
                String package_name = entry.second;
                String package_identifier = entry.first;

                try {
                    ArrayList<VariantItem> type1a = new ArrayList<>();
                    ArrayList<VariantItem> type1b = new ArrayList<>();
                    ArrayList<VariantItem> type1c = new ArrayList<>();
                    ArrayList<VariantItem> type2 = new ArrayList<>();
                    ArrayList<String> typeArray = new ArrayList<>();

                    Object typeArrayRaw;
                    if (References.isCachingEnabled(mContext)) {
                        typeArrayRaw = new File(mContext.getCacheDir().getAbsoluteFile() +
                                SUBSTRATUM_BUILDER_CACHE + theme_pid
                                + "/assets/overlays/" + package_identifier);
                    } else {
                        // Begin the no caching algorithm
                        typeArrayRaw = themeAssetManager.list(
                                overlaysDir + "/" + package_identifier);

                        // Sort the typeArray so that the types are asciibetical
                        Collections.addAll(typeArray, (String[]) typeArrayRaw);
                        Collections.sort(typeArray);
                    }

                    if (!References.checkOMS(mContext)) {
                        File check_file = new File(
                                mContext.getCacheDir().getAbsoluteFile() +
                                        SUBSTRATUM_BUILDER_CACHE + theme_pid
                                        + "/assets/overlays_legacy/" + package_identifier + "/");
                        if (check_file.exists() && check_file.isDirectory()) {
                            typeArrayRaw = new File(check_file.getAbsolutePath());
                        }
                    }

                    File[] fileArray;
                    if (References.isCachingEnabled(mContext)) {
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
                    if (typeArray.contains("type1a")) {
                        type1a.add(setTypeOneSpinners(typeArrayRaw, package_identifier, "a"));
                    }

                    if (typeArray.contains("type1b")) {
                        type1b.add(setTypeOneSpinners(typeArrayRaw, package_identifier, "b"));
                    }

                    if (typeArray.contains("type1c")) {
                        type1c.add(setTypeOneSpinners(typeArrayRaw, package_identifier, "c"));
                    }

                    if (References.isCachingEnabled(mContext) && typeArray.contains("type2")) {
                        type2.add(setTypeTwoSpinners(typeArrayRaw,
                                new InputStreamReader(new FileInputStream(
                                        new File(((File) typeArrayRaw).getAbsolutePath() +
                                                "/type2")))));
                    } else if (typeArray.contains("type2")) {
                        type2.add(setTypeTwoSpinners(typeArrayRaw,
                                new InputStreamReader(themeAssetManager.open(overlaysDir +
                                        "/" + package_identifier + "/type2"))));
                    }
                    if (typeArray.size() > 1) {
                        for (int i = 0; i < typeArray.size(); i++) {
                            String current = typeArray.get(i);
                            if (!current.equals("res")) {
                                if (current.contains(".xml")) {
                                    switch (current.substring(0, 7)) {
                                        case "type1a_":
                                            type1a.add(
                                                    setTypeOneHexAndSpinner(
                                                            current, package_identifier));
                                            break;
                                        case "type1b_":
                                            type1b.add(
                                                    setTypeOneHexAndSpinner(
                                                            current, package_identifier));
                                            break;
                                        case "type1c_":
                                            type1c.add(
                                                    setTypeOneHexAndSpinner(
                                                            current, package_identifier));
                                            break;
                                    }
                                } else if (!current.contains(".") && current.length() > 5 &&
                                        current.substring(0, 6).equals("type2_")) {
                                    type2.add(new VariantItem(current.substring(6), null));
                                }
                            }
                        }

                        VariantAdapter adapter1 = new VariantAdapter(getActivity(), type1a);
                        VariantAdapter adapter2 = new VariantAdapter(getActivity(), type1b);
                        VariantAdapter adapter3 = new VariantAdapter(getActivity(), type1c);
                        VariantAdapter adapter4 = new VariantAdapter(getActivity(), type2);

                        boolean adapterOneChecker = type1a.size() == 0;
                        boolean adapterTwoChecker = type1b.size() == 0;
                        boolean adapterThreeChecker = type1c.size() == 0;
                        boolean adapterFourChecker = type2.size() == 0;

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
                                        mContext,
                                        versionName,
                                        sUrl[0],
                                        state5overlays,
                                        References.checkOMS(mContext));
                        values2.add(overlaysItem);
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
                                        mContext,
                                        versionName,
                                        sUrl[0],
                                        state5overlays,
                                        References.checkOMS(mContext));
                        values2.add(overlaysItem);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private class Phase2_InitializeCache extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            final_runner = new ArrayList<>();
            late_install = new ArrayList<>();

            if (!enable_mode && !disable_mode) {
                Log.d(TAG,
                        "Decompiling and initializing work area with the " +
                                "selected theme's assets...");
                int notification_priority = Notification.PRIORITY_MAX;

                // Create an Intent for the BroadcastReceiver
                Intent buttonIntent = new Intent(mContext, NotificationButtonReceiver.class);

                // Create the PendingIntent
                PendingIntent btPendingIntent = PendingIntent.getBroadcast(
                        mContext, 0, buttonIntent, 0);
                PendingIntent resultPendingIntent = PendingIntent.getActivity(
                        mContext, 0, new Intent(), 0);

                // This is the time when the notification should be shown on the user's screen
                mNotifyManager =
                        (NotificationManager) mContext.getSystemService(
                                Context.NOTIFICATION_SERVICE);
                mBuilder = new NotificationCompat.Builder(mContext);
                mBuilder.setContentTitle(getString(R.string.notification_initial_title))
                        .setProgress(100, 0, true)
                        .addAction(android.R.color.transparent, getString(R.string
                                .notification_hide), btPendingIntent)
                        .setSmallIcon(android.R.drawable.ic_popup_sync)
                        .setPriority(notification_priority)
                        .setContentIntent(resultPendingIntent)
                        .setOngoing(true);
                mNotifyManager.notify(id, mBuilder.build());

                mProgressDialog = null;
                mProgressDialog = new ProgressDialog(getActivity(),
                        R.style.SubstratumBuilder_ActivityTheme);
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                mProgressDialog.setContentView(R.layout.compile_dialog_loader);
                if (mProgressDialog.getWindow() != null) mProgressDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                final float radius = 5;
                final View decorView = getActivity().getWindow().getDecorView();
                final ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);
                final Drawable windowBackground = decorView.getBackground();

                BlurView blurView = (BlurView) mProgressDialog.findViewById(R.id.blurView);

                if (rootView != null) {
                    blurView.setupWith(rootView)
                            .windowBackground(windowBackground)
                            .blurAlgorithm(new RenderScriptBlur(mContext))
                            .blurRadius(radius);
                }

                dialogProgress = (ProgressBar) mProgressDialog.findViewById(R.id.loading_bar);
                dialogProgress.setProgressTintList(ColorStateList.valueOf(mContext.getColor(
                        R.color.compile_dialog_wave_color)));
                dialogProgress.setIndeterminate(false);

                loader_string = (TextView) mProgressDialog.findViewById(R.id.title);
                loader_string.setText(mContext.getResources().getString(
                        R.string.sb_phase_1_loader));
            }
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Phase3_mainFunction phase3_mainFunction = new Phase3_mainFunction();
                phase3_mainFunction.execute(result);
            } else {
                Phase3_mainFunction phase3_mainFunction = new Phase3_mainFunction();
                phase3_mainFunction.execute("");
            }
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            if (!enable_mode && !disable_mode) {
                // Initialize Substratum cache with theme only if permitted
                if (!has_initialized_cache && References.isCachingEnabled(mContext)) {
                    sb = new SubstratumBuilder();

                    File versioning = new File(mContext.getCacheDir().getAbsoluteFile() +
                            SUBSTRATUM_BUILDER_CACHE + theme_pid + "/substratum.xml");
                    if (versioning.exists()) {
                        has_initialized_cache = true;
                    } else {
                        new CacheCreator().initializeCache(mContext, theme_pid);
                        has_initialized_cache = true;
                    }
                } else {
                    try {
                        Resources themeResources = mContext.getPackageManager()
                                .getResourcesForApplication(theme_pid);
                        themeAssetManager = themeResources.getAssets();
                    } catch (PackageManager.NameNotFoundException e) {
                        // Suppress exception
                    }
                    Log.d(TAG, "Work area is ready with decompiled assets already!");
                }
                if (sUrl[0].length() != 0) {
                    return sUrl[0];
                } else {
                    return null;
                }
            }
            return null;
        }
    }

    private class Phase3_mainFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "Substratum is proceeding with your actions and is now actively running...");

            missingType3 = false;
            has_failed = false;
            fail_count = 0;
            error_logs = "";

            if (!enable_mode && !disable_mode) {
                // Change title in preparation for loop to change subtext
                if (checkActiveNotifications()) {
                    mBuilder.setContentTitle(getString(R.string.notification_processing_n))
                            .setProgress(100, 0, false);
                    mNotifyManager.notify(id, mBuilder.build());
                }
                loader_string.setText(mContext.getResources().getString(
                        R.string.sb_phase_2_loader));
            }
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            TextView textView = (TextView) mProgressDialog.findViewById(R.id.current_object);
            textView.setText(current_dialog_overlay);
            double progress = (current_amount / total_amount) * 100;
            dialogProgress.setProgress((int) progress, true);
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            final_command = new ArrayList<>();

            // Check if not compile_enable_mode
            if (!compile_enable_mode) {
                for (int i = 0; i < final_runner.size(); i++) {
                    final_command.add(final_runner.get(i));
                }
            } else {
                // It's compile and enable mode, we have to first sort out all the "pm install"'s
                // from the final_commands
                for (int i = 0; i < final_runner.size(); i++) {
                    final_command.add(final_runner.get(i));
                }
            }

            if (!enable_mode && !disable_mode) {
                finishFunction(mContext);
                if (has_failed) {
                    failedFunction(mContext);
                } else {
                    // Restart SystemUI if an enabled SystemUI overlay is updated
                    for (int i = 0; i < checkedOverlays.size(); i++) {
                        String targetOverlay = checkedOverlays.get(i).getPackageName();
                        if (targetOverlay.equals("android") ||
                                targetOverlay.equals("com.android.systemui")) {
                            String packageName = checkedOverlays.get(i).getFullOverlayParameters();
                            if (ThemeManager.isOverlayEnabled(packageName)) {
                                ThemeManager.restartSystemUI(mContext);
                                break;
                            }
                        }
                    }
                }
                try {
                    mContext.unregisterReceiver(finishReceiver);
                } catch (IllegalArgumentException e) {
                    // Suppress warning
                }
            } else if (enable_mode) {
                if (final_runner.size() > 0) {
                    enable_mode = false;

                    if (mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        for (int i = 0; i < all_installed_overlays.size(); i++) {
                            if (!References.grabOverlayParent(getContext(),
                                    all_installed_overlays.get(i)).equals(theme_pid)) {
                                disableBeforeEnabling.add(all_installed_overlays.get(i));
                            }
                        }
                        progressBar.setVisibility(View.VISIBLE);
                        if (toggle_all.isChecked()) toggle_all.setChecked(false);
                        if (checkThemeInterfacer(getContext())) {
                            ThemeManager.disableOverlay(mContext, disableBeforeEnabling);
                            ThemeManager.enableOverlay(mContext, final_command);
                        } else {
                            String final_commands = "";
                            if (disableBeforeEnabling.size() > 0)
                                final_commands = ThemeManager.disableOverlay;
                            for (int i = 0; i < disableBeforeEnabling.size(); i++) {
                                final_commands += " " + disableBeforeEnabling.get(i);
                            }
                            if (final_commands.length() > 0 && final_command.size() > 0) {
                                final_commands += " " + ThemeManager.enableOverlay;
                            } else if (final_command.size() > 0) {
                                final_commands = ThemeManager.enableOverlay;
                            }
                            for (int i = 0; i < final_command.size(); i++) {
                                final_commands += " " + final_command.get(i);
                            }
                            if (!checkThemeInterfacer(getContext()) &&
                                    isPackageInstalled(getContext(), MASQUERADE_PACKAGE)) {
                                Log.d(TAG, "Using Masquerade as the fallback system...");
                                Intent runCommand = MasqueradeService.getMasquerade(getContext());
                                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                runCommand.setAction("masquerade.substratum.COMMANDS");
                                runCommand.putExtra("om-commands", final_commands);
                                getContext().sendBroadcast(runCommand);
                            }
                        }
                    } else {
                        progressBar.setVisibility(View.VISIBLE);
                        if (toggle_all.isChecked()) toggle_all.setChecked(false);
                        if (checkThemeInterfacer(getContext())) {
                            ThemeManager.enableOverlay(mContext, final_command);
                        } else {
                            String final_commands = ThemeManager.enableOverlay;
                            for (int i = 0; i < final_command.size(); i++) {
                                final_commands += " " + final_command.get(i);
                            }
                            if (!checkThemeInterfacer(getContext()) &&
                                    isPackageInstalled(getContext(), MASQUERADE_PACKAGE)) {
                                Log.d(SUBSTRATUM_LOG, "Using Masquerade as the fallback system...");
                                Intent runCommand = MasqueradeService.getMasquerade(getContext());
                                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                runCommand.setAction("masquerade.substratum.COMMANDS");
                                runCommand.putExtra("om-commands", final_commands);
                                getContext().sendBroadcast(runCommand);
                            }
                        }
                    }

                    progressBar.setVisibility(View.GONE);
                    if (needsRecreate()) {
                        Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                                for (int i = 0; i < overlaysLists.size(); i++) {
                                    OverlaysItem currentOverlay = overlaysLists.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(updateEnabledOverlays());
                                    mAdapter.notifyDataSetChanged();
                                }
                            } catch (Exception e) {
                                // Consume window refresh
                            }
                        }, REFRESH_WINDOW_DELAY);
                    }
                } else {
                    compile_enable_mode = false;
                    enable_mode = false;
                    Lunchbar.make(
                            getActivityView(),
                            R.string.toast_disabled3,
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            } else if (disable_mode) {
                if (final_runner.size() > 0) {
                    disable_mode = false;
                    progressBar.setVisibility(View.VISIBLE);
                    if (toggle_all.isChecked()) toggle_all.setChecked(false);
                    if (checkThemeInterfacer(getContext())) {
                        ThemeManager.disableOverlay(mContext, final_command);
                    } else {
                        String final_commands = "";
                        if (final_commands.length() > 0 && final_command.size() > 0) {
                            final_commands += " " + ThemeManager.disableOverlay;
                        } else if (final_command.size() > 0) {
                            final_commands = ThemeManager.disableOverlay;
                        }
                        for (int i = 0; i < final_command.size(); i++) {
                            final_commands += " " + final_command.get(i);
                        }
                        if (!checkThemeInterfacer(getContext()) &&
                                isPackageInstalled(getContext(), MASQUERADE_PACKAGE)) {
                            Log.d(TAG, "Using Masquerade as the fallback system...");
                            Intent runCommand = MasqueradeService.getMasquerade(getContext());
                            runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                            runCommand.setAction("masquerade.substratum.COMMANDS");
                            runCommand.putExtra("om-commands", final_commands);
                            getContext().sendBroadcast(runCommand);
                        }
                    }

                    progressBar.setVisibility(View.GONE);
                    if (needsRecreate()) {
                        Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                                for (int i = 0; i < overlaysLists.size(); i++) {
                                    OverlaysItem currentOverlay = overlaysLists.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(updateEnabledOverlays());
                                    mAdapter.notifyDataSetChanged();
                                }
                            } catch (Exception e) {
                                // Consume window refresh
                            }
                        }, REFRESH_WINDOW_DELAY);
                    }
                } else {
                    disable_mode = false;
                    Lunchbar.make(
                            getActivityView(),
                            R.string.toast_disabled4,
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            }
            if (!References.checkOMS(mContext) && final_runner.size() == fail_count) {
                final AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(mContext);
                alertDialogBuilder
                        .setTitle(getString(R.string.legacy_dialog_soft_reboot_title));
                alertDialogBuilder
                        .setMessage(getString(R.string.legacy_dialog_soft_reboot_text));
                alertDialogBuilder
                        .setPositiveButton(android.R.string.ok,
                                (dialog, id12) -> ElevatedCommands.reboot());
                alertDialogBuilder
                        .setNegativeButton(R.string.remove_dialog_later,
                                (dialog, id1) -> {
                                    progressBar.setVisibility(View.GONE);
                                    dialog.dismiss();
                                });
                alertDialogBuilder.setCancelable(false);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
            is_active = false;
            mAdapter.notifyDataSetChanged();
            if (toggle_all.isChecked()) toggle_all.setChecked(false);
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(String... sUrl) {
            String parsedVariant = sUrl[0].replaceAll("\\s+", "");
            String unparsedVariant = sUrl[0];

            if (mixAndMatchMode && !References.checkOMS(mContext)) {
                String current_directory;
                if (References.inNexusFilter()) {
                    current_directory = PIXEL_NEXUS_DIR;
                } else {
                    current_directory = LEGACY_NEXUS_DIR;
                }
                File file = new File(current_directory);
                if (file.exists()) {
                    FileOperations.mountRW();
                    FileOperations.delete(mContext, current_directory);
                }
            }

            // Enable listener
            if (References.checkThemeInterfacer(mContext) &&
                    !References.isBinderInterfacer(mContext)) {
                if (finishReceiver == null) finishReceiver = new FinishReceiver();
                IntentFilter filter = new IntentFilter(STATUS_CHANGED);
                mContext.registerReceiver(finishReceiver, filter);
            }

            total_amount = checkedOverlays.size();
            for (int i = 0; i < checkedOverlays.size(); i++) {
                current_amount = i + 1;
                String theme_name_parsed =
                        theme_name.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
                String current_overlay = checkedOverlays.get(i).getPackageName();
                current_dialog_overlay =
                        "'" + References.grabPackageName(mContext, current_overlay) + "'";

                if (!enable_mode && !disable_mode) {
                    publishProgress((int) current_amount);
                    if (compile_enable_mode) {
                        if (final_runner == null) final_runner = new ArrayList<>();
                        String package_name = checkedOverlays.get(i).getFullOverlayParameters();
                        if (References.isPackageInstalled(mContext, package_name) ||
                                compile_enable_mode) {
                            final_runner.add(package_name);
                        }
                    }
                    try {
                        String packageTitle = "";
                        if (References.allowedSystemUIOverlay(current_overlay)) {
                            switch (current_overlay) {
                                case "com.android.systemui.headers":
                                    packageTitle = getString(R.string.systemui_headers);
                                    break;
                                case "com.android.systemui.navbars":
                                    packageTitle = getString(R.string.systemui_navigation);
                                    break;
                                case "com.android.systemui.statusbars":
                                    packageTitle = getString(R.string.systemui_statusbar);
                                    break;
                                case "com.android.systemui.tiles":
                                    packageTitle = getString(R.string.systemui_qs_tiles);
                                    break;
                            }
                        } else if (References.allowedSettingsOverlay(current_overlay)) {
                            switch (current_overlay) {
                                case "com.android.settings.icons":
                                    packageTitle = getString(R.string.settings_icons);
                                    break;
                            }
                        } else {
                            ApplicationInfo applicationInfo =
                                    mContext.getPackageManager()
                                            .getApplicationInfo(current_overlay, 0);
                            packageTitle = mContext.getPackageManager()
                                    .getApplicationLabel(applicationInfo).toString();
                        }

                        // Initialize working notification
                        if (checkActiveNotifications()) {
                            mBuilder.setProgress(100, (int) (((double) (i + 1) / checkedOverlays
                                    .size()) * 100), false);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                mBuilder.setContentText("\"" + packageTitle + "\"");
                            } else {
                                mBuilder.setContentText(
                                        getString(R.string.notification_processing) +
                                                "\"" + packageTitle + "\"");
                            }
                            mNotifyManager.notify(id, mBuilder.build());
                        }

                        String workingDirectory = mContext.getCacheDir().getAbsolutePath() +
                                SUBSTRATUM_BUILDER_CACHE + theme_pid +
                                "/assets/overlays/" + current_overlay;

                        if (!References.checkOMS(mContext)) {
                            File check_legacy = new File(mContext.getCacheDir()
                                    .getAbsolutePath() + SUBSTRATUM_BUILDER_CACHE +
                                    theme_pid + "/assets/overlays_legacy/" +
                                    current_overlay);
                            if (check_legacy.exists()) {
                                workingDirectory = check_legacy.getAbsolutePath();
                            }
                        }
                        String suffix = ((sUrl[0].length() != 0) ?
                                "/type3_" + parsedVariant : "/res");
                        String unparsedSuffix =
                                ((sUrl[0].length() != 0) ? "/type3_" + unparsedVariant : "/res");
                        if (References.isCachingEnabled(mContext)) {
                            File srcDir = new File(workingDirectory +
                                    ((sUrl[0].length() != 0) ? "/type3_" + sUrl[0] : "/res"));
                            File destDir = new File(workingDirectory + "/workdir");
                            if (destDir.exists()) {
                                FileOperations.delete(mContext, destDir.getAbsolutePath());
                            }
                            FileUtils.copyDirectory(srcDir, destDir);
                        } else {
                            workingDirectory = mContext.getCacheDir().getAbsolutePath() +
                                    SUBSTRATUM_BUILDER_CACHE.substring(0,
                                            SUBSTRATUM_BUILDER_CACHE.length() - 1);
                            File created = new File(workingDirectory);
                            if (created.exists()) {
                                FileOperations.delete(mContext, created.getAbsolutePath());
                                FileOperations.createNewFolder(mContext, created.getAbsolutePath());
                            } else {
                                FileOperations.createNewFolder(mContext, created.getAbsolutePath());
                            }
                            String listDir = overlaysDir + "/" + current_overlay + unparsedSuffix;
                            FileOperations.copyFileOrDir(
                                    themeAssetManager,
                                    listDir,
                                    workingDirectory + suffix,
                                    listDir);
                        }

                        if (checkedOverlays.get(i).is_variant_chosen || sUrl[0].length() != 0) {
                            // Type 1a
                            if (checkedOverlays.get(i).is_variant_chosen1) {
                                if (References.isCachingEnabled(mContext)) {
                                    String sourceLocation = workingDirectory + "/type1a_" +
                                            checkedOverlays.get(i).getSelectedVariantName() +
                                            ".xml";

                                    String targetLocation = workingDirectory +
                                            "/workdir/values/type1a.xml";

                                    Log.d(TAG,
                                            "You have selected variant file \"" +
                                                    checkedOverlays.get(i).getSelectedVariantName
                                                            () + "\"");
                                    Log.d(TAG, "Moving variant file to: " + targetLocation);
                                    FileOperations.copy(
                                            mContext,
                                            sourceLocation,
                                            targetLocation);
                                } else {
                                    Log.d(TAG, "You have selected variant file \"" +
                                            checkedOverlays.get(i).getSelectedVariantName() + "\"");
                                    Log.d(TAG, "Moving variant file to: " +
                                            workingDirectory + suffix + "/values/type1a.xml");

                                    String to_copy =
                                            overlaysDir +
                                                    "/" +
                                                    current_overlay +
                                                    "/type1a_" +
                                                    checkedOverlays.get(i).getSelectedVariantName
                                                            () +
                                                    ".xml";
                                    FileOperations.copyFileOrDir(
                                            themeAssetManager,
                                            to_copy,
                                            workingDirectory + suffix + "/values/type1a.xml",
                                            to_copy);
                                }
                            }

                            // Type 1b
                            if (checkedOverlays.get(i).is_variant_chosen2) {
                                if (References.isCachingEnabled(mContext)) {
                                    String sourceLocation2 = workingDirectory + "/type1b_" +
                                            checkedOverlays.get(i).getSelectedVariantName2() +
                                            ".xml";

                                    String targetLocation2 = workingDirectory +
                                            "/workdir/values/type1b.xml";

                                    Log.d(TAG, "You have selected variant file \"" +
                                            checkedOverlays.get(i).getSelectedVariantName2() +
                                            "\"");
                                    Log.d(TAG, "Moving variant file to: " + targetLocation2);
                                    FileOperations.copy(mContext, sourceLocation2, targetLocation2);
                                } else {
                                    Log.d(TAG, "You have selected variant file \"" +
                                            checkedOverlays.get(i)
                                                    .getSelectedVariantName2() + "\"");
                                    Log.d(TAG, "Moving variant file to: " +
                                            workingDirectory + suffix + "/values/type1b.xml");

                                    String to_copy =
                                            overlaysDir +
                                                    "/" +
                                                    current_overlay +
                                                    "/type1b_" +
                                                    checkedOverlays.get(i)
                                                            .getSelectedVariantName2() +
                                                    ".xml";
                                    FileOperations.copyFileOrDir(
                                            themeAssetManager,
                                            to_copy,
                                            workingDirectory + suffix + "/values/type1b.xml",
                                            to_copy);
                                }
                            }
                            // Type 1c
                            if (checkedOverlays.get(i).is_variant_chosen3) {
                                if (References.isCachingEnabled(mContext)) {
                                    String sourceLocation3 = workingDirectory + "/type1c_" +
                                            checkedOverlays.get(i).getSelectedVariantName3() +
                                            ".xml";

                                    String targetLocation3 = workingDirectory +
                                            "/workdir/values/type1c.xml";

                                    Log.d(TAG, "You have selected variant file \"" +
                                            checkedOverlays.get(i).getSelectedVariantName3() +
                                            "\"");
                                    Log.d(TAG, "Moving variant file to: " + targetLocation3);

                                    FileOperations.copy(
                                            mContext,
                                            sourceLocation3,
                                            targetLocation3);
                                } else {
                                    Log.d(TAG, "You have selected variant file \"" +
                                            checkedOverlays.get(i)
                                                    .getSelectedVariantName3() + "\"");
                                    Log.d(TAG, "Moving variant file to: " +
                                            workingDirectory + suffix + "/values/type1c.xml");

                                    String to_copy =
                                            overlaysDir +
                                                    "/" +
                                                    current_overlay +
                                                    "/type1c_" +
                                                    checkedOverlays.get(i)
                                                            .getSelectedVariantName3() +
                                                    ".xml";
                                    FileOperations.copyFileOrDir(
                                            themeAssetManager,
                                            to_copy,
                                            workingDirectory + suffix + "/values/type1c.xml",
                                            to_copy);
                                }
                            }

                            String packageName =
                                    (checkedOverlays.get(i).is_variant_chosen1 ?
                                            checkedOverlays.get(i).getSelectedVariantName() : "") +
                                            (checkedOverlays.get(i).is_variant_chosen2 ?
                                                    checkedOverlays.get(i)
                                                            .getSelectedVariantName2() : "") +
                                            (checkedOverlays.get(i).is_variant_chosen3 ?
                                                    checkedOverlays.get(i)
                                                            .getSelectedVariantName3() : "").
                                                    replaceAll("\\s+", "").replaceAll
                                                    ("[^a-zA-Z0-9]+", "");

                            if (checkedOverlays.get(i).is_variant_chosen4) {
                                packageName = (packageName + checkedOverlays.get(i)
                                        .getSelectedVariantName4()).replaceAll("\\s+", "")
                                        .replaceAll("[^a-zA-Z0-9]+", "");
                                String type2folder = "/type2_" +
                                        checkedOverlays.get(i).getSelectedVariantName4();
                                String to_copy = overlaysDir + "/" + current_overlay + type2folder;
                                FileOperations.copyFileOrDir(themeAssetManager, to_copy,
                                        workingDirectory + type2folder, to_copy);
                                Log.d(TAG, "Currently processing package" +
                                        " \"" + checkedOverlays.get(i).getFullOverlayParameters() +
                                        "\"...");

                                if (sUrl[0].length() != 0) {
                                    sb = new SubstratumBuilder();
                                    sb.beginAction(
                                            mContext,
                                            theme_pid,
                                            current_overlay,
                                            theme_name,
                                            packageName,
                                            checkedOverlays.get(i).getSelectedVariantName4(),
                                            sUrl[0],
                                            versionName,
                                            References.checkOMS(mContext),
                                            theme_pid,
                                            suffix);
                                } else {
                                    sb = new SubstratumBuilder();
                                    sb.beginAction(
                                            mContext,
                                            theme_pid,
                                            current_overlay,
                                            theme_name,
                                            packageName,
                                            checkedOverlays.get(i).getSelectedVariantName4(),
                                            null,
                                            versionName,
                                            References.checkOMS(mContext),
                                            theme_pid,
                                            suffix);
                                }
                            } else {
                                Log.d(TAG, "Currently processing package" +
                                        " \"" + checkedOverlays.get(i).getFullOverlayParameters() +
                                        "\"...");

                                if (sUrl[0].length() != 0) {
                                    sb = new SubstratumBuilder();
                                    sb.beginAction(
                                            mContext,
                                            theme_pid,
                                            current_overlay,
                                            theme_name,
                                            packageName,
                                            null,
                                            sUrl[0],
                                            versionName,
                                            References.checkOMS(mContext),
                                            theme_pid,
                                            suffix);
                                } else {
                                    sb = new SubstratumBuilder();
                                    sb.beginAction(
                                            mContext,
                                            theme_pid,
                                            current_overlay,
                                            theme_name,
                                            packageName,
                                            null,
                                            null,
                                            versionName,
                                            References.checkOMS(mContext),
                                            theme_pid,
                                            suffix);
                                }
                            }
                            if (sb.has_errored_out) {
                                if (!sb.getErrorLogs().contains("type3") ||
                                        !sb.getErrorLogs().contains("does not exist")) {
                                    fail_count += 1;
                                    if (error_logs.length() == 0) {
                                        error_logs = sb.getErrorLogs();
                                    } else {
                                        error_logs += "\n" + sb.getErrorLogs();
                                    }
                                    has_failed = true;
                                } else {
                                    missingType3 = true;
                                }
                            } else {
                                if (sb.special_snowflake) {
                                    late_install.add(sb.no_install);
                                } else if (References.checkThemeInterfacer(mContext) &&
                                        !References.isBinderInterfacer(mContext)) {
                                    // Thread wait
                                    isWaiting = true;
                                    do {
                                        try {
                                            Thread.sleep(THREAD_WAIT_DURATION);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    } while (isWaiting);
                                }
                            }
                        } else {
                            Log.d(TAG, "Currently processing package" +
                                    " \"" + current_overlay + "." + theme_name_parsed + "\"...");
                            sb = new SubstratumBuilder();
                            sb.beginAction(
                                    mContext,
                                    theme_pid,
                                    current_overlay,
                                    theme_name,
                                    null,
                                    null,
                                    null,
                                    versionName,
                                    References.checkOMS(mContext),
                                    theme_pid,
                                    suffix);

                            if (sb.has_errored_out) {
                                fail_count += 1;
                                if (error_logs.length() == 0) {
                                    error_logs = sb.getErrorLogs();
                                } else {
                                    error_logs += "\n" + sb.getErrorLogs();
                                }
                                has_failed = true;
                            } else {
                                if (sb.special_snowflake) {
                                    late_install.add(sb.no_install);
                                } else if (References.checkThemeInterfacer(mContext) &&
                                        !References.isBinderInterfacer(mContext)) {
                                    // Thread wait
                                    isWaiting = true;
                                    do {
                                        try {
                                            Thread.sleep(THREAD_WAIT_DURATION);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    } while (isWaiting);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "Main function has unexpectedly stopped!");
                    }
                } else {
                    if (final_runner == null) final_runner = new ArrayList<>();
                    if (enable_mode || compile_enable_mode) {
                        String package_name = checkedOverlays.get(i).getFullOverlayParameters();
                        if (References.isPackageInstalled(mContext, package_name))
                            final_runner.add(package_name);
                    } else if (disable_mode) {
                        String package_name = checkedOverlays.get(i).getFullOverlayParameters();
                        if (References.isPackageInstalled(mContext, package_name))
                            final_runner.add(package_name);
                    }
                }
            }
            return null;
        }
    }

    class FinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String PRIMARY_COMMAND_KEY = "primary_command_key";
            String COMMAND_VALUE_JOB_COMPLETE = "job_complete";
            String command = intent.getStringExtra(PRIMARY_COMMAND_KEY);

            if (command.equals(COMMAND_VALUE_JOB_COMPLETE)) {
                Log.d(TAG,
                        "Substratum is now refreshing its resources after " +
                                "successful job completion!");
                isWaiting = false;
            }
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
                case "MixAndMatchMode":
                    if (mAdapter != null) {
                        boolean newValue = intent.getBooleanExtra("newValue", false);
                        setMixAndMatchMode(newValue);
                    }
                    break;
            }
        }
    }
}