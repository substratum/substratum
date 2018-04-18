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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.graphics.Typeface;
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
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
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
import java.util.Locale;
import java.util.Map;

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
import projekt.substratum.databinding.ManagerFragmentBinding;
import projekt.substratum.util.helpers.StringUtils;
import projekt.substratum.util.views.FloatingActionMenu;

import static projekt.substratum.MainActivity.userInput;
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
import static projekt.substratum.common.Systems.checkAndromeda;
import static projekt.substratum.common.Systems.checkOMS;
import static projekt.substratum.common.platform.ThemeManager.STATE_DISABLED;
import static projekt.substratum.common.platform.ThemeManager.STATE_ENABLED;
import static projekt.substratum.common.platform.ThemeManager.isOverlayEnabled;
import static projekt.substratum.util.helpers.MapUtils.sortMapByValues;

public class ManagerFragment extends Fragment {

    // TODO: Need more delay or change how we decide when to refresh the fragment
    private static final int MANAGER_FRAGMENT_INITIAL_DELAY = 500;
    public static MaterialSheetFab materialSheetFab;
    public static LayoutReloader layoutReloader;
    private RelativeLayout toggleZone;
    private RelativeLayout noOverlaysEnabled;
    @SuppressLint("StaticFieldLeak")
    public static RecyclerView recyclerView;
    private ProgressBar loadingBar;
    private FloatingActionMenu floatingActionButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Switch toggleAll;
    private TextView enableSelected;
    private TextView titleView;
    private TextView textView;
    private ArrayList<String> activatedOverlays;
    private ManagerAdapter mAdapter;
    private SharedPreferences prefs;
    private List<ManagerItem> overlaysList;
    private Boolean firstRun;
    private List<ManagerItem> overlayList;
    private FinishReceiver finishReceiver;
    private Context context;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver refreshReceiver;
    private SearchView searchView;
    private boolean firstBoot = true;

    /**
     * Reset the RecyclerView and Adapter
     */
    private void resetRecyclerView() {
        // Initialize the recycler view with an empty adapter first
        ArrayList<ManagerItem> emptyArray = new ArrayList<>();
        RecyclerView.Adapter emptyAdapter = new ManagerAdapter(emptyArray);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(emptyAdapter);
    }

    /**
     * Callable function to simulate the swipe refresh layout to be refreshing the whole list
     */
    public void setSwipeRefreshLayoutRefreshing() {
        if (searchView.isIconified()) {
            if ((firstRun != null) && recyclerView.isShown() && !firstRun) {
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (layoutReloader != null && !layoutReloader.isCancelled()) {
            layoutReloader.cancel(true);
            layoutReloader = new LayoutReloader(ManagerFragment.this, userInput);
            layoutReloader.execute();
        } else {
            layoutReloader = new LayoutReloader(ManagerFragment.this, userInput);
            layoutReloader.execute();
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

        ManagerFragmentBinding managerFragmentBinding =
                DataBindingUtil.inflate(inflater, R.layout.manager_fragment, container, false);

        View view = managerFragmentBinding.getRoot();

        toggleZone = managerFragmentBinding.toggleZone;
        noOverlaysEnabled = managerFragmentBinding.noOverlaysEnabled;
        recyclerView = managerFragmentBinding.overlaysRecyclerView;
        loadingBar = managerFragmentBinding.headerLoadingBar;
        floatingActionButton = managerFragmentBinding.applyFab;
        View sheetView = managerFragmentBinding.fabSheet;
        View overlay = managerFragmentBinding.overlay;
        swipeRefreshLayout = managerFragmentBinding.swipeRefreshLayout;
        toggleAll = managerFragmentBinding.selectAll;
        TextView enableDisableSelected = managerFragmentBinding.enableDisableSelected;
        enableSelected = managerFragmentBinding.enableSelected;
        TextView disableSelected = managerFragmentBinding.disableSelected;
        TextView uninstallSelected = managerFragmentBinding.uninstall;
        titleView = managerFragmentBinding.noThemesTitle;
        textView = managerFragmentBinding.noThemesDescription;

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new RefreshReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(refreshReceiver, new IntentFilter(MANAGER_REFRESH));

        loadingBar.setVisibility(View.GONE);

        // Don't even display the "enableDisableSelected" button to non-oms users.
        if (!checkOMS(context))
            sheetView.findViewById(R.id.enable_disable_selected).setVisibility(View.GONE);
        if (MainActivity.instanceBasedAndromedaFailure) {
            // Andromeda is broken!
            sheetView.findViewById(R.id.enable_disable_selected).setVisibility(View.GONE);
            sheetView.findViewById(R.id.enable_selected).setVisibility(View.GONE);
            sheetView.findViewById(R.id.disable_selected).setVisibility(View.GONE);
        }
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
        toggleAll.setOnCheckedChangeListener(
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

        toggleZone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    overlayList = mAdapter
                            .getOverlayManagerList();
                    if (toggleAll.isChecked()) {
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
                    toggleAll.setChecked(!toggleAll
                            .isChecked());
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(),
                            "Window has lost connection with the host.");
                }
            }
        });

        enableDisableSelected.setOnClickListener(v ->
                new RunEnableDisable(ManagerFragment.this).execute());
        enableSelected.setOnClickListener(v ->
                new RunEnable(ManagerFragment.this).execute());
        if (!Systems.checkOMS(context)) {
            if (!Systems.isSamsungDevice(context)) {
                disableSelected.setText(getString(R.string.fab_menu_uninstall));
            } else {
                disableSelected.setVisibility(View.GONE);
            }
        }
        disableSelected.setOnClickListener(v ->
                new RunDisable(ManagerFragment.this).execute());
        if (!Systems.checkOMS(context) && !Systems.isSamsungDevice(context))
            uninstallSelected.setVisibility(View.GONE);
        uninstallSelected.setOnClickListener(v ->
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
        menu.clear();
        inflater.inflate(R.menu.overlays_list_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.searchView = (SearchView) searchItem.getActionView();
            mainActivity.searchView.setOnQueryTextListener(mainActivity);
            searchView = ((MainActivity) getActivity()).searchView;
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem menuItem) {
                    mainActivity.searchView.setIconified(false);
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                    if (!userInput.equals("")) {
                        userInput = "";
                        if (layoutReloader != null && !layoutReloader.isCancelled()) {
                            layoutReloader.cancel(true);
                            layoutReloader = new LayoutReloader(ManagerFragment.this, userInput);
                            layoutReloader.execute();
                        } else {
                            layoutReloader = new LayoutReloader(ManagerFragment.this, userInput);
                            layoutReloader.execute();
                        }
                    }
                    return true;
                }
            });
        }

        assert getActivity() != null;
        updateMenuButtonState(menu.findItem(R.id.alphabetize));
        updateSortingMenuButtonState(menu.findItem(R.id.sort_by_state));
        if (!checkOMS(context) ||
                checkAndromeda(context) ||
                MainActivity.instanceBasedAndromedaFailure) {
            menu.findItem(R.id.restart_systemui).setVisible(false);
        }
        if (!checkOMS(context)) {
            menu.findItem(R.id.sort_by_state).setVisible(false);
            prefs.edit().remove("manager_sorting_mode").apply();
        }
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
     * Update the options menu icons
     *
     * @param menuItem Object of menu item
     */
    private void updateSortingMenuButtonState(MenuItem menuItem) {
        String sortingMode = prefs.getString("manager_sorting_mode", "default");
        String assignedMode = context.getString(R.string.menu_state_prefix);
        switch (sortingMode) {
            case "default":
                String message = String.format(assignedMode,
                        context.getString(R.string.menu_state_default));
                menuItem.setIcon(R.drawable.toolbar_manager_all);
                menuItem.setTitle(message);
                break;
            case "enabled":
                String message2 = String.format(assignedMode,
                        context.getString(R.string.menu_state_enabled));
                menuItem.setIcon(R.drawable.toolbar_manager_enabled);
                menuItem.setTitle(message2);
                break;
            case "disabled":
                String message3 = String.format(assignedMode,
                        context.getString(R.string.menu_state_disabled));
                menuItem.setIcon(R.drawable.toolbar_manager_disabled);
                menuItem.setTitle(message3);
                break;
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
            switch (item.getItemId()) {
                case R.id.alphabetize:
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
                case R.id.sort_by_state:
                    String assignedMode = context.getString(R.string.menu_state_prefix);
                    String sortingMode = prefs.getString("manager_sorting_mode", "default");
                    switch (sortingMode) {
                        case "default":
                            String message = String.format(assignedMode,
                                    context.getString(R.string.menu_state_enabled));
                            prefs.edit().putString("manager_sorting_mode", "enabled").apply();
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            break;
                        case "enabled":
                            String message2 = String.format(assignedMode,
                                    context.getString(R.string.menu_state_disabled));
                            prefs.edit().putString("manager_sorting_mode", "disabled").apply();
                            Toast.makeText(context, message2, Toast.LENGTH_SHORT).show();
                            break;
                        case "disabled":
                            String message3 = String.format(assignedMode,
                                    context.getString(R.string.menu_state_default));
                            prefs.edit().putString("manager_sorting_mode", "default").apply();
                            Toast.makeText(context, message3, Toast.LENGTH_SHORT).show();
                            break;
                    }
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
                        SpannableStringBuilder pName = StringUtils.format(
                                context.getString(R.string.manager_theme_name),
                                getPackageName(context, metadata),
                                Typeface.BOLD);
                        overlayList.get(i).setThemeName(pName.toString());
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        recyclerView.invalidate();
        recyclerView = null;

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
                currentPosition = ((LinearLayoutManager) fragment.recyclerView
                        .getLayoutManager())
                        .findFirstCompletelyVisibleItemPosition();
                fragment.swipeRefreshLayout.setRefreshing(true);
                fragment.toggleAll.setChecked(false);
                fragment.toggleAll.setEnabled(false);
                fragment.recyclerView.setEnabled(false);
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
                    fragment.activatedOverlays = new ArrayList<>();

                    if (Systems.checkOMS(fragment.context)) {
                        fragment.activatedOverlays = new ArrayList<>();

                        List<String> enabledOverlays = new ArrayList<>(
                                ThemeManager.listOverlays(fragment.context, STATE_ENABLED));
                        List<String> disabledOverlays = new ArrayList<>(
                                ThemeManager.listOverlays(fragment.context, STATE_DISABLED));

                        List<String> allOverlays = new ArrayList<>();
                        switch (fragment.prefs.getString("manager_sorting_mode", "default")) {
                            case "default":
                                fragment.activatedOverlays = new ArrayList<>(enabledOverlays);
                                allOverlays.addAll(enabledOverlays);
                                allOverlays.addAll(disabledOverlays);
                                break;
                            case "enabled":
                                fragment.activatedOverlays = new ArrayList<>(enabledOverlays);
                                allOverlays.addAll(enabledOverlays);
                                break;
                            case "disabled":
                                allOverlays.addAll(disabledOverlays);
                                break;
                        }
                        Collections.sort(allOverlays);

                        // Create the map for {package name: package identifier}
                        Map<String, String> unsortedMap = new HashMap<>();

                        // Then let's convert all the package names to their app names
                        for (int i = 0; i < allOverlays.size(); i++) {
                            boolean canContinue = true;
                            final String userInputString = userInput.get();
                            if (userInputString != null && !userInputString.isEmpty()) {
                                StringBuilder combined = new StringBuilder();
                                String metadata = Packages.getOverlayMetadata(
                                        context,
                                        allOverlays.get(i),
                                        References.metadataOverlayParent);
                                if ((metadata != null) && !metadata.isEmpty()) {
                                    combined.append(Packages.getPackageName(context, metadata));
                                }
                                combined.append(getPackageName(context,
                                        getOverlayTarget(context, allOverlays.get(i))));
                                if (!combined.toString().toLowerCase(Locale.US).contains(
                                        userInputString.toLowerCase(Locale.US))) {
                                    canContinue = false;
                                }
                            }
                            if (canContinue) {
                                try {
                                    ApplicationInfo applicationInfo = context
                                            .getPackageManager()
                                            .getApplicationInfo(allOverlays.get(i), 0);
                                    String packageTitle = context.getPackageManager()
                                            .getApplicationLabel(applicationInfo).toString();
                                    String targetApplication = Packages.getOverlayTarget(
                                            context,
                                            packageTitle);

                                    if (isPackageInstalled(context, targetApplication)) {
                                        unsortedMap.put(
                                                allOverlays.get(i),
                                                getPackageName(context, targetApplication));
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }

                        if (!unsortedMap.isEmpty()) {
                            // Sort the values list
                            List<Pair<String, String>> sortedMap =
                                    sortMapByValues(unsortedMap);

                            for (Pair<String, String> entry : sortedMap) {
                                if (disabledOverlays.contains(entry.first)) {
                                    ManagerItem st = new ManagerItem(
                                            context,
                                            entry.first,
                                            false);
                                    fragment.overlaysList.add(st);
                                } else if (fragment.activatedOverlays.contains(entry.first)) {
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
                        fragment.activatedOverlays.addAll(listed);
                        Collections.sort(fragment.activatedOverlays);
                        for (int i = 0; i < fragment.activatedOverlays.size(); i++) {
                            ManagerItem st = new ManagerItem(context,
                                    fragment.activatedOverlays.get(i), true);
                            StringBuilder combined = new StringBuilder();
                            combined.append(st.getLabelName());
                            combined.append(st.getThemeName());
                            if (combined.toString().toLowerCase(Locale.US).contains(
                                    userInput.get().toLowerCase(Locale.US)))
                                fragment.overlaysList.add(st);
                        }
                    }

                    try {
                        Thread.sleep((long) (fragment.firstBoot ?
                                MANAGER_FRAGMENT_INITIAL_DELAY : 0));
                    } catch (InterruptedException ignored) {
                    }
                    if (fragment.firstBoot) fragment.firstBoot = false;
                } catch (Exception ignored) {
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
                fragment.toggleAll.setEnabled(true);
                fragment.loadingBar.setVisibility(View.GONE);
                fragment.mAdapter = new ManagerAdapter(fragment.overlaysList);
                fragment.recyclerView.setAdapter(fragment.mAdapter);
                fragment.recyclerView.getLayoutManager().scrollToPosition(this.currentPosition);
                fragment.recyclerView.setEnabled(true);
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
                    fragment.toggleZone.setVisibility(View.INVISIBLE);
                    fragment.noOverlaysEnabled.setVisibility(View.VISIBLE);
                    fragment.recyclerView.setVisibility(View.GONE);
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
                    fragment.toggleZone.setVisibility(View.VISIBLE);
                    fragment.noOverlaysEnabled.setVisibility(View.GONE);
                    fragment.recyclerView.setVisibility(View.VISIBLE);
                }
                if (!Systems.checkOMS(fragment.context)) {
                    fragment.enableSelected.setVisibility(View.GONE);
                }
                if (fragment.firstRun == null) fragment.firstRun = false;
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
                if ("unauthorized".equals(result)) {
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
                    ThemeManager.enableOverlay(context, data);

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

                    fragment.activatedOverlays.clear();
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
                                fragment.activatedOverlays.add(packageInfo.packageName);
                            }
                        }
                    } else {
                        File currentDir = new File(LEGACY_NEXUS_DIR);
                        String[] listed = currentDir.list();
                        for (String file : listed) {
                            if (".apk".equals(file.substring(file.length() - 4))) {
                                fragment.activatedOverlays.add(file.substring(0,
                                        file.length() - 4));
                            }
                        }
                    }
                }

                // Automatically sort the activated overlays by alphabetical order
                Collections.sort(fragment.activatedOverlays);

                for (int i = 0; i < fragment.activatedOverlays.size(); i++) {
                    ManagerItem st = new ManagerItem(context,
                            fragment.activatedOverlays.get(i), true);
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
                boolean hasFailed = false;
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
                            hasFailed = true;
                        }
                    }
                }
                if ((!enabled.isEmpty() || !disabled.isEmpty()) && !hasFailed) {
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
                if (layoutReloader != null && !layoutReloader.isCancelled()) {
                    layoutReloader.cancel(true);
                    layoutReloader = new LayoutReloader(fragment, MainActivity.userInput);
                    layoutReloader.execute();
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