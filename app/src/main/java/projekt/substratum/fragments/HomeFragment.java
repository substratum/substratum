package projekt.substratum.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import projekt.substratum.InformationActivityTabs;
import projekt.substratum.R;
import projekt.substratum.adapters.DataAdapter;
import projekt.substratum.util.ReadOverlaysFile;
import projekt.substratum.util.Root;
import projekt.substratum.util.ThemeParser;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class HomeFragment extends Fragment {

    private final int THEME_INFORMATION_REQUEST_CODE = 1;
    private HashMap<String, String[]> substratum_packages;
    private RecyclerView recyclerView;
    private Map<String, String[]> map;
    private Context mContext;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<ApplicationInfo> list;
    private DataAdapter adapter;
    private View cardView;
    private SharedPreferences prefs;
    private List<String> unauthorized_packages;
    private List<String> installed_themes;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == THEME_INFORMATION_REQUEST_CODE) {
            try {
                Boolean uninstalled = data.getBooleanExtra("Uninstalled", false);
                if (uninstalled) {
                    refreshLayout();
                }
            } catch (Exception e) {
                // Handle NPE when the window is refreshed too many times
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.home_fragment, null);

        mContext = getActivity();
        prefs = PreferenceManager.getDefaultSharedPreferences(
                getContext());

        installed_themes = new ArrayList<>();
        unauthorized_packages = new ArrayList<>();
        substratum_packages = new HashMap<>();
        recyclerView = (RecyclerView) root.findViewById(R.id.theme_list);
        cardView = root.findViewById(R.id.no_entry_card_view);
        cardView.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View v) {
                                            String playURL = getString(R.string
                                                    .search_play_store_url);
                                            Intent i = new Intent(Intent.ACTION_VIEW);
                                            i.setData(Uri.parse(playURL));
                                            startActivity(i);
                                        }
                                    }
        );
        cardView.setVisibility(View.GONE);

        // Create it so it uses a recyclerView to parse substratum-based themes

        PackageManager packageManager = mContext.getPackageManager();
        list = packageManager.getInstalledApplications(PackageManager
                .GET_META_DATA);
        for (ApplicationInfo packageInfo : list) {
            getSubstratumPackages(mContext, packageInfo.packageName);
        }

        AntiPiracyCheck antiPiracyCheck = new AntiPiracyCheck();
        antiPiracyCheck.execute("");

        if (substratum_packages.size() == 0) {
            cardView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            cardView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        // Now we need to sort the buffered installed Layers themes
        map = new TreeMap<>(substratum_packages);

        ArrayList<ThemeParser> themeParsers = prepareData();
        adapter = new DataAdapter(getContext(), themeParsers);

        // Assign adapter to RecyclerView
        recyclerView.setAdapter(adapter);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            GestureDetector gestureDetector = new GestureDetector(getContext(),
                    new GestureDetector.SimpleOnGestureListener() {

                        @Override
                        public boolean onSingleTapUp(MotionEvent e) {
                            return true;
                        }

                    });

            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                View child = rv.findChildViewUnder(e.getX(), e.getY());
                if (child != null && gestureDetector.onTouchEvent(e)) {
                    // RecyclerView Clicked item value
                    int position = rv.getChildAdapterPosition(child);

                    // Process fail case if user uninstalls an app and goes back an activity
                    if (isPackageInstalled(getContext(),
                            map.get(map.keySet().toArray()[position].toString())[1])) {
                        Intent myIntent = new Intent(getContext(), InformationActivityTabs.class);
                        myIntent.putExtra("theme_name", map.keySet().toArray()[position].toString
                                ());
                        myIntent.putExtra("theme_pid", map.get(map.keySet().toArray()[position]
                                .toString())[1]);
                        startActivityForResult(myIntent, THEME_INFORMATION_REQUEST_CODE);
                    } else {
                        Toast toast = Toast.makeText(getContext(), getString(R.string
                                        .toast_uninstalled),
                                Toast.LENGTH_SHORT);
                        toast.show();
                        refreshLayout();
                    }
                }

                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh items
                refreshLayout();
            }
        });
        return root;
    }

    private String getDeviceIMEI() {
        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context
                .TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    private boolean isPackageInstalled(Context context, String package_name) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean findOverlayParent(Context context, String theme_parent) {
        try {
            PackageManager packageManager = mContext.getPackageManager();
            List<ApplicationInfo> pm = packageManager.getInstalledApplications(PackageManager
                    .GET_META_DATA);
            for (int i = 0; i < pm.size(); i++) {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        pm.get(i).packageName, PackageManager.GET_META_DATA);
                if (appInfo.metaData != null) {
                    if (appInfo.metaData.getString("Substratum_Theme") != null) {
                        String parse1_themeName = appInfo.metaData.getString("Substratum_Theme")
                                .replaceAll("\\s+", "");
                        String parse2_themeName = parse1_themeName.replaceAll
                                ("[^a-zA-Z0-9]+", "");
                        if (parse2_themeName.equals(theme_parent)) {
                            return true;
                        }
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException nnfe) {
        }
        return false;
    }

    private void getSubstratumPackages(Context context, String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString("Substratum_Theme") != null) {
                    if (appInfo.metaData.getString("Substratum_Author") != null) {
                        String[] data = {appInfo.metaData.getString
                                ("Substratum_Author"),
                                package_name};
                        substratum_packages.put(appInfo.metaData.getString
                                ("Substratum_Theme"), data);
                        installed_themes.add(package_name);
                        Log.d("Substratum Ready Theme", package_name);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("SubstratumLogger", "Unable to find package identifier (INDEX OUT OF BOUNDS)");
        }
    }

    private void checkOverlayIntegrity(Context context, String package_name) {
        // Check whether all overlay packages installed matches the current device's information
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString("Substratum_ID") != null) {
                    if (appInfo.metaData.getString("Substratum_ID").equals(Settings.
                            Secure.getString(context.getContentResolver(),
                            Settings.Secure.ANDROID_ID))) {
                        if (appInfo.metaData.getString("Substratum_IMEI") != null) {
                            if (appInfo.metaData.getString("Substratum_IMEI").equals("!" +
                                    getDeviceIMEI())) {
                                if (appInfo.metaData.getString("Substratum_Parent") != null) {
                                    if (!findOverlayParent(context, appInfo.metaData.getString
                                            ("Substratum_Parent"))) {
                                        Log.d("OverlayVerification", package_name + " " +
                                                "unauthorized to" +
                                                " be" +
                                                " used on this device.");
                                        unauthorized_packages.add(package_name);
                                    } else {
                                        Log.d("OverlayVerification", package_name + " verified to" +
                                                " be " +
                                                "used" +
                                                " on this device.");
                                    }
                                }
                            } else {
                                Log.d("OverlayVerification", package_name + " unauthorized to be" +
                                        " used on this device.");
                                unauthorized_packages.add(package_name);
                            }
                        }
                    } else {
                        Log.d("OverlayVerification", package_name + " unauthorized to be" +
                                " used on this device.");
                        unauthorized_packages.add(package_name);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("SubstratumLogger", "Unable to find package identifier (INDEX OUT OF BOUNDS)");
        }
    }

    private ArrayList<ThemeParser> prepareData() {

        ArrayList<ThemeParser> themes = new ArrayList<>();
        for (int i = 0; i < map.size(); i++) {
            ThemeParser themeParser = new ThemeParser();
            themeParser.setThemeName(map.keySet().toArray()[i].toString());
            themeParser.setThemeAuthor(map.get(map.keySet().toArray()[i].toString())[0]);
            themeParser.setThemePackage(map.get(map.keySet().toArray()[i].toString())[1]);
            themes.add(themeParser);
        }
        return themes;
    }

    private void refreshLayout() {
        PackageManager packageManager = mContext.getPackageManager();
        installed_themes = new ArrayList<>();
        list.clear();
        recyclerView.setAdapter(null);
        substratum_packages = new HashMap<>();
        list = packageManager.getInstalledApplications(PackageManager
                .GET_META_DATA);
        for (ApplicationInfo packageInfo : list) {
            getSubstratumPackages(mContext, packageInfo.packageName);
        }

        doCleanUp cleanUp = new doCleanUp();
        cleanUp.execute("");

        if (substratum_packages.size() == 0) {
            cardView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            cardView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        // Now we need to sort the buffered installed Layers themes
        map = new TreeMap<>(substratum_packages);
        ArrayList<ThemeParser> themeParsers = prepareData();
        adapter = new DataAdapter(mContext.getApplicationContext(), themeParsers);
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing(false);
    }

    private class AntiPiracyCheck extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            if (unauthorized_packages.size() > 0) {
                PurgeUnauthorizedOverlays purgeUnauthorizedOverlays = new
                        PurgeUnauthorizedOverlays();
                purgeUnauthorizedOverlays.execute("");
            }
            doCleanUp cleanUp = new doCleanUp();
            cleanUp.execute("");
        }

        @Override
        protected String doInBackground(String... sUrl) {
            try {
                for (ApplicationInfo packageInfo : list) {
                    checkOverlayIntegrity(mContext, packageInfo.packageName);
                }
            } catch (Exception e) {
                Log.e("SubstratumLogger", "An attempt to run a concurrent job has been triggered " +
                        "and will be ignored.");
            }
            Set<String> installed = new HashSet<>();
            installed.addAll(installed_themes);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putStringSet("installed_themes", installed);
            edit.apply();
            return null;
        }
    }

    private class doCleanUp extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            Root.runCommand("cp /data/system/overlays.xml " +
                    Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");

            String[] commands = {Environment.getExternalStorageDirectory()
                    .getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "1"};
            List<String> state1 = ReadOverlaysFile.main(commands);  // Overlays with non-existent
            // targets
            for (int i = 0; i < state1.size(); i++) {
                Log.e("OverlayCleaner", "Target APK not found for \"" + state1.get(i) + "\" and " +
                        "will " +
                        "be removed.");
                Root.runCommand("pm uninstall " + state1.get(i));
            }
            return null;
        }
    }

    private class PurgeUnauthorizedOverlays extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.d("SubstratumAntiPiracy", "The device has found unauthorized overlays created by " +
                    "another device.");
            Toast toast = Toast.makeText(mContext.getApplicationContext(),
                    getString(R.string
                            .antipiracy_toast),
                    Toast.LENGTH_LONG);
            toast.show();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Toast toast = Toast.makeText(mContext.getApplicationContext(),
                    getString(R.string
                            .antipiracy_toast_complete),
                    Toast.LENGTH_LONG);
            toast.show();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            for (int i = 0; i < unauthorized_packages.size(); i++) {
                Root.runCommand("pm uninstall " + unauthorized_packages.get(i));
            }
            return null;
        }
    }
}