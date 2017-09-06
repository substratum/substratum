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
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.R;
import projekt.substratum.adapters.fragments.manager.ManagerAdapter;
import projekt.substratum.adapters.fragments.manager.ManagerItem;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.views.FloatingActionMenu;

import static projekt.substratum.common.References.DATA_RESOURCE_DIR;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.MANAGER_REFRESH;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.REFRESH_WINDOW_DELAY;
import static projekt.substratum.common.References.VENDOR_DIR;
import static projekt.substratum.common.References.checkOMS;
import static projekt.substratum.common.References.isSamsung;
import static projekt.substratum.common.platform.ThemeManager.STATE_DISABLED;
import static projekt.substratum.common.platform.ThemeManager.STATE_ENABLED;
import static projekt.substratum.util.files.MapUtils.sortMapByValues;

public class ManagerFragment extends Fragment {

    private static final int MANAGER_FRAGMENT_LOAD_DELAY = 500;
    private ArrayList<String> activated_overlays;
    private RecyclerView.Adapter mAdapter;
    private MaterialSheetFab materialSheetFab;
    private SharedPreferences prefs;
    private RelativeLayout relativeLayout;
    private RelativeLayout toggle_zone;
    private ViewGroup root;
    private List<ManagerItem> overlaysList;
    private FloatingActionMenu floatingActionButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Boolean first_run = null;
    private MaterialProgressBar progressBar;
    private RecyclerView mRecyclerView;
    private ProgressBar loadingBar;
    private List<ManagerItem> overlayList;
    private Switch toggle_all;
    private FinishReceiver finishReceiver;
    private Context context;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver refreshReceiver;

    private void refreshList() {
        if (overlayList != null && mAdapter != null) {
            List<ManagerItem> updated = new ArrayList<>();
            if (References.checkOMS(getContext()) && !References.isSamsung(getContext())) {
                for (int i = 0; i < overlayList.size(); i++) {
                    if (!overlayList.get(i).getName().endsWith(".icon") &&
                            References.isPackageInstalled(
                                    getContext(),
                                    overlayList.get(i).getName())) {
                        updated.add(overlayList.get(i));
                    }
                }
            } else {
                List<String> listed =
                        ThemeManager.listOverlays(getContext(), STATE_ENABLED);
                Collections.sort(listed);
                updated.addAll(listed.stream().map(file ->
                        new ManagerItem(getContext(), file, true)).collect(Collectors.toList()));
            }
            ((ManagerAdapter) mAdapter).setOverlayManagerList(updated);
            overlayList = updated;
            if (References.checkOMS(getContext())) {
                try {
                    List<String> update = updateEnabledOverlays();
                    for (int i = 0; i < overlayList.size(); i++) {
                        ManagerItem currentOverlay = overlayList.get(i);
                        currentOverlay.setSelected(false);
                        currentOverlay.updateEnabledOverlays(
                                update.contains(currentOverlay.getName()));
                        mAdapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    // Consume window refresh
                }
            }
            int overlayListSize = overlayList.size();
            if (overlayList != null && overlayListSize == 0) {
                floatingActionButton.hide();
                toggle_zone.setVisibility(View.GONE);
                relativeLayout.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            } else if (overlayList != null) {
                floatingActionButton.show();
                toggle_zone.setVisibility(View.VISIBLE);
                relativeLayout.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }
            mAdapter.notifyDataSetChanged();
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }

            boolean alphabetize = prefs.getBoolean("alphabetize_overlays", true);
            if (alphabetize) {
                overlayList.sort(Comparator.comparing(ManagerItem::getLabelName));
            } else
                overlayList.sort(Comparator.comparing(ManagerItem::getThemeName));
        }
        toggle_all.setChecked(false);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new RefreshReceiver();
        IntentFilter if1 = new IntentFilter(MANAGER_REFRESH);
        localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
        localBroadcastManager.registerReceiver(refreshReceiver, if1);

        context = getContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("alphabetize_overlays", true).apply();
        root = (ViewGroup) inflater.inflate(R.layout.manager_fragment, container, false);
        toggle_zone = root.findViewById(R.id.toggle_zone);
        relativeLayout = root.findViewById(R.id.no_overlays_enabled);
        mRecyclerView = root.findViewById(R.id.overlays_recycler_view);

        loadingBar = root.findViewById(R.id.header_loading_bar);
        loadingBar.setVisibility(View.GONE);

        View sheetView = root.findViewById(R.id.fab_sheet);
        //Don't even display the "enable_disable_selected" button to non-oms users.
        if (!checkOMS(context))
            sheetView.findViewById(R.id.enable_disable_selected).setVisibility(View.GONE);
        View overlay = root.findViewById(R.id.overlay);
        int sheetColor = context.getColor(R.color.fab_menu_background_card);
        int fabColor = context.getColor(R.color.fab_background_color);

        progressBar = root.findViewById(R.id.progress_bar_loader);

        floatingActionButton = root.findViewById(R.id.apply_fab);

        // Create material sheet FAB
        if (sheetView != null && overlay != null) {
            materialSheetFab = new MaterialSheetFab<>(
                    floatingActionButton,
                    sheetView,
                    overlay,
                    sheetColor,
                    fabColor);
        }

        new LayoutReloader(ManagerFragment.this).execute();

        if (References.checkThemeInterfacer(context)) {
            finishReceiver = new FinishReceiver(ManagerFragment.this);
            IntentFilter intentFilter = new IntentFilter(References.STATUS_CHANGED);
            context.registerReceiver(finishReceiver, intentFilter);
        }

        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (first_run != null && mRecyclerView.isShown() && !first_run) {
                refreshList();
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        toggle_all = root.findViewById(R.id.select_all);
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
        toggle_zone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    overlayList = ((ManagerAdapter) mAdapter).getOverlayManagerList();
                    if (toggle_all.isChecked()) {
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
                    toggle_all.setChecked(!toggle_all.isChecked());
                } catch (Exception e) {
                    Log.e(this.getClass().getSimpleName(),
                            "Window has lost connection with the host.");
                }
            }
        });

        TextView disable_selected = root.findViewById(R.id.disable_selected);
        if (!References.checkOMS(context))
            disable_selected.setText(getString(R.string.fab_menu_uninstall));
        if (disable_selected != null) {
            disable_selected.setOnClickListener(v ->
                    new RunDisable(ManagerFragment.this).execute());
        }

        TextView enable_disable_selected = (TextView) root.findViewById(R.id
                .enable_disable_selected);
        if (enable_disable_selected != null)
            enable_disable_selected.setOnClickListener(v ->
                    new RunEnableDisable(ManagerFragment.this).execute());

        TextView uninstall_selected = root.findViewById(R.id.uninstall);
        if (!References.checkOMS(context))
            uninstall_selected.setVisibility(View.GONE);
        if (uninstall_selected != null)
            uninstall_selected.setOnClickListener(v ->
                    new RunUninstall(ManagerFragment.this).execute());

        if (!References.isSamsung(context)
                && !References.checkOMS(context)
                && !prefs.getBoolean("seen_legacy_warning", false))
            new AlertDialog.Builder(context)
                    .setNeutralButton(R.string.dialog_ok, (dialogInterface, i) -> {
                        prefs.edit().putBoolean("seen_legacy_warning", true).apply();
                        dialogInterface.dismiss();
                    })
                    .setTitle(R.string.warning_title)
                    .setCancelable(false)
                    .setMessage(R.string.legacy_overlay_uninstall_warning_text)
                    .show();
        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.overlays_list_menu, menu);
        menu.findItem(R.id.search).setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem alphabetizeMenu = menu.findItem(R.id.alphabetize);
        boolean alphabetize = prefs.getBoolean("alphabetize_overlays", false);
        if (alphabetize)
            alphabetizeMenu.setIcon(R.drawable.actionbar_alphabetize);
        else
            alphabetizeMenu.setIcon(R.drawable.actionbar_randomize);
        if ((overlayList != null && overlayList.size() > 0)) {
            if (!alphabetize)
                refreshThemeName();
            refreshList();
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.alphabetize) {
            materialSheetFab.hideSheet();
            boolean alphabetize = prefs.getBoolean("alphabetize_overlays", false);
            if (alphabetize) {
                prefs.edit().putBoolean("alphabetize_overlays", false).apply();
            } else {
                prefs.edit().putBoolean("alphabetize_overlays", true).apply();
            }
            getActivity().invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void refreshThemeName() {
        if (overlayList != null && overlayList.size() > 0) {
            for (int i = 0; i < overlayList.size(); i++) {
                Context context = overlayList.get(i).getContext();
                String packageName = overlayList.get(i).getName();
                if (overlayList.get(i).getThemeName() == null) {
                    String metadata = References.getOverlayMetadata(
                            context, packageName, References.metadataOverlayParent);
                    if (metadata != null && metadata.length() > 0) {
                        String pName = "<b>" + context.getString(R.string.manager_theme_name) +
                                "</b> " +
                                References.grabPackageName(context, metadata);
                        overlayList.get(i).setThemeName(pName);
                    }
                }
            }
        }
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

        try {
            localBroadcastManager.unregisterReceiver(refreshReceiver);
        } catch (IllegalArgumentException e) {
            // Unregistered already
        }
    }

    private List<String> updateEnabledOverlays() {
        List<String> state5 = ThemeManager.listOverlays(getContext(), STATE_ENABLED);
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
            if (fragment != null) {
                fragment.progressBar.setVisibility(View.VISIBLE);
                fragment.mRecyclerView.setHasFixedSize(true);
                fragment.mRecyclerView.setLayoutManager(new LinearLayoutManager(fragment.context));
                ArrayList<ManagerItem> empty_array = new ArrayList<>();
                RecyclerView.Adapter empty_adapter = new ManagerAdapter(empty_array, false);
                fragment.mRecyclerView.setAdapter(empty_adapter);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                try {
                    Context context = fragment.context;
                    fragment.overlaysList = new ArrayList<>();
                    fragment.activated_overlays = new ArrayList<>();
                    ArrayList<String> disabled_overlays;
                    ArrayList<String> all_overlays;

                    if (References.checkOMS(fragment.context)) {
                        fragment.activated_overlays = new ArrayList<>(
                                ThemeManager.listOverlays(fragment.context,
                                        STATE_ENABLED));

                        disabled_overlays = new ArrayList<>(
                                ThemeManager.listOverlays(fragment.context,
                                        STATE_DISABLED));

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
                                                References.grabOverlayTarget(
                                                        context,
                                                        packageTitle)));
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
                        // At this point, the object is an RRO formatted check
                        List<String> listed =
                                ThemeManager.listOverlays(fragment.context, STATE_ENABLED);
                        fragment.activated_overlays.addAll(listed);
                        Collections.sort(fragment.activated_overlays);
                        for (int i = 0; i < fragment.activated_overlays.size(); i++) {
                            ManagerItem st = new ManagerItem(context,
                                    fragment.activated_overlays.get(i), true);
                            fragment.overlaysList.add(st);
                        }
                    }
                    try {
                        Thread.sleep(MANAGER_FRAGMENT_LOAD_DELAY);
                    } catch (InterruptedException ie) {
                        // Suppress warning
                    }
                } catch (Exception e) {
                    // Consume window refresh
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                fragment.progressBar.setVisibility(View.GONE);
                fragment.mRecyclerView.setHasFixedSize(true);
                fragment.mRecyclerView.setLayoutManager(new LinearLayoutManager(fragment.context));
                fragment.mAdapter = new ManagerAdapter(fragment.overlaysList, false);
                fragment.mRecyclerView.setAdapter(fragment.mAdapter);

                fragment.overlayList = ((ManagerAdapter) fragment.mAdapter).getOverlayManagerList();

                if (fragment.overlaysList.size() == 0) {
                    fragment.floatingActionButton.hide();
                    fragment.toggle_zone.setVisibility(View.GONE);
                    fragment.relativeLayout.setVisibility(View.VISIBLE);
                    fragment.mRecyclerView.setVisibility(View.GONE);
                } else {
                    fragment.floatingActionButton.show();
                    fragment.toggle_zone.setVisibility(View.VISIBLE);
                    fragment.relativeLayout.setVisibility(View.GONE);
                    fragment.mRecyclerView.setVisibility(View.VISIBLE);
                }
                if (!fragment.prefs.getBoolean("manager_disabled_overlays", true) ||
                        !References.checkOMS(fragment.context)) {
                    LinearLayout enable_view = fragment.root.findViewById(R.id.enable);
                    enable_view.setVisibility(View.GONE);
                }
                if (fragment.first_run == null) fragment.first_run = false;
            }
        }
    }

    private static class RunDisable extends AsyncTask<Void, Void, Void> {//This will be the rro
        // disable
        private WeakReference<ManagerFragment> ref;

        private RunDisable(ManagerFragment fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                fragment.materialSheetFab.hideSheet();
                fragment.loadingBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
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
                    ThemeManager.disableOverlay(context, data);

                    if (!References.checkThemeInterfacer(context) &&
                            References.needsRecreate(context, data)) {
                        Handler handler = new Handler(Looper.getMainLooper());
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
                            if (References.isSamsung(context)) {
                                ArrayList<String> overlay = new ArrayList<>();
                                overlay.add(fragment.overlaysList.get(i).getName());
                                ThemeManager.uninstallOverlay(context, overlay);
                            } else {
                                FileOperations.mountRW();
                                FileOperations.mountRWData();
                                FileOperations.mountRWVendor();
                                FileOperations.bruteforceDelete(DATA_RESOURCE_DIR +
                                        "overlays.list");

                                FileOperations.bruteforceDelete(LEGACY_NEXUS_DIR +
                                        fragment.overlaysList.get(i).getName() + ".apk");
                                FileOperations.bruteforceDelete(PIXEL_NEXUS_DIR +
                                        fragment.overlaysList.get(i).getName() + ".apk");
                                FileOperations.bruteforceDelete(VENDOR_DIR +
                                        fragment.overlaysList.get(i).getName() + ".apk");
                                String legacy_resource_idmap =
                                        (LEGACY_NEXUS_DIR.substring(1, LEGACY_NEXUS_DIR
                                                .length()) +
                                                fragment.overlaysList.get(i).getName())
                                                .replace("/", "@") + ".apk@idmap";
                                String pixel_resource_idmap =
                                        (PIXEL_NEXUS_DIR.substring(1, PIXEL_NEXUS_DIR.length
                                                ()) +
                                                fragment.overlaysList.get(i).getName())
                                                .replace("/", "@") + ".apk@idmap";
                                String vendor_resource_idmap =
                                        (VENDOR_DIR.substring(1, VENDOR_DIR.length()) +
                                                fragment.overlaysList.get(i).getName())
                                                .replace("/", "@") + ".apk@idmap";
                                Log.d(this.getClass().getSimpleName(),
                                        "Removing idmap resource pointer '" +
                                                legacy_resource_idmap + "'");

                                FileOperations.bruteforceDelete(DATA_RESOURCE_DIR +
                                        legacy_resource_idmap);
                                Log.d(this.getClass().getSimpleName(),
                                        "Removing idmap resource pointer '" +
                                                pixel_resource_idmap + "'");
                                FileOperations.bruteforceDelete(DATA_RESOURCE_DIR +
                                        pixel_resource_idmap);
                                Log.d(this.getClass().getSimpleName(),
                                        "Removing idmap resource pointer '" +
                                                vendor_resource_idmap + "'");
                                FileOperations.bruteforceDelete(DATA_RESOURCE_DIR +
                                        vendor_resource_idmap);
                                FileOperations.mountROVendor();
                                FileOperations.mountROData();
                                FileOperations.mountRO();
                            }
                        }
                    }
                }

                // Since we had to parse the directory to process the recyclerView,
                // reparse it to notifyDataSetChanged

                fragment.activated_overlays.clear();
                fragment.overlaysList.clear();

                if (isSamsung(context)) {
                    final PackageManager pm = context.getPackageManager();
                    List<ApplicationInfo> packages =
                            pm.getInstalledApplications(PackageManager.GET_META_DATA);
                    for (ApplicationInfo packageInfo : packages) {
                        if (References.getOverlayMetadata(
                                context,
                                packageInfo.packageName,
                                References.metadataOverlayParent) != null) {
                            fragment.activated_overlays.add(packageInfo.packageName);
                        }
                    }
                } else if (!checkOMS(context)) {
                    File currentDir = new File(LEGACY_NEXUS_DIR);
                    String[] listed = currentDir.list();
                    for (String file : listed) {
                        if (file.substring(file.length() - 4).equals(".apk")) {
                            fragment.activated_overlays.add(file.substring(0, file.length() - 4));
                        }
                    }
                }

                // Automatically sort the activated overlays by alphabetical order
                Collections.sort(fragment.activated_overlays);

                for (int i = 0; i < fragment.activated_overlays.size(); i++) {
                    ManagerItem st = new ManagerItem(context,
                            fragment.activated_overlays.get(i), true);
                    fragment.overlaysList.add(st);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.context;
                fragment.mAdapter.notifyDataSetChanged();
                fragment.loadingBar.setVisibility(View.GONE);

                if (!References.checkOMS(context) && !References.isSamsung(context)) {
                    Toast.makeText(
                            context,
                            fragment.getString(R.string.toast_disabled6),
                            Toast.LENGTH_SHORT).show();

                    AlertDialog.Builder alertDialogBuilder =
                            new AlertDialog.Builder(context);
                    alertDialogBuilder
                            .setTitle(fragment.getString(R.string
                                    .legacy_dialog_soft_reboot_title));
                    alertDialogBuilder
                            .setMessage(
                                    fragment.getString(R.string
                                            .legacy_dialog_soft_reboot_text));
                    alertDialogBuilder
                            .setPositiveButton(android.R.string.ok,
                                    (dialog, id) -> ElevatedCommands.reboot());
                    alertDialogBuilder.setNegativeButton(
                            R.string.remove_dialog_later, (dialog, id1) -> dialog.dismiss());
                    alertDialogBuilder.setCancelable(false);
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            }
        }
    }

    private static class RunEnableDisable extends AsyncTask<String, Integer, String> {//This
        // will
        // be the oms enable/disable
        private WeakReference<ManagerFragment> ref;

        private RunEnableDisable(ManagerFragment fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                fragment.materialSheetFab.hideSheet();
                fragment.loadingBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.context;
                if (result != null && result.equals("unauthorized")) {
                    Toast.makeText(context,
                            fragment.getString(R.string.manage_system_not_permitted),
                            Toast.LENGTH_LONG).show();
                }
                fragment.loadingBar.setVisibility(View.GONE);
            }
        }

        @Override
        protected String doInBackground(String... params) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.context;
                ArrayList<String> data = new ArrayList<>();     //enabled list
                ArrayList<String> data2 = new ArrayList<>();    //disabled list
                fragment.overlayList = ((ManagerAdapter) fragment.mAdapter).getOverlayManagerList();
                boolean has_failed = false;
                int len = fragment.overlayList.size();
                for (int i = 0; i < len; i++) {
                    ManagerItem managerItem = fragment.overlayList.get(i);
                    if (managerItem.isSelected()) {
                        if (References.isPackageInstalled(context,
                                References.grabOverlayParent(context, managerItem.getName()))) {
                            if (ThemeManager.listOverlays(fragment.context,
                                    STATE_DISABLED).contains(managerItem.getName()))
                                data.add(managerItem.getName());
                            else
                                data2.add(managerItem.getName());
                        } else {
                            has_failed = true;
                        }
                    }
                }
                if (!data.isEmpty() || !data2.isEmpty()) {
                    if (has_failed) {       //could be deleted
                        Toast.makeText(context,
                                fragment.getString(R.string.manage_system_not_permitted),
                                Toast.LENGTH_LONG).show();
                    }

                    // The magic goes here
                    if (!data.isEmpty())
                        ThemeManager.enableOverlay(context, data);
                    if (!data2.isEmpty())
                        ThemeManager.disableOverlay(context, data2);

                    if (!References.checkThemeInterfacer(context) &&
                            References.needsRecreate(context, data)) {
                        Handler handler = new Handler(Looper.getMainLooper());
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
                    //can be deleted now, but I'll keep it here just in case.
                    return "unauthorized";
                }
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
            if (fragment != null) {
                fragment.materialSheetFab.hideSheet();
                fragment.loadingBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
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
                if (!data.isEmpty()) {
                    ThemeManager.uninstallOverlay(context, data);

                    if (!References.checkThemeInterfacer(context) &&
                            References.needsRecreate(context, data) &&
                            !References.isSamsung(context)) {
                        Handler handler = new Handler(Looper.getMainLooper());
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
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() ->
                            Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_could_not_uninstall),
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                fragment.loadingBar.setVisibility(View.GONE);
            }
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
            if (fragment != null) {
                fragment.refreshList();
                fragment.loadingBar.setVisibility(View.GONE);
            }
        }
    }

    class RefreshReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("ManagerRefresher", "A package has been modified, now refreshing the list...");
            refreshList();
        }
    }
}
