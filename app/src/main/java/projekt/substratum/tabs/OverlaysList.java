package projekt.substratum.tabs;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.gordonwong.materialsheetfab.MaterialSheetFab;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.InformationActivityTabs;
import projekt.substratum.R;
import projekt.substratum.adapters.OverlaysAdapter;
import projekt.substratum.model.OverlaysInfo;
import projekt.substratum.util.FloatingActionMenu;
import projekt.substratum.util.ReadOverlaysFile;
import projekt.substratum.util.Root;
import projekt.substratum.util.SubstratumBuilder;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class OverlaysList extends Fragment {

    private SubstratumBuilder sb;
    private List<OverlaysInfo> overlaysLists, checkedOverlays;
    private RecyclerView.Adapter mAdapter;
    private String theme_name, theme_pid, versionName;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private boolean has_initialized_cache = false;
    private int id = 1;
    private ViewGroup root;
    private ArrayList<OverlaysInfo> values2;
    private RecyclerView mRecyclerView;
    private Spinner base_spinner;
    private SharedPreferences prefs;
    private boolean mixAndMatchMode;
    private ArrayList<String> final_runner;
    private boolean enable_mode, disable_mode;
    private ArrayList<String> all_installed_overlays;
    private Context mContext;
    private Switch toggle_all;
    private List<ApplicationInfo> packages;

    private boolean isPackageInstalled(String package_name) {
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(package_name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_2, container, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        mContext = getContext();

        PackageManager pm = mContext.getPackageManager();
        packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        // Run through phase one - checking whether aapt exists on the device
        Phase1_AAPT_Check phase1_aapt_check = new Phase1_AAPT_Check();
        phase1_aapt_check.execute("");

        theme_name = InformationActivityTabs.getThemeName();
        theme_pid = InformationActivityTabs.getThemePID();

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        mRecyclerView = (RecyclerView) root.findViewById(R.id.overlayRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayList<OverlaysInfo> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new OverlaysAdapter(empty_array);
        mRecyclerView.setAdapter(empty_adapter);

        toggle_all = (Switch) root.findViewById(R.id.toggle_all_overlays);
        toggle_all.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
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
                    }
                });

        View sheetView = root.findViewById(R.id.fab_sheet);
        View overlay = root.findViewById(R.id.overlay);
        int sheetColor = getContext().getColor(R.color.fab_menu_background_card);
        int fabColor = getContext().getColor(R.color.colorAccent);

        final FloatingActionMenu floatingActionButton = (FloatingActionMenu) root.findViewById(R
                .id.apply_fab);
        floatingActionButton.show();

        // Create material sheet FAB
        if (sheetView != null && overlay != null) {
            new MaterialSheetFab<>(floatingActionButton, sheetView, overlay, sheetColor, fabColor);
        }

        Switch enable_swap = (Switch) root.findViewById(R.id.enable_swap);
        if (enable_swap != null) {
            if (prefs.getBoolean("enable_swapping_overlays", true)) {
                mixAndMatchMode = true;
                enable_swap.setChecked(true);
            } else {
                mixAndMatchMode = false;
                enable_swap.setChecked(false);
            }
            enable_swap.setOnCheckedChangeListener(new CompoundButton
                    .OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        prefs.edit().putBoolean("enable_swapping_overlays", true).apply();
                        mixAndMatchMode = true;
                    } else {
                        prefs.edit().putBoolean("enable_swapping_overlays", false).apply();
                        mixAndMatchMode = false;
                    }
                }
            });
        }

        TextView compile_enable_selected = (TextView) root.findViewById(R.id
                .compile_enable_selected);
        if (compile_enable_selected != null)
            compile_enable_selected.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Toast toast = Toast.makeText(getContext(),
                            getString(R.string
                                    .toast_updating),
                            Toast.LENGTH_LONG);
                    toast.show();

                    overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                    checkedOverlays = new ArrayList<>();

                    for (int i = 0; i < overlaysLists.size(); i++) {
                        OverlaysInfo currentOverlay = overlaysLists.get(i);
                        if (currentOverlay.isSelected()) {
                            checkedOverlays.add(currentOverlay);
                        }
                    }

                    if (base_spinner.getSelectedItemPosition() != 0 &&
                            base_spinner.getVisibility() == View.VISIBLE) {
                        Phase2_InitializeCache phase2_initializeCache = new
                                Phase2_InitializeCache();
                        phase2_initializeCache.execute(base_spinner.getSelectedItem().toString());
                    } else {
                        Phase2_InitializeCache phase2_initializeCache = new
                                Phase2_InitializeCache();
                        phase2_initializeCache.execute("");
                    }
                }
            });

        TextView disable_selected = (TextView) root.findViewById(R.id.disable_selected);
        if (disable_selected != null)
            disable_selected.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    disable_mode = true;

                    overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                    checkedOverlays = new ArrayList<>();

                    for (int i = 0; i < overlaysLists.size(); i++) {
                        OverlaysInfo currentOverlay = overlaysLists.get(i);
                        if (currentOverlay.isSelected()) {
                            checkedOverlays.add(currentOverlay);
                        }
                    }

                    if (base_spinner.getSelectedItemPosition() != 0 &&
                            base_spinner.getVisibility() == View.VISIBLE) {
                        Phase2_InitializeCache phase2_initializeCache = new
                                Phase2_InitializeCache();
                        phase2_initializeCache.execute(base_spinner.getSelectedItem().toString());
                    } else {
                        Phase2_InitializeCache phase2_initializeCache = new
                                Phase2_InitializeCache();
                        phase2_initializeCache.execute("");
                    }
                }
            });

        TextView enable_selected = (TextView) root.findViewById(R.id.enable_selected);
        if (enable_selected != null) enable_selected.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                enable_mode = true;

                overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                checkedOverlays = new ArrayList<>();

                for (int i = 0; i < overlaysLists.size(); i++) {
                    OverlaysInfo currentOverlay = overlaysLists.get(i);
                    if (currentOverlay.isSelected()) {
                        checkedOverlays.add(currentOverlay);
                    }
                }
                if (base_spinner.getSelectedItemPosition() != 0 &&
                        base_spinner.getVisibility() == View.VISIBLE) {
                    Phase2_InitializeCache phase2_initializeCache = new
                            Phase2_InitializeCache();
                    phase2_initializeCache.execute(base_spinner.getSelectedItem().toString());
                } else {
                    Phase2_InitializeCache phase2_initializeCache = new
                            Phase2_InitializeCache();
                    phase2_initializeCache.execute("");
                }
            }
        });

        // PLUGIN TYPE 3: Parse each overlay folder to see if they have folder options

        base_spinner = (Spinner) root.findViewById(R.id.type3_spinner);
        base_spinner.setOnItemSelectedListener(new AdapterView
                .OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int pos, long id) {
                if (pos == 0) {
                    new LoadOverlays().execute("");
                } else {
                    String[] commands = {arg0.getSelectedItem().toString()};
                    new LoadOverlays().execute(commands);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        try {
            Context otherContext = getContext().createPackageContext(theme_pid, 0);
            AssetManager am = otherContext.getAssets();

            ArrayList<String> type3 = new ArrayList<>();

            String[] stringArray = am.list("overlays/android");
            if (Arrays.asList(stringArray).contains("type3")) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(
                            new InputStreamReader(am.open("overlays/android/type3"), "UTF-8"));
                    String formatter = String.format(getString(R.string
                            .overlays_variant_substitute), reader.readLine());
                    type3.add(formatter);
                } catch (IOException e) {
                    Log.e("SubstratumLogger", "There was an error parsing asset " +
                            "file!");
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            Log.e("SubstratumLogger", "Could not read type3 file" +
                                    " properly, falling back to default string...");
                            type3.add(getString(R.string
                                    .overlays_variant_default_3));
                        }
                    }
                }
            } else {
                type3.add(getString(R.string.overlays_variant_default_3));
            }

            if (stringArray.length > 1) {
                for (int i = 0; i < stringArray.length; i++) {
                    String current = stringArray[i];
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
                    base_spinner.setVisibility(View.VISIBLE);
                    base_spinner.setAdapter(adapter1);
                } else {
                    base_spinner.setVisibility(View.GONE);
                    new LoadOverlays().execute("");
                }
            } else {
                base_spinner.setVisibility(View.GONE);
                new LoadOverlays().execute("");
            }
        } catch (Exception e) {
            if (base_spinner.getVisibility() == View.VISIBLE) base_spinner.setVisibility(View.GONE);
            Log.e("SubstratumLogger", "Could not parse list of base options for this theme!");
        }
        return root;
    }

    private class LoadOverlays extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            MaterialProgressBar materialProgressBar = (MaterialProgressBar) root.findViewById(R.id
                    .progress_bar_loader);
            if (materialProgressBar != null) materialProgressBar.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.INVISIBLE);
            toggle_all.setClickable(false);
        }

        @Override
        protected void onPostExecute(String result) {
            MaterialProgressBar materialProgressBar = (MaterialProgressBar) root.findViewById(R.id
                    .progress_bar_loader);
            if (materialProgressBar != null) materialProgressBar.setVisibility(View.GONE);
            toggle_all.setClickable(true);
            mAdapter = new OverlaysAdapter(values2);
            mRecyclerView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
            mRecyclerView.setVisibility(View.VISIBLE);
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Grab the current theme_pid's versionName so that we can version our overlays
            try {
                PackageInfo pinfo = getContext().getPackageManager().getPackageInfo(
                        theme_pid, 0);
                versionName = pinfo.versionName;
            } catch (Exception e) {
                Log.e("OverlaysList", "Window was destroyed before AsyncTask could perform " +
                        "postExecute()");
            }

            Root.runCommand("cp /data/system/overlays.xml " +
                    Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");
            String[] commands5 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "5"};

            List<String> state5 = ReadOverlaysFile.main(commands5);
            all_installed_overlays = new ArrayList<>(state5);
            List<String> state5overlays = new ArrayList<>(all_installed_overlays);

            String parse1_themeName = theme_name.replaceAll("\\s+", "");
            String parse2_themeName = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");

            for (int i = 0; i < all_installed_overlays.size(); i++) {
                try {
                    ApplicationInfo appInfo = getContext().getPackageManager().getApplicationInfo(
                            all_installed_overlays.get(i), PackageManager.GET_META_DATA);
                    if (appInfo.metaData != null) {
                        if (appInfo.metaData.getString("Substratum_Parent") != null) {
                            if (appInfo.metaData.getString("Substratum_Parent")
                                    .equals(parse2_themeName)) {
                                all_installed_overlays.remove(i);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("OverlaysList", "Window was destroyed before AsyncTask could " +
                            "perform postExecute()");
                }
            }

            ArrayList<String> unsortedList = new ArrayList<>();
            ArrayList<String> unsortedListWithNames = new ArrayList<>();
            ArrayList<String> values = new ArrayList<>();
            values2 = new ArrayList<>();

            // Buffer the initial values list so that we get the list of packages inside this theme
            try {
                values = new ArrayList<>();
                Context otherContext = getContext().createPackageContext(theme_pid, 0);
                AssetManager am = otherContext.getAssets();
                String[] am_list = am.list("overlays");
                for (String package_name : am_list) {
                    if (isPackageInstalled(package_name)) {
                        values.add(package_name);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("SubstratumLogger", "Could not refresh list of overlay folders.");
            }

            // Then let's convert all the package names to their app names
            for (int i = 0; i < values.size(); i++) {
                try {
                    ApplicationInfo applicationInfo = getContext().getPackageManager()
                            .getApplicationInfo
                                    (values.get(i), 0);
                    String packageTitle = getContext().getPackageManager().getApplicationLabel
                            (applicationInfo).toString();

                    // Organized list of packages
                    unsortedList.add(values.get(i));  // Add this to be parsed later
                    unsortedListWithNames.add(packageTitle);  // Add this to be parsed later

                    values.set(i, packageTitle);
                } catch (PackageManager.NameNotFoundException nnfe) {
                    Log.e("SubstratumLogger", "Could not find explicit package identifier in " +
                            "package manager list.");
                }
            }

            // Sort the values list
            Collections.sort(values);

            // Change the names of each of the values back into package identifiers
            for (int i = 0; i < values.size(); i++) {
                int counter = -1;
                for (int j = 0; j < unsortedList.size(); j++) {
                    if (unsortedListWithNames.get(j).equals(values.get(i))) {
                        counter = j;
                    }
                }
                if (counter > -1) {
                    values.set(i, unsortedList.get(counter));
                } else {
                    Log.e("SubstratumLogger", "Could not assign specific index \"" + values.get
                            (i) + "\" for sorted values list.");
                }
            }

            // Now let's add the new information so that the adapter can recognize custom method
            // calls
            for (String package_name : values) {
                try {
                    String parsed_name;
                    ApplicationInfo applicationInfo = getContext().getPackageManager()
                            .getApplicationInfo
                                    (package_name, 0);
                    parsed_name = getContext().getPackageManager().getApplicationLabel
                            (applicationInfo).toString();

                    try {
                        Context otherContext = getContext().createPackageContext(theme_pid, 0);
                        AssetManager am = otherContext.getAssets();

                        ArrayList<String> type1a = new ArrayList<>();
                        ArrayList<String> type1b = new ArrayList<>();
                        ArrayList<String> type1c = new ArrayList<>();
                        ArrayList<String> type2 = new ArrayList<>();

                        String[] stringArray = am.list("overlays/" + package_name);
                        if (Arrays.asList(stringArray).contains("type1a")) {
                            BufferedReader reader = null;
                            try {
                                reader = new BufferedReader(
                                        new InputStreamReader(am.open("overlays/" +
                                                package_name + "/type1a"), "UTF-8"));
                                String formatter = String.format(getString(R.string
                                        .overlays_variant_substitute), reader.readLine());
                                type1a.add(formatter);
                            } catch (IOException e) {
                                Log.e("SubstratumLogger", "There was an error parsing asset " +
                                        "file!");
                            } finally {
                                if (reader != null) {
                                    try {
                                        reader.close();
                                    } catch (IOException e) {
                                        Log.e("SubstratumLogger", "Could not read type1a file" +
                                                " properly, falling back to default string...");
                                        type1a.add(getString(R.string
                                                .overlays_variant_default_1a));
                                    }
                                }
                            }
                        } else {
                            type1a.add(getString(R.string.overlays_variant_default_1a));
                        }

                        if (Arrays.asList(stringArray).contains("type1b")) {
                            BufferedReader reader = null;
                            try {
                                reader = new BufferedReader(
                                        new InputStreamReader(am.open("overlays/" +
                                                package_name + "/type1b"), "UTF-8"));
                                String formatter = String.format(getString(R.string
                                        .overlays_variant_substitute), reader.readLine());
                                type1b.add(formatter);
                            } catch (IOException e) {
                                Log.e("SubstratumLogger", "There was an error parsing asset " +
                                        "file!");
                            } finally {
                                if (reader != null) {
                                    try {
                                        reader.close();
                                    } catch (IOException e) {
                                        Log.e("SubstratumLogger", "Could not read type1b file" +
                                                " properly, falling back to default string...");
                                        type1b.add(getString(R.string
                                                .overlays_variant_default_1b));
                                    }
                                }
                            }
                        } else {
                            type1b.add(getString(R.string.overlays_variant_default_1b));
                        }

                        if (Arrays.asList(stringArray).contains("type1c")) {
                            BufferedReader reader = null;
                            try {
                                reader = new BufferedReader(
                                        new InputStreamReader(am.open("overlays/" +
                                                package_name + "/type1c"), "UTF-8"));
                                String formatter = String.format(getString(R.string
                                        .overlays_variant_substitute), reader.readLine());
                                type1c.add(formatter);
                            } catch (IOException e) {
                                Log.e("SubstratumLogger", "There was an error parsing asset " +
                                        "file!");
                            } finally {
                                if (reader != null) {
                                    try {
                                        reader.close();
                                    } catch (IOException e) {
                                        Log.e("SubstratumLogger", "Could not read type1c file" +
                                                " properly, falling back to default string...");
                                        type1c.add(getString(R.string
                                                .overlays_variant_default_1c));
                                    }
                                }
                            }
                        } else {
                            type1c.add(getString(R.string.overlays_variant_default_1c));
                        }

                        if (Arrays.asList(stringArray).contains("type2")) {
                            BufferedReader reader = null;
                            try {
                                reader = new BufferedReader(
                                        new InputStreamReader(am.open("overlays/" +
                                                package_name + "/type2"), "UTF-8"));
                                String formatter = String.format(getString(R.string
                                        .overlays_variant_substitute), reader.readLine());
                                type2.add(formatter);
                            } catch (IOException e) {
                                Log.e("SubstratumLogger", "There was an error parsing asset " +
                                        "file!");
                            } finally {
                                if (reader != null) {
                                    try {
                                        reader.close();
                                    } catch (IOException e) {
                                        Log.e("SubstratumLogger", "Could not read type2 file " +
                                                "properly, falling back to default string...");
                                        type2.add(getString(R.string
                                                .overlays_variant_default_2));
                                    }
                                }
                            }
                        } else {
                            type2.add(getString(R.string.overlays_variant_default_2));
                        }

                        if (stringArray.length > 1) {
                            for (int i = 0; i < stringArray.length; i++) {
                                String current = stringArray[i];
                                if (!current.equals("res")) {
                                    if (current.contains(".xml")) {
                                        if (current.substring(0, 7).equals("type1a_")) {
                                            type1a.add(current.substring(7, current.length()
                                                    - 4));
                                        }
                                        if (current.substring(0, 7).equals("type1b_")) {
                                            type1b.add(current.substring(7, current.length()
                                                    - 4));
                                        }
                                        if (current.substring(0, 7).equals("type1c_")) {
                                            type1c.add(current.substring(7, current.length()
                                                    - 4));
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

                            ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getActivity(),
                                    android.R.layout.simple_spinner_dropdown_item, type1a);
                            ArrayAdapter<String> adapter2 = new ArrayAdapter<>(getActivity(),
                                    android.R.layout.simple_spinner_dropdown_item, type1b);
                            ArrayAdapter<String> adapter3 = new ArrayAdapter<>(getActivity(),
                                    android.R.layout.simple_spinner_dropdown_item, type1c);
                            ArrayAdapter<String> adapter4 = new ArrayAdapter<>(getActivity(),
                                    android.R.layout.simple_spinner_dropdown_item, type2);

                            boolean adapterOneChecker = type1a.size() == 1;
                            boolean adapterTwoChecker = type1b.size() == 1;
                            boolean adapterThreeChecker = type1c.size() == 1;
                            boolean adapterFourChecker = type2.size() == 1;

                            OverlaysInfo overlaysInfo = new OverlaysInfo(parse2_themeName,
                                    parsed_name,
                                    package_name, false,
                                    (adapterOneChecker ? null : adapter1),
                                    (adapterTwoChecker ? null : adapter2),
                                    (adapterThreeChecker ? null : adapter3),
                                    (adapterFourChecker ? null : adapter4),
                                    getContext(), versionName, sUrl[0], state5overlays);
                            values2.add(overlaysInfo);
                        } else {
                            // At this point, there is no spinner adapter, so it should be null
                            OverlaysInfo overlaysInfo = new OverlaysInfo(parse2_themeName,
                                    parsed_name,
                                    package_name, false, null, null, null, null, getContext(),
                                    versionName, sUrl[0], state5overlays);
                            values2.add(overlaysInfo);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("SubstratumLogger", "Could not properly buffer AssetManager " +
                                "listing");
                    }
                } catch (Exception e) {
                    Log.e("OverlaysList", "Window was destroyed before AsyncTask could " +
                            "perform postExecute()");
                }
            }
            return null;
        }
    }

    private class Phase1_AAPT_Check extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.d("SubstratumBuilder", "Substratum is now checking for AAPT system binary " +
                    "integrity...");
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Check whether device has AAPT installed
            SubstratumBuilder aaptCheck = new SubstratumBuilder();
            aaptCheck.injectAAPT(getContext());
            return null;
        }
    }

    private class Phase2_InitializeCache extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            final_runner = new ArrayList<>();

            if (!enable_mode && !disable_mode) {
                Log.d("SubstratumBuilder", "Decompiling and initializing work area with the " +
                        "selected " +

                        "theme's assets...");
                int notification_priority = 2; // PRIORITY_MAX == 2

                // This is the time when the notification should be shown on the user's screen
                mNotifyManager =
                        (NotificationManager) getContext().getSystemService(
                                Context.NOTIFICATION_SERVICE);
                mBuilder = new NotificationCompat.Builder(getContext());
                mBuilder.setContentTitle(getString(R.string.notification_initial_title))
                        .setProgress(100, 0, true)
                        .setSmallIcon(android.R.drawable.ic_popup_sync)
                        .setPriority(notification_priority)
                        .setOngoing(true);
                mNotifyManager.notify(id, mBuilder.build());
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
                    sb.initializeCache(getContext(), theme_pid);
                    has_initialized_cache = true;
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

            if (!enable_mode && !disable_mode) {
                // Change title in preparation for loop to change subtext
                mBuilder.setContentTitle(getString(R.string
                        .notification_compiling_signing_installing))
                        .setContentText(getString(R.string.notification_extracting_assets_text))
                        .setProgress(100, 0, false);
                mNotifyManager.notify(id, mBuilder.build());
            }
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            String final_commands = "";
            if (enable_mode) {
                final_commands = "om enable ";
            } else {
                if (disable_mode) {
                    final_commands = "om disable ";
                }
            }
            for (int i = 0; i < final_runner.size(); i++) {
                final_commands = final_commands + final_runner.get(i) + " ";
            }
            Log.e("finalCommands", final_commands);

            if (!enable_mode && !disable_mode) {
                Intent notificationIntent = new Intent();
                notificationIntent.putExtra("theme_name", theme_name);
                notificationIntent.putExtra("theme_pid", theme_pid);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);

                PendingIntent intent =
                        PendingIntent.getActivity(getActivity(), 0, notificationIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT);

                // Closing off the persistent notification
                mBuilder.setAutoCancel(true);
                mBuilder.setProgress(0, 0, false);
                mBuilder.setOngoing(false);
                mBuilder.setContentIntent(intent);
                mBuilder.setSmallIcon(R.drawable.notification_success_icon);
                mBuilder.setContentTitle(getString(R.string.notification_done_title));
                mBuilder.setContentText(getString(R.string.notification_no_errors_found));
                mBuilder.getNotification().flags |= Notification.FLAG_AUTO_CANCEL;
                mNotifyManager.notify(id, mBuilder.build());

                Toast toast = Toast.makeText(getContext(), getString(R
                                .string.toast_compiled_updated),
                        Toast.LENGTH_SHORT);
                toast.show();

                if (final_commands.length() == 0) {
                    if (base_spinner.getSelectedItemPosition() == 0) {
                        mAdapter.notifyDataSetChanged();
                    } else {
                        mAdapter.notifyDataSetChanged();
                    }
                } else {
                    mAdapter.notifyDataSetChanged();
                    Root.runCommand(final_commands);
                }

            } else {
                if (enable_mode) {
                    if (final_commands.length() > 0) {
                        String disableBeforeEnabling = "om disable ";
                        if (mixAndMatchMode) {
                            for (int i = 0; i < all_installed_overlays.size(); i++) {
                                disableBeforeEnabling = disableBeforeEnabling +
                                        all_installed_overlays.get(i) + " ";
                            }
                        }

                        Toast toast = Toast.makeText(getContext(), getString(R
                                        .string.toast_enabled),
                                Toast.LENGTH_SHORT);
                        toast.show();
                        enable_mode = false;
                        if (mixAndMatchMode) {
                            Root.runCommand(disableBeforeEnabling +
                                    " && " + final_commands);
                        } else {
                            Root.runCommand(final_commands);
                        }
                    } else {
                        Toast toast = Toast.makeText(getContext(), getString(R
                                        .string.toast_disabled3),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }
                } else {
                    if (final_commands.length() > 0) {
                        String disableBeforeEnabling = "om disable ";
                        if (mixAndMatchMode) {
                            for (int i = 0; i < all_installed_overlays.size(); i++) {
                                disableBeforeEnabling = disableBeforeEnabling +
                                        all_installed_overlays.get(i) + " ";
                            }
                        }
                        Toast toast = Toast.makeText(getContext(), getString(R
                                        .string.toast_disabled),
                                Toast.LENGTH_SHORT);
                        toast.show();
                        disable_mode = false;
                        if (mixAndMatchMode) {
                            Root.runCommand(disableBeforeEnabling +
                                    " && " + final_commands);
                        } else {
                            Root.runCommand(final_commands);
                        }
                    } else {
                        Toast toast = Toast.makeText(getContext(), getString(R
                                        .string.toast_disabled4),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {

            for (int i = 0; i < checkedOverlays.size(); i++) {
                String theme_name_parsed = theme_name.replaceAll("\\s+", "").replaceAll
                        ("[^a-zA-Z0-9]+", "");
                String current_overlay = checkedOverlays.get(i).getPackageName();

                if (!enable_mode && !disable_mode) {
                    try {
                        ApplicationInfo applicationInfo = getContext().getPackageManager()
                                .getApplicationInfo(current_overlay, 0);
                        String packageTitle = getContext().getPackageManager().getApplicationLabel
                                (applicationInfo).toString();

                        // Initialize working notification

                        mBuilder.setProgress(100, (int) (((double) (i + 1) / checkedOverlays.size
                                ()) *

                                100), false);
                        mBuilder.setContentText(getString(R.string.notification_processing) + " " +
                                "\"" +
                                packageTitle + "\"");
                        mNotifyManager.notify(id, mBuilder.build());

                        // With OMS3, overlay updating causes a configChange to happen, so we
                        // check for
                        // whatever is activated first and delay their installs to a one liner

                        Root.runCommand("cp /data/system/overlays.xml " +
                                Environment
                                        .getExternalStorageDirectory().getAbsolutePath() +
                                "/.substratum/current_overlays.xml");
                        String[] commands1 = {Environment.getExternalStorageDirectory()
                                .getAbsolutePath() +
                                "/.substratum/current_overlays.xml", "5"};

                        List<String> state5 = ReadOverlaysFile.main(commands1);
                        ArrayList<String> activated_overlays = new ArrayList<>(state5);
                        if (activated_overlays.size() > 0) {
                            Log.d("SubstratumLogger", "There are activated overlays in this " +
                                    "current " +
                                    "device set up, so we will cherry pick whatever is enabled " +
                                    "from " +
                                    "this theme...");
                        }
                        ArrayList<String> activated_overlays_from_theme = new ArrayList<>();
                        for (int j = 0; j < activated_overlays.size(); j++) {
                            try {
                                String current = activated_overlays.get(j);
                                ApplicationInfo appInfo = getContext().getPackageManager()
                                        .getApplicationInfo(
                                                current, PackageManager.GET_META_DATA);
                                if (appInfo.metaData != null) {
                                    if (appInfo.metaData.getString("Substratum_Parent") != null) {
                                        if (appInfo.metaData.getString("Substratum_Parent")
                                                .equals(theme_name_parsed)) {
                                            activated_overlays_from_theme.add(current);
                                        }
                                    }
                                }
                            } catch (PackageManager.NameNotFoundException nnfe) {
                                Log.e("SubstratumLogger", "Could not find explicit package for " +
                                        "this overlay...");
                            }
                        }

                        if (checkedOverlays.get(i).is_variant_chosen || sUrl[0].length() != 0) {
                            String workingDirectory = getContext().getCacheDir().toString() +
                                    "/SubstratumBuilder/assets/overlays/" +
                                    current_overlay;

                            // Type 1a
                            if (checkedOverlays.get(i).is_variant_chosen1) {
                                String sourceLocation = workingDirectory + "/type1a_" +
                                        checkedOverlays.get(i).getSelectedVariantName() + ".xml";

                                String targetLocation = workingDirectory +
                                        "/res/values/type1a.xml";
                                String targetLocation1a = workingDirectory + "/type3_" + sUrl[0] +
                                        "/values/";

                                File checker = new File(targetLocation1a);
                                if (checker.exists()) {
                                    targetLocation = targetLocation1a + "type1a.xml";
                                }

                                Log.d("SubstratumBuilder", "You have selected variant file \"" +
                                        checkedOverlays.get(i).getSelectedVariantName() + "\"");
                                Log.d("SubstratumBuilder", "Moving variant file to: " +
                                        targetLocation);

                                Root.runCommand(
                                        "mv -f " + sourceLocation + " " + targetLocation);
                            }

                            // Type 1b
                            if (checkedOverlays.get(i).is_variant_chosen2) {
                                String sourceLocation2 = workingDirectory + "/type1b_" +
                                        checkedOverlays.get(i).getSelectedVariantName2() + ".xml";

                                String targetLocation2 = workingDirectory +
                                        "/res/values/type1b.xml";
                                String targetLocation1b = workingDirectory + "/type3_" + sUrl[0] +
                                        "/values/";

                                File checker = new File(targetLocation1b);
                                if (checker.exists()) {
                                    targetLocation2 = targetLocation1b + "type1b.xml";
                                    ;
                                }

                                Log.d("SubstratumBuilder", "You have selected variant file \"" +
                                        checkedOverlays.get(i).getSelectedVariantName2() + "\"");
                                Log.d("SubstratumBuilder", "Moving variant file to: " +
                                        targetLocation2);

                                Root.runCommand(
                                        "mv -f " + sourceLocation2 + " " + targetLocation2);
                            }
                            // Type 1c
                            if (checkedOverlays.get(i).is_variant_chosen3) {
                                String sourceLocation3 = workingDirectory + "/type1c_" +
                                        checkedOverlays.get(i).getSelectedVariantName3() + ".xml";

                                String targetLocation3 = workingDirectory +
                                        "/res/values/type1c.xml";
                                String targetLocation1c = workingDirectory + "/type3_" + sUrl[0] +
                                        "/values/";

                                File checker = new File(targetLocation1c);
                                if (checker.exists()) {
                                    targetLocation3 = targetLocation1c + "type1c.xml";
                                    ;
                                }

                                Log.d("SubstratumBuilder", "You have selected variant file \"" +
                                        checkedOverlays.get(i).getSelectedVariantName3() + "\"");
                                Log.d("SubstratumBuilder", "Moving variant file to: " +
                                        targetLocation3);

                                Root.runCommand(
                                        "mv -f " + sourceLocation3 + " " + targetLocation3);
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

                            String update_bool = "true";
                            if (activated_overlays_from_theme.size() > 0) {
                                for (int j = 0; j < activated_overlays_from_theme.size(); j++) {
                                    if (activated_overlays_from_theme.get(j).equals
                                            (current_overlay +
                                                    "." + theme_name_parsed)) {
                                        Log.d("SubstratumLogger", "The flag to update this " +
                                                "overlay " +
                                                "has been triggered.");
                                        update_bool = "false";
                                    }
                                }
                            }

                            if (checkedOverlays.get(i).is_variant_chosen4) {
                                packageName = (packageName + checkedOverlays.get(i)
                                        .getSelectedVariantName4()).replaceAll("\\s+", "")
                                        .replaceAll("[^a-zA-Z0-9]+", "");
                                Log.d("PackageProcessor", "Currently processing package" +
                                        " \"" + checkedOverlays.get(i).getFullOverlayParameters() +
                                        "\"...");

                                if (sUrl[0].length() != 0) {
                                    sb = new SubstratumBuilder();
                                    sb.beginAction(getContext(), current_overlay, theme_name,
                                            update_bool,
                                            packageName, checkedOverlays.get(i)
                                                    .getSelectedVariantName4(), sUrl[0],
                                            versionName);
                                } else {
                                    sb = new SubstratumBuilder();
                                    sb.beginAction(getContext(), current_overlay, theme_name,
                                            update_bool,
                                            packageName, checkedOverlays.get(i)
                                                    .getSelectedVariantName4(), null,
                                            versionName);
                                }
                            } else {
                                Log.d("PackageProcessor", "Currently processing package" +
                                        " \"" + checkedOverlays.get(i).getFullOverlayParameters() +
                                        "\"...");

                                if (sUrl[0].length() != 0) {
                                    sb = new SubstratumBuilder();
                                    sb.beginAction(getContext(), current_overlay, theme_name,
                                            update_bool,
                                            packageName, null, sUrl[0],
                                            versionName);
                                } else {
                                    sb = new SubstratumBuilder();
                                    sb.beginAction(getContext(), current_overlay, theme_name,
                                            update_bool,
                                            packageName, null, null,
                                            versionName);
                                }
                            }
                            if (update_bool.equals("false")) {
                                final_runner.add(sb.no_install);
                            }
                        } else {
                            String update_bool = "true";
                            if (activated_overlays_from_theme.size() > 0) {
                                for (int j = 0; j < activated_overlays_from_theme.size(); j++) {
                                    if (activated_overlays_from_theme.get(j).equals
                                            (current_overlay +
                                                    "." + theme_name_parsed)) {
                                        Log.d("SubstratumLogger", "The flag to update this " +
                                                "overlay " +
                                                "has been triggered.");
                                        update_bool = "false";
                                    }
                                }
                            }

                            Log.d("SubstratumBuilder", "Currently processing package" +
                                    " \"" + current_overlay + "." + theme_name_parsed + "\"...");
                            sb = new SubstratumBuilder();
                            sb.beginAction(getContext(), current_overlay, theme_name, update_bool,
                                    null, null, null, versionName);

                            if (update_bool.equals("false")) {
                                final_runner.add(sb.no_install);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("SubstratumLogger", "Main function has unexpectedly stopped!");
                    }
                } else {
                    if (final_runner == null) final_runner = new ArrayList<>();
                    if (enable_mode) {
                        String package_name = checkedOverlays.get(i).getFullOverlayParameters();
                        if (isPackageInstalled(package_name))
                            final_runner.add(package_name);
                    } else {
                        if (disable_mode) {
                            String package_name = checkedOverlays.get(i).getFullOverlayParameters();
                            if (isPackageInstalled(package_name))
                                final_runner.add(package_name);
                        }
                    }
                }
            }
            return null;
        }
    }
}