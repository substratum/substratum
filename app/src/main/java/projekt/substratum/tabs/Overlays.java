package projekt.substratum.tabs;

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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.Fragment;
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
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.gordonwong.materialsheetfab.MaterialSheetFab;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.adapters.OverlaysAdapter;
import projekt.substratum.adapters.VariantsAdapter;
import projekt.substratum.config.ElevatedCommands;
import projekt.substratum.config.FileOperations;
import projekt.substratum.config.References;
import projekt.substratum.config.ThemeManager;
import projekt.substratum.model.OverlaysInfo;
import projekt.substratum.model.VariantInfo;
import projekt.substratum.services.NotificationButtonReceiver;
import projekt.substratum.util.CacheCreator;
import projekt.substratum.util.FloatingActionMenu;
import projekt.substratum.util.SubstratumBuilder;

import static android.content.Context.CLIPBOARD_SERVICE;
import static projekt.substratum.config.References.REFRESH_WINDOW_DELAY;
import static projekt.substratum.config.References.SUBSTRATUM_LOG;
import static projekt.substratum.util.MapUtils.sortMapByValues;

public class Overlays extends Fragment {

    private TextView loader_string;
    private ProgressDialog mProgressDialog;
    private SubstratumBuilder sb;
    private List<OverlaysInfo> overlaysLists, checkedOverlays;
    private RecyclerView.Adapter mAdapter;
    private String theme_name, theme_pid, versionName;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private boolean has_initialized_cache = false;
    private boolean has_failed = false;
    private int fail_count;
    private int id = References.notification_id;
    private ArrayList<OverlaysInfo> values2;
    private RecyclerView mRecyclerView;
    private Spinner base_spinner;
    private SharedPreferences prefs;
    private ArrayList<String> final_runner;
    private boolean mixAndMatchMode, enable_mode, disable_mode, compile_enable_mode;
    private ArrayList<String> all_installed_overlays;
    private Context mContext;
    private Switch toggle_all;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialSheetFab materialSheetFab;
    private ProgressBar progressBar;
    private ArrayList<String> current_theme_overlays;
    private Boolean is_active = false;
    private Boolean DEBUG = References.DEBUG;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_1, container, false);
        mContext = getContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        theme_name = InformationActivity.getThemeName();
        theme_pid = InformationActivity.getThemePID();

        progressBar = (ProgressBar) root.findViewById(R.id.header_loading_bar);
        progressBar.setVisibility(View.GONE);

        materialProgressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        mRecyclerView = (RecyclerView) root.findViewById(R.id.overlayRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        ArrayList<OverlaysInfo> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new OverlaysAdapter(empty_array);
        mRecyclerView.setAdapter(empty_adapter);

        TextView toggle_all_overlays_text = (TextView)
                root.findViewById(R.id.toggle_all_overlays_text);
        toggle_all_overlays_text.setVisibility(View.VISIBLE);

        File work_area = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/.substratum");
        if (!work_area.exists()) {
            boolean created = work_area.mkdir();
            if (created) Log.d(SUBSTRATUM_LOG,
                    "Updating the internal storage with proper file directories...");
        }

        toggle_all = (Switch) root.findViewById(R.id.toggle_all_overlays);
        toggle_all.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    try {
                        overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                        if (isChecked) {
                            for (int i = 0; i < overlaysLists.size(); i++) {
                                OverlaysInfo currentOverlay = overlaysLists.get(i);
                                if (!currentOverlay.isSelected()) {
                                    currentOverlay.setSelected(true);
                                }
                                mAdapter.notifyDataSetChanged();
                            }
                        } else {
                            for (int i = 0; i < overlaysLists.size(); i++) {
                                OverlaysInfo currentOverlay = overlaysLists.get(i);
                                if (currentOverlay.isSelected()) {
                                    currentOverlay.setSelected(false);
                                }
                            }
                            mAdapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        Log.e("Overlays", "Window has lost connection with the host.");
                    }
                });

        swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(false);
            getActivity().recreate();
        });
        swipeRefreshLayout.setVisibility(View.GONE);

        View sheetView = root.findViewById(R.id.fab_sheet);
        View overlay = root.findViewById(R.id.overlay);
        int sheetColor = mContext.getColor(R.color.fab_menu_background_card);
        int fabColor = mContext.getColor(R.color.fab_background_color);

        final FloatingActionMenu floatingActionButton = (FloatingActionMenu) root.findViewById(R
                .id.apply_fab);
        floatingActionButton.show();

        // Create material sheet FAB
        if (sheetView != null && overlay != null) {
            materialSheetFab = new MaterialSheetFab<>(floatingActionButton, sheetView, overlay,
                    sheetColor, fabColor);
        }

        Switch enable_swap = (Switch) root.findViewById(R.id.enable_swap);
        if (!References.checkOMS(mContext))
            enable_swap.setText(getString(R.string.fab_menu_swap_toggle_legacy));
        if (enable_swap != null) {
            if (prefs.getBoolean("enable_swapping_overlays", true)) {
                mixAndMatchMode = true;
                enable_swap.setChecked(true);
            } else {
                mixAndMatchMode = false;
                enable_swap.setChecked(false);
            }
            enable_swap.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    prefs.edit().putBoolean("enable_swapping_overlays", true).apply();
                    mixAndMatchMode = true;
                } else {
                    prefs.edit().putBoolean("enable_swapping_overlays", false).apply();
                    mixAndMatchMode = false;
                }
            });
        }

        final TextView compile_enable_selected = (TextView) root.findViewById(R.id
                .compile_enable_selected);
        if (!References.checkOMS(mContext))
            compile_enable_selected.setVisibility(View.GONE);
        if (compile_enable_selected != null)
            compile_enable_selected.setOnClickListener(v -> {
                materialSheetFab.hideSheet();
                if (!is_active) {
                    is_active = true;
                    compile_enable_mode = true;
                    enable_mode = false;
                    disable_mode = false;

                    overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                    checkedOverlays = new ArrayList<>();

                    for (int i = 0; i < overlaysLists.size(); i++) {
                        OverlaysInfo currentOverlay = overlaysLists.get(i);
                        if (currentOverlay.isSelected()) {
                            checkedOverlays.add(currentOverlay);
                        }
                    }
                    if (!checkedOverlays.isEmpty()) {
                        if (base_spinner.getSelectedItemPosition() != 0 &&
                                base_spinner.getVisibility() == View.VISIBLE) {
                            Phase2_InitializeCache phase2_initializeCache = new
                                    Phase2_InitializeCache();
                            phase2_initializeCache.execute(base_spinner.getSelectedItem()
                                    .toString());
                        } else {
                            Phase2_InitializeCache phase2_initializeCache = new
                                    Phase2_InitializeCache();
                            phase2_initializeCache.execute("");
                        }
                    } else {
                        if (toggle_all.isChecked()) toggle_all.setChecked(false);
                        is_active = false;
                        Toast toast2 = Toast.makeText(mContext, getString(R
                                        .string.toast_disabled5),
                                Toast.LENGTH_SHORT);
                        toast2.show();
                    }
                }
            });

        TextView compile_update_selected = (TextView) root.findViewById(R.id
                .compile_update_selected);
        if (!References.checkOMS(mContext))
            compile_update_selected.setText(getString(R.string.fab_menu_compile_install));
        if (compile_update_selected != null)
            compile_update_selected.setOnClickListener(v -> {
                materialSheetFab.hideSheet();
                if (!is_active) {
                    is_active = true;
                    compile_enable_mode = false;

                    overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                    checkedOverlays = new ArrayList<>();

                    for (int i = 0; i < overlaysLists.size(); i++) {
                        OverlaysInfo currentOverlay = overlaysLists.get(i);
                        if (currentOverlay.isSelected()) {
                            checkedOverlays.add(currentOverlay);
                        }
                    }
                    if (!checkedOverlays.isEmpty()) {
                        if (base_spinner.getSelectedItemPosition() != 0 &&
                                base_spinner.getVisibility() == View.VISIBLE) {
                            Phase2_InitializeCache phase2_initializeCache = new
                                    Phase2_InitializeCache();
                            phase2_initializeCache.execute(base_spinner.getSelectedItem()
                                    .toString());
                        } else {
                            Phase2_InitializeCache phase2_initializeCache = new
                                    Phase2_InitializeCache();
                            phase2_initializeCache.execute("");
                        }
                    } else {
                        if (toggle_all.isChecked()) toggle_all.setChecked(false);
                        is_active = false;
                        Toast toast2 = Toast.makeText(mContext, getString(R
                                        .string.toast_disabled5),
                                Toast.LENGTH_SHORT);
                        toast2.show();
                    }
                }
            });

        TextView disable_selected = (TextView) root.findViewById(R.id.disable_selected);
        if (!References.checkOMS(mContext))
            disable_selected.setText(getString(R.string.fab_menu_uninstall));
        if (disable_selected != null)
            disable_selected.setOnClickListener(v -> {
                materialSheetFab.hideSheet();
                if (!is_active) {
                    is_active = true;

                    overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                    checkedOverlays = new ArrayList<>();

                    if (References.checkOMS(mContext)) {
                        compile_enable_mode = false;
                        enable_mode = false;
                        disable_mode = true;

                        for (int i = 0; i < overlaysLists.size(); i++) {
                            OverlaysInfo currentOverlay = overlaysLists.get(i);
                            if (currentOverlay.isSelected() &&
                                    currentOverlay.isOverlayEnabled()) {
                                checkedOverlays.add(currentOverlay);
                            } else {
                                currentOverlay.setSelected(false);
                                mAdapter.notifyDataSetChanged();
                            }
                        }
                        if (!checkedOverlays.isEmpty()) {
                            if (base_spinner.getSelectedItemPosition() != 0 &&
                                    base_spinner.getVisibility() == View.VISIBLE) {
                                Phase2_InitializeCache phase2_initializeCache = new
                                        Phase2_InitializeCache();
                                phase2_initializeCache.execute(base_spinner.getSelectedItem()
                                        .toString());
                            } else {
                                Phase2_InitializeCache phase2_initializeCache = new
                                        Phase2_InitializeCache();
                                phase2_initializeCache.execute("");
                            }
                        } else {
                            if (toggle_all.isChecked()) toggle_all.setChecked(false);
                            is_active = false;
                            Toast toast2 = Toast.makeText(mContext, getString(R
                                            .string.toast_disabled5),
                                    Toast.LENGTH_SHORT);
                            toast2.show();
                        }
                    } else {
                        compile_enable_mode = false;
                        enable_mode = false;
                        disable_mode = true;

                        for (int i = 0; i < overlaysLists.size(); i++) {
                            OverlaysInfo currentOverlay = overlaysLists.get(i);
                            if (currentOverlay.isSelected()) {
                                checkedOverlays.add(currentOverlay);
                            } else {
                                currentOverlay.setSelected(false);
                                mAdapter.notifyDataSetChanged();
                            }
                        }

                        String current_directory;
                        if (References.inNexusFilter()) {
                            current_directory = "/system/overlay/";
                        } else {
                            current_directory = "/system/vendor/overlay/";
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
                                OverlaysInfo currentOverlay = overlaysLists.get(i);
                                if (currentOverlay.isSelected()) {
                                    currentOverlay.setSelected(false);
                                }
                            }
                            Toast toast2 = Toast.makeText(mContext, getString(R
                                            .string.toast_disabled6),
                                    Toast.LENGTH_SHORT);
                            toast2.show();
                            AlertDialog.Builder alertDialogBuilder =
                                    new AlertDialog.Builder(mContext);
                            alertDialogBuilder
                                    .setTitle(getString(R.string
                                            .legacy_dialog_soft_reboot_title));
                            alertDialogBuilder
                                    .setMessage(getString(
                                            R.string.legacy_dialog_soft_reboot_text));
                            alertDialogBuilder
                                    .setPositiveButton(android.R.string.ok,
                                            (dialog, id12) -> ElevatedCommands.reboot());
                            alertDialogBuilder
                                    .setNegativeButton(R.string.remove_dialog_later, (dialog,
                                                                                      id1) -> {
                                        progressBar.setVisibility(View.GONE);
                                        dialog.dismiss();
                                    });
                            AlertDialog alertDialog = alertDialogBuilder.create();
                            alertDialog.show();
                        } else {
                            if (toggle_all.isChecked()) toggle_all.setChecked(false);
                            is_active = false;
                            Toast toast2 = Toast.makeText(mContext, getString(R
                                            .string.toast_disabled5),
                                    Toast.LENGTH_SHORT);
                            toast2.show();
                        }
                        is_active = false;
                        disable_mode = false;
                    }
                }
            });

        LinearLayout enable_zone = (LinearLayout) root.findViewById(R.id.enable);
        if (!References.checkOMS(mContext)) enable_zone.setVisibility(View.GONE);
        TextView enable_selected = (TextView) root.findViewById(R.id.enable_selected);
        if (enable_selected != null) enable_selected.setOnClickListener(v -> {
            materialSheetFab.hideSheet();
            if (!is_active) {
                is_active = true;
                compile_enable_mode = false;
                enable_mode = true;
                disable_mode = false;

                overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                checkedOverlays = new ArrayList<>();

                for (int i = 0; i < overlaysLists.size(); i++) {
                    OverlaysInfo currentOverlay = overlaysLists.get(i);
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
                        Phase2_InitializeCache phase2_initializeCache = new
                                Phase2_InitializeCache();
                        phase2_initializeCache.execute(
                                base_spinner.getSelectedItem().toString());

                    } else {
                        Phase2_InitializeCache phase2_initializeCache = new
                                Phase2_InitializeCache();
                        phase2_initializeCache.execute("");
                    }
                } else {
                    if (toggle_all.isChecked()) toggle_all.setChecked(false);
                    is_active = false;
                    Toast toast2 = Toast.makeText(mContext, getString(R
                                    .string.toast_disabled5),
                            Toast.LENGTH_SHORT);
                    toast2.show();
                }
            }
        });

        // PLUGIN TYPE 3: Parse each overlay folder to see if they have folder options

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
            ArrayList<String> type3 = new ArrayList<>();
            File f = new File(mContext.getCacheDir().getAbsoluteFile() + "/SubstratumBuilder/" +
                    theme_pid + "/assets/overlays/android/");

            if (!References.checkOMS(mContext)) {
                File check_file = new File(mContext.getCacheDir().getAbsoluteFile() +
                        "/SubstratumBuilder/" + theme_pid + "/assets/overlays_legacy/android/");
                if (check_file.exists() && check_file.isDirectory()) {
                    f = new File(check_file.getAbsolutePath());
                }
            }
            File[] fileArray = f.listFiles();
            ArrayList<String> stringArray = new ArrayList<>();
            if (fileArray != null && fileArray.length > 0) {
                for (File file : fileArray) {
                    stringArray.add(file.getName());
                }
            }

            if (stringArray.contains("type3")) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(
                                new File(f.getAbsolutePath() + "/type3"))))) {
                    String formatter = String.format(getString(R.string
                            .overlays_variant_substitute), reader.readLine());
                    type3.add(formatter);
                } catch (IOException e) {
                    Log.e(References.SUBSTRATUM_LOG, "There was an error parsing asset " +
                            "file!");
                    type3.add(getString(R.string
                            .overlays_variant_default_3));
                }
            } else {
                type3.add(getString(R.string.overlays_variant_default_3));
            }

            if (stringArray.size() > 1) {
                for (int i = 0; i < stringArray.size(); i++) {
                    String current = stringArray.get(i);
                    if (!current.equals("res")) {
                        if (!current.contains(".")) {
                            if (current.length() >= 6) {
                                if (current.substring(0, 6).equals("type3_")) {
                                    type3.add(current.substring(6));
                                }
                            }
                        }
                    }
                }
                ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getActivity(),
                        android.R.layout.simple_spinner_dropdown_item, type3);
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
            Log.e(References.SUBSTRATUM_LOG, "Could not parse list of base options for this " +
                    "theme!");
        }
        return root;
    }

    private String checkXposedVersion() {
        String xposed_version = "";

        File f = new File("/system/framework/XposedBridge.jar");
        if (f.exists() && !f.isDirectory()) {
            File file = new File("/system/", "xposed.prop");
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String unparsed_br = br.readLine();
                xposed_version = unparsed_br.substring(8, 10);
            } catch (FileNotFoundException e) {
                Log.e("XposedChecker", "'xposed.prop' could not be found!");
            } catch (IOException e) {
                Log.e("XposedChecker", "Unable to parse BufferedReader from 'xposed.prop'");
            }
            xposed_version = ", " + R.string.logcat_email_xposed_check + " (" +
                    xposed_version + ")";
        }
        return xposed_version;
    }

    private boolean checkActiveNotifications() {
        StatusBarNotification[] activeNotifications = mNotifyManager.getActiveNotifications();
        for (StatusBarNotification statusBarNotification : activeNotifications) {
            if (statusBarNotification.getPackageName().equals("projekt.substratum")) {
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
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
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

            Toast toast = Toast.makeText(context, context.getString(R
                            .string.toast_compiled_updated),
                    Toast.LENGTH_LONG);
            toast.show();
        }

        if (!has_failed || final_runner.size() > fail_count) {
            if (compile_enable_mode && mixAndMatchMode) {
                // Buffer the disableBeforeEnabling String
                ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                if (all_installed_overlays.size() - current_theme_overlays.size() != 0) {
                    for (int i = 0; i < all_installed_overlays.size(); i++) {
                        if (!current_theme_overlays.contains(
                                all_installed_overlays.get(i))) {
                            disableBeforeEnabling.add(all_installed_overlays.get(i));
                        }
                    }
                }
                ThemeManager.disableOverlay(context, disableBeforeEnabling);
            }

            if (compile_enable_mode) ThemeManager.enableOverlay(context, final_command);

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
                        getActivity().recreate();
                    } catch (Exception e) {
                        // Consume window refresh
                    }
                }, REFRESH_WINDOW_DELAY);
            }

            if (!final_runner.isEmpty()) {
                // Install remaining overlays
                ThemeManager.installOverlay(context, final_runner);
            }
        }
    }

    private void failedFunction(Context context) {
        // Add dummy intent to be able to close the notification on click
        Intent notificationIntent = new Intent(context, InformationActivity.class);
        notificationIntent.putExtra("theme_name", theme_name);
        notificationIntent.putExtra("theme_pid", theme_pid);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
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

        Toast toast = Toast.makeText(context, context.getString(R
                        .string.toast_compiled_updated_with_errors),
                Toast.LENGTH_LONG);
        toast.show();

        final Dialog dialog = new Dialog(context, android.R.style
                .Theme_DeviceDefault_Dialog);
        dialog.setContentView(R.layout.logcat_dialog);
        dialog.setTitle(R.string.logcat_dialog_title);
        if (dialog.getWindow() != null)
            dialog.getWindow().setLayout(RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT);

        TextView text = (TextView) dialog.findViewById(R.id.textField);
        text.setText(error_logs);
        ImageButton confirm = (ImageButton) dialog.findViewById(R.id.confirm);
        confirm.setOnClickListener(view -> dialog.dismiss());

        ImageButton copy_clipboard = (ImageButton) dialog.findViewById(
                R.id.copy_clipboard);
        copy_clipboard.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context
                    .getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("substratum_log", error_logs);
            clipboard.setPrimaryClip(clip);
            Toast toast1 = Toast.makeText(context, context.getString(R
                            .string.logcat_dialog_copy_success),
                    Toast.LENGTH_SHORT);
            toast1.show();
        });

        ImageButton send = (ImageButton) dialog.findViewById(
                R.id.send);
        send.setVisibility(View.GONE);

        theme_author = "";
        themer_email = "";
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(theme_pid, PackageManager.GET_META_DATA);
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
                String email_subject =
                        String.format(context.getString(R.string.logcat_email_subject),
                                theme_name);
                String xposed = checkXposedVersion();
                if (xposed.length() > 0) {
                    device += " {" + xposed + "}";
                }
                String email_body =
                        String.format(context.getString(R.string.logcat_email_body),
                                theme_author, theme_name, device, error_logs);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{themer_email});
                i.putExtra(Intent.EXTRA_SUBJECT, email_subject);
                i.putExtra(Intent.EXTRA_TEXT, email_body);
                try {
                    startActivity(Intent.createChooser(
                            i, context.getString(R.string.logcat_email_activity)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(
                            context,
                            context.getString(R.string.logcat_email_activity_error),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mContext.unregisterReceiver(finishReceiver);
        } catch (IllegalArgumentException e) {
            // unregistered already
        }
    }

    private boolean needsRecreate() {
        for (OverlaysInfo oi : checkedOverlays) {
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
            // Grab the current theme_pid's versionName so that we can version our overlays
            try {
                PackageInfo pinfo = mContext.getPackageManager().getPackageInfo(
                        theme_pid, 0);
                versionName = pinfo.versionName;
            } catch (Exception e) {
                // Exception
            }
            List<String> state5 = ThemeManager.listOverlays(5);
            ArrayList<String> all = new ArrayList<>(state5);

            all_installed_overlays = new ArrayList<>();

            // Filter out icon pack overlays from all overlays
            for (int i = 0; i < all.size(); i++) {
                if (!all.get(i).endsWith(".icon")) {
                    all_installed_overlays.add(all.get(i));
                }
            }

            List<String> state5overlays = new ArrayList<>(all_installed_overlays);

            String parse1_themeName = theme_name.replaceAll("\\s+", "");
            String parse2_themeName = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");

            current_theme_overlays = new ArrayList<>();
            for (int i = 0; i < all_installed_overlays.size(); i++) {
                if (all_installed_overlays.get(i).contains(parse2_themeName)) {
                    current_theme_overlays.add(all_installed_overlays.get(i));
                }
            }

            ArrayList<String> values = new ArrayList<>();
            values2 = new ArrayList<>();

            // Buffer the initial values list so that we get the list of packages inside this theme
            try {
                values = new ArrayList<>();
                ArrayList<String> overlaysFolder = new ArrayList<>();
                File overlaysDirectory = new File(mContext.getCacheDir().getAbsoluteFile() +
                        "/SubstratumBuilder/" + theme_pid + "/assets/overlays/");

                if (!References.checkOMS(mContext)) {
                    File check_file = new File(mContext.getCacheDir().getAbsoluteFile() +
                            "/SubstratumBuilder/" + theme_pid + "/assets/overlays_legacy/");
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

                for (String package_name : overlaysFolder) {
                    if ((References.isPackageInstalled(mContext, package_name) ||
                            References.allowedSystemUIOverlay(package_name) ||
                            References.allowedSettingsOverlay(package_name)) &&
                            (!ThemeManager.blacklisted(package_name))) {
                        values.add(package_name);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(References.SUBSTRATUM_LOG, "Could not refresh list of overlay folders.");
            }

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
                    try {
                        ArrayList<VariantInfo> type1a = new ArrayList<>();
                        ArrayList<VariantInfo> type1b = new ArrayList<>();
                        ArrayList<VariantInfo> type1c = new ArrayList<>();
                        ArrayList<String> type2 = new ArrayList<>();

                        ArrayList<String> typeArray = new ArrayList<>();
                        File typeArrayRaw = new File(mContext.getCacheDir().getAbsoluteFile() +
                                "/SubstratumBuilder/" + theme_pid
                                + "/assets/overlays/" + package_identifier);

                        if (!References.checkOMS(mContext)) {
                            File check_file = new File(mContext.getCacheDir().getAbsoluteFile() +
                                    "/SubstratumBuilder/" + theme_pid
                                    + "/assets/overlays_legacy/" + package_identifier + "/");
                            if (check_file.exists() && check_file.isDirectory()) {
                                typeArrayRaw = new File(check_file.getAbsolutePath());
                            }
                        }

                        File[] fileArray = typeArrayRaw.listFiles();
                        if (fileArray != null && fileArray.length > 0) {
                            for (File file : fileArray) {
                                typeArray.add(file.getName());
                            }
                        }

                        // Sort the typeArray so that the types are asciibetical
                        Collections.sort(typeArray);

                        if (typeArray.contains("type1a")) {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(new FileInputStream(
                                            new File(typeArrayRaw.getAbsolutePath() +
                                                    "/type1a"))))) {
                                String formatter = String.format(getString(R.string
                                        .overlays_variant_substitute), reader.readLine());
                                File current_file = new File(mContext.getCacheDir().
                                        getAbsoluteFile() + "/SubstratumBuilder/"
                                        + theme_pid + "/assets/overlays/"
                                        + package_identifier + "/res/values/type1a.xml");
                                String hex = References.getOverlayResource(
                                        current_file);
                                type1a.add(new VariantInfo(formatter, hex));
                            } catch (IOException e) {
                                Log.e(References.SUBSTRATUM_LOG, "There was an error parsing " +
                                        "asset file!");
                                File current_file = new File(mContext.getCacheDir().
                                        getAbsoluteFile() + "/SubstratumBuilder/"
                                        + theme_pid + "/assets/overlays/"
                                        + package_identifier + "/res/values/type1a.xml");
                                String hex = References.getOverlayResource(
                                        current_file);
                                type1a.add(new VariantInfo(getString(R.string
                                        .overlays_variant_default_1a), hex));
                            }
                        } else {
                            File current_file = new File(mContext.getCacheDir().
                                    getAbsoluteFile() + "/SubstratumBuilder/"
                                    + theme_pid + "/assets/overlays/"
                                    + package_identifier + "/res/values/type1a.xml");
                            String hex = References.getOverlayResource(
                                    current_file);
                            type1a.add(new VariantInfo(getString(R.string
                                    .overlays_variant_default_1a), hex));
                        }

                        if (typeArray.contains("type1b")) {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(new FileInputStream(
                                            new File(typeArrayRaw.getAbsolutePath() +
                                                    "/type1b"))))) {
                                String formatter = String.format(getString(R.string
                                        .overlays_variant_substitute), reader.readLine());
                                File current_file = new File(mContext.getCacheDir().
                                        getAbsoluteFile() + "/SubstratumBuilder/"
                                        + theme_pid + "/assets/overlays/"
                                        + package_identifier + "/res/values/type1b.xml");
                                String hex = References.getOverlayResource(
                                        current_file);
                                type1b.add(new VariantInfo(formatter, hex));
                            } catch (IOException e) {
                                Log.e(References.SUBSTRATUM_LOG, "There was an error parsing " +
                                        "asset " +
                                        "file!");
                                File current_file = new File(mContext.getCacheDir().
                                        getAbsoluteFile() + "/SubstratumBuilder/"
                                        + theme_pid + "/assets/overlays/"
                                        + package_identifier + "/res/values/type1b.xml");
                                String hex = References.getOverlayResource(
                                        current_file);
                                type1b.add(new VariantInfo(getString(R.string
                                        .overlays_variant_default_1b), hex));
                            }
                        } else {
                            File current_file = new File(mContext.getCacheDir().
                                    getAbsoluteFile() + "/SubstratumBuilder/"
                                    + theme_pid + "/assets/overlays/"
                                    + package_identifier + "/res/values/type1b.xml");
                            String hex = References.getOverlayResource(
                                    current_file);
                            type1b.add(new VariantInfo(getString(R.string
                                    .overlays_variant_default_1b), hex));
                        }

                        if (typeArray.contains("type1c")) {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(new FileInputStream(
                                            new File(typeArrayRaw.getAbsolutePath() +
                                                    "/type1c"))))) {
                                String formatter = String.format(getString(R.string
                                        .overlays_variant_substitute), reader.readLine());
                                File current_file = new File(mContext.getCacheDir().
                                        getAbsoluteFile() + "/SubstratumBuilder/"
                                        + theme_pid + "/assets/overlays/"
                                        + package_identifier + "/res/values/type1c.xml");
                                String hex = References.getOverlayResource(
                                        current_file);
                                type1c.add(new VariantInfo(formatter, hex));
                            } catch (IOException e) {
                                Log.e(References.SUBSTRATUM_LOG, "There was an error parsing " +
                                        "asset file!");
                                File current_file = new File(mContext.getCacheDir().
                                        getAbsoluteFile() + "/SubstratumBuilder/"
                                        + theme_pid + "/assets/overlays/"
                                        + package_identifier + "/res/values/type1c.xml");
                                String hex = References.getOverlayResource(
                                        current_file);
                                type1c.add(new VariantInfo(getString(R.string
                                        .overlays_variant_default_1c), hex));
                            }
                        } else {
                            File current_file = new File(mContext.getCacheDir().
                                    getAbsoluteFile() + "/SubstratumBuilder/"
                                    + theme_pid + "/assets/overlays/"
                                    + package_identifier + "/res/values/type1c.xml");
                            String hex = References.getOverlayResource(
                                    current_file);
                            type1c.add(new VariantInfo(getString(R.string
                                    .overlays_variant_default_1c), hex));
                        }

                        if (typeArray.contains("type2")) {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(new FileInputStream(
                                            new File(typeArrayRaw.getAbsolutePath() +
                                                    "/type2"))))) {
                                String formatter = String.format(getString(R.string
                                        .overlays_variant_substitute), reader.readLine());
                                type2.add(formatter);
                            } catch (IOException e) {
                                Log.e(References.SUBSTRATUM_LOG, "There was an error parsing " +
                                        "asset " +
                                        "file!");
                                type2.add(getString(R.string
                                        .overlays_variant_default_2));
                            }
                        } else {
                            type2.add(getString(R.string.overlays_variant_default_2));
                        }

                        if (typeArray.size() > 1) {
                            for (int i = 0; i < typeArray.size(); i++) {
                                String current = typeArray.get(i);
                                if (!current.equals("res")) {
                                    if (current.contains(".xml")) {
                                        if (current.substring(0, 7).equals("type1a_")) {
                                            File current_file = new File(mContext.getCacheDir().
                                                    getAbsoluteFile() + "/SubstratumBuilder/"
                                                    + theme_pid + "/assets/overlays/"
                                                    + package_identifier + "/" + current);
                                            String hex = References.getOverlayResource(
                                                    current_file);
                                            type1a.add(new VariantInfo(current.substring
                                                    (7, current.length() - 4), hex));
                                        }
                                        if (current.substring(0, 7).equals("type1b_")) {
                                            File current_file = new File(mContext.getCacheDir().
                                                    getAbsoluteFile() + "/SubstratumBuilder/"
                                                    + theme_pid + "/assets/overlays/"
                                                    + package_identifier + "/" + current);
                                            String hex = References.getOverlayResource(
                                                    current_file);
                                            type1b.add(new VariantInfo(current.substring
                                                    (7, current.length() - 4), hex));
                                        }
                                        if (current.substring(0, 7).equals("type1c_")) {
                                            File current_file = new File(mContext.getCacheDir().
                                                    getAbsoluteFile() + "/SubstratumBuilder/"
                                                    + theme_pid + "/assets/overlays/"
                                                    + package_identifier + "/" + current);
                                            String hex = References.getOverlayResource(
                                                    current_file);
                                            type1c.add(new VariantInfo(current.substring
                                                    (7, current.length() - 4), hex));
                                        }
                                    } else {
                                        if (!current.contains(".")) {
                                            if (current.length() > 5) {
                                                if (current.substring(0, 6).equals("type2_")) {
                                                    type2.add(current.substring(6));
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            VariantsAdapter adapter1 = new VariantsAdapter(getActivity(), type1a);
                            VariantsAdapter adapter2 = new VariantsAdapter(getActivity(), type1b);
                            VariantsAdapter adapter3 = new VariantsAdapter(getActivity(), type1c);
                            ArrayAdapter<String> adapter4 = new ArrayAdapter<>(getActivity(),
                                    android.R.layout.simple_spinner_dropdown_item, type2);

                            boolean adapterOneChecker = type1a.size() == 1;
                            boolean adapterTwoChecker = type1b.size() == 1;
                            boolean adapterThreeChecker = type1c.size() == 1;
                            boolean adapterFourChecker = type2.size() == 1;

                            OverlaysInfo overlaysInfo = new OverlaysInfo(parse2_themeName,
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
                            values2.add(overlaysInfo);
                        } else {
                            // At this point, there is no spinner adapter, so it should be null
                            OverlaysInfo overlaysInfo = new OverlaysInfo(parse2_themeName,
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
                            values2.add(overlaysInfo);
                        }
                    } catch (Exception e) {
                        // Exception
                    }
                } catch (Exception e) {
                    // Exception
                }
            }
            return null;
        }
    }

    private class Phase2_InitializeCache extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            final_runner = new ArrayList<>();

            if (!enable_mode && !disable_mode) {
                Log.d("SubstratumBuilder", "Decompiling and initializing work area with the " +
                        "selected theme's assets...");
                int notification_priority = 2; // PRIORITY_MAX == 2

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
                mProgressDialog = new ProgressDialog(getActivity(), R.style
                        .SubstratumBuilder_ActivityTheme);
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                mProgressDialog.setContentView(R.layout.compile_dialog_loader);
                if (mProgressDialog.getWindow() != null) mProgressDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                final float radius = 5;
                final View decorView = getActivity().getWindow().getDecorView();
                final View rootView = decorView.findViewById(android.R.id.content);
                final Drawable windowBackground = decorView.getBackground();

                BlurView blurView = (BlurView) mProgressDialog.findViewById(R.id.blurView);

                if (rootView != null) {
                    blurView.setupWith(rootView)
                            .windowBackground(windowBackground)
                            .blurAlgorithm(new RenderScriptBlur(mContext, true))
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
                // Initialize Substratum cache with theme
                if (!has_initialized_cache) {
                    sb = new SubstratumBuilder();

                    File versioning = new File(mContext.getCacheDir().getAbsoluteFile() +
                            "/SubstratumBuilder/" + theme_pid + "/substratum.xml");
                    if (versioning.exists()) {
                        has_initialized_cache = true;
                    } else {
                        new CacheCreator().initializeCache(mContext, theme_pid);
                        has_initialized_cache = true;
                    }
                } else {
                    Log.d("SubstratumBuilder", "Work area is ready with decompiled assets " +
                            "already!");
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
            Log.d("Phase 3", "This phase has started it's asynchronous task.");

            has_failed = false;
            fail_count = 0;
            error_logs = "";

            if (!enable_mode && !disable_mode) {
                // Change title in preparation for loop to change subtext
                if (checkActiveNotifications()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        mBuilder.setContentTitle(getString(R.string
                                .notification_processing_n))
                                .setProgress(100, 0, false);
                    } else {
                        mBuilder.setContentTitle(getString(R.string
                                .notification_compiling_signing_installing))
                                .setContentText(getString(
                                        R.string.notification_extracting_assets_text))
                                .setProgress(100, 0, false);
                    }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                dialogProgress.setProgress((int) progress, true);
            } else {
                dialogProgress.setProgress((int) progress);
            }
        }

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
                        if (targetOverlay.equals("com.android.systemui")) {
                            String packageName = checkedOverlays.get(i).getFullOverlayParameters();
                            if (ThemeManager.isOverlayEnabled(packageName)) {
                                ThemeManager.restartSystemUI(mContext);
                                break;
                            }
                        }
                    }
                }
                mContext.unregisterReceiver(finishReceiver);
            } else if (enable_mode) {
                if (final_runner.size() > 0) {
                    enable_mode = false;

                    if (mixAndMatchMode) {
                        // Buffer the disableBeforeEnabling String
                        ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                        if (all_installed_overlays.size() - current_theme_overlays.size() != 0) {
                            for (int i = 0; i < all_installed_overlays.size(); i++) {
                                if (!current_theme_overlays.contains(
                                        all_installed_overlays.get(i))) {
                                    disableBeforeEnabling.add(all_installed_overlays.get(i));
                                }
                            }
                        }
                        progressBar.setVisibility(View.VISIBLE);
                        if (toggle_all.isChecked()) toggle_all.setChecked(false);
                        ThemeManager.disableOverlay(mContext, disableBeforeEnabling);
                        ThemeManager.enableOverlay(mContext, final_command);
                    } else {
                        progressBar.setVisibility(View.VISIBLE);
                        if (toggle_all.isChecked()) toggle_all.setChecked(false);
                        ThemeManager.enableOverlay(mContext, final_command);
                    }

                    progressBar.setVisibility(View.GONE);
                    if (needsRecreate()) {
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
                    compile_enable_mode = false;
                    enable_mode = false;
                    Toast toast = Toast.makeText(mContext, getString(R
                                    .string.toast_disabled3),
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            } else {
                if (final_runner.size() > 0) {
                    ArrayList<String> disableBeforeEnabling = new ArrayList<>();
                    if (mixAndMatchMode) {
                        if (all_installed_overlays.size() -
                                current_theme_overlays.size() != 0) {
                            for (int i = 0; i < all_installed_overlays.size(); i++) {
                                if (!current_theme_overlays.contains(
                                        all_installed_overlays.get(i))) {
                                    disableBeforeEnabling.add(all_installed_overlays.get(i));
                                }
                            }
                        }
                    }
                    disable_mode = false;

                    if (mixAndMatchMode) {
                        progressBar.setVisibility(View.VISIBLE);
                        if (toggle_all.isChecked()) toggle_all.setChecked(false);
                        ThemeManager.disableOverlay(mContext, disableBeforeEnabling);
                        ThemeManager.enableOverlay(mContext, final_command);
                    } else {
                        progressBar.setVisibility(View.VISIBLE);
                        if (toggle_all.isChecked()) toggle_all.setChecked(false);
                        ThemeManager.disableOverlay(mContext, final_command);
                    }

                    progressBar.setVisibility(View.GONE);
                    if (needsRecreate()) {
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
                    disable_mode = false;
                    Toast toast = Toast.makeText(mContext, getString(R
                                    .string.toast_disabled4),
                            Toast.LENGTH_SHORT);
                    toast.show();
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
                        .setPositiveButton(android.R.string.ok, (dialog, id12) -> ElevatedCommands
                                .reboot());
                alertDialogBuilder
                        .setNegativeButton(R.string.remove_dialog_later, (dialog, id1) -> {
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

        @Override
        protected String doInBackground(String... sUrl) {

            if (mixAndMatchMode && !References.checkOMS(mContext)) {
                String current_directory;
                if (References.inNexusFilter()) {
                    current_directory = "/system/overlay/";
                } else {
                    current_directory = "/system/vendor/overlay/";
                }
                File file = new File(current_directory);
                if (file.exists()) {
                    FileOperations.mountRW();
                    FileOperations.delete(mContext, current_directory);
                }
            }

            // Enable listener
            if (finishReceiver == null) finishReceiver = new FinishReceiver();
            IntentFilter intentFilter = new IntentFilter(
                    "masquerade.substratum.STATUS_CHANGED");
            mContext.registerReceiver(finishReceiver, intentFilter);

            total_amount = checkedOverlays.size();
            for (int i = 0; i < checkedOverlays.size(); i++) {
                current_amount = i + 1;
                String theme_name_parsed = theme_name.replaceAll("\\s+", "").replaceAll
                        ("[^a-zA-Z0-9]+", "");
                String current_overlay = checkedOverlays.get(i).getPackageName();
                current_dialog_overlay = "'" + References.grabPackageName(
                        mContext, current_overlay) + "'";

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
                        } else {
                            if (References.allowedSettingsOverlay(current_overlay)) {
                                switch (current_overlay) {
                                    case "com.android.settings.icons":
                                        packageTitle = getString(R.string.settings_icons);
                                        break;
                                }
                            } else {
                                ApplicationInfo applicationInfo = mContext.getPackageManager()
                                        .getApplicationInfo
                                                (current_overlay, 0);
                                packageTitle = mContext.getPackageManager().getApplicationLabel
                                        (applicationInfo).toString();
                            }
                        }

                        // Initialize working notification

                        if (checkActiveNotifications()) {
                            mBuilder.setProgress(100, (int) (((double) (i + 1) / checkedOverlays
                                    .size()) * 100), false);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                mBuilder.setContentText("\"" + packageTitle + "\"");
                            } else {
                                mBuilder.setContentText(getString(R.string
                                        .notification_processing) +
                                        " " +
                                        "\"" +
                                        packageTitle + "\"");
                            }
                            mNotifyManager.notify(id, mBuilder.build());
                        }

                        String workingDirectory = mContext.getCacheDir().getAbsolutePath() +
                                "/SubstratumBuilder/" + theme_pid +
                                "/assets/overlays/" + current_overlay;

                        if (!References.checkOMS(mContext)) {
                            File check_legacy = new File(mContext.getCacheDir()
                                    .getAbsolutePath() + "/SubstratumBuilder/" +
                                    theme_pid + "/assets/overlays_legacy/" +
                                    current_overlay);
                            if (check_legacy.exists()) {
                                workingDirectory = check_legacy.getAbsolutePath();
                            }
                        }

                        File srcDir = new File(workingDirectory +
                                ((sUrl[0].length() != 0) ? "/type3_" + sUrl[0] : "/res"));
                        File destDir = new File(workingDirectory + "/workdir");
                        if (destDir.exists()) {
                            FileOperations.delete(mContext, destDir.getAbsolutePath());
                        }
                        FileUtils.copyDirectory(srcDir, destDir);

                        if (checkedOverlays.get(i).is_variant_chosen || sUrl[0].length() != 0) {
                            // Type 1a
                            if (checkedOverlays.get(i).is_variant_chosen1) {
                                String sourceLocation = workingDirectory + "/type1a_" +
                                        checkedOverlays.get(i).getSelectedVariantName() + ".xml";

                                String targetLocation = workingDirectory +
                                        "/workdir/values/type1a.xml";

                                Log.d("SubstratumBuilder", "You have selected variant file \"" +
                                        checkedOverlays.get(i).getSelectedVariantName() + "\"");
                                Log.d("SubstratumBuilder", "Moving variant file to: " +
                                        targetLocation);
                                FileOperations.copy(mContext, sourceLocation, targetLocation);
                            }

                            // Type 1b
                            if (checkedOverlays.get(i).is_variant_chosen2) {
                                String sourceLocation2 = workingDirectory + "/type1b_" +
                                        checkedOverlays.get(i).getSelectedVariantName2() + ".xml";

                                String targetLocation2 = workingDirectory +
                                        "/workdir/values/type1b.xml";

                                Log.d("SubstratumBuilder", "You have selected variant file \"" +
                                        checkedOverlays.get(i).getSelectedVariantName2() + "\"");
                                Log.d("SubstratumBuilder", "Moving variant file to: " +
                                        targetLocation2);
                                FileOperations.copy(mContext, sourceLocation2, targetLocation2);
                            }
                            // Type 1c
                            if (checkedOverlays.get(i).is_variant_chosen3) {
                                String sourceLocation3 = workingDirectory + "/type1c_" +
                                        checkedOverlays.get(i).getSelectedVariantName3() + ".xml";

                                String targetLocation3 = workingDirectory +
                                        "/workdir/values/type1c.xml";

                                Log.d("SubstratumBuilder", "You have selected variant file \"" +
                                        checkedOverlays.get(i).getSelectedVariantName3() + "\"");
                                Log.d("SubstratumBuilder", "Moving variant file to: " +
                                        targetLocation3);

                                FileOperations.copy(mContext, sourceLocation3, targetLocation3);
                            }

                            String packageName =
                                    (checkedOverlays.get(i).is_variant_chosen1 ? checkedOverlays
                                            .get(i).getSelectedVariantName() : "") +
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
                                Log.d("PackageProcessor", "Currently processing package" +
                                        " \"" + checkedOverlays.get(i).getFullOverlayParameters() +
                                        "\"...");

                                if (sUrl[0].length() != 0) {
                                    sb = new SubstratumBuilder();
                                    sb.beginAction(mContext, theme_pid, current_overlay,
                                            theme_name,
                                            packageName,
                                            checkedOverlays.get(i).getSelectedVariantName4(),
                                            sUrl[0],
                                            versionName,
                                            References.checkOMS(mContext),
                                            theme_pid);
                                } else {
                                    sb = new SubstratumBuilder();
                                    sb.beginAction(mContext, theme_pid, current_overlay,
                                            theme_name,
                                            packageName,
                                            checkedOverlays.get(i).getSelectedVariantName4(),
                                            null,
                                            versionName,
                                            References.checkOMS(mContext),
                                            theme_pid);
                                }
                            } else {
                                Log.d("PackageProcessor", "Currently processing package" +
                                        " \"" + checkedOverlays.get(i).getFullOverlayParameters() +
                                        "\"...");

                                if (sUrl[0].length() != 0) {
                                    sb = new SubstratumBuilder();
                                    sb.beginAction(mContext, theme_pid, current_overlay,
                                            theme_name,
                                            packageName,
                                            null,
                                            sUrl[0],
                                            versionName,
                                            References.checkOMS(mContext),
                                            theme_pid);
                                } else {
                                    sb = new SubstratumBuilder();
                                    sb.beginAction(mContext, theme_pid, current_overlay,
                                            theme_name,
                                            packageName,
                                            null,
                                            null,
                                            versionName,
                                            References.checkOMS(mContext),
                                            theme_pid);
                                }
                            }
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
                                    final_runner.add(sb.no_install);
                                } else if (References.checkThemeInterface(mContext)) {
                                    // Thread wait
                                    isWaiting = true;
                                    do {
                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    } while (isWaiting);
                                }
                            }
                        } else {
                            Log.d("SubstratumBuilder", "Currently processing package" +
                                    " \"" + current_overlay + "." + theme_name_parsed + "\"...");
                            sb = new SubstratumBuilder();
                            sb.beginAction(mContext,
                                    theme_pid,
                                    current_overlay,
                                    theme_name,
                                    null,
                                    null,
                                    null,
                                    versionName,
                                    References.checkOMS(mContext),
                                    theme_pid);

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
                                    final_runner.add(sb.no_install);
                                } else if (References.checkThemeInterface(mContext)) {
                                    // Thread wait
                                    isWaiting = true;
                                    do {
                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    } while (isWaiting);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(References.SUBSTRATUM_LOG, "Main function has unexpectedly stopped!");
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
                Log.d(References.SUBSTRATUM_LOG, "Don't you wait no more!");
                isWaiting = false;
            }
        }
    }
}