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

package projekt.substratum.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.gordonwong.materialsheetfab.MaterialSheetFab;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.R;
import projekt.substratum.adapters.fragments.manager.ManagerAdapter;
import projekt.substratum.adapters.fragments.manager.ManagerItem;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.MasqueradeService;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.views.FloatingActionMenu;

import static android.content.om.OverlayInfo.STATE_APPROVED_DISABLED;
import static android.content.om.OverlayInfo.STATE_APPROVED_ENABLED;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.MASQUERADE_PACKAGE;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.REFRESH_WINDOW_DELAY;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.References.VENDOR_DIR;
import static projekt.substratum.common.References.checkThemeInterfacer;
import static projekt.substratum.common.References.isPackageInstalled;
import static projekt.substratum.util.files.MapUtils.sortMapByValues;

public class ManagerFragment extends Fragment {

    private ArrayList<String> activated_overlays;
    private RecyclerView.Adapter mAdapter;
    private MaterialSheetFab materialSheetFab;
    private SharedPreferences prefs;
    private RelativeLayout relativeLayout;
    private ViewGroup root;
    private List<ManagerItem> overlaysList;
    private FloatingActionMenu floatingActionButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean swipeRefreshing;
    private Boolean first_run = null;
    private MaterialProgressBar progressBar;
    private RecyclerView mRecyclerView;
    private ProgressBar loadingBar;
    private List<ManagerItem> overlayList;
    private FinishReceiver finishReceiver;
    private Context context;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        root = (ViewGroup) inflater.inflate(R.layout.advanced_manager_fragment, container, false);
        relativeLayout = (RelativeLayout) root.findViewById(R.id.no_overlays_enabled);
        mRecyclerView = (RecyclerView) root.findViewById(R.id.overlays_recycler_view);

        loadingBar = (ProgressBar) root.findViewById(R.id.header_loading_bar);
        loadingBar.setVisibility(View.GONE);

        View sheetView = root.findViewById(R.id.fab_sheet);
        View overlay = root.findViewById(R.id.overlay);
        int sheetColor = context.getColor(R.color.fab_menu_background_card);
        int fabColor = context.getColor(R.color.fab_background_color);

        progressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);

        floatingActionButton = (FloatingActionMenu) root.findViewById(R.id.apply_fab);
        floatingActionButton.hide();

        // Create material sheet FAB
        if (sheetView != null && overlay != null) {
            materialSheetFab = new MaterialSheetFab<>(floatingActionButton, sheetView, overlay,
                    sheetColor, fabColor);
        }

        new LayoutReloader(ManagerFragment.this).execute();

        if (References.checkThemeInterfacer(context)) {
            finishReceiver = new FinishReceiver(ManagerFragment.this);
            IntentFilter intentFilter = new IntentFilter(References.STATUS_CHANGED);
            context.registerReceiver(finishReceiver, intentFilter);
        }

        swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (first_run != null && mRecyclerView.isShown() && !first_run) {
                swipeRefreshing = true;
                new LayoutReloader(ManagerFragment.this).execute();
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        Switch toggle_all = (Switch) root.findViewById(R.id.select_all);
        toggle_all.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    try {
                        overlayList = ((ManagerAdapter) mAdapter).getOverlayManagerList();
                        if (isChecked) {
                            for (int i = 0; i < overlayList.size(); i++) {
                                ManagerItem currentOverlay = overlayList.get(i);
                                if (!currentOverlay.isSelected()) {
                                    currentOverlay.setSelected(true);
                                }
                                mAdapter.notifyDataSetChanged();
                            }
                        } else {
                            for (int i = 0; i < overlayList.size(); i++) {
                                ManagerItem currentOverlay = overlayList.get(i);
                                if (currentOverlay.isSelected()) {
                                    currentOverlay.setSelected(false);
                                }
                            }
                            mAdapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        Log.e(this.getClass().getSimpleName(),
                                "Window has lost connection with the host.");
                    }
                });

        TextView disable_selected = (TextView) root.findViewById(R.id.disable_selected);
        if (!References.checkOMS(context))
            disable_selected.setText(getString(R.string.fab_menu_uninstall));
        if (disable_selected != null) {
            disable_selected.setOnClickListener(v ->
                    new RunDisable(ManagerFragment.this).execute());
        }

        TextView enable_selected = (TextView) root.findViewById(R.id.enable_selected);
        if (enable_selected != null)
            enable_selected.setOnClickListener(v ->
                    new RunEnable(ManagerFragment.this).execute());

        TextView uninstall_selected = (TextView) root.findViewById(R.id.uninstall);
        if (!References.checkOMS(context))
            uninstall_selected.setVisibility(View.GONE);
        if (uninstall_selected != null)
            uninstall_selected.setOnClickListener(v ->
                    new RunUninstall(ManagerFragment.this).execute());

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (References.checkThemeInterfacer(context)) {
            try {
                context.unregisterReceiver(finishReceiver);
            } catch (IllegalArgumentException e) {
                // Unregistered already
            }
        }
    }

    private List<String> updateEnabledOverlays() {
        List<String> state5 = ThemeManager.listOverlays(STATE_APPROVED_ENABLED);
        ArrayList<String> all = new ArrayList<>(state5);

        ArrayList<String> all_installed_overlays = new ArrayList<>();

        // Filter out icon pack overlays from all overlays
        for (int i = 0; i < all.size(); i++) {
            if (!all.get(i).endsWith(".icon")) {
                all_installed_overlays.add(all.get(i));
            }
        }
        return new ArrayList<>(all_installed_overlays);
    }

    private static class LayoutReloader extends AsyncTask<Void, Void, Void> {
        private WeakReference<ManagerFragment> ref;

        private LayoutReloader(ManagerFragment fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            ManagerFragment fragment = ref.get();
            fragment.progressBar.setVisibility(View.VISIBLE);
            fragment.mRecyclerView.setHasFixedSize(true);
            fragment.mRecyclerView.setLayoutManager(new LinearLayoutManager(fragment.context));
            ArrayList<ManagerItem> empty_array = new ArrayList<>();
            RecyclerView.Adapter empty_adapter = new ManagerAdapter(empty_array);
            fragment.mRecyclerView.setAdapter(empty_adapter);
        }

        @Override
        protected Void doInBackground(Void... params) {
            ManagerFragment fragment = ref.get();
            Context context = fragment.context;

            fragment.overlaysList = new ArrayList<>();
            fragment.activated_overlays = new ArrayList<>();
            ArrayList<String> disabled_overlays;
            ArrayList<String> all_overlays;

            if (References.checkOMS(fragment.context)) {
                ArrayList<String> active = new ArrayList<>(
                        ThemeManager.listOverlays(STATE_APPROVED_ENABLED));
                ArrayList<String> disabled = new ArrayList<>(
                        ThemeManager.listOverlays(STATE_APPROVED_DISABLED));

                // ValidatorFilter out icon pack overlays from the advanced manager
                fragment.activated_overlays = new ArrayList<>();
                for (int i = 0; i < active.size(); i++) {
                    if (!active.get(i).endsWith(".icon")) {
                        fragment.activated_overlays.add(active.get(i));
                    }
                }

                // ValidatorFilter out icon pack overlays from the advanced manager
                disabled_overlays = new ArrayList<>();
                for (int i = 0; i < disabled.size(); i++) {
                    if (!disabled.get(i).endsWith(".icon")) {
                        disabled_overlays.add(disabled.get(i));
                    }
                }

                if (fragment.prefs.getBoolean("manager_disabled_overlays", true)) {
                    all_overlays = new ArrayList<>(fragment.activated_overlays);
                    all_overlays.addAll(disabled_overlays);
                    Collections.sort(all_overlays);

                    // Create the map for {package name: package identifier}
                    HashMap<String, String> unsortedMap = new HashMap<>();

                    // Then let's convert all the package names to their app names
                    for (int i = 0; i < all_overlays.size(); i++) {
                        try {
                            ApplicationInfo applicationInfo = context.getPackageManager()
                                    .getApplicationInfo(all_overlays.get(i), 0);
                            String packageTitle = context.getPackageManager()
                                    .getApplicationLabel(applicationInfo).toString();
                            unsortedMap.put(
                                    all_overlays.get(i),
                                    References.grabPackageName(context,
                                            References.grabOverlayTarget(context, packageTitle)));
                        } catch (Exception e) {
                            // Suppress warning
                        }
                    }

                    // Sort the values list
                    List<Pair<String, String>> sortedMap = sortMapByValues(unsortedMap);

                    for (Pair<String, String> entry : sortedMap) {
                        if (disabled_overlays.contains(entry.first)) {
                            ManagerItem st = new ManagerItem(context, entry.first, false);
                            fragment.overlaysList.add(st);
                        } else if (fragment.activated_overlays.contains(entry.first)) {
                            ManagerItem st = new ManagerItem(context, entry.first, true);
                            fragment.overlaysList.add(st);
                        }
                    }
                } else {
                    all_overlays = new ArrayList<>(fragment.activated_overlays);
                    Collections.sort(all_overlays);

                    try {
                        // Create the map for {package name: package identifier}
                        HashMap<String, String> unsortedMap = new HashMap<>();

                        // Then let's convert all the package names to their app names
                        for (int i = 0; i < all_overlays.size(); i++) {
                            try {
                                ApplicationInfo applicationInfo = context.getPackageManager()
                                        .getApplicationInfo(all_overlays.get(i), 0);
                                String packageTitle = context.getPackageManager()
                                        .getApplicationLabel(applicationInfo).toString();
                                unsortedMap.put(
                                        all_overlays.get(i),
                                        References.grabPackageName(context,
                                                References.grabOverlayTarget(context,
                                                        packageTitle)));
                            } catch (Exception e) {
                                // Suppress warning
                            }
                        }

                        // Sort the values list
                        List<Pair<String, String>> sortedMap = sortMapByValues(unsortedMap);

                        sortedMap.stream().filter(entry ->
                                fragment.activated_overlays.contains(entry.first))
                                .forEach(entry -> {
                                    ManagerItem st = new ManagerItem(context, entry.first, true);
                                    fragment.overlaysList.add(st);
                                });
                    } catch (Exception e) {
                        Toast toast = Toast.makeText(context,
                                fragment.getString(R.string.advanced_manager_overlay_read_error),
                                Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
            } else {
                // At this point, the object is an RRO formatted check
                File currentDir = new File(LEGACY_NEXUS_DIR);
                if (currentDir.exists() && currentDir.isDirectory()) {
                    String[] listed = currentDir.list();
                    for (String aListed : listed) {
                        if (aListed.substring(aListed.length() - 4).equals(".apk")) {
                            fragment.activated_overlays.add(
                                    aListed.substring(0, aListed.length() - 4));
                        }
                    }
                    Collections.sort(fragment.activated_overlays);
                    for (int i = 0; i < fragment.activated_overlays.size(); i++) {
                        ManagerItem st = new ManagerItem(context,
                                fragment.activated_overlays.get(i), true);
                        fragment.overlaysList.add(st);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ManagerFragment fragment = ref.get();
            fragment.progressBar.setVisibility(View.GONE);
            fragment.mRecyclerView.setHasFixedSize(true);
            fragment.mRecyclerView.setLayoutManager(new LinearLayoutManager(fragment.context));
            fragment.mAdapter = new ManagerAdapter(fragment.overlaysList);
            fragment.mRecyclerView.setAdapter(fragment.mAdapter);

            fragment.overlayList = ((ManagerAdapter) fragment.mAdapter).getOverlayManagerList();

            if (fragment.overlaysList.size() == 0) {
                fragment.floatingActionButton.hide();
                fragment.relativeLayout.setVisibility(View.VISIBLE);
                fragment.mRecyclerView.setVisibility(View.GONE);
            } else {
                fragment.floatingActionButton.show();
                fragment.relativeLayout.setVisibility(View.GONE);
                fragment.mRecyclerView.setVisibility(View.VISIBLE);
            }
            if (!fragment.prefs.getBoolean("manager_disabled_overlays", true) ||
                    !References.checkOMS(fragment.context)) {
                LinearLayout enable_view = (LinearLayout) fragment.root.findViewById(R.id.enable);
                enable_view.setVisibility(View.GONE);
            }
            if (fragment.swipeRefreshing) {
                fragment.swipeRefreshing = false;
                fragment.swipeRefreshLayout.setRefreshing(false);
            }
            if (fragment.first_run == null) fragment.first_run = false;
            super.onPostExecute(result);
        }
    }

    private static class RunDisable extends AsyncTask<Void, Void, Void> {
        private WeakReference<ManagerFragment> ref;

        private RunDisable(ManagerFragment fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            ManagerFragment fragment = ref.get();
            fragment.materialSheetFab.hideSheet();
            fragment.loadingBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            ManagerFragment fragment = ref.get();
            Context context = fragment.context;

            if (References.checkOMS(context)) {
                ArrayList<String> data = new ArrayList<>();
                fragment.overlayList = ((ManagerAdapter) fragment.mAdapter)
                        .getOverlayManagerList();
                int len = fragment.overlayList.size();
                for (int i = 0; i < len; i++) {
                    ManagerItem managerItem = fragment.overlayList.get(i);
                    if (managerItem.isSelected()) data.add(managerItem.getName());
                }

                // The magic goes here
                if (checkThemeInterfacer(context)) {
                    ThemeManager.disableOverlay(context, data);
                } else {
                    String final_commands = ThemeManager.disableOverlay;
                    for (int i = 0; i < data.size(); i++) {
                        final_commands += " " + data.get(i);
                    }
                    if (!checkThemeInterfacer(context) &&
                            isPackageInstalled(context, MASQUERADE_PACKAGE)) {
                        Log.d(SUBSTRATUM_LOG, "Using Masquerade as the fallback system...");
                        Intent runCommand = MasqueradeService.getMasquerade(context);
                        runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        runCommand.setAction("masquerade.substratum.COMMANDS");
                        runCommand.putExtra("om-commands", final_commands);
                        context.sendBroadcast(runCommand);
                    }
                }

                if (!References.checkThemeInterfacer(context) &&
                        References.needsRecreate(context, data)) {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        // OMS may not have written all the changes so quickly just yet
                        // so we may need to have a small delay
                        try {
                            List<String> updated = fragment.updateEnabledOverlays();
                            for (int i = 0; i < fragment.overlayList.size(); i++) {
                                ManagerItem currentOverlay = fragment.overlayList.get(i);
                                currentOverlay.setSelected(false);
                                currentOverlay.updateEnabledOverlays(updated.contains(
                                        currentOverlay.getName()));
                                fragment.loadingBar.setVisibility(View.GONE);
                                fragment.mAdapter.notifyDataSetChanged();
                            }
                        } catch (Exception e) {
                            // Consume window refresh
                        }
                    }, REFRESH_WINDOW_DELAY);
                }
            } else {
                for (int i = 0; i < fragment.overlaysList.size(); i++) {
                    if (fragment.overlaysList.get(i).isSelected()) {
                        Log.e(this.getClass().getSimpleName(),
                                fragment.overlaysList.get(i).getName());
                        FileOperations.mountRW();
                        if (References.inNexusFilter()) {
                            FileOperations.mountRWVendor();
                            FileOperations.delete(context, PIXEL_NEXUS_DIR +
                                    fragment.overlaysList.get(i).getName() + ".apk");
                            FileOperations.delete(context, VENDOR_DIR +
                                    fragment.overlaysList.get(i).getName() + ".apk");
                            FileOperations.mountROVendor();
                        } else {
                            FileOperations.delete(context, LEGACY_NEXUS_DIR +
                                    fragment.overlaysList.get(i).getName() + ".apk");
                        }
                        FileOperations.mountRO();
                    }
                }

                // Since we had to parse the directory to process the recyclerView,
                // reparse it to notifyDataSetChanged

                fragment.activated_overlays.clear();
                fragment.overlaysList.clear();

                File currentDir = new File(LEGACY_NEXUS_DIR);
                String[] listed = currentDir.list();
                for (String file : listed) {
                    if (file.substring(file.length() - 4).equals(".apk")) {
                        fragment.activated_overlays.add(file.substring(0, file.length() - 4));
                    }
                }

                // Automatically sort the activated overlays by alphabetical order
                Collections.sort(fragment.activated_overlays);

                for (int i = 0; i < fragment.activated_overlays.size(); i++) {
                    ManagerItem st = new ManagerItem(context,
                            fragment.activated_overlays.get(i), true);
                    fragment.overlaysList.add(st);
                }

                fragment.mAdapter.notifyDataSetChanged();

                Toast toast2 = Toast.makeText(context, fragment.getString(R
                                .string.toast_disabled6),
                        Toast.LENGTH_SHORT);
                toast2.show();
                AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(context);
                alertDialogBuilder
                        .setTitle(fragment.getString(R.string.legacy_dialog_soft_reboot_title));
                alertDialogBuilder
                        .setMessage(fragment.getString(R.string.legacy_dialog_soft_reboot_text));
                alertDialogBuilder
                        .setPositiveButton(android.R.string.ok,
                                (dialog, id) -> ElevatedCommands.reboot());
                alertDialogBuilder.setCancelable(false);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ManagerFragment fragment = ref.get();
            fragment.loadingBar.setVisibility(View.GONE);
        }
    }

    private static class RunEnable extends AsyncTask<String, Integer, String> {
        private WeakReference<ManagerFragment> ref;

        private RunEnable(ManagerFragment fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            ManagerFragment fragment = ref.get();
            fragment.materialSheetFab.hideSheet();
            fragment.loadingBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String result) {
            ManagerFragment fragment = ref.get();
            Context context = fragment.context;
            if (result != null && result.equals("unauthorized")) {
                Toast.makeText(context, fragment.getString(R.string.manage_system_not_permitted),
                        Toast.LENGTH_LONG).show();
            }
            fragment.loadingBar.setVisibility(View.GONE);
        }

        @Override
        protected String doInBackground(String... params) {
            ManagerFragment fragment = ref.get();
            Context context = fragment.context;

            ArrayList<String> data = new ArrayList<>();
            fragment.overlayList = ((ManagerAdapter) fragment.mAdapter).getOverlayManagerList();
            boolean has_failed = false;
            int len = fragment.overlayList.size();
            for (int i = 0; i < len; i++) {
                ManagerItem managerItem = fragment.overlayList.get(i);
                if (managerItem.isSelected()) {
                    if (References.isPackageInstalled(context,
                            References.grabOverlayParent(context, managerItem.getName()))) {
                        data.add(managerItem.getName());
                    } else {
                        has_failed = true;
                    }
                }
            }
            if (!data.isEmpty()) {
                if (has_failed) {
                    Toast.makeText(context,
                            fragment.getString(R.string.manage_system_not_permitted),
                            Toast.LENGTH_LONG).show();
                }

                // The magic goes here
                if (checkThemeInterfacer(context)) {
                    ThemeManager.enableOverlay(context, data);
                } else {
                    String final_commands = ThemeManager.enableOverlay;
                    for (int i = 0; i < data.size(); i++) {
                        final_commands += " " + data.get(i);
                    }
                    if (!checkThemeInterfacer(context) &&
                            isPackageInstalled(context, MASQUERADE_PACKAGE)) {
                        Log.d(SUBSTRATUM_LOG, "Using Masquerade as the fallback system...");
                        Intent runCommand = MasqueradeService.getMasquerade(context);
                        runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        runCommand.setAction("masquerade.substratum.COMMANDS");
                        runCommand.putExtra("om-commands", final_commands);
                        context.sendBroadcast(runCommand);
                    }
                }

                if (!References.checkThemeInterfacer(context) &&
                        References.needsRecreate(context, data)) {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        // OMS may not have written all the changes so quickly just yet
                        // so we may need to have a small delay
                        try {
                            List<String> updated = fragment.updateEnabledOverlays();
                            for (int i = 0; i < fragment.overlayList.size(); i++) {
                                ManagerItem currentOverlay = fragment.overlayList.get(i);
                                currentOverlay.setSelected(false);
                                currentOverlay.updateEnabledOverlays(updated.contains(
                                        currentOverlay.getName()));
                                fragment.loadingBar.setVisibility(View.GONE);
                                fragment.mAdapter.notifyDataSetChanged();
                            }
                        } catch (Exception e) {
                            // Consume window refresh
                        }
                    }, REFRESH_WINDOW_DELAY);
                }
            } else {
                return "unauthorized";
            }
            return null;
        }
    }

    private static class RunUninstall extends AsyncTask<Void, Void, Void> {
        private WeakReference<ManagerFragment> ref;

        private RunUninstall(ManagerFragment fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            ManagerFragment fragment = ref.get();
            Context context = fragment.context;
            fragment.materialSheetFab.hideSheet();
            fragment.loadingBar.setVisibility(View.VISIBLE);
            Toast.makeText(context, fragment.getString(R
                    .string.toast_uninstalling), Toast.LENGTH_LONG).show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            ManagerFragment fragment = ref.get();
            Context context = fragment.context;

            ArrayList<String> data = new ArrayList<>();
            fragment.overlayList = ((ManagerAdapter) fragment.mAdapter)
                    .getOverlayManagerList();
            int len = fragment.overlayList.size();
            for (int i = 0; i < len; i++) {
                ManagerItem overlay1 = fragment.overlayList.get(i);
                if (overlay1.isSelected()) data.add(overlay1.getName());
            }

            // The magic goes here
            if (checkThemeInterfacer(context)) {
                ThemeManager.uninstallOverlay(context, data);
            } else {
                ArrayList<String> final_commands = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    final_commands.add(data.get(i));
                }
                if (!checkThemeInterfacer(context) &&
                        isPackageInstalled(context, MASQUERADE_PACKAGE)) {
                    Log.d(SUBSTRATUM_LOG, "Using Masquerade as the fallback system...");
                    Intent runCommand = MasqueradeService.getMasquerade(context);
                    runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    runCommand.setAction("masquerade.substratum.COMMANDS");
                    runCommand.putExtra("pm-uninstall", final_commands);
                    context.sendBroadcast(runCommand);
                }
            }

            if (!References.checkThemeInterfacer(context) &&
                    References.needsRecreate(context, data)) {
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    // OMS may not have written all the changes so quickly just yet
                    // so we may need to have a small delay
                    try {
                        List<String> updated = fragment.updateEnabledOverlays();
                        for (int i = 0; i < fragment.overlayList.size(); i++) {
                            ManagerItem currentOverlay = fragment.overlayList.get(i);
                            currentOverlay.setSelected(false);
                            currentOverlay.updateEnabledOverlays(updated.contains(
                                    currentOverlay.getName()));
                            fragment.loadingBar.setVisibility(View.GONE);
                            fragment.mAdapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        // Consume window refresh
                    }
                }, REFRESH_WINDOW_DELAY);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ManagerFragment fragment = ref.get();
            fragment.loadingBar.setVisibility(View.GONE);
        }
    }

    private static class FinishReceiver extends BroadcastReceiver {
        private WeakReference<ManagerFragment> ref;

        private FinishReceiver(ManagerFragment fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            ManagerFragment fragment = ref.get();

            // Refresh the overlay list, do it twice since in some uninstallation case the overlay
            // list isn't getting removed because the package is still detected in system.
            for (int j = 0; j < 2; j++) {
                List<String> updated = fragment.updateEnabledOverlays();
                if (fragment.overlayList == null) break;
                int len = fragment.overlayList.size();
                for (int i = 0; i < len; i++) {
                    ManagerItem currentOverlay = fragment.overlayList.get(i);
                    currentOverlay.setSelected(false);
                    currentOverlay.updateEnabledOverlays(
                            updated.contains(currentOverlay.getName()));
                    if (!References.isPackageInstalled(context, currentOverlay.getName())) {
                        fragment.overlayList.remove(i);
                        len--;
                    }
                    fragment.mAdapter.notifyDataSetChanged();
                }
            }
            fragment.loadingBar.setVisibility(View.GONE);
        }
    }
}