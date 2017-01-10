package projekt.substratum.fragments;

import android.content.Context;
import android.content.Intent;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.R;
import projekt.substratum.adapters.OverlayManagerAdapter;
import projekt.substratum.config.References;
import projekt.substratum.model.OverlayManager;
import projekt.substratum.util.FloatingActionMenu;
import projekt.substratum.util.ReadOverlays;

import static projekt.substratum.config.References.REFRESH_WINDOW_DELAY;
import static projekt.substratum.util.MapUtils.sortMapByValues;

public class AdvancedManagerFragment extends Fragment {

    private ArrayList<String> activated_overlays;
    private RecyclerView.Adapter mAdapter;
    private MaterialSheetFab materialSheetFab;
    private SharedPreferences prefs;
    private RelativeLayout relativeLayout;
    private ViewGroup root;
    private List<OverlayManager> overlaysList;
    private FloatingActionMenu floatingActionButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean swipeRefreshing;
    private Boolean first_run = null;
    private MaterialProgressBar progressBar;
    private RecyclerView mRecyclerView;
    private ProgressBar loadingBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        root = (ViewGroup) inflater.inflate(R.layout.advanced_manager_fragment, container, false);
        relativeLayout = (RelativeLayout) root.findViewById(R.id.no_overlays_enabled);
        mRecyclerView = (RecyclerView) root.findViewById(R.id.overlays_recycler_view);

        loadingBar = (ProgressBar) root.findViewById(R.id.header_loading_bar);
        loadingBar.setVisibility(View.GONE);

        View sheetView = root.findViewById(R.id.fab_sheet);
        View overlay = root.findViewById(R.id.overlay);
        int sheetColor = getContext().getColor(R.color.fab_menu_background_card);
        int fabColor = getContext().getColor(R.color.fab_background_color);

        progressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);

        floatingActionButton = (FloatingActionMenu) root.findViewById(R
                .id.apply_fab);
        floatingActionButton.hide();

        // Create material sheet FAB
        if (sheetView != null && overlay != null) {
            materialSheetFab = new MaterialSheetFab<>(floatingActionButton, sheetView, overlay,
                    sheetColor, fabColor);
        }

        LayoutReloader layoutReloader = new LayoutReloader(getContext());
        layoutReloader.execute("");

        swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (first_run != null) {
                if (mRecyclerView.isShown() && !first_run) {
                    swipeRefreshing = true;
                    LayoutReloader layoutReloader1 = new LayoutReloader(getContext());
                    layoutReloader1.execute("");
                } else {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });

        Switch toggle_all = (Switch) root.findViewById(R.id.select_all);
        toggle_all.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    try {
                        List<OverlayManager> overlayList = ((OverlayManagerAdapter) mAdapter)
                                .getOverlayManagerList();
                        if (isChecked) {
                            for (int i = 0; i < overlayList.size(); i++) {
                                OverlayManager currentOverlay = overlayList.get(i);
                                if (!currentOverlay.isSelected()) {
                                    currentOverlay.setSelected(true);
                                }
                                mAdapter.notifyDataSetChanged();
                            }
                        } else {
                            for (int i = 0; i < overlayList.size(); i++) {
                                OverlayManager currentOverlay = overlayList.get(i);
                                if (currentOverlay.isSelected()) {
                                    currentOverlay.setSelected(false);
                                }
                            }
                            mAdapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        Log.e("OverlaysList", "Window has lost connection with the host.");
                    }
                });

        TextView disable_selected = (TextView) root.findViewById(R.id.disable_selected);
        if (!References.checkOMS(getContext()))
            disable_selected.setText(getString(R.string.fab_menu_uninstall));
        if (disable_selected != null)
            disable_selected.setOnClickListener(v -> {
                materialSheetFab.hideSheet();
                loadingBar.setVisibility(View.VISIBLE);
                if (References.checkOMS(getContext())) {
                    String data = References.disableOverlay();
                    List<OverlayManager> overlayList = ((OverlayManagerAdapter) mAdapter)
                            .getOverlayManagerList();
                    for (int i = 0; i < overlayList.size(); i++) {
                        OverlayManager overlay13 = overlayList.get(i);
                        if (overlay13.isSelected()) {
                            data = data + " " + overlay13.getName();
                        }
                    }
                    if (!prefs.getBoolean("systemui_recreate", false) &&
                            data.contains("systemui")) {
                        data = data + " && pkill -f com.android.systemui";
                    }
                    Toast toast = Toast.makeText(getContext(), getString(R
                                    .string.toast_disabled),
                            Toast.LENGTH_LONG);
                    toast.show();
                    if (References.isPackageInstalled(getContext(),
                            "masquerade.substratum")) {
                        Intent runCommand = new Intent();
                        runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        runCommand.setAction("masquerade.substratum.COMMANDS");
                        runCommand.putExtra("om-commands", data);
                        getContext().sendBroadcast(runCommand);
                    } else {
                        new References.ThreadRunner().execute(data);
                    }
                    if (References.checkOMSVersion(getContext()) == 7 &&
                            !data.contains("projekt.substratum")) {
                        Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            // OMS may not have written all the changes so quickly just yet
                            // so we may need to have a small delay
                            try {
                                getActivity().recreate();
                            } catch (Exception e) {
                                // Consume window refresh
                            }
                        }, REFRESH_WINDOW_DELAY);
                    }
                } else {
                    String current_directory;
                    if (References.inNexusFilter()) {
                        current_directory = "/system/overlay/";
                    } else {
                        current_directory = "/system/vendor/overlay/";
                    }

                    for (int i = 0; i < overlaysList.size(); i++) {
                        if (overlaysList.get(i).isSelected()) {
                            Log.e("overlays", overlaysList.get(i).getName());
                            References.mountRW();
                            References.delete(current_directory +
                                    overlaysList.get(i).getName() + ".apk");
                        }
                    }

                    // Since we had to parse the directory to process the recyclerView,
                    // reparse it to notifyDataSetChanged

                    activated_overlays.clear();
                    overlaysList.clear();
                    if (References.inNexusFilter()) {
                        current_directory = "/system/overlay/";
                    } else {
                        current_directory = "/system/vendor/overlay/";
                    }

                    File currentDir = new File(current_directory);
                    String[] listed = currentDir.list();
                    for (String file : listed) {
                        if (file.substring(file.length() - 4).equals(".apk")) {
                            activated_overlays.add(file.substring(0,
                                    file.length() - 4));
                        }
                    }

                    // Automatically sort the activated overlays by alphabetical order
                    Collections.sort(activated_overlays);

                    for (int i = 0; i < activated_overlays.size(); i++) {
                        OverlayManager st = new OverlayManager(getContext(),
                                activated_overlays.get(i), true);
                        overlaysList.add(st);
                    }

                    mAdapter.notifyDataSetChanged();

                    Toast toast2 = Toast.makeText(getContext(), getString(R
                                    .string.toast_disabled6),
                            Toast.LENGTH_SHORT);
                    toast2.show();
                    AlertDialog.Builder alertDialogBuilder =
                            new AlertDialog.Builder(getContext());
                    alertDialogBuilder
                            .setTitle(getString(R.string.legacy_dialog_soft_reboot_title));
                    alertDialogBuilder
                            .setMessage(getString(R.string.legacy_dialog_soft_reboot_text));
                    alertDialogBuilder
                            .setPositiveButton(android.R.string.ok,
                                    (dialog, id) -> References.reboot());
                    alertDialogBuilder.setCancelable(false);
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            });

        TextView enable_selected = (TextView) root.findViewById(R.id.enable_selected);
        if (enable_selected != null)
            enable_selected.setOnClickListener(v -> {
                materialSheetFab.hideSheet();
                loadingBar.setVisibility(View.VISIBLE);
                String data = References.enableOverlay();
                List<OverlayManager> overlayList = ((OverlayManagerAdapter) mAdapter)
                        .getOverlayManagerList();
                for (int i = 0; i < overlayList.size(); i++) {
                    OverlayManager overlay12 = overlayList.get(i);
                    if (overlay12.isSelected()) {
                        data = data + " " + overlay12.getName();
                    }
                }
                if (!prefs.getBoolean("systemui_recreate", false) &&
                        data.contains("systemui")) {
                    data = data + " && pkill -f com.android.systemui";
                }
                Toast toast = Toast.makeText(getContext(), getString(R
                                .string.toast_enabled),
                        Toast.LENGTH_LONG);
                toast.show();
                if (References.isPackageInstalled(getContext(), "masquerade.substratum")) {
                    Intent runCommand = new Intent();
                    runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    runCommand.setAction("masquerade.substratum.COMMANDS");
                    runCommand.putExtra("om-commands", data);
                    getContext().sendBroadcast(runCommand);
                } else {
                    new References.ThreadRunner().execute(data);
                }
                if (References.checkOMSVersion(getContext()) == 7 &&
                        !data.contains("projekt.substratum")) {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        // OMS may not have written all the changes so quickly just yet
                        // so we may need to have a small delay
                        try {
                            getActivity().recreate();
                        } catch (Exception e) {
                            // Consume window refresh
                        }
                    }, REFRESH_WINDOW_DELAY);
                }
            });

        TextView uninstall_selected = (TextView) root.findViewById(R.id.uninstall);
        if (!References.checkOMS(getContext()))
            uninstall_selected.setVisibility(View.GONE);
        if (uninstall_selected != null)
            uninstall_selected.setOnClickListener(v -> {
                materialSheetFab.hideSheet();
                loadingBar.setVisibility(View.VISIBLE);
                String data = "";
                List<OverlayManager> overlayList = ((OverlayManagerAdapter) mAdapter)
                        .getOverlayManagerList();
                for (int i = 0; i < overlayList.size(); i++) {
                    OverlayManager overlay1 = overlayList.get(i);
                    if (data.length() == 0) {
                        if (overlay1.isSelected()) {
                            data = "pm uninstall " + overlay1.getName();
                        }
                    } else {
                        if (overlay1.isSelected()) {
                            data = data + " && pm uninstall " + overlay1.getName();
                        }
                    }
                }
                if (!prefs.getBoolean("systemui_recreate", false) &&
                        data.contains("systemui")) {
                    data = data + " && " + References.refreshWindows() +
                            " && pkill -f com.android.systemui";
                } else {
                    data += " && " + References.refreshWindows();
                }
                Toast toast = Toast.makeText(getContext(), getString(R
                                .string.toast_uninstalling),
                        Toast.LENGTH_LONG);
                toast.show();
                if (References.isPackageInstalled(getContext(), "masquerade.substratum")) {
                    Intent runCommand = new Intent();
                    runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    runCommand.setAction("masquerade.substratum.COMMANDS");
                    runCommand.putExtra("om-commands", data);
                    getContext().sendBroadcast(runCommand);
                } else {
                    new References.ThreadRunner().execute(data);
                }
                if (References.checkOMSVersion(getContext()) == 7 &&
                        !data.contains("projekt.substratum")) {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        // OMS may not have written all the changes so quickly just yet
                        // so we may need to have a small delay
                        try {
                            getActivity().recreate();
                        } catch (Exception e) {
                            // Consume window refresh
                        }
                    }, REFRESH_WINDOW_DELAY);
                }
            });

        return root;
    }

    private class LayoutReloader extends AsyncTask<String, Integer, String> {

        private Context mContext;

        private LayoutReloader(Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String result) {
            progressBar.setVisibility(View.GONE);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
            mAdapter = new OverlayManagerAdapter(overlaysList);
            mRecyclerView.setAdapter(mAdapter);

            if (overlaysList.size() == 0) {
                floatingActionButton.hide();
                relativeLayout.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            } else {
                floatingActionButton.show();
                relativeLayout.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
            }
            if (!prefs.getBoolean("manager_disabled_overlays", true) ||
                    !References.checkOMS(mContext)) {
                LinearLayout enable_view = (LinearLayout) root.findViewById(R.id.enable);
                enable_view.setVisibility(View.GONE);
            }
            if (swipeRefreshing) {
                swipeRefreshing = false;
                swipeRefreshLayout.setRefreshing(false);
            }
            if (first_run == null) first_run = false;
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            overlaysList = new ArrayList<>();
            activated_overlays = new ArrayList<>();
            ArrayList<String> disabled_overlays;
            ArrayList<String> all_overlays;

            if (References.checkOMS(mContext)) {
                ArrayList<String> active = new ArrayList<>(ReadOverlays.main(5, mContext));
                ArrayList<String> disabled = new ArrayList<>(ReadOverlays.main(4, mContext));

                // Filter out icon pack overlays from the advanced manager
                activated_overlays = new ArrayList<>();
                for (int i = 0; i < active.size(); i++) {
                    if (!active.get(i).endsWith(".icon")) {
                        activated_overlays.add(active.get(i));
                    }
                }

                // Filter out icon pack overlays from the advanced manager
                disabled_overlays = new ArrayList<>();
                for (int i = 0; i < disabled.size(); i++) {
                    if (!disabled.get(i).endsWith(".icon")) {
                        disabled_overlays.add(disabled.get(i));
                    }
                }

                if (prefs.getBoolean("manager_disabled_overlays", true)) {
                    all_overlays = new ArrayList<>(activated_overlays);
                    all_overlays.addAll(disabled_overlays);
                    Collections.sort(all_overlays);

                    // Create the map for {package name: package identifier}
                    HashMap<String, String> unsortedMap = new HashMap<>();

                    // Then let's convert all the package names to their app names
                    for (int i = 0; i < all_overlays.size(); i++) {
                        try {
                            ApplicationInfo applicationInfo = getContext().getPackageManager()
                                    .getApplicationInfo(all_overlays.get(i), 0);
                            String packageTitle = getContext().getPackageManager()
                                    .getApplicationLabel(applicationInfo).toString();
                            unsortedMap.put(all_overlays.get(i), References.grabPackageName(
                                    getContext(), References.grabOverlayTarget(getContext(),
                                            packageTitle)));
                        } catch (Exception e) {
                            // Suppress warning
                        }
                    }

                    // Sort the values list
                    List<Pair<String, String>> sortedMap = sortMapByValues(unsortedMap);

                    for (Pair<String, String> entry : sortedMap) {
                        if (disabled_overlays.contains(entry.first)) {
                            OverlayManager st = new OverlayManager(mContext,
                                    entry.first, false);
                            overlaysList.add(st);
                        } else if (activated_overlays.contains(entry.first)) {
                            OverlayManager st = new OverlayManager(mContext,
                                    entry.first, true);
                            overlaysList.add(st);
                        }
                    }
                } else {
                    all_overlays = new ArrayList<>(activated_overlays);
                    Collections.sort(all_overlays);

                    try {
                        // Create the map for {package name: package identifier}
                        HashMap<String, String> unsortedMap = new HashMap<>();

                        // Then let's convert all the package names to their app names
                        for (int i = 0; i < all_overlays.size(); i++) {
                            try {
                                ApplicationInfo applicationInfo = getContext().getPackageManager()
                                        .getApplicationInfo(all_overlays.get(i), 0);
                                String packageTitle = getContext().getPackageManager()
                                        .getApplicationLabel(applicationInfo).toString();
                                unsortedMap.put(all_overlays.get(i), References.grabPackageName(
                                        getContext(), References.grabOverlayTarget(getContext(),
                                                packageTitle)));
                            } catch (Exception e) {
                                // Suppress warning
                            }
                        }

                        // Sort the values list
                        List<Pair<String, String>> sortedMap = sortMapByValues(unsortedMap);

                        for (Pair<String, String> entry : sortedMap) {
                            if (activated_overlays.contains(entry.first)) {
                                OverlayManager st = new OverlayManager(mContext,
                                        entry.first, true);
                                overlaysList.add(st);
                            }
                        }
                    } catch (Exception e) {
                        Toast toast = Toast.makeText(getContext(), getString(R
                                        .string.advanced_manager_overlay_read_error),
                                Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
            } else {
                // At this point, the object is an RRO formatted check
                String current_directory;
                if (References.inNexusFilter()) {
                    current_directory = "/system/overlay/";
                } else {
                    current_directory = "/system/vendor/overlay/";
                }

                File currentDir = new File(current_directory);
                if (currentDir.exists() && currentDir.isDirectory()) {
                    String[] listed = currentDir.list();
                    for (int i = 0; i < listed.length; i++) {
                        if (listed[i].substring(listed[i].length() - 4).equals(".apk")) {
                            activated_overlays.add(listed[i].substring(0, listed[i].length() - 4));
                        }
                    }
                    Collections.sort(activated_overlays);
                    for (int i = 0; i < activated_overlays.size(); i++) {
                        OverlayManager st = new OverlayManager(mContext, activated_overlays.get
                                (i), true);
                        overlaysList.add(st);
                    }
                }
            }
            return null;
        }
    }
}
