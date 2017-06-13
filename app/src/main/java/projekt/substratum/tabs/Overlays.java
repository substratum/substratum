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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
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
import static projekt.substratum.common.References.ENABLE_PACKAGE_LOGGING;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.MAIN_NOTIFICATION_CHANNEL_ID;
import static projekt.substratum.common.References.MASQUERADE_PACKAGE;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.REFRESH_WINDOW_DELAY;
import static projekt.substratum.common.References.STATUS_CHANGED;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;
import static projekt.substratum.common.References.checkThemeInterfacer;
import static projekt.substratum.common.References.isPackageInstalled;
import static projekt.substratum.common.References.metadataAuthor;
import static projekt.substratum.common.References.metadataEmail;
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
    private String theme_name;
    private String theme_pid;
    private String versionName;
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
    private Switch toggle_all;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private Boolean is_active = false;
    private StringBuilder error_logs;
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
    private LocalBroadcastManager localBroadcastManager, localBroadcastManager2;
    private String type1a = "";
    private String type1b = "";
    private String type1c = "";
    private String type2 = "";
    private String type3 = "";
    private Phase3_mainFunction phase3_mainFunction;
    private Boolean encrypted = false;
    private Cipher cipher = null;
    private RefreshReceiver refreshReceiver;

    private void logTypes() {
        if (ENABLE_PACKAGE_LOGGING) {
            Log.d("Theme Type1a Resource", type1a);
            Log.d("Theme Type1b Resource", type1b);
            Log.d("Theme Type1c Resource", type1c);
            Log.d("Theme Type2  Resource", type2);
            Log.d("Theme Type3  Resource", type3);
        }
    }

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
                Phase2_InitializeCache phase2 = new Phase2_InitializeCache(this);
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

            // TODO: Disable the one overlay checker
            if (References.isSamsung(getContext())) {
                if (checkedOverlays.size() > 1) {
                    Lunchbar.make(
                            getActivityView(),
                            R.string.toast_samsung_prototype_one_overlay,
                            Lunchbar.LENGTH_LONG)
                            .show();
                    for (int i = 0; i < overlaysLists.size(); i++) {
                        OverlaysItem currentOverlay = overlaysLists.get(i);
                        if (currentOverlay.isSelected()) {
                            currentOverlay.setSelected(false);
                        }
                        mAdapter.notifyDataSetChanged();
                    }
                    is_active = false;
                    compile_enable_mode = false;
                    return;
                }
            }

            if (!checkedOverlays.isEmpty()) {
                Phase2_InitializeCache phase2 = new Phase2_InitializeCache(this);
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
                    Phase2_InitializeCache phase2 = new Phase2_InitializeCache(this);
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

                // TODO: Disable the one overlay checker
                if (References.isSamsung(getContext())) {
                    if (checkedOverlays.size() > 1) {
                        Lunchbar.make(
                                getActivityView(),
                                R.string.toast_samsung_prototype_one_overlay,
                                Lunchbar.LENGTH_LONG)
                                .show();
                        for (int i = 0; i < overlaysLists.size(); i++) {
                            OverlaysItem currentOverlay = overlaysLists.get(i);
                            if (currentOverlay.isSelected()) {
                                currentOverlay.setSelected(false);
                            }
                            mAdapter.notifyDataSetChanged();
                        }

                        compile_enable_mode = false;
                        enable_mode = false;
                        disable_mode = false;
                        is_active = false;
                        return;
                    } else {
                        // TODO: Do not hardcode to the 0th overlay
                        if (!References.isPackageInstalled(getContext(),
                                checkedOverlays.get(0).getPackageName() + "." +
                                        checkedOverlays.get(0).getThemeName())) {
                            Lunchbar.make(
                                    getActivityView(),
                                    R.string.toast_disabled5,
                                    Lunchbar.LENGTH_LONG)
                                    .show();

                            compile_enable_mode = false;
                            enable_mode = false;
                            disable_mode = false;
                            is_active = false;
                            return;
                        }
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
                        // TODO: Needs verifying this works with multiple APKs
                        for (int i = 0; i < checkedOverlays.size(); i++) {
                            Uri packageURI = Uri.parse("package:" +
                                    checkedOverlays.get(i).getPackageName() + "." +
                                    checkedOverlays.get(i).getThemeName());
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
                Phase2_InitializeCache phase2 = new Phase2_InitializeCache(this);
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

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new RefreshReceiver();
        IntentFilter filter = new IntentFilter("Overlay.REFRESH");
        localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        localBroadcastManager.registerReceiver(refreshReceiver, filter);

        theme_name = InformationActivity.getThemeName();
        theme_pid = InformationActivity.getThemePID();
        byte[] encryption_key = InformationActivity.getEncryptionKey();
        byte[] iv_encrypt_key = InformationActivity.getIVEncryptKey();

        if (encryption_key != null && iv_encrypt_key != null) {
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

    private List<String> updateEnabledOverlays() {
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
                mBuilder = new NotificationCompat.Builder(context, MAIN_NOTIFICATION_CHANNEL_ID);
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
            StringBuilder final_commands = new StringBuilder();
            if (compile_enable_mode && mixAndMatchMode) {
                // Buffer the disableBeforeEnabling String
                ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                for (String p : all_installed_overlays) {
                    if (!theme_pid.equals(References.grabOverlayParent(getContext(), p))) {
                        disableBeforeEnabling.add(p);
                    } else {
                        for (OverlaysItem oi : checkedOverlays) {
                            String targetOverlay = oi.getPackageName();
                            if (targetOverlay.equals(
                                    References.grabOverlayTarget(getContext(), p))) {
                                disableBeforeEnabling.add(p);
                            }
                        }
                    }
                }
                if (checkThemeInterfacer(context)) {
                    ThemeManager.disableOverlay(context, disableBeforeEnabling);
                } else {
                    final_commands = new StringBuilder(ThemeManager.disableOverlay);
                    for (int i = 0; i < disableBeforeEnabling.size(); i++) {
                        final_commands.append(" ").append(disableBeforeEnabling.get(i)).append(" ");
                    }
                    Log.d(TAG, final_commands.toString());
                }
            }

            if (compile_enable_mode) {
                ThemeManager.enableOverlay(context, final_command);
            }

            if (!checkThemeInterfacer(context) && isPackageInstalled(context, MASQUERADE_PACKAGE)) {
                Log.d(TAG, "Using Masquerade as the fallback system...");
                Intent runCommand = MasqueradeService.getMasquerade(getContext());
                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                runCommand.setAction("masquerade.substratum.COMMANDS");
                runCommand.putExtra("om-commands", final_commands.toString());
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
            if (needsRecreate(getContext())) {
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

            if (!late_install.isEmpty() && !References.isSamsung(context)) {
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
            mBuilder = new NotificationCompat.Builder(context, MAIN_NOTIFICATION_CHANNEL_ID);
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
            mBuilder.setChannel(References.MAIN_NOTIFICATION_CHANNEL_ID);
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
                new SendErrorReport(context, theme_pid, error_logs.toString()).execute();
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

    private boolean needsRecreate(Context context) {
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
        }
    }

    private void refreshList() {
        if (mAdapter != null) mAdapter.notifyDataSetChanged();

        for (int i = 0; i < overlaysLists.size(); i++) {
            OverlaysItem currentOverlay = overlaysLists.get(i);
            if (currentOverlay.isSelected()) {
                currentOverlay.setSelected(false);
            }

            // Try and kill the background processes of the app
            ActivityManager am = (ActivityManager)
                    getContext().getSystemService(Activity.ACTIVITY_SERVICE);
            am.killBackgroundProcesses(currentOverlay.getPackageName());

            mAdapter.notifyDataSetChanged();
        }
    }

    private static class SendErrorReport extends AsyncTask<Void, Void, File> {
        @SuppressLint("StaticFieldLeak")
        private Context context;
        private WeakReference<Context> contextRef;
        private String themePid;
        private String errorLog;
        private String themeName, themeAuthor, themeEmail;
        private String emailSubject, emailBody;
        private ProgressDialog progressDialog;

        SendErrorReport(Context context_, String themePid_, String errorLog_) {
            contextRef = new WeakReference<>(context_);
            themePid = themePid_;
            errorLog = errorLog_;

            themeName = References.grabPackageName(context_, themePid);
            themeAuthor = References.getOverlayMetadata(context_, themePid, metadataAuthor);
            themeEmail = References.getOverlayMetadata(context_, themePid, metadataEmail);

            emailSubject = String.format(
                    context_.getString(R.string.logcat_email_subject), themeName);
            emailBody = String.format(
                    context_.getString(R.string.logcat_email_body), themeAuthor, themeName);
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(contextRef.get());
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(
                    contextRef.get().getString(R.string.logcat_processing_dialog));
            progressDialog.show();
        }

        @Override
        protected File doInBackground(Void... sUrl) {
            String rom = References.checkFirmwareSupport(contextRef.get(),
                    contextRef.get().getString(R.string.supported_roms_url),
                    "supported_roms.xml");
            String version = Build.VERSION.RELEASE + " - " + (!rom.isEmpty() ? rom : "Unknown");

            String device = Build.MODEL + " (" + Build.DEVICE + ") " +
                    "[" + Build.FINGERPRINT + "]";
            String xposed = References.checkXposedVersion();
            if (!xposed.isEmpty()) device += " {" + xposed + "}";

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HH:mm", Locale.US);
            File log = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/theme_error-" + dateFormat.format(new Date()) + ".txt");
            try (FileWriter fw = new FileWriter(log, false);
                 BufferedWriter out = new BufferedWriter(fw)) {

                String attachment = String.format(
                        contextRef.get().getString(R.string.logcat_attachment_body),
                        device,
                        version,
                        References.grabAppVersion(contextRef.get(), themePid),
                        BuildConfig.VERSION_CODE, errorLog);
                out.write(attachment);
            } catch (IOException e) {
                // Suppress exception
            }
            return log;
        }

        @Override
        protected void onPostExecute(File result) {
            progressDialog.dismiss();

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{themeEmail});
            i.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
            i.putExtra(Intent.EXTRA_TEXT, emailBody);
            i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(result));
            try {
                contextRef.get().startActivity(Intent.createChooser(
                        i, contextRef.get().getString(R.string.logcat_email_activity)));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(contextRef.get(),
                        R.string.logcat_email_activity_error,
                        Toast.LENGTH_LONG)
                        .show();
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
            if (fragment.materialProgressBar != null) {
                fragment.materialProgressBar.setVisibility(View.VISIBLE);
            }
            fragment.mRecyclerView.setVisibility(View.INVISIBLE);
            fragment.swipeRefreshLayout.setVisibility(View.GONE);
            fragment.toggle_all.setEnabled(false);
            fragment.base_spinner.setEnabled(false);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Overlays fragment = ref.get();
            if (References.isSamsung(fragment.getContext()) &&
                    !References.isSamsungTheme(fragment.getContext(), fragment.theme_pid)) {
                Lunchbar.make(
                        fragment.getActivityView(),
                        R.string.toast_samsung_prototype_alert,
                        Lunchbar.LENGTH_LONG)
                        .show();
            }

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

        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(String... sUrl) {
            Overlays fragment = ref.get();
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
                            SUBSTRATUM_BUILDER_CACHE + fragment.theme_pid + "/assets/overlays/");

                    if (!References.checkOMS(context)) {
                        File check_file = new File(context.getCacheDir().getAbsoluteFile() +
                                SUBSTRATUM_BUILDER_CACHE + fragment.theme_pid +
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
                        String[] overlayList = fragment.themeAssetManager.list(overlaysDir);
                        Collections.addAll(overlaysFolder, overlayList);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                values.addAll(overlaysFolder.stream().filter(package_name -> (References
                        .isPackageInstalled(context, package_name) ||
                        References.allowedSystemUIOverlay(package_name) ||
                        References.allowedSettingsOverlay(package_name)) &&
                        (!ThemeManager.blacklisted(
                                package_name,
                                References.isSamsung(context) &&
                                        !References.isSamsungTheme(context, fragment.theme_pid))))
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
                                    package_name = context.getString(R.string.systemui_navigation);
                                    break;
                                case "com.android.systemui.statusbars":
                                    package_name = context.getString(R.string.systemui_statusbar);
                                    break;
                                case "com.android.systemui.tiles":
                                    package_name = context.getString(R.string.systemui_qs_tiles);
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
                        if (References.isCachingEnabled(context)) {
                            typeArrayRaw = new File(context.getCacheDir().getAbsoluteFile() +
                                    SUBSTRATUM_BUILDER_CACHE + fragment.theme_pid
                                    + "/assets/overlays/" + package_identifier);
                        } else {
                            // Begin the no caching algorithm
                            typeArrayRaw = fragment.themeAssetManager.list(
                                    overlaysDir + "/" + package_identifier);

                            // Sort the typeArray so that the types are asciibetical
                            Collections.addAll(typeArray, (String[]) typeArrayRaw);
                            Collections.sort(typeArray);
                        }

                        if (!References.checkOMS(context)) {
                            File check_file = new File(
                                    context.getCacheDir().getAbsoluteFile() +
                                            SUBSTRATUM_BUILDER_CACHE + fragment.theme_pid
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

                        if (References.isCachingEnabled(fragment.getContext()) &&
                                (typeArray.contains("type2") || typeArray.contains("type2.enc"))) {
                            type2.add(fragment.setTypeTwoSpinners(
                                    new InputStreamReader(new FileInputStream(
                                            new File(((File) typeArrayRaw).getAbsolutePath() +
                                                    "/type2")))));
                        } else if (typeArray.contains("type2") || typeArray.contains("type2.enc")) {
                            if (fragment.encrypted) {
                                type2.add(fragment.setTypeTwoSpinners(new InputStreamReader(
                                        FileOperations.getInputStream(
                                                fragment.themeAssetManager,
                                                overlaysDir + "/" + package_identifier +
                                                        "/type2.enc",
                                                fragment.cipher))));
                            } else {
                                type2.add(fragment.setTypeTwoSpinners(
                                        new InputStreamReader(
                                                fragment.themeAssetManager.open(overlaysDir +
                                                        "/" + package_identifier + "/type2"))));
                            }
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
                                    } else if (!current.contains(".") && current.length() > 5 &&
                                            current.substring(0, 6).equals("type2_")) {
                                        type2.add(new VariantItem(current.substring(6), null));
                                    }
                                }
                            }

                            VariantAdapter adapter1 = new VariantAdapter(context, type1a);
                            VariantAdapter adapter2 = new VariantAdapter(context, type1b);
                            VariantAdapter adapter3 = new VariantAdapter(context, type1c);
                            VariantAdapter adapter4 = new VariantAdapter(context, type2);

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
            return null;
        }
    }

    private static class Phase2_InitializeCache extends AsyncTask<String, Integer, String> {
        private WeakReference<Overlays> ref;

        Phase2_InitializeCache(Overlays fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            Overlays fragment = ref.get();
            Context context = fragment.getActivity();
            fragment.final_runner = new ArrayList<>();
            fragment.late_install = new ArrayList<>();

            if (!fragment.enable_mode && !fragment.disable_mode) {
                int notification_priority = Notification.PRIORITY_MAX;

                // Create an Intent for the BroadcastReceiver
                Intent buttonIntent = new Intent(context, NotificationButtonReceiver.class);

                // Create the PendingIntent
                PendingIntent btPendingIntent = PendingIntent.getBroadcast(
                        context, 0, buttonIntent, 0);
                PendingIntent resultPendingIntent = PendingIntent.getActivity(
                        context, 0, new Intent(), 0);

                // This is the time when the notification should be shown on the user's screen
                fragment.mNotifyManager =
                        (NotificationManager) context.getSystemService(
                                Context.NOTIFICATION_SERVICE);
                fragment.mBuilder = new NotificationCompat.Builder(context,
                        MAIN_NOTIFICATION_CHANNEL_ID);
                fragment.mBuilder.setContentTitle(
                        context.getString(R.string.notification_initial_title))
                        .setProgress(100, 0, true)
                        .addAction(android.R.color.transparent, context.getString(R.string
                                .notification_hide), btPendingIntent)
                        .setSmallIcon(android.R.drawable.ic_popup_sync)
                        .setPriority(notification_priority)
                        .setContentIntent(resultPendingIntent)
                        .setChannel(References.MAIN_NOTIFICATION_CHANNEL_ID)
                        .setOngoing(true);
                fragment.mNotifyManager.notify(fragment.id, fragment.mBuilder.build());

                fragment.mProgressDialog = null;
                fragment.mProgressDialog = new ProgressDialog(context,
                        R.style.SubstratumBuilder_ActivityTheme);
                fragment.mProgressDialog.setIndeterminate(false);
                fragment.mProgressDialog.setCancelable(false);
                fragment.mProgressDialog.show();
                fragment.mProgressDialog.setContentView(R.layout.compile_dialog_loader);
                if (fragment.mProgressDialog.getWindow() != null) {
                    fragment.mProgressDialog.getWindow().addFlags(
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }

                final float radius = 5;
                final View decorView = fragment.getActivity().getWindow().getDecorView();
                final ViewGroup rootView = decorView.findViewById(android.R.id.content);
                final Drawable windowBackground = decorView.getBackground();

                BlurView blurView = fragment.mProgressDialog.findViewById(R.id.blurView);

                if (rootView != null) {
                    blurView.setupWith(rootView)
                            .windowBackground(windowBackground)
                            .blurAlgorithm(new RenderScriptBlur(context))
                            .blurRadius(radius);
                }

                fragment.dialogProgress = fragment.mProgressDialog.findViewById(R.id.loading_bar);
                fragment.dialogProgress.setProgressTintList(ColorStateList.valueOf(context.getColor(
                        R.color.compile_dialog_wave_color)));
                fragment.dialogProgress.setIndeterminate(false);

                fragment.loader_string = fragment.mProgressDialog.findViewById(R.id.title);
                fragment.loader_string.setText(context.getResources().getString(
                        R.string.sb_phase_1_loader));
            }
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            Overlays fragment = ref.get();
            fragment.phase3_mainFunction = new Phase3_mainFunction(fragment);
            if (result != null) {
                fragment.phase3_mainFunction.execute(result);
            } else {
                fragment.phase3_mainFunction.execute("");
            }
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            Overlays fragment = ref.get();
            Context context = fragment.getActivity();
            if (!fragment.enable_mode && !fragment.disable_mode) {
                // Initialize Substratum cache with theme only if permitted
                if (References.isCachingEnabled(context) && !fragment.has_initialized_cache) {
                    Log.d(TAG,
                            "Decompiling and initializing work area with the " +
                                    "selected theme's assets...");
                    fragment.sb = new SubstratumBuilder();

                    File versioning = new File(context.getCacheDir().getAbsoluteFile() +
                            SUBSTRATUM_BUILDER_CACHE + fragment.theme_pid + "/substratum.xml");
                    if (versioning.exists()) {
                        fragment.has_initialized_cache = true;
                    } else {
                        new CacheCreator().initializeCache(context, fragment.theme_pid,
                                fragment.cipher);
                        fragment.has_initialized_cache = true;
                    }
                } else {
                    try {
                        Resources themeResources = context.getPackageManager()
                                .getResourcesForApplication(fragment.theme_pid);
                        fragment.themeAssetManager = themeResources.getAssets();
                    } catch (PackageManager.NameNotFoundException e) {
                        // Suppress exception
                    }
                    Log.d(TAG, "Work area is ready to be compiled!");
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

    private static class Phase3_mainFunction extends AsyncTask<String, Integer, String> {
        private WeakReference<Overlays> ref;

        Phase3_mainFunction(Overlays fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "Substratum is proceeding with your actions and is now actively running...");
            Overlays fragment = ref.get();
            Context context = fragment.getActivity();

            fragment.missingType3 = false;
            fragment.has_failed = false;
            fragment.fail_count = 0;
            fragment.error_logs = new StringBuilder();

            if (!fragment.enable_mode && !fragment.disable_mode) {
                // Change title in preparation for loop to change subtext
                if (fragment.checkActiveNotifications()) {
                    fragment.mBuilder
                            .setContentTitle(context.getString(R.string.notification_processing_n))
                            .setProgress(100, 0, false);
                    fragment.mNotifyManager.notify(fragment.id, fragment.mBuilder.build());
                }
                fragment.loader_string.setText(context.getResources().getString(
                        R.string.sb_phase_2_loader));
            } else {
                fragment.progressBar.setVisibility(View.VISIBLE);
            }
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            Overlays fragment = ref.get();
            TextView textView = fragment.mProgressDialog.findViewById(R.id.current_object);
            textView.setText(fragment.current_dialog_overlay);
            double progress = (fragment.current_amount / fragment.total_amount) * 100;
            fragment.dialogProgress.setProgress((int) progress, true);
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected void onPostExecute(String result) {
            // TODO: onPostExecute runs on UI thread, so move the hard job to doInBackground
            super.onPostExecute(result);
            Overlays fragment = ref.get();
            Context context = fragment.getActivity();

            fragment.final_command = new ArrayList<>();

            // Check if not compile_enable_mode
            if (!fragment.compile_enable_mode) {
                fragment.final_command.addAll(fragment.final_runner);
            } else {
                // It's compile and enable mode, we have to first sort out all the "pm install"'s
                // from the final_commands
                fragment.final_command.addAll(fragment.final_runner);
            }

            if (!fragment.enable_mode && !fragment.disable_mode) {
                fragment.finishFunction(context);
                if (fragment.has_failed) {
                    fragment.failedFunction(context);
                } else {
                    // Restart SystemUI if an enabled SystemUI overlay is updated
                    for (int i = 0; i < fragment.checkedOverlays.size(); i++) {
                        String targetOverlay = fragment.checkedOverlays.get(i).getPackageName();
                        if (targetOverlay.equals("android") ||
                                targetOverlay.equals("com.android.systemui")) {
                            String packageName =
                                    fragment.checkedOverlays.get(i).getFullOverlayParameters();
                            if (ThemeManager.isOverlayEnabled(context, packageName)) {
                                ThemeManager.restartSystemUI(context);
                                break;
                            }
                        }
                    }
                }
                try {
                    context.unregisterReceiver(fragment.finishReceiver);
                } catch (IllegalArgumentException e) {
                    // Suppress warning
                }
            } else if (fragment.enable_mode) {
                if (fragment.final_runner.size() > 0) {
                    fragment.enable_mode = false;

                    if (fragment.mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        for (int i = 0; i < fragment.all_installed_overlays.size(); i++) {
                            if (!References.grabOverlayParent(context,
                                    fragment.all_installed_overlays.get(i))
                                    .equals(fragment.theme_pid)) {
                                disableBeforeEnabling.add(fragment.all_installed_overlays.get(i));
                            }
                        }
                        fragment.progressBar.setVisibility(View.VISIBLE);
                        if (fragment.toggle_all.isChecked()) fragment.toggle_all.setChecked(false);
                        ThemeManager.disableOverlay(context, disableBeforeEnabling);
                        ThemeManager.enableOverlay(context, fragment.final_command);
                    } else {
                        fragment.progressBar.setVisibility(View.VISIBLE);
                        if (fragment.toggle_all.isChecked()) fragment.toggle_all.setChecked(false);
                        ThemeManager.enableOverlay(context, fragment.final_command);
                    }

                    fragment.progressBar.setVisibility(View.GONE);
                    if (fragment.needsRecreate(context)) {
                        Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                fragment.overlaysLists = ((OverlaysAdapter) fragment.mAdapter)
                                        .getOverlayList();
                                for (int i = 0; i < fragment.overlaysLists.size(); i++) {
                                    OverlaysItem currentOverlay = fragment.overlaysLists.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(
                                            fragment.updateEnabledOverlays());
                                    fragment.mAdapter.notifyDataSetChanged();
                                }
                            } catch (Exception e) {
                                // Consume window refresh
                            }
                        }, REFRESH_WINDOW_DELAY);
                    }
                } else {
                    fragment.compile_enable_mode = false;
                    fragment.enable_mode = false;
                    Lunchbar.make(
                            fragment.getActivityView(),
                            R.string.toast_disabled3,
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            } else if (fragment.disable_mode) {
                if (fragment.final_runner.size() > 0) {
                    fragment.disable_mode = false;
                    fragment.progressBar.setVisibility(View.VISIBLE);
                    if (fragment.toggle_all.isChecked()) fragment.toggle_all.setChecked(false);
                    ThemeManager.disableOverlay(context, fragment.final_command);

                    fragment.progressBar.setVisibility(View.GONE);
                    if (fragment.needsRecreate(context)) {
                        Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                fragment.overlaysLists = ((OverlaysAdapter) fragment.mAdapter)
                                        .getOverlayList();
                                for (int i = 0; i < fragment.overlaysLists.size(); i++) {
                                    OverlaysItem currentOverlay = fragment.overlaysLists.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(
                                            fragment.updateEnabledOverlays());
                                    fragment.mAdapter.notifyDataSetChanged();
                                }
                            } catch (Exception e) {
                                // Consume window refresh
                            }
                        }, REFRESH_WINDOW_DELAY);
                    }
                } else {
                    fragment.disable_mode = false;
                    Lunchbar.make(
                            fragment.getActivityView(),
                            R.string.toast_disabled4,
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            }
            // TODO: Handle multiple APKs
            if (References.isSamsung(context) &&
                    fragment.late_install != null &&
                    fragment.late_install.size() > 0) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = FileProvider.getUriForFile(
                        context,
                        context.getApplicationContext().getPackageName() + ".provider",
                        new File(fragment.late_install.get(0)));
                intent.setDataAndType(
                        uri,
                        "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                fragment.startActivityForResult(intent, 2486);
            } else if (!References.checkOMS(context) &&
                    fragment.final_runner.size() == fragment.fail_count) {
                final AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(context);
                alertDialogBuilder
                        .setTitle(context.getString(R.string.legacy_dialog_soft_reboot_title));
                alertDialogBuilder
                        .setMessage(context.getString(R.string.legacy_dialog_soft_reboot_text));
                alertDialogBuilder
                        .setPositiveButton(android.R.string.ok,
                                (dialog, id12) -> ElevatedCommands.reboot());
                alertDialogBuilder
                        .setNegativeButton(R.string.remove_dialog_later,
                                (dialog, id1) -> {
                                    fragment.progressBar.setVisibility(View.GONE);
                                    dialog.dismiss();
                                });
                alertDialogBuilder.setCancelable(false);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
            fragment.is_active = false;
            fragment.mAdapter.notifyDataSetChanged();
            if (fragment.toggle_all.isChecked()) fragment.toggle_all.setChecked(false);
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        protected String doInBackground(String... sUrl) {
            Overlays fragment = ref.get();
            Context context = fragment.getActivity();
            String parsedVariant = sUrl[0].replaceAll("\\s+", "");
            String unparsedVariant = sUrl[0];

            if (fragment.mixAndMatchMode && !References.checkOMS(context)) {
                String current_directory;
                if (References.inNexusFilter()) {
                    current_directory = PIXEL_NEXUS_DIR;
                } else {
                    current_directory = LEGACY_NEXUS_DIR;
                }
                File file = new File(current_directory);
                if (file.exists()) {
                    FileOperations.mountRW();
                    FileOperations.delete(context, current_directory);
                }
            }

            // Enable listener
            if (References.checkThemeInterfacer(context) &&
                    !References.isBinderInterfacer(context)) {
                if (fragment.finishReceiver == null) {
                    fragment.finishReceiver = new FinishReceiver(fragment);
                }
                IntentFilter filter = new IntentFilter(STATUS_CHANGED);
                context.registerReceiver(fragment.finishReceiver, filter);
            }

            fragment.total_amount = fragment.checkedOverlays.size();
            for (int i = 0; i < fragment.checkedOverlays.size(); i++) {
                fragment.type1a = "";
                fragment.type1b = "";
                fragment.type1c = "";
                fragment.type2 = "";
                fragment.type3 = "";

                fragment.current_amount = i + 1;
                String theme_name_parsed =
                        fragment.theme_name.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
                String current_overlay = fragment.checkedOverlays.get(i).getPackageName();
                fragment.current_dialog_overlay =
                        "'" + References.grabPackageName(context, current_overlay) + "'";

                if (!fragment.enable_mode && !fragment.disable_mode) {
                    publishProgress((int) fragment.current_amount);
                    if (fragment.compile_enable_mode) {
                        if (fragment.final_runner == null) {
                            fragment.final_runner = new ArrayList<>();
                        }
                        String package_name = fragment.checkedOverlays.get(i)
                                .getFullOverlayParameters();
                        if (References.isPackageInstalled(context, package_name) ||
                                fragment.compile_enable_mode) {
                            fragment.final_runner.add(package_name);
                        }
                    }
                    try {
                        String packageTitle = "";
                        if (References.allowedSystemUIOverlay(current_overlay)) {
                            switch (current_overlay) {
                                case "com.android.systemui.headers":
                                    packageTitle = context.getString(R.string.systemui_headers);
                                    break;
                                case "com.android.systemui.navbars":
                                    packageTitle = context.getString(R.string.systemui_navigation);
                                    break;
                                case "com.android.systemui.statusbars":
                                    packageTitle = context.getString(R.string.systemui_statusbar);
                                    break;
                                case "com.android.systemui.tiles":
                                    packageTitle = context.getString(R.string.systemui_qs_tiles);
                                    break;
                            }
                        } else if (References.allowedSettingsOverlay(current_overlay)) {
                            switch (current_overlay) {
                                case "com.android.settings.icons":
                                    packageTitle = context.getString(R.string.settings_icons);
                                    break;
                            }
                        } else {
                            ApplicationInfo applicationInfo =
                                    context.getPackageManager()
                                            .getApplicationInfo(current_overlay, 0);
                            packageTitle = context.getPackageManager()
                                    .getApplicationLabel(applicationInfo).toString();
                        }

                        // Initialize working notification
                        if (fragment.checkActiveNotifications()) {
                            fragment.mBuilder.setProgress(100, (int) (((double) (i + 1) /
                                    fragment.checkedOverlays.size()) * 100), false);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                fragment.mBuilder.setContentText("\"" + packageTitle + "\"");
                            } else {
                                fragment.mBuilder.setContentText(
                                        context.getString(R.string.notification_processing) +
                                                "\"" + packageTitle + "\"");
                            }
                            fragment.mNotifyManager.notify(fragment.id, fragment.mBuilder.build());
                        }

                        String workingDirectory = context.getCacheDir().getAbsolutePath() +
                                SUBSTRATUM_BUILDER_CACHE + fragment.theme_pid +
                                "/assets/overlays/" + current_overlay;

                        if (!References.checkOMS(context)) {
                            File check_legacy = new File(context.getCacheDir()
                                    .getAbsolutePath() + SUBSTRATUM_BUILDER_CACHE +
                                    fragment.theme_pid + "/assets/overlays_legacy/" +
                                    current_overlay);
                            if (check_legacy.exists()) {
                                workingDirectory = check_legacy.getAbsolutePath();
                            }
                        }
                        String suffix = ((sUrl[0].length() != 0) ?
                                "/type3_" + parsedVariant : "/res");
                        String unparsedSuffix =
                                ((sUrl[0].length() != 0) ? "/type3_" + unparsedVariant : "/res");
                        fragment.type3 = parsedVariant;
                        if (References.isCachingEnabled(context)) {
                            File srcDir = new File(workingDirectory +
                                    ((sUrl[0].length() != 0) ? "/type3_" + sUrl[0] : "/res"));
                            File destDir = new File(workingDirectory + "/workdir");
                            if (destDir.exists()) {
                                FileOperations.delete(context, destDir.getAbsolutePath());
                            }
                            FileUtils.copyDirectory(srcDir, destDir);
                        } else {
                            workingDirectory = context.getCacheDir().getAbsolutePath() +
                                    SUBSTRATUM_BUILDER_CACHE.substring(0,
                                            SUBSTRATUM_BUILDER_CACHE.length() - 1);
                            File created = new File(workingDirectory);
                            if (created.exists()) {
                                FileOperations.delete(context, created.getAbsolutePath());
                                FileOperations.createNewFolder(context, created
                                        .getAbsolutePath());
                            } else {
                                FileOperations.createNewFolder(context, created
                                        .getAbsolutePath());
                            }
                            String listDir = overlaysDir + "/" + current_overlay + unparsedSuffix;

                            FileOperations.copyFileOrDir(
                                    fragment.themeAssetManager,
                                    listDir,
                                    workingDirectory + suffix,
                                    listDir,
                                    fragment.cipher
                            );
                        }

                        if (fragment.checkedOverlays.get(i).is_variant_chosen ||
                                sUrl[0].length() != 0) {
                            // Type 1a
                            if (fragment.checkedOverlays.get(i).is_variant_chosen1) {
                                fragment.type1a =
                                        fragment.checkedOverlays.get(i).getSelectedVariantName();
                                if (References.isCachingEnabled(context)) {
                                    String sourceLocation = workingDirectory + "/type1a_" +
                                            fragment.checkedOverlays.get(i).getSelectedVariantName()
                                            + ".xml";

                                    String targetLocation = workingDirectory +
                                            "/workdir/values/type1a.xml";

                                    Log.d(TAG,
                                            "You have selected variant file \"" +
                                                    fragment.checkedOverlays.get(i)
                                                            .getSelectedVariantName() + "\"");
                                    Log.d(TAG, "Moving variant file to: " + targetLocation);
                                    FileOperations.copy(
                                            context,
                                            sourceLocation,
                                            targetLocation);
                                } else {
                                    Log.d(TAG, "You have selected variant file \"" +
                                            fragment.checkedOverlays.get(i).getSelectedVariantName()
                                            + "\"");
                                    Log.d(TAG, "Moving variant file to: " +
                                            workingDirectory + suffix + "/values/type1a.xml");

                                    String to_copy =
                                            overlaysDir + "/" + current_overlay + "/type1a_" +
                                                    fragment.checkedOverlays.get(i)
                                                            .getSelectedVariantName() +
                                                    (fragment.encrypted ? ".xml.enc" : ".xml");

                                    FileOperations.copyFileOrDir(
                                            fragment.themeAssetManager,
                                            to_copy,
                                            workingDirectory + suffix + (
                                                    fragment.encrypted ?
                                                            "/values/type1a.xml.enc" :
                                                            "/values/type1a.xml"),
                                            to_copy,
                                            fragment.cipher);
                                }
                            }

                            // Type 1b
                            if (fragment.checkedOverlays.get(i).is_variant_chosen2) {
                                fragment.type1b =
                                        fragment.checkedOverlays.get(i).getSelectedVariantName2();
                                if (References.isCachingEnabled(context)) {
                                    String sourceLocation2 = workingDirectory + "/type1b_" +
                                            fragment.checkedOverlays.get(i)
                                                    .getSelectedVariantName2() + ".xml";

                                    String targetLocation2 = workingDirectory +
                                            "/workdir/values/type1b.xml";

                                    Log.d(TAG, "You have selected variant file \"" +
                                            fragment.checkedOverlays.get(i)
                                                    .getSelectedVariantName2() + "\"");
                                    Log.d(TAG, "Moving variant file to: " + targetLocation2);
                                    FileOperations.copy(context, sourceLocation2,
                                            targetLocation2);
                                } else {
                                    Log.d(TAG, "You have selected variant file \"" +
                                            fragment.checkedOverlays.get(i)
                                                    .getSelectedVariantName2() + "\"");
                                    Log.d(TAG, "Moving variant file to: " +
                                            workingDirectory + suffix + "/values/type1b.xml");

                                    String to_copy =
                                            overlaysDir + "/" + current_overlay + "/type1b_" +
                                                    fragment.checkedOverlays.get(i)
                                                            .getSelectedVariantName2() +
                                                    (fragment.encrypted ? ".xml.enc" : ".xml");

                                    FileOperations.copyFileOrDir(
                                            fragment.themeAssetManager,
                                            to_copy,
                                            workingDirectory + suffix + (
                                                    fragment.encrypted ?
                                                            "/values/type1b.xml.enc" :
                                                            "/values/type1b.xml"),
                                            to_copy,
                                            fragment.cipher);
                                }
                            }
                            // Type 1c
                            if (fragment.checkedOverlays.get(i).is_variant_chosen3) {
                                fragment.type1c =
                                        fragment.checkedOverlays.get(i).getSelectedVariantName3();
                                if (References.isCachingEnabled(context)) {
                                    String sourceLocation3 = workingDirectory + "/type1c_" +
                                            fragment.checkedOverlays.get(i)
                                                    .getSelectedVariantName3() + ".xml";

                                    String targetLocation3 = workingDirectory +
                                            "/workdir/values/type1c.xml";

                                    Log.d(TAG, "You have selected variant file \"" +
                                            fragment.checkedOverlays.get(i)
                                                    .getSelectedVariantName3() + "\"");
                                    Log.d(TAG, "Moving variant file to: " + targetLocation3);

                                    FileOperations.copy(
                                            context,
                                            sourceLocation3,
                                            targetLocation3);
                                } else {
                                    Log.d(TAG, "You have selected variant file \"" +
                                            fragment.checkedOverlays.get(i)
                                                    .getSelectedVariantName3() + "\"");
                                    Log.d(TAG, "Moving variant file to: " +
                                            workingDirectory + suffix + "/values/type1c.xml");

                                    String to_copy =
                                            overlaysDir + "/" + current_overlay + "/type1c_" +
                                                    fragment.checkedOverlays.get(i)
                                                            .getSelectedVariantName3() +
                                                    (fragment.encrypted ? ".xml.enc" : ".xml");

                                    FileOperations.copyFileOrDir(
                                            fragment.themeAssetManager,
                                            to_copy,
                                            workingDirectory + suffix + (
                                                    fragment.encrypted ?
                                                            "/values/type1c.xml.enc" :
                                                            "/values/type1c.xml"),
                                            to_copy,
                                            fragment.cipher);
                                }
                            }

                            String packageName =
                                    (fragment.checkedOverlays.get(i).is_variant_chosen1 ?
                                            fragment.checkedOverlays.get(i)
                                                    .getSelectedVariantName() : "") +
                                            (fragment.checkedOverlays.get(i).is_variant_chosen2 ?
                                                    fragment.checkedOverlays.get(i)
                                                            .getSelectedVariantName2() : "") +
                                            (fragment.checkedOverlays.get(i).is_variant_chosen3 ?
                                                    fragment.checkedOverlays.get(i)
                                                            .getSelectedVariantName3() : "").
                                                    replaceAll("\\s+", "").replaceAll
                                                    ("[^a-zA-Z0-9]+", "");

                            if (fragment.checkedOverlays.get(i).is_variant_chosen4) {
                                packageName = (packageName + fragment.checkedOverlays.get(i)
                                        .getSelectedVariantName4()).replaceAll("\\s+", "")
                                        .replaceAll("[^a-zA-Z0-9]+", "");
                                fragment.type2 = fragment.checkedOverlays.get(i)
                                        .getSelectedVariantName4();
                                String type2folder = "/type2_" + fragment.type2;
                                String to_copy = overlaysDir + "/" + current_overlay + type2folder;
                                FileOperations.copyFileOrDir(
                                        fragment.themeAssetManager,
                                        to_copy,
                                        workingDirectory + type2folder,
                                        to_copy,
                                        fragment.cipher);
                                Log.d(TAG, "Currently processing package" +
                                        " \"" + fragment.checkedOverlays.get(i)
                                        .getFullOverlayParameters() + "\"...");

                                if (sUrl[0].length() != 0) {
                                    fragment.sb = new SubstratumBuilder();
                                    fragment.sb.beginAction(
                                            context,
                                            fragment.theme_pid,
                                            current_overlay,
                                            fragment.theme_name,
                                            packageName,
                                            fragment.checkedOverlays.get(i)
                                                    .getSelectedVariantName4(),
                                            sUrl[0],
                                            fragment.versionName,
                                            References.checkOMS(context),
                                            fragment.theme_pid,
                                            suffix,
                                            fragment.type1a,
                                            fragment.type1b,
                                            fragment.type1c,
                                            fragment.type2,
                                            fragment.type3,
                                            null);
                                    fragment.logTypes();
                                } else {
                                    fragment.sb = new SubstratumBuilder();
                                    fragment.sb.beginAction(
                                            context,
                                            fragment.theme_pid,
                                            current_overlay,
                                            fragment.theme_name,
                                            packageName,
                                            fragment.checkedOverlays.get(i)
                                                    .getSelectedVariantName4(),
                                            null,
                                            fragment.versionName,
                                            References.checkOMS(context),
                                            fragment.theme_pid,
                                            suffix,
                                            fragment.type1a,
                                            fragment.type1b,
                                            fragment.type1c,
                                            fragment.type2,
                                            fragment.type3,
                                            null);
                                    fragment.logTypes();
                                }
                            } else {
                                Log.d(TAG, "Currently processing package" +
                                        " \"" + fragment.checkedOverlays.get(i)
                                        .getFullOverlayParameters() + "\"...");

                                if (sUrl[0].length() != 0) {
                                    fragment.sb = new SubstratumBuilder();
                                    fragment.sb.beginAction(
                                            context,
                                            fragment.theme_pid,
                                            current_overlay,
                                            fragment.theme_name,
                                            packageName,
                                            null,
                                            sUrl[0],
                                            fragment.versionName,
                                            References.checkOMS(context),
                                            fragment.theme_pid,
                                            suffix,
                                            fragment.type1a,
                                            fragment.type1b,
                                            fragment.type1c,
                                            fragment.type2,
                                            fragment.type3,
                                            null);
                                    fragment.logTypes();
                                } else {
                                    fragment.sb = new SubstratumBuilder();
                                    fragment.sb.beginAction(
                                            context,
                                            fragment.theme_pid,
                                            current_overlay,
                                            fragment.theme_name,
                                            packageName,
                                            null,
                                            null,
                                            fragment.versionName,
                                            References.checkOMS(context),
                                            fragment.theme_pid,
                                            suffix,
                                            fragment.type1a,
                                            fragment.type1b,
                                            fragment.type1c,
                                            fragment.type2,
                                            fragment.type3,
                                            null);
                                    fragment.logTypes();
                                }
                            }
                            if (fragment.sb.has_errored_out) {
                                if (!fragment.sb.getErrorLogs().contains("type3") ||
                                        !fragment.sb.getErrorLogs().contains("does not exist")) {
                                    fragment.fail_count += 1;
                                    if (fragment.error_logs.length() == 0) {
                                        fragment.error_logs.append(fragment.sb.getErrorLogs());
                                    } else {
                                        fragment.error_logs.append("\n")
                                                .append(fragment.sb.getErrorLogs());
                                    }
                                    fragment.has_failed = true;
                                } else {
                                    fragment.missingType3 = true;
                                }
                            } else {
                                if (fragment.sb.special_snowflake ||
                                        fragment.sb.no_install.length() > 0) {
                                    fragment.late_install.add(fragment.sb.no_install);
                                } else if (References.checkThemeInterfacer(context) &&
                                        !References.isBinderInterfacer(context)) {
                                    // Thread wait
                                    fragment.isWaiting = true;
                                    do {
                                        try {
                                            Thread.sleep(THREAD_WAIT_DURATION);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    } while (fragment.isWaiting);
                                }
                            }
                        } else {
                            Log.d(TAG, "Currently processing package" +
                                    " \"" + current_overlay + "." + theme_name_parsed + "\"...");
                            fragment.sb = new SubstratumBuilder();
                            fragment.sb.beginAction(
                                    context,
                                    fragment.theme_pid,
                                    current_overlay,
                                    fragment.theme_name,
                                    null,
                                    null,
                                    null,
                                    fragment.versionName,
                                    References.checkOMS(context),
                                    fragment.theme_pid,
                                    suffix,
                                    fragment.type1a,
                                    fragment.type1b,
                                    fragment.type1c,
                                    fragment.type2,
                                    fragment.type3,
                                    null);
                            fragment.logTypes();

                            if (fragment.sb.has_errored_out) {
                                fragment.fail_count += 1;
                                if (fragment.error_logs.length() == 0) {
                                    fragment.error_logs.append(fragment.sb.getErrorLogs());
                                } else {
                                    fragment.error_logs.append("\n")
                                            .append(fragment.sb.getErrorLogs());
                                }
                                fragment.has_failed = true;
                            } else {
                                if (fragment.sb.special_snowflake ||
                                        fragment.sb.no_install.length() > 0) {
                                    fragment.late_install.add(fragment.sb.no_install);
                                } else if (References.checkThemeInterfacer(context) &&
                                        !References.isBinderInterfacer(context)) {
                                    // Thread wait
                                    fragment.isWaiting = true;
                                    do {
                                        try {
                                            Thread.sleep(THREAD_WAIT_DURATION);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    } while (fragment.isWaiting);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "Main function has unexpectedly stopped!");
                    }
                } else {
                    if (fragment.final_runner == null) fragment.final_runner = new ArrayList<>();
                    if (fragment.enable_mode || fragment.compile_enable_mode ||
                            fragment.disable_mode) {
                        String package_name =
                                fragment.checkedOverlays.get(i).getFullOverlayParameters();
                        if (References.isPackageInstalled(context, package_name)) {
                            fragment.final_runner.add(package_name);
                        }
                    }
                }
            }
            return null;
        }
    }

    private static class FinishReceiver extends BroadcastReceiver {
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

    private class JobReceiver extends BroadcastReceiver {
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

    class RefreshReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshList();
        }
    }
}