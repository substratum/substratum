package projekt.substratum.tabs;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.InformationActivityTabs;
import projekt.substratum.R;
import projekt.substratum.adapters.OverlaysAdapter;
import projekt.substratum.model.OverlaysInfo;
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
    private int pluginType;
    private ViewGroup root;
    private ArrayList<OverlaysInfo> values2;
    private RecyclerView mRecyclerView;

    private boolean isPackageInstalled(Context context, String package_name) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_2, container, false);

        Button btnSelection = (Button) root.findViewById(R.id.btnShow);

        // Run through phase one - checking whether aapt exists on the device
        Phase1_AAPT_Check phase1_aapt_check = new Phase1_AAPT_Check();
        phase1_aapt_check.execute("");

        theme_name = InformationActivityTabs.getThemeName();
        theme_pid = InformationActivityTabs.getThemePID();
        pluginType = InformationActivityTabs.getThemeMode();

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        mRecyclerView = (RecyclerView) root.findViewById(R.id.overlayRecyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayList<OverlaysInfo> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new OverlaysAdapter(empty_array);
        mRecyclerView.setAdapter(empty_adapter);

        // Buffer the recyclerView for the information required
        LoadOverlays loadOverlays = new LoadOverlays();
        loadOverlays.execute("");

        btnSelection.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                overlaysLists = ((OverlaysAdapter) mAdapter).getOverlayList();
                checkedOverlays = new ArrayList<>();

                for (int i = 0; i < overlaysLists.size(); i++) {
                    OverlaysInfo currentOverlay = overlaysLists.get(i);
                    if (currentOverlay.isSelected()) {
                        checkedOverlays.add(currentOverlay);
                    }
                }
                Phase2_InitializeCache phase2_initializeCache = new
                        Phase2_InitializeCache();
                phase2_initializeCache.execute("");
            }
        });
        return root;
    }

    private class LoadOverlays extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            MaterialProgressBar materialProgressBar = (MaterialProgressBar) root.findViewById(R.id
                    .progress_bar_loader);
            if (materialProgressBar != null) materialProgressBar.setVisibility(View.GONE);

            mAdapter = new OverlaysAdapter(values2);
            mRecyclerView.setAdapter(mAdapter);
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Grab the current theme_pid's versionName so that we can version our overlays
            try {
                PackageInfo pinfo = getContext().getPackageManager().getPackageInfo(
                        theme_pid, 0);
                versionName = pinfo.versionName;
            } catch (PackageManager.NameNotFoundException nnfe) {
                Log.e("SubstratumLogger", "Could not find explicit package identifier in " +
                        "package manager list.");
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
                    if (isPackageInstalled(getContext(), package_name)) {
                        values.add(package_name);
                    }
                }
            } catch (Exception e) {
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

                    String parse1_themeName = theme_name.replaceAll("\\s+", "");
                    String parse2_themeName = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");

                    // PLUGIN TYPE 1: Parse each overlay folder to see if they have xml options
                    // To use this from a themer's POV, please add Substratum_Mode = XML into
                    // Manifest

                    if (pluginType <= 1) {
                        try {
                            Context otherContext = getContext().createPackageContext(theme_pid, 0);
                            AssetManager am = otherContext.getAssets();

                            ArrayList<String> type1a = new ArrayList<>();
                            ArrayList<String> type1b = new ArrayList<>();
                            ArrayList<String> type1c = new ArrayList<>();
                            ArrayList<String> type2 = new ArrayList<>();

                            String[] stringArray = am.list("overlays/" + package_name);

                            type1a.add(getString(R.string.overlays_variant_default_1a));
                            type1b.add(getString(R.string.overlays_variant_default_1b));
                            type1c.add(getString(R.string.overlays_variant_default_1c));
                            type2.add(getString(R.string.overlays_variant_default_2));

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
                                                if (current.substring(0, 6).equals("type2_")) {
                                                    type2.add(current.substring(6));
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
                                        getContext(), versionName);
                                values2.add(overlaysInfo);
                            } else {
                                // At this point, there is no spinner adapter, so it should be null
                                OverlaysInfo overlaysInfo = new OverlaysInfo(parse2_themeName,
                                        parsed_name,
                                        package_name, false, null, null, null, null, getContext(),
                                        versionName);
                                values2.add(overlaysInfo);
                            }
                        } catch (Exception e) {
                            Log.e("SubstratumLogger", "Could not properly buffer AssetManager " +
                                    "listing" +
                                    " " +
                                    "(PLUGIN TYPE 1)");
                        }
                    }
                } catch (PackageManager.NameNotFoundException nnfe) {
                    Log.e("SubstratumLogger", "Could not find explicit package identifier" +
                            " in package manager list.");
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
            Log.d("SubstratumBuilder", "Decompiling and initializing work area with the selected " +
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
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            Phase3_mainFunction phase3_mainFunction = new Phase3_mainFunction();
            phase3_mainFunction.execute("");
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Initialize Substratum cache with theme
            if (!has_initialized_cache) {
                sb = new SubstratumBuilder();
                sb.initializeCache(getContext(), theme_pid);
                has_initialized_cache = true;
            } else {
                Log.d("SubstratumBuilder", "Work area is ready with decompiled assets already!");
            }
            return null;
        }
    }

    private class Phase3_mainFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.d("Phase 3", "This phase has started it's asynchronous task.");

            // Change title in preparation for loop to change subtext
            mBuilder.setContentTitle(getString(R.string
                    .notification_compiling_signing_installing))
                    .setContentText(getString(R.string.notification_extracting_assets_text))
                    .setProgress(100, 0, false);
            mNotifyManager.notify(id, mBuilder.build());
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {

            Intent notificationIntent = new Intent();
            notificationIntent.putExtra("theme_name", theme_name);
            notificationIntent.putExtra("theme_pid", theme_pid);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            /*
            PendingIntent intent =
                    PendingIntent.getActivity(getActivity(), 0, notificationIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);*/

            // Closing off the persistent notification
            mBuilder.setAutoCancel(true);
            mBuilder.setProgress(0, 0, false);
            mBuilder.setOngoing(false);
            //mBuilder.setContentIntent(intent);
            mBuilder.setSmallIcon(R.drawable.notification_success_icon);
            mBuilder.setContentTitle(getString(R.string.notification_done_title));
            mBuilder.setContentText(getString(R.string.notification_no_errors_found));
            mBuilder.getNotification().flags |= Notification.FLAG_AUTO_CANCEL;
            mNotifyManager.notify(id, mBuilder.build());

            Toast toast = Toast.makeText(getContext(), getString(R
                            .string.toast_compiled_updated),
                    Toast.LENGTH_SHORT);
            toast.show();

            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {

            for (int i = 0; i < checkedOverlays.size(); i++) {
                String theme_name_parsed = theme_name.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
                String current_overlay = checkedOverlays.get(i).getPackageName();
                try {
                    ApplicationInfo applicationInfo = getContext().getPackageManager()
                            .getApplicationInfo(current_overlay, 0);
                    String packageTitle = getContext().getPackageManager().getApplicationLabel
                            (applicationInfo).toString();

                    // Initialize working notification

                    mBuilder.setProgress(100, (int) (((double) (i + 1) / checkedOverlays.size()) *
                            100), false);
                    mBuilder.setContentText(getString(R.string.notification_processing) + " " +
                            "\"" +
                            packageTitle + "\"");
                    mNotifyManager.notify(id, mBuilder.build());

                    // PLUGIN TYPE 1: Move root XML file into the res/values folder

                    if (pluginType <= 1) {
                        if (checkedOverlays.get(i).is_variant_chosen) {
                            String workingDirectory = getContext().getCacheDir().toString() +
                                    "/SubstratumBuilder/assets/overlays/" +
                                    current_overlay;

                            // Type 1a
                            if (checkedOverlays.get(i).is_variant_chosen1) {
                                String sourceLocation = workingDirectory + "/type1a_" +
                                        checkedOverlays.get(i).getSelectedVariantName() + ".xml";

                                String targetLocation = workingDirectory +
                                        "/res/values/type1a.xml";

                                Log.d("SubstratumBuilder", "You have selected variant file \"" +
                                        checkedOverlays.get(i).getSelectedVariantName() + "\"");
                                Log.d("SubstratumBuilder", "Moving variant file to: " + targetLocation);

                                eu.chainfire.libsuperuser.Shell.SU.run(
                                        "mv -f " + sourceLocation + " " + targetLocation);
                            }

                            // Type 1b
                            if (checkedOverlays.get(i).is_variant_chosen2) {
                                String sourceLocation2 = workingDirectory + "/type1b_" +
                                        checkedOverlays.get(i).getSelectedVariantName2() + ".xml";

                                String targetLocation2 = workingDirectory +
                                        "/res/values/type1b.xml";

                                Log.d("SubstratumBuilder", "You have selected variant file \"" +
                                        checkedOverlays.get(i).getSelectedVariantName2() + "\"");
                                Log.d("SubstratumBuilder", "Moving variant file to: " + targetLocation2);

                                eu.chainfire.libsuperuser.Shell.SU.run(
                                        "mv -f " + sourceLocation2 + " " + targetLocation2);
                            }
                            // Type 1c
                            if (checkedOverlays.get(i).is_variant_chosen3) {
                                String sourceLocation3 = workingDirectory + "/type1c_" +
                                        checkedOverlays.get(i).getSelectedVariantName3() + ".xml";

                                String targetLocation3 = workingDirectory +
                                        "/res/values/type1c.xml";

                                Log.d("SubstratumBuilder", "You have selected variant file \"" +
                                        checkedOverlays.get(i).getSelectedVariantName3() + "\"");
                                Log.d("SubstratumBuilder", "Moving variant file to: " + targetLocation3);

                                eu.chainfire.libsuperuser.Shell.SU.run(
                                        "mv -f " + sourceLocation3 + " " + targetLocation3);
                            }

                            String packageName =
                                    (checkedOverlays.get(i).is_variant_chosen1 ? checkedOverlays.get(i).getSelectedVariantName() : "") +
                                    (checkedOverlays.get(i).is_variant_chosen2 ? checkedOverlays.get(i).getSelectedVariantName2() : "") +
                                    (checkedOverlays.get(i).is_variant_chosen3 ? checkedOverlays.get(i).getSelectedVariantName3() : "").
                                            replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");

                            if (checkedOverlays.get(i).is_variant_chosen4) {
                                packageName = (packageName + checkedOverlays.get(i).getSelectedVariantName4()).replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
                                Log.d("SubstratumBuilder", "Currently processing package" +
                                        " \"" + current_overlay + "." + packageName + "\"...");
                                sb = new SubstratumBuilder();
                                sb.beginAction(getContext(), current_overlay, theme_name, "true",
                                        packageName, checkedOverlays.get(i).getSelectedVariantName4(),
                                        versionName);
                            }
                            Log.d("SubstratumBuilder", "Currently processing package" +
                                    " \"" + current_overlay + "." + packageName + "\"...");
                            sb = new SubstratumBuilder();
                            sb.beginAction(getContext(), current_overlay, theme_name, "true",
                                    packageName, null,
                                    versionName);
                        } else {
                            Log.d("SubstratumBuilder", "Currently processing package" +
                                    " \"" + current_overlay + "." + theme_name_parsed + "\"...");
                            sb = new SubstratumBuilder();
                            sb.beginAction(getContext(), current_overlay, theme_name, "true",
                                    null, null, versionName);
                        }
                    }
                } catch (Exception e) {
                    Log.e("SubstratumLogger", "Main function has unexpectedly stopped!");
                }
            }

            return null;
        }
    }
}