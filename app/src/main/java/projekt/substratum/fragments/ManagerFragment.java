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
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Map;

import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.adapters.fragments.manager.ManagerAdapter;
import projekt.substratum.adapters.fragments.manager.ManagerItem;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.helpers.ManagerCallback;
import projekt.substratum.util.views.FloatingActionMenu;

import static projekt.substratum.common.Packages.getOverlayMetadata;
import static projekt.substratum.common.Packages.getOverlayParent;
import static projekt.substratum.common.Packages.getOverlayTarget;
import static projekt.substratum.common.Packages.getPackageName;
import static projekt.substratum.common.Packages.isPackageInstalled;
import static projekt.substratum.common.References.DATA_RESOURCE_DIR;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.MANAGER_REFRESH;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.REFRESH_WINDOW_DELAY;
import static projekt.substratum.common.References.VENDOR_DIR;
import static projekt.substratum.common.Systems.checkOMS;
import static projekt.substratum.common.platform.ThemeManager.STATE_DISABLED;
import static projekt.substratum.common.platform.ThemeManager.STATE_ENABLED;
import static projekt.substratum.common.platform.ThemeManager.isOverlayEnabled;
import static projekt.substratum.util.files.MapUtils.sortMapByValues;

public class ManagerFragment extends Fragment implements SearchView.OnQueryTextListener {

    private static final int MANAGER_FRAGMENT_INITIAL_DELAY = 500;
    private ArrayList<String> activated_overlays;
    private ManagerAdapter mAdapter;
    private MaterialSheetFab materialSheetFab;
    private SharedPreferences prefs;
    private RelativeLayout relativeLayout;
    private RelativeLayout toggle_zone;
    private ViewGroup root;
    private List<ManagerItem> overlaysList;
    private FloatingActionMenu floatingActionButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Boolean first_run;
    private RecyclerView mRecyclerView;
    private ProgressBar loadingBar;
    private List<ManagerItem> overlayList;
    private Switch toggle_all;
    private FinishReceiver finishReceiver;
    private Context context;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver refreshReceiver;
    private SearchView searchView;
    private String userInput = "";
    private Boolean first_boot = true;

    public MaterialSheetFab getFAB() {
        return materialSheetFab;
    }

    private void resetRecyclerView() {
        // Initialize the recycler view with an empty adapter first
        final ArrayList<ManagerItem> empty_array = new ArrayList<>();
        final RecyclerView.Adapter empty_adapter = new ManagerAdapter(empty_array, false);
        this.mRecyclerView.setHasFixedSize(true);
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(this.context));
        this.mRecyclerView.setAdapter(empty_adapter);
    }

    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHasOptionsMenu(true);

        // Register the theme install receiver to auto refresh the fragment
        this.refreshReceiver = new RefreshReceiver();
        this.localBroadcastManager = LocalBroadcastManager.getInstance(this.getContext());
        this.localBroadcastManager.registerReceiver(this.refreshReceiver, new IntentFilter
                (MANAGER_REFRESH));

        this.context = this.getContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        this.root = (ViewGroup) inflater.inflate(R.layout.manager_fragment, container, false);
        this.toggle_zone = this.root.findViewById(R.id.toggle_zone);
        this.relativeLayout = this.root.findViewById(R.id.no_overlays_enabled);
        this.mRecyclerView = this.root.findViewById(R.id.overlays_recycler_view);

        this.loadingBar = this.root.findViewById(R.id.header_loading_bar);
        this.loadingBar.setVisibility(View.GONE);

        final View sheetView = this.root.findViewById(R.id.fab_sheet);
        //Don't even display the "enable_disable_selected" button to non-oms users.
        if (!checkOMS(this.context))
            sheetView.findViewById(R.id.enable_disable_selected).setVisibility(View.GONE);
        final View overlay = this.root.findViewById(R.id.overlay);
        final int sheetColor = this.context.getColor(R.color.fab_menu_background_card);
        final int fabColor = this.context.getColor(R.color.fab_background_color);

        this.floatingActionButton = this.root.findViewById(R.id.apply_fab);

        // Create material sheet FAB
        if (overlay != null) {
            this.materialSheetFab = new MaterialSheetFab<>(
                    this.floatingActionButton,
                    sheetView,
                    overlay,
                    sheetColor,
                    fabColor);
        }

        this.swipeRefreshLayout = this.root.findViewById(R.id.swipeRefreshLayout);
        this.swipeRefreshLayout.setOnRefreshListener(() -> {
            if (this.searchView.isIconified()) {
                if ((this.first_run != null) && this.mRecyclerView.isShown() && !this.first_run) {
                    new LayoutReloader(ManagerFragment.this, this.userInput).execute();
                } else {
                    this.swipeRefreshLayout.setRefreshing(false);
                }
            } else {
                new LayoutReloader(ManagerFragment.this, this.userInput).execute();
            }
        });

        this.toggle_all = this.root.findViewById(R.id.select_all);
        this.toggle_all.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    try {
                        this.overlayList = this.mAdapter.getOverlayManagerList();
                        if (isChecked) {
                            for (int i = 0; i < this.overlayList.size(); i++) {
                                final ManagerItem currentOverlay = this.overlayList.get(i);
                                if (!currentOverlay.isSelected()) {
                                    currentOverlay.setSelected(true);
                                }
                                this.mAdapter.notifyDataSetChanged();
                            }
                        } else {
                            for (int i = 0; i < this.overlayList.size(); i++) {
                                final ManagerItem currentOverlay = this.overlayList.get(i);
                                if (currentOverlay.isSelected()) {
                                    currentOverlay.setSelected(false);
                                }
                            }
                            this.mAdapter.notifyDataSetChanged();
                        }
                    } catch (final Exception e) {
                        Log.e(this.getClass().getSimpleName(),
                                "Window has lost connection with the host.");
                    }
                });

        this.resetRecyclerView();
        new LayoutReloader(ManagerFragment.this, this.userInput).execute();

        if (Systems.checkThemeInterfacer(this.context)) {
            this.finishReceiver = new FinishReceiver(ManagerFragment.this);
            final IntentFilter intentFilter = new IntentFilter(References.STATUS_CHANGED);
            this.context.registerReceiver(this.finishReceiver, intentFilter);
        }

        this.toggle_zone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                try {
                    ManagerFragment.this.overlayList = ManagerFragment.this.mAdapter
                            .getOverlayManagerList();
                    if (ManagerFragment.this.toggle_all.isChecked()) {
                        for (int i = 0; i < ManagerFragment.this.overlayList.size(); i++) {
                            final ManagerItem currentOverlay = ManagerFragment.this.overlayList
                                    .get(i);
                            if (!currentOverlay.isSelected()) {
                                currentOverlay.setSelected(true);
                            }
                            ManagerFragment.this.mAdapter.notifyDataSetChanged();
                        }
                    } else {
                        for (int i = 0; i < ManagerFragment.this.overlayList.size(); i++) {
                            final ManagerItem currentOverlay = ManagerFragment.this.overlayList
                                    .get(i);
                            if (currentOverlay.isSelected()) {
                                currentOverlay.setSelected(false);
                            }
                        }
                        ManagerFragment.this.mAdapter.notifyDataSetChanged();
                    }
                    ManagerFragment.this.toggle_all.setChecked(!ManagerFragment.this.toggle_all
                            .isChecked());
                } catch (final Exception e) {
                    Log.e(this.getClass().getSimpleName(),
                            "Window has lost connection with the host.");
                }
            }
        });

        final TextView enable_disable_selected = this.root.findViewById(R.id
                .enable_disable_selected);
        if (enable_disable_selected != null) {
            enable_disable_selected.setOnClickListener(v ->
                    new RunEnableDisable(ManagerFragment.this).execute());
        }

        final TextView enable_selected = this.root.findViewById(R.id.enable_selected);
        if (enable_selected != null) {
            enable_selected.setOnClickListener(v -> new RunEnable(ManagerFragment.this).execute());
        }

        final TextView disable_selected = this.root.findViewById(R.id.disable_selected);
        if (disable_selected != null) {
            if (!Systems.checkOMS(this.context)) {
                if (!Systems.isSamsungDevice(this.context)) {
                    disable_selected.setText(this.getString(R.string.fab_menu_uninstall));
                } else {
                    disable_selected.setVisibility(View.GONE);
                }
            }
            disable_selected.setOnClickListener(v ->
                    new RunDisable(ManagerFragment.this).execute());
        }

        final TextView uninstall_selected = this.root.findViewById(R.id.uninstall);
        if (!Systems.checkOMS(this.context) && !Systems.isSamsungDevice(this.context))
            uninstall_selected.setVisibility(View.GONE);
        if (uninstall_selected != null)
            uninstall_selected.setOnClickListener(v ->
                    new RunUninstall(ManagerFragment.this).execute());

        if (!Systems.isSamsung(this.context)
                && !Systems.checkOMS(this.context)
                && !this.prefs.getBoolean("seen_legacy_warning", false))
            new AlertDialog.Builder(this.context)
                    .setNeutralButton(R.string.dialog_ok, (dialogInterface, i) -> {
                        this.prefs.edit().putBoolean("seen_legacy_warning", true).apply();
                        dialogInterface.dismiss();
                    })
                    .setTitle(R.string.warning_title)
                    .setCancelable(false)
                    .setMessage(R.string.legacy_overlay_uninstall_warning_text)
                    .show();
        return this.root;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.overlays_list_menu, menu);
        menu.findItem(R.id.search).setVisible(false);
        menu.findItem(R.id.restart_systemui).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.findItem(R.id.per_app).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.findItem(R.id.action_search).setVisible(true);
        menu.findItem(R.id.action_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        this.searchView = ((MainActivity) this.getActivity()).searchView;
        if (this.searchView != null) this.searchView.setOnQueryTextListener(this);
        this.updateMenuButtonState(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private boolean updateMenuButtonState(final Menu menu) {
        final MenuItem alphabetizeMenu = menu.findItem(R.id.alphabetize);
        final boolean alphabetize = this.prefs.getBoolean("alphabetize_overlays", true);
        if (alphabetize) {
            alphabetizeMenu.setIcon(R.drawable.actionbar_alphabetize);
        } else {
            alphabetizeMenu.setIcon(R.drawable.actionbar_randomize);
        }
        return alphabetize;
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        final Boolean alphabetize = this.updateMenuButtonState(menu);
        if (((this.overlayList != null) && !this.overlayList.isEmpty())) {
            if (!alphabetize) this.refreshThemeName();
            new LayoutReloader(ManagerFragment.this, this.userInput).execute();
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (this.materialSheetFab.isSheetVisible()) {
            this.materialSheetFab.hideSheet();
        } else {
            if (item.getItemId() == R.id.alphabetize) {
                final boolean alphabetize = this.prefs.getBoolean("alphabetize_overlays", true);
                if (alphabetize) {
                    this.prefs.edit().putBoolean("alphabetize_overlays", false).apply();
                } else {
                    this.prefs.edit().putBoolean("alphabetize_overlays", true).apply();
                }
                this.getActivity().invalidateOptionsMenu();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshThemeName() {
        if ((this.overlayList != null) && !this.overlayList.isEmpty()) {
            for (int i = 0; i < this.overlayList.size(); i++) {
                final Context context = this.overlayList.get(i).getContext();
                final String packageName = this.overlayList.get(i).getName();
                if (this.overlayList.get(i).getThemeName() == null) {
                    final String metadata = getOverlayMetadata(
                            context, packageName, References.metadataOverlayParent);
                    if ((metadata != null) && !metadata.isEmpty()) {
                        final String pName = "<b>" + context.getString(R.string
                                .manager_theme_name) +
                                "</b> " +
                                getPackageName(context, metadata);
                        this.overlayList.get(i).setThemeName(pName);
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Systems.checkThemeInterfacer(this.context)) {
            try {
                this.context.unregisterReceiver(this.finishReceiver);
            } catch (final IllegalArgumentException e) {
                // Unregistered already
            }
        }

        try {
            this.localBroadcastManager.unregisterReceiver(this.refreshReceiver);
        } catch (final IllegalArgumentException e) {
            // Unregistered already
        }
    }

    private List<String> updateEnabledOverlays() {
        return new ArrayList<>(ThemeManager.listOverlays(this.getContext(), STATE_ENABLED));
    }

    @Override
    public boolean onQueryTextSubmit(final String query) {
        if (!this.userInput.equals(query)) {
            this.userInput = query;
        }
        new LayoutReloader(ManagerFragment.this, this.userInput).execute();

        return true;
    }

    @Override
    public boolean onQueryTextChange(final String newText) {
        //if (!userInput.equals(newText)) {
        //    userInput = newText;
        //}
        //new LayoutReloader(ManagerFragment.this, userInput).execute();
        //handler.removeCallbacks(null);
        //Runnable task = () -> new LayoutReloader(ManagerFragment.this,
        //        userInput).execute();
        //handler.postDelayed(task, 600);
        return false;
    }

    private static final class LayoutReloader extends AsyncTask<Void, Void, Void> {
        private final WeakReference<ManagerFragment> ref;
        private final WeakReference<String> userInput;
        private int currentPosition;

        private LayoutReloader(final ManagerFragment fragment, final String input) {
            super();
            this.ref = new WeakReference<>(fragment);
            this.userInput = new WeakReference<>(input);
        }

        @Override
        protected void onPreExecute() {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                this.currentPosition = ((LinearLayoutManager) fragment.mRecyclerView
                        .getLayoutManager())
                        .findFirstCompletelyVisibleItemPosition();
                fragment.swipeRefreshLayout.setRefreshing(true);
                fragment.toggle_all.setChecked(false);
                fragment.toggle_all.setEnabled(false);
                fragment.mRecyclerView.setEnabled(false);
                if ((this.userInput.get() != null) && !this.userInput.get().isEmpty()) {
                    fragment.resetRecyclerView();
                }
            }
        }

        @Override
        protected Void doInBackground(final Void... params) {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                try {
                    final Context context = fragment.context;
                    fragment.overlaysList = new ArrayList<>();
                    fragment.activated_overlays = new ArrayList<>();

                    if (Systems.checkOMS(fragment.context)) {
                        fragment.activated_overlays = new ArrayList<>(
                                ThemeManager.listOverlays(fragment.context,
                                        STATE_ENABLED));

                        final List<String> disabled_overlays = new ArrayList<>(
                                ThemeManager.listOverlays(fragment.context,
                                        STATE_DISABLED));

                        final List<String> all_overlays = new ArrayList<>(fragment
                                .activated_overlays);
                        all_overlays.addAll(disabled_overlays);
                        Collections.sort(all_overlays);

                        // Create the map for {package name: package identifier}
                        final Map<String, String> unsortedMap = new HashMap<>();

                        // Then let's convert all the package names to their app names
                        for (int i = 0; i < all_overlays.size(); i++) {
                            boolean can_continue = true;
                            if ((this.userInput.get() != null) && !this.userInput.get().isEmpty()) {
                                final StringBuilder combined = new StringBuilder();
                                //TODO
                                //Do we really want to check for theme name too?
                                //THIS IS NOT THE OPTIMAL FIX! THAT'S ONLY TO GET THINGS WORKING,
                                //BUT THE PROPER FIX WILL BE TO START DEALING WITH ManagerItem FROM
                                //HERE ON.
                                final String metadata = Packages.getOverlayMetadata(
                                        context,
                                        all_overlays.get(i),
                                        References.metadataOverlayParent);
                                if ((metadata != null) && !metadata.isEmpty()) {
                                    combined.append(Packages.getPackageName(context, metadata));
                                } else {
                                    combined.append("");
                                }
                                combined.append(getPackageName(context,
                                        getOverlayTarget(context, all_overlays.get(i))));
                                if (!combined.toString().toLowerCase().contains(
                                        this.userInput.get().toLowerCase())) {
                                    can_continue = false;
                                }
                            }
                            if (can_continue) {
                                try {
                                    final ApplicationInfo applicationInfo = context
                                            .getPackageManager()
                                            .getApplicationInfo(all_overlays.get(i), 0);
                                    final String packageTitle = context.getPackageManager()
                                            .getApplicationLabel(applicationInfo).toString();
                                    final String targetApplication = Packages.getOverlayTarget(
                                            context,
                                            packageTitle);

                                    if (isPackageInstalled(context, targetApplication)) {
                                        unsortedMap.put(
                                                all_overlays.get(i),
                                                getPackageName(context, targetApplication));
                                    }
                                } catch (final Exception e) {
                                    // Suppress warning
                                }
                            }
                        }

                        if (!unsortedMap.isEmpty()) {
                            // Sort the values list
                            final List<Pair<String, String>> sortedMap = sortMapByValues
                                    (unsortedMap);

                            for (final Pair<String, String> entry : sortedMap) {
                                if (disabled_overlays.contains(entry.first)) {
                                    final ManagerItem st = new ManagerItem(context, entry.first,
                                            false);
                                    fragment.overlaysList.add(st);
                                } else if (fragment.activated_overlays.contains(entry.first)) {
                                    final ManagerItem st = new ManagerItem(context, entry.first,
                                            true);
                                    fragment.overlaysList.add(st);
                                }
                            }
                        }
                    } else {
                        // At this point, the object is an RRO formatted check
                        final List<String> listed =
                                ThemeManager.listOverlays(fragment.context, STATE_ENABLED);
                        fragment.activated_overlays.addAll(listed);
                        Collections.sort(fragment.activated_overlays);
                        for (int i = 0; i < fragment.activated_overlays.size(); i++) {
                            final ManagerItem st = new ManagerItem(context,
                                    fragment.activated_overlays.get(i), true);
                            final StringBuilder combined = new StringBuilder();
                            combined.append(st.getLabelName());
                            combined.append(st.getThemeName());
                            if (combined.toString().toLowerCase().contains(
                                    this.userInput.get().toLowerCase()))
                                fragment.overlaysList.add(st);
                        }
                    }

                    try {
                        Thread.sleep((long) (fragment.first_boot ? MANAGER_FRAGMENT_INITIAL_DELAY
                                : 0));
                    } catch (final InterruptedException ie) {
                        // Suppress warning
                    }
                    if (fragment.first_boot) fragment.first_boot = false;
                } catch (final Exception e) {
                    // Consume window refresh
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            super.onPostExecute(result);
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.context;
                fragment.swipeRefreshLayout.setRefreshing(false);
                fragment.toggle_all.setEnabled(true);
                fragment.loadingBar.setVisibility(View.GONE);
                // On the first start, when adapter is null, use the old style of refreshing RV
                if (fragment.mAdapter == null) {
                    fragment.mAdapter = new ManagerAdapter(fragment.overlaysList, false);
                    fragment.mRecyclerView.setAdapter(fragment.mAdapter);
                    fragment.mRecyclerView.setEnabled(true);
                }
                fragment.overlayList = fragment.mAdapter.getOverlayManagerList();
                // If the adapter isn't null, reload using DiffUtil
                DiffUtil.DiffResult diffResult =
                        DiffUtil.calculateDiff(
                                new ManagerCallback(
                                        fragment.mAdapter.getList(), fragment.overlaysList));
                fragment.mAdapter.setList(fragment.overlaysList);
                diffResult.dispatchUpdatesTo(fragment.mAdapter);
                // Scroll to the proper position where the user was
                ((LinearLayoutManager)
                        fragment.mRecyclerView.getLayoutManager()).
                        scrollToPositionWithOffset(this.currentPosition, 20);
                fragment.mAdapter.notifyDataSetChanged();

                new MainActivity.DoCleanUp(context).execute();

                final boolean alphabetize = fragment.prefs.getBoolean("alphabetize_overlays", true);
                if (!fragment.overlaysList.isEmpty()) {
                    if (alphabetize) {
                        fragment.overlaysList.sort(
                                Comparator.comparing(ManagerItem::getLabelName,
                                        String.CASE_INSENSITIVE_ORDER)
                                        .thenComparing(ManagerItem::getThemeName,
                                                String.CASE_INSENSITIVE_ORDER)
                        );
                    } else {
                        fragment.overlaysList.sort(
                                Comparator.comparing(ManagerItem::getThemeName,
                                        String.CASE_INSENSITIVE_ORDER)
                                        .thenComparing(ManagerItem::getLabelName,
                                                String.CASE_INSENSITIVE_ORDER));
                    }
                }

                if (fragment.overlaysList.isEmpty()) {
                    fragment.floatingActionButton.hide();
                    fragment.toggle_zone.setVisibility(View.GONE);
                    fragment.relativeLayout.setVisibility(View.VISIBLE);
                    fragment.mRecyclerView.setVisibility(View.GONE);

                    final TextView titleView = fragment.root.findViewById(R.id.no_themes_title);
                    titleView.setText(fragment.getString(R.string.manager_no_overlays_title));
                    final TextView textView = fragment.root.findViewById(R.id
                            .no_themes_description);
                    textView.setText(fragment.getString(R.string.manager_no_overlays_text));

                    if ((this.userInput.get() != null) && !fragment.searchView.isIconified() &&
                            !this.userInput.get().isEmpty()) {
                        titleView.setText(fragment.getString(R.string.no_overlays_title));
                        final String formatter = String.format(fragment.getString(
                                R.string.no_overlays_description_search), this.userInput.get());
                        textView.setText(formatter);
                    }
                } else {
                    fragment.floatingActionButton.show();
                    fragment.toggle_zone.setVisibility(View.VISIBLE);
                    fragment.relativeLayout.setVisibility(View.GONE);
                    fragment.mRecyclerView.setVisibility(View.VISIBLE);
                }
                if (!fragment.prefs.getBoolean("manager_disabled_overlays", true) ||
                        !Systems.checkOMS(fragment.context)) {
                    final TextView enable_view = fragment.root.findViewById(R.id.enable_selected);
                    enable_view.setVisibility(View.GONE);
                }
                if (fragment.first_run == null) fragment.first_run = false;
            }
        }
    }

    private static final class RunEnable extends AsyncTask<String, Integer, String> {
        // This will be the oms enable
        private final WeakReference<ManagerFragment> ref;

        private RunEnable(final ManagerFragment fragment) {
            super();
            this.ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                fragment.materialSheetFab.hideSheet();
                fragment.loadingBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.context;
                if ((result != null) && "unauthorized".equals(result)) {
                    Toast.makeText(context,
                            fragment.getString(R.string.manage_system_not_permitted),
                            Toast.LENGTH_LONG).show();
                }
                new LayoutReloader(fragment, fragment.userInput).execute();
            }
        }

        @Override
        protected String doInBackground(final String... params) {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.context;
                fragment.overlayList = fragment.mAdapter.getOverlayManagerList();
                boolean has_failed = false;
                final int len = fragment.overlayList.size();
                final ArrayList<String> data = new ArrayList<>();
                for (int i = 0; i < len; i++) {
                    final ManagerItem managerItem = fragment.overlayList.get(i);
                    if (managerItem.isSelected()) {
                        if (isPackageInstalled(context,
                                getOverlayParent(context, managerItem.getName()))) {
                            if (ThemeManager.listOverlays(fragment.context, STATE_DISABLED)
                                    .contains(managerItem.getName())) {
                                data.add(managerItem.getName());
                            }
                        } else {
                            has_failed = true;
                        }
                    }
                }
                if (!data.isEmpty() && !has_failed) {
                    // The magic goes here
                    if (!data.isEmpty()) ThemeManager.enableOverlay(context, data);

                    if (!Systems.checkThemeInterfacer(context) &&
                            Packages.needsRecreate(context, data)) {
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                final List<String> updated = fragment.updateEnabledOverlays();
                                for (int i = 0; i < fragment.overlayList.size(); i++) {
                                    final ManagerItem currentOverlay = fragment.overlayList.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(updated.contains(
                                            currentOverlay.getName()));
                                    fragment.loadingBar.setVisibility(View.GONE);
                                    fragment.mAdapter.notifyDataSetChanged();
                                }
                            } catch (final Exception e) {
                                // Consume window refresh
                            }
                        }, (long) REFRESH_WINDOW_DELAY);
                    }
                } else {
                    return "unauthorized";
                }
            }
            return null;
        }
    }

    private static final class RunDisable extends AsyncTask<Void, Void, String> {
        // This will be the rro disable
        private final WeakReference<ManagerFragment> ref;

        private RunDisable(final ManagerFragment fragment) {
            super();
            this.ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                fragment.materialSheetFab.hideSheet();
                fragment.loadingBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected String doInBackground(final Void... voids) {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.context;

                if (Systems.checkOMS(context) && !Systems.isSamsung(context)) {
                    fragment.overlayList = fragment.mAdapter.getOverlayManagerList();
                    final int len = fragment.overlayList.size();
                    final ArrayList<String> data = new ArrayList<>();
                    for (int i = 0; i < len; i++) {
                        final ManagerItem managerItem = fragment.overlayList.get(i);
                        if (managerItem.isSelected() &&
                                isOverlayEnabled(context, managerItem.getName())) {
                            data.add(managerItem.getName());
                            managerItem.setSelected(false);
                        } else if (managerItem.isSelected() &&
                                !isOverlayEnabled(context, managerItem.getName())) {
                            managerItem.setSelected(false);
                        }
                    }

                    if (!data.isEmpty()) {
                        // The magic goes here
                        ThemeManager.disableOverlay(context, data);

                        if (!Systems.checkThemeInterfacer(context) &&
                                Packages.needsRecreate(context, data)) {
                            final Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(() -> {
                                // OMS may not have written all the changes so quickly just yet
                                // so we may need to have a small delay
                                try {
                                    final List<String> updated = fragment.updateEnabledOverlays();
                                    for (int i = 0; i < fragment.overlayList.size(); i++) {
                                        final ManagerItem currentOverlay = fragment.overlayList
                                                .get(i);
                                        currentOverlay.setSelected(false);
                                        currentOverlay.updateEnabledOverlays(updated.contains(
                                                currentOverlay.getName()));
                                        fragment.loadingBar.setVisibility(View.GONE);
                                        fragment.mAdapter.notifyDataSetChanged();
                                    }
                                } catch (final Exception e) {
                                    // Consume window refresh
                                }
                            }, (long) REFRESH_WINDOW_DELAY);
                        }
                    } else {
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(() ->
                                Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_could_not_uninstall),
                                        Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    for (int i = 0; i < fragment.overlaysList.size(); i++) {
                        if (fragment.overlaysList.get(i).isSelected()) {
                            if (Systems.isSamsung(context)) {
                                final ArrayList<String> overlay = new ArrayList<>();
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
                                final String legacy_resource_idmap =
                                        (LEGACY_NEXUS_DIR.substring(1, LEGACY_NEXUS_DIR
                                                .length()) +
                                                fragment.overlaysList.get(i).getName())
                                                .replace("/", "@") + ".apk@idmap";
                                final String pixel_resource_idmap =
                                        (PIXEL_NEXUS_DIR.substring(1, PIXEL_NEXUS_DIR.length
                                                ()) +
                                                fragment.overlaysList.get(i).getName())
                                                .replace("/", "@") + ".apk@idmap";
                                final String vendor_resource_idmap =
                                        (VENDOR_DIR.substring(1, VENDOR_DIR.length()) +
                                                fragment.overlaysList.get(i).getName())
                                                .replace("/", "@") + ".apk@idmap";
                                Log.d(this.getClass().getSimpleName(),
                                        "Removing idmap resource pointer '" +
                                                legacy_resource_idmap + '\'');

                                FileOperations.bruteforceDelete(DATA_RESOURCE_DIR +
                                        legacy_resource_idmap);
                                Log.d(this.getClass().getSimpleName(),
                                        "Removing idmap resource pointer '" +
                                                pixel_resource_idmap + '\'');
                                FileOperations.bruteforceDelete(DATA_RESOURCE_DIR +
                                        pixel_resource_idmap);
                                Log.d(this.getClass().getSimpleName(),
                                        "Removing idmap resource pointer '" +
                                                vendor_resource_idmap + '\'');
                                FileOperations.bruteforceDelete(DATA_RESOURCE_DIR +
                                        vendor_resource_idmap);
                                FileOperations.mountROVendor();
                                FileOperations.mountROData();
                                FileOperations.mountRO();
                            }
                        }
                    }

                    // Since we had to parse the directory to process the recyclerView,
                    // reparse it to notifyDataSetChanged

                    fragment.activated_overlays.clear();
                    fragment.overlaysList.clear();

                    if (Systems.isSamsung(context)) {
                        final PackageManager pm = context.getPackageManager();
                        final List<ApplicationInfo> packages =
                                pm.getInstalledApplications(PackageManager.GET_META_DATA);
                        for (final ApplicationInfo packageInfo : packages) {
                            if (getOverlayMetadata(
                                    context,
                                    packageInfo.packageName,
                                    References.metadataOverlayParent) != null) {
                                fragment.activated_overlays.add(packageInfo.packageName);
                            }
                        }
                    } else {
                        final File currentDir = new File(LEGACY_NEXUS_DIR);
                        final String[] listed = currentDir.list();
                        for (final String file : listed) {
                            if (".apk".equals(file.substring(file.length() - 4))) {
                                fragment.activated_overlays.add(file.substring(0,
                                        file.length() - 4));
                            }
                        }
                    }
                }

                // Automatically sort the activated overlays by alphabetical order
                Collections.sort(fragment.activated_overlays);

                for (int i = 0; i < fragment.activated_overlays.size(); i++) {
                    final ManagerItem st = new ManagerItem(context,
                            fragment.activated_overlays.get(i), true);
                    fragment.overlaysList.add(st);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(final String result) {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.context;
                new LayoutReloader(fragment, fragment.userInput).execute();

                if (!Systems.checkOMS(context) && !Systems.isSamsung(context)) {
                    Toast.makeText(
                            context,
                            fragment.getString(R.string.toast_disabled6),
                            Toast.LENGTH_SHORT).show();

                    final AlertDialog.Builder alertDialogBuilder =
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
                    final AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            }
        }
    }

    private static final class RunEnableDisable extends AsyncTask<String, Integer, String> {
        // This will be the oms enable/disable
        private final WeakReference<ManagerFragment> ref;

        private RunEnableDisable(final ManagerFragment fragment) {
            super();
            this.ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                fragment.materialSheetFab.hideSheet();
                fragment.loadingBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.context;
                if ((result != null) && "unauthorized".equals(result)) {
                    Toast.makeText(context,
                            fragment.getString(R.string.manage_system_not_permitted),
                            Toast.LENGTH_LONG).show();
                }
                new LayoutReloader(fragment, fragment.userInput).execute();
            }
        }

        @Override
        protected String doInBackground(final String... params) {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.context;
                fragment.overlayList = fragment.mAdapter.getOverlayManagerList();
                boolean has_failed = false;
                final int len = fragment.overlayList.size();
                final ArrayList<String> data2 = new ArrayList<>();    //disabled list
                final ArrayList<String> data = new ArrayList<>();     //enabled list
                for (int i = 0; i < len; i++) {
                    final ManagerItem managerItem = fragment.overlayList.get(i);
                    if (managerItem.isSelected()) {
                        if (isPackageInstalled(context,
                                getOverlayParent(context, managerItem.getName()))) {
                            if (ThemeManager.listOverlays(fragment.context, STATE_DISABLED)
                                    .contains(managerItem.getName())) {
                                data.add(managerItem.getName());
                            } else {
                                data2.add(managerItem.getName());
                            }
                        } else {
                            has_failed = true;
                        }
                    }
                }
                if ((!data.isEmpty() || !data2.isEmpty()) && !has_failed) {
                    // The magic goes here
                    if (!data.isEmpty()) ThemeManager.enableOverlay(context, data);
                    if (!data2.isEmpty()) ThemeManager.disableOverlay(context, data2);

                    if (!Systems.checkThemeInterfacer(context) &&
                            Packages.needsRecreate(context, data)) {
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                final List<String> updated = fragment.updateEnabledOverlays();
                                for (int i = 0; i < fragment.overlayList.size(); i++) {
                                    final ManagerItem currentOverlay = fragment.overlayList.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(updated.contains(
                                            currentOverlay.getName()));
                                    fragment.loadingBar.setVisibility(View.GONE);
                                    fragment.mAdapter.notifyDataSetChanged();
                                }
                            } catch (final Exception e) {
                                // Consume window refresh
                            }
                        }, (long) REFRESH_WINDOW_DELAY);
                    }
                } else {
                    return "unauthorized";
                }
            }
            return null;
        }
    }

    private static final class RunUninstall extends AsyncTask<Void, Void, Void> {
        private final WeakReference<ManagerFragment> ref;

        private RunUninstall(final ManagerFragment fragment) {
            super();
            this.ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                MainActivity.queuedUninstall = new ArrayList<>();
                fragment.materialSheetFab.hideSheet();
                fragment.loadingBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Void doInBackground(final Void... params) {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.context;

                fragment.overlayList = fragment.mAdapter.getOverlayManagerList();
                final int len = fragment.overlayList.size();
                final ArrayList<String> data = new ArrayList<>();
                for (int i = 0; i < len; i++) {
                    final ManagerItem overlay1 = fragment.overlayList.get(i);
                    if (overlay1.isSelected()) {
                        data.add(overlay1.getName());
                        if (Systems.isSamsungDevice(context))
                            MainActivity.queuedUninstall.add(overlay1.getName());
                    }
                }

                // The magic goes here
                if (!data.isEmpty()) {
                    if (!Systems.isSamsungDevice(context)) {
                        ThemeManager.uninstallOverlay(context, data);
                    }
                    if (!Systems.checkThemeInterfacer(context) &&
                            Packages.needsRecreate(context, data) &&
                            !Systems.isSamsungDevice(context)) {
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                final List<String> updated = fragment.updateEnabledOverlays();
                                for (int i = 0; i < fragment.overlayList.size(); i++) {
                                    final ManagerItem currentOverlay = fragment.overlayList.get(i);
                                    currentOverlay.setSelected(false);
                                    currentOverlay.updateEnabledOverlays(updated.contains(
                                            currentOverlay.getName()));
                                    fragment.loadingBar.setVisibility(View.GONE);
                                    fragment.mAdapter.notifyDataSetChanged();
                                }
                            } catch (final Exception e) {
                                // Consume window refresh
                            }
                        }, (long) REFRESH_WINDOW_DELAY);
                    }
                } else {
                    final Handler handler = new Handler(Looper.getMainLooper());
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
        protected void onPostExecute(final Void result) {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                new LayoutReloader(fragment, fragment.userInput).execute();
                if (Systems.isSamsungDevice(fragment.context)) {
                    MainActivity.uninstallMultipleAPKS(fragment.getActivity());
                }
            }
        }
    }

    private static final class FinishReceiver extends BroadcastReceiver {
        private final WeakReference<ManagerFragment> ref;

        private FinishReceiver(final ManagerFragment fragment) {
            super();
            this.ref = new WeakReference<>(fragment);
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final ManagerFragment fragment = this.ref.get();
            if (fragment != null) {
                new LayoutReloader(fragment, fragment.userInput);
                fragment.loadingBar.setVisibility(View.GONE);
            }
        }
    }

    class RefreshReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.d("ManagerRefresher", "A package has been modified, now refreshing the list...");
            new LayoutReloader(ManagerFragment.this, ManagerFragment.this.userInput).execute();
        }
    }
}
