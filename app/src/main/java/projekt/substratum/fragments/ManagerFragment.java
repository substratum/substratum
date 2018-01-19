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
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.adapters.fragments.manager.ManagerAdapter;
import projekt.substratum.adapters.fragments.manager.ManagerItem;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
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
    public static MaterialSheetFab materialSheetFab;
    @BindView(R.id.toggle_zone)
    RelativeLayout toggle_zone;
    @BindView(R.id.no_overlays_enabled)
    RelativeLayout no_overlays_enabled;
    @BindView(R.id.overlays_recycler_view)
    RecyclerView mRecyclerView;
    @BindView(R.id.header_loading_bar)
    ProgressBar loadingBar;
    @BindView(R.id.fab_sheet)
    View sheetView;
    @BindView(R.id.apply_fab)
    FloatingActionMenu floatingActionButton;
    @BindView(R.id.overlay)
    View overlay;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.select_all)
    Switch toggle_all;
    @BindView(R.id.enable_disable_selected)
    TextView enable_disable_selected;
    @BindView(R.id.enable_selected)
    TextView enable_selected;
    @BindView(R.id.disable_selected)
    TextView disable_selected;
    @BindView(R.id.uninstall)
    TextView uninstall_selected;
    @BindView(R.id.no_themes_title)
    TextView titleView;
    @BindView(R.id.no_themes_description)
    TextView textView;
    private ArrayList<String> activated_overlays;
    private ManagerAdapter mAdapter;
    private SharedPreferences prefs;
    private List<ManagerItem> overlaysList;
    private Boolean first_run;
    private List<ManagerItem> overlayList;
    private FinishReceiver finishReceiver;
    private Context context;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver refreshReceiver;
    private SearchView searchView;
    private String userInput = "";
    private Boolean first_boot = true;
    private LayoutReloader layoutReloader;

    /**
     * Returns the MaterialSheetFab object within this fragment
     *
     * @return Returns the MaterialSheetFab object
     */
    public MaterialSheetFab getFab() {
        return materialSheetFab;
    }

    /**
     * Reset the RecyclerView and Adapter
     */
    private void resetRecyclerView() {
        // Initialize the recycler view with an empty adapter first
        ArrayList<ManagerItem> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new ManagerAdapter(empty_array, false);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.setAdapter(empty_adapter);
    }

    /**
     * Callable function to simulate the swipe refresh layout to be refreshing the whole list
     */
    public void setSwipeRefreshLayoutRefreshing() {
        if (searchView.isIconified()) {
            if ((first_run != null) && mRecyclerView.isShown() && !first_run) {
                if (layoutReloader != null && !layoutReloader.isCancelled()) {
                    layoutReloader.cancel(true);
                    layoutReloader = new LayoutReloader(ManagerFragment.this, userInput);
                    layoutReloader.execute();
                }
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        } else {
            if (layoutReloader != null && !layoutReloader.isCancelled()) {
                layoutReloader.cancel(true);
                layoutReloader = new LayoutReloader(ManagerFragment.this, userInput);
                layoutReloader.execute();
            }
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        context = Substratum.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        View view = inflater.inflate(R.layout.manager_fragment, container, false);
        ButterKnife.bind(this, view);

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new RefreshReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(refreshReceiver,
                new IntentFilter(MANAGER_REFRESH));

        loadingBar.setVisibility(View.GONE);

        // Don't even display the "enable_disable_selected" button to non-oms users.
        if (!checkOMS(context))
            sheetView.findViewById(R.id.enable_disable_selected).setVisibility(View.GONE);
        int sheetColor = context.getColor(R.color.fab_menu_background_card);
        int fabColor = context.getColor(R.color.fab_background_color);

        // Create material sheet FAB
        materialSheetFab = new MaterialSheetFab<>(
                floatingActionButton,
                sheetView,
                overlay,
                sheetColor,
                fabColor);

        // Swipe Refresh
        swipeRefreshLayout.setOnRefreshListener(this::setSwipeRefreshLayoutRefreshing);

        // Adjust toggle all switch
        toggle_all.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    try {
                        overlayList = mAdapter.getOverlayManagerList();
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
                        Log.e(getClass().getSimpleName(),
                                "Window has lost connection with the host.");
                    }
                });

        resetRecyclerView();
        if (layoutReloader != null && !layoutReloader.isCancelled()) {
            layoutReloader.cancel(true);
            layoutReloader = new LayoutReloader(ManagerFragment.this, userInput);
            layoutReloader.execute();
        } else {
            layoutReloader = new LayoutReloader(ManagerFragment.this, userInput);
            layoutReloader.execute();
        }

        if (Systems.checkThemeInterfacer(context)) {
            finishReceiver = new FinishReceiver(ManagerFragment.this);
            IntentFilter intentFilter = new IntentFilter(References.STATUS_CHANGED);
            context.registerReceiver(finishReceiver, intentFilter);
        }

        toggle_zone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    overlayList = mAdapter
                            .getOverlayManagerList();
                    if (toggle_all.isChecked()) {
                        for (int i = 0; i < overlayList.size(); i++) {
                            ManagerItem currentOverlay = overlayList
                                    .get(i);
                            if (!currentOverlay.isSelected()) {
                                currentOverlay.setSelected(true);
                            }
                            mAdapter.notifyDataSetChanged();
                        }
                    } else {
                        for (int i = 0; i < overlayList.size(); i++) {
                            ManagerItem currentOverlay = overlayList
                                    .get(i);
                            if (currentOverlay.isSelected()) {
                                currentOverlay.setSelected(false);
                            }
                        }
                        mAdapter.notifyDataSetChanged();
                    }
                    toggle_all.setChecked(!toggle_all
                            .isChecked());
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(),
                            "Window has lost connection with the host.");
                }
            }
        });

        enable_disable_selected.setOnClickListener(v ->
                new RunEnableDisable(ManagerFragment.this).execute());
        enable_selected.setOnClickListener(v ->
                new RunEnable(ManagerFragment.this).execute());
        if (!Systems.checkOMS(context)) {
            if (!Systems.isSamsungDevice(context)) {
                disable_selected.setText(getString(R.string.fab_menu_uninstall));
            } else {
                disable_selected.setVisibility(View.GONE);
            }
        }
        disable_selected.setOnClickListener(v ->
                new RunDisable(ManagerFragment.this).execute());
        if (!Systems.checkOMS(context) && !Systems.isSamsungDevice(context))
            uninstall_selected.setVisibility(View.GONE);
        uninstall_selected.setOnClickListener(v ->
                new RunUninstall(ManagerFragment.this).execute());

        if (!Systems.isSamsungDevice(context)
                && !Systems.checkOMS(context)
                && !prefs.getBoolean("seen_legacy_warning", false))
            if (getActivity() != null) {
                new AlertDialog.Builder(getActivity(), R.style.Theme_AppCompat_Dialog_Alert)
                        .setNeutralButton(R.string.dialog_ok, (dialogInterface, i) -> {
                            prefs.edit().putBoolean("seen_legacy_warning", true).apply();
                            dialogInterface.dismiss();
                        })
                        .setTitle(R.string.warning_title)
                        .setCancelable(false)
                        .setMessage(R.string.legacy_overlay_uninstall_warning_text)
                        .show();
            }
        return view;
    }

    /**
     * Creating the options menu (3dot overflow menu)
     *
     * @param menu     Menu object
     * @param inflater The inflated menu object
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.overlays_list_menu, menu);
        menu.findItem(R.id.action_search).setVisible(true);
        assert getActivity() != null;
        searchView = ((MainActivity) getActivity()).searchView;
        if (searchView != null) searchView.setOnQueryTextListener(this);
        updateMenuButtonState(menu.findItem(R.id.alphabetize));
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Update the options menu icons
     *
     * @param menuItem Object of menu item
     */
    private void updateMenuButtonState(MenuItem menuItem) {
        boolean alphabetize = prefs.getBoolean("alphabetize_overlays", true);
        if (alphabetize) {
            menuItem.setIcon(R.drawable.toolbar_alphabetize);
        } else {
            menuItem.setIcon(R.drawable.toolbar_randomize);
        }
    }

    /**
     * Assign actions to every option when they are selected
     *
     * @param item Object of menu item
     * @return True, if something has changed.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (materialSheetFab.isSheetVisible()) {
            materialSheetFab.hideSheet();
        } else {
            if (item.getItemId() == R.id.alphabetize) {
                boolean alphabetize = prefs.getBoolean("alphabetize_overlays", true);
                if (alphabetize) {
                    prefs.edit().putBoolean("alphabetize_overlays", false).apply();
                } else {
                    prefs.edit().putBoolean("alphabetize_overlays", true).apply();
                }
                updateMenuButtonState(item);
                if (!alphabetize) refreshThemeName();
                if (layoutReloader != null && !layoutReloader.isCancelled()) {
                    layoutReloader.cancel(true);
                    layoutReloader = new LayoutReloader(ManagerFragment.this, userInput);
                    layoutReloader.execute();
                } else {
                    layoutReloader = new LayoutReloader(ManagerFragment.this, userInput);
                    layoutReloader.execute();
                }
                assert getActivity() != null;
                getActivity().invalidateOptionsMenu();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sort the overlay list by theme name
     */
    private void refreshThemeName() {
        if ((overlayList != null) && !overlayList.isEmpty()) {
            for (int i = 0; i < overlayList.size(); i++) {
                Context context = overlayList.get(i).getContext();
                String packageName = overlayList.get(i).getName();
                if (overlayList.get(i).getThemeName() == null) {
                    String metadata = getOverlayMetadata(
                            context, packageName, References.metadataOverlayParent);
                    if ((metadata != null) && !metadata.isEmpty()) {
                        String pName = "<b>" +
                                context.getString(R.string.manager_theme_name) +
                                "</b> " +
                                getPackageName(context, metadata);
                        overlayList.get(i).setThemeName(pName);
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Systems.checkThemeInterfacer(context)) {
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

    /**
     * Obtain the list of enabled overlays
     *
     * @return Returns the list of enabled overlays
     */
    private List<String> updateEnabledOverlays() {
        return new ArrayList<>(ThemeManager.listOverlays(context, STATE_ENABLED));
    }

    /**
     * When the search bar text was changed, and then the user presses enter
     *
     * @param query User's input
     * @return True, if the text was changed
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    /**
     * When the search bar text was changed, reload the fragment
     *
     * @param newText User's input
     * @return True, if the text was changed
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        if (!userInput.equals(newText)) {
            userInput = newText;
            if (layoutReloader != null && !layoutReloader.isCancelled()) {
                layoutReloader.cancel(true);
                layoutReloader = new LayoutReloader(ManagerFragment.this, userInput);
                layoutReloader.execute();
            }
        }
        return true;
    }

    /**
     * The beef of reloading the whole manager's list
     */
    private static class LayoutReloader extends AsyncTask<Void, Void, Void> {
        private WeakReference<ManagerFragment> ref;
        private WeakReference<String> userInput;
        private int currentPosition;

        private LayoutReloader(ManagerFragment fragment, String input) {
            super();
            ref = new WeakReference<>(fragment);
            userInput = new WeakReference<>(input);
        }

        @Override
        protected void onPreExecute() {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                currentPosition = ((LinearLayoutManager) fragment.mRecyclerView
                        .getLayoutManager())
                        .findFirstCompletelyVisibleItemPosition();
                fragment.swipeRefreshLayout.setRefreshing(true);
                fragment.toggle_all.setChecked(false);
                fragment.toggle_all.setEnabled(false);
                fragment.mRecyclerView.setEnabled(false);
                final String userInputString = userInput.get();
                if (userInputString != null && !userInputString.isEmpty()) {
                    fragment.resetRecyclerView();
                }
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

                    if (Systems.checkOMS(fragment.context)) {
                        fragment.activated_overlays = new ArrayList<>(
                                ThemeManager.listOverlays(fragment.context, STATE_ENABLED));
                        List<String> disabled_overlays = new ArrayList<>(
                                ThemeManager.listOverlays(fragment.context, STATE_DISABLED));
                        List<String> all_overlays = new ArrayList<>(
                                fragment.activated_overlays);
                        all_overlays.addAll(disabled_overlays);
                        Collections.sort(all_overlays);

                        // Create the map for {package name: package identifier}
                        Map<String, String> unsortedMap = new HashMap<>();

                        // Then let's convert all the package names to their app names
                        for (int i = 0; i < all_overlays.size(); i++) {
                            boolean can_continue = true;
                            final String userInputString = userInput.get();
                            if (userInputString != null && !userInputString.isEmpty()) {
                                StringBuilder combined = new StringBuilder();
                                String metadata = Packages.getOverlayMetadata(
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
                                        userInputString.toLowerCase())) {
                                    can_continue = false;
                                }
                            }
                            if (can_continue) {
                                try {
                                    ApplicationInfo applicationInfo = context
                                            .getPackageManager()
                                            .getApplicationInfo(all_overlays.get(i), 0);
                                    String packageTitle = context.getPackageManager()
                                            .getApplicationLabel(applicationInfo).toString();
                                    String targetApplication = Packages.getOverlayTarget(
                                            context,
                                            packageTitle);

                                    if (isPackageInstalled(context, targetApplication)) {
                                        unsortedMap.put(
                                                all_overlays.get(i),
                                                getPackageName(context, targetApplication));
                                    }
                                } catch (Exception e) {
                                    // Suppress warning
                                }
                            }
                        }

                        if (!unsortedMap.isEmpty()) {
                            // Sort the values list
                            List<Pair<String, String>> sortedMap =
                                    sortMapByValues(unsortedMap);

                            for (Pair<String, String> entry : sortedMap) {
                                if (disabled_overlays.contains(entry.first)) {
                                    ManagerItem st = new ManagerItem(
                                            context,
                                            entry.first,
                                            false);
                                    fragment.overlaysList.add(st);
                                } else if (fragment.activated_overlays.contains(entry.first)) {
                                    ManagerItem st = new ManagerItem(
                                            context,
                                            entry.first,
                                            true);
                                    fragment.overlaysList.add(st);
                                }
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
                            StringBuilder combined = new StringBuilder();
                            combined.append(st.getLabelName());
                            combined.append(st.getThemeName());
                            if (combined.toString().toLowerCase().contains(
                                    userInput.get().toLowerCase()))
                                fragment.overlaysList.add(st);
                        }
                    }

                    try {
                        Thread.sleep((long) (fragment.first_boot ?
                                MANAGER_FRAGMENT_INITIAL_DELAY : 0));
                    } catch (InterruptedException ie) {
                        // Suppress warning
                    }
                    if (fragment.first_boot) fragment.first_boot = false;
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
                Context context = fragment.context;
                fragment.swipeRefreshLayout.setRefreshing(false);
                fragment.toggle_all.setEnabled(true);
                fragment.loadingBar.setVisibility(View.GONE);
                fragment.mAdapter = new ManagerAdapter(fragment.overlaysList, false);
                fragment.mRecyclerView.setAdapter(fragment.mAdapter);
                fragment.mRecyclerView.getLayoutManager().scrollToPosition(this.currentPosition);
                fragment.mRecyclerView.setEnabled(true);
                fragment.overlayList = fragment.mAdapter.getOverlayManagerList();

                new MainActivity.DoCleanUp(context).execute();

                boolean alphabetize = fragment.prefs.getBoolean("alphabetize_overlays", true);
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
                    fragment.toggle_zone.setVisibility(View.INVISIBLE);
                    fragment.no_overlays_enabled.setVisibility(View.VISIBLE);
                    fragment.mRecyclerView.setVisibility(View.GONE);
                    fragment.textView.setText(
                            context.getString(R.string.manager_no_overlays_text));

                    if (fragment.searchView != null) {
                        final String userInputString = userInput.get();
                        if (userInputString != null && !fragment.searchView.isIconified() &&
                                !userInputString.isEmpty()) {
                            fragment.titleView.setText(
                                    context.getString(R.string.no_overlays_title));
                            String formatter = String.format(context.getString(
                                    R.string.no_overlays_description_search), userInputString);
                            fragment.textView.setText(formatter);
                        }
                    }
                } else {
                    fragment.floatingActionButton.show();
                    fragment.toggle_zone.setVisibility(View.VISIBLE);
                    fragment.no_overlays_enabled.setVisibility(View.GONE);
                    fragment.mRecyclerView.setVisibility(View.VISIBLE);
                }
                if (!fragment.prefs.getBoolean("manager_disabled_overlays", true) ||
                        !Systems.checkOMS(fragment.context)) {
                    fragment.enable_selected.setVisibility(View.GONE);
                }
                if (fragment.first_run == null) fragment.first_run = false;
            }
        }
    }

    /**
     * OMS Enable Function
     */
    private static class RunEnable extends AsyncTask<String, Integer, String> {
        private WeakReference<ManagerFragment> ref;

        private RunEnable(ManagerFragment fragment) {
            super();
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                materialSheetFab.hideSheet();
                fragment.loadingBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.context;
                if ((result != null) && "unauthorized".equals(result)) {
                    Toast.makeText(context,
                            fragment.getString(R.string.manage_system_not_permitted),
                            Toast.LENGTH_LONG).show();
                }

                if (Systems.isAndromedaDevice(context)) {
                    new Handler().postDelayed(fragment::setSwipeRefreshLayoutRefreshing,
                            MANAGER_FRAGMENT_INITIAL_DELAY);
                } else {
                    fragment.setSwipeRefreshLayoutRefreshing();
                }
            }
        }

        @Override
        protected String doInBackground(String... params) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.context;
                fragment.overlayList = fragment.mAdapter.getOverlayManagerList();
                boolean has_failed = false;
                int len = fragment.overlayList.size();
                ArrayList<String> data = new ArrayList<>();
                for (int i = 0; i < len; i++) {
                    ManagerItem managerItem = fragment.overlayList.get(i);
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

                    if (Systems.checkSubstratumService(context) &&
                            !Systems.checkThemeInterfacer(context) &&
                            Packages.needsRecreate(context, data)) {
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
                        }, (long) REFRESH_WINDOW_DELAY);
                    }
                } else {
                    return "unauthorized";
                }
            }
            return null;
        }
    }

    /**
     * RRO Disable Function
     */
    private static class RunDisable extends AsyncTask<Void, Void, String> {
        private WeakReference<ManagerFragment> ref;

        private RunDisable(ManagerFragment fragment) {
            super();
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                materialSheetFab.hideSheet();
                fragment.loadingBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected String doInBackground(Void... voids) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.context;

                if (Systems.checkOMS(context) && !Systems.isSamsungDevice(context)) {
                    fragment.overlayList = fragment.mAdapter.getOverlayManagerList();
                    int len = fragment.overlayList.size();
                    ArrayList<String> data = new ArrayList<>();
                    for (int i = 0; i < len; i++) {
                        ManagerItem managerItem = fragment.overlayList.get(i);
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

                        if (Systems.checkSubstratumService(context) &&
                                !Systems.checkThemeInterfacer(context) &&
                                Packages.needsRecreate(context, data)) {
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(() -> {
                                // OMS may not have written all the changes so quickly just yet
                                // so we may need to have a small delay
                                try {
                                    List<String> updated = fragment.updateEnabledOverlays();
                                    for (int i = 0; i < fragment.overlayList.size(); i++) {
                                        ManagerItem currentOverlay = fragment.overlayList
                                                .get(i);
                                        currentOverlay.setSelected(false);
                                        currentOverlay.updateEnabledOverlays(updated.contains(
                                                currentOverlay.getName()));
                                        fragment.loadingBar.setVisibility(View.GONE);
                                        fragment.mAdapter.notifyDataSetChanged();
                                    }
                                } catch (Exception e) {
                                    // Consume window refresh
                                }
                            }, (long) REFRESH_WINDOW_DELAY);
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
                } else {
                    for (int i = 0; i < fragment.overlaysList.size(); i++) {
                        if (fragment.overlaysList.get(i).isSelected()) {
                            if (Systems.isSamsungDevice(context)) {
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
                                Log.d(getClass().getSimpleName(),
                                        "Removing idmap resource pointer '" +
                                                legacy_resource_idmap + '\'');

                                FileOperations.bruteforceDelete(DATA_RESOURCE_DIR +
                                        legacy_resource_idmap);
                                Log.d(getClass().getSimpleName(),
                                        "Removing idmap resource pointer '" +
                                                pixel_resource_idmap + '\'');
                                FileOperations.bruteforceDelete(DATA_RESOURCE_DIR +
                                        pixel_resource_idmap);
                                Log.d(getClass().getSimpleName(),
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

                    if (Systems.isSamsungDevice(context)) {
                        PackageManager pm = context.getPackageManager();
                        List<ApplicationInfo> packages =
                                pm.getInstalledApplications(PackageManager.GET_META_DATA);
                        for (ApplicationInfo packageInfo : packages) {
                            if (getOverlayMetadata(
                                    context,
                                    packageInfo.packageName,
                                    References.metadataOverlayParent) != null) {
                                fragment.activated_overlays.add(packageInfo.packageName);
                            }
                        }
                    } else {
                        File currentDir = new File(LEGACY_NEXUS_DIR);
                        String[] listed = currentDir.list();
                        for (String file : listed) {
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
                    ManagerItem st = new ManagerItem(context,
                            fragment.activated_overlays.get(i), true);
                    fragment.overlaysList.add(st);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.context;
                if (Systems.isAndromedaDevice(context)) {
                    new Handler().postDelayed(fragment::setSwipeRefreshLayoutRefreshing,
                            MANAGER_FRAGMENT_INITIAL_DELAY);
                } else {
                    fragment.setSwipeRefreshLayoutRefreshing();
                }

                if (!Systems.checkOMS(context) && !Systems.isSamsungDevice(context)) {
                    Toast.makeText(
                            context,
                            fragment.getString(R.string.toast_disabled6),
                            Toast.LENGTH_SHORT
                    ).show();

                    if (fragment.getActivity() != null) {
                        AlertDialog.Builder alertDialogBuilder =
                                new AlertDialog.Builder(fragment.getActivity(),
                                        R.style.Theme_AppCompat_Dialog_Alert);
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
    }

    /**
     * OMS Enable/Disable function
     */
    private static class RunEnableDisable extends AsyncTask<String, Integer, String> {
        private WeakReference<ManagerFragment> ref;

        private RunEnableDisable(ManagerFragment fragment) {
            super();
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                materialSheetFab.hideSheet();
                fragment.loadingBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.context;
                if ((result != null) && "unauthorized".equals(result)) {
                    Toast.makeText(context,
                            fragment.getString(R.string.manage_system_not_permitted),
                            Toast.LENGTH_LONG).show();
                }
                if (Systems.isAndromedaDevice(context)) {
                    new Handler().postDelayed(fragment::setSwipeRefreshLayoutRefreshing,
                            MANAGER_FRAGMENT_INITIAL_DELAY);
                } else {
                    fragment.setSwipeRefreshLayoutRefreshing();
                }
            }
        }

        @Override
        protected String doInBackground(String... params) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.context;
                fragment.overlayList = fragment.mAdapter.getOverlayManagerList();
                boolean has_failed = false;
                int len = fragment.overlayList.size();
                ArrayList<String> disabled = new ArrayList<>();
                ArrayList<String> enabled = new ArrayList<>();
                for (int i = 0; i < len; i++) {
                    ManagerItem managerItem = fragment.overlayList.get(i);
                    if (managerItem.isSelected()) {
                        if (isPackageInstalled(context,
                                getOverlayParent(context, managerItem.getName()))) {
                            if (ThemeManager.listOverlays(fragment.context, STATE_DISABLED)
                                    .contains(managerItem.getName())) {
                                enabled.add(managerItem.getName());
                            } else {
                                disabled.add(managerItem.getName());
                            }
                        } else {
                            has_failed = true;
                        }
                    }
                }
                if ((!enabled.isEmpty() || !disabled.isEmpty()) && !has_failed) {
                    // The magic goes here
                    if (!enabled.isEmpty()) ThemeManager.enableOverlay(context, enabled);
                    if (!disabled.isEmpty()) ThemeManager.disableOverlay(context, disabled);

                    if (!Systems.checkThemeInterfacer(context) &&
                            Packages.needsRecreate(context, enabled)) {
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
                        }, (long) REFRESH_WINDOW_DELAY);
                    }
                } else {
                    return "unauthorized";
                }
            }
            return null;
        }
    }

    /**
     * Uninstall function
     */
    private static class RunUninstall extends AsyncTask<Void, Void, Void> {
        private WeakReference<ManagerFragment> ref;

        private RunUninstall(ManagerFragment fragment) {
            super();
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                MainActivity.queuedUninstall = new ArrayList<>();
                materialSheetFab.hideSheet();
                fragment.loadingBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.context;

                fragment.overlayList = fragment.mAdapter.getOverlayManagerList();
                int len = fragment.overlayList.size();
                ArrayList<String> data = new ArrayList<>();
                for (int i = 0; i < len; i++) {
                    ManagerItem overlay1 = fragment.overlayList.get(i);
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
                    if (Systems.checkSubstratumService(context) &&
                            !Systems.checkThemeInterfacer(context) &&
                            Packages.needsRecreate(context, data) &&
                            !Systems.isSamsungDevice(context)) {
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
                        }, (long) REFRESH_WINDOW_DELAY);
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
                if (Systems.isAndromedaDevice(fragment.context)) {
                    new Handler().postDelayed(fragment::setSwipeRefreshLayoutRefreshing,
                            MANAGER_FRAGMENT_INITIAL_DELAY);
                } else {
                    fragment.setSwipeRefreshLayoutRefreshing();
                }
                if (Systems.isSamsungDevice(fragment.context)) {
                    MainActivity.uninstallMultipleAPKS(fragment.getActivity());
                }
            }
        }
    }

    /**
     * Concluding function to end the update process gracefully
     */
    private static class FinishReceiver extends BroadcastReceiver {
        private WeakReference<ManagerFragment> ref;

        private FinishReceiver(ManagerFragment fragment) {
            super();
            ref = new WeakReference<>(fragment);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            ManagerFragment fragment = ref.get();
            if (fragment != null) {
                if (fragment.layoutReloader != null && !fragment.layoutReloader.isCancelled()) {
                    fragment.layoutReloader.cancel(true);
                    fragment.layoutReloader = new LayoutReloader(fragment, fragment.userInput);
                    fragment.layoutReloader.execute();
                }
                fragment.loadingBar.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Receiver to reload the whole manager after a package was installed
     */
    class RefreshReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("ManagerRefresher", "A package has been modified, now refreshing the list...");
            if (layoutReloader != null && !layoutReloader.isCancelled()) {
                layoutReloader.cancel(true);
                layoutReloader = new LayoutReloader(ManagerFragment.this, userInput);
                layoutReloader.execute();
            }
        }
    }
}