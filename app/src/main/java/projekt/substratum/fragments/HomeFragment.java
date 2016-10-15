package projekt.substratum.fragments;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.R;
import projekt.substratum.adapters.ThemeEntryAdapter;
import projekt.substratum.config.References;
import projekt.substratum.model.ThemeInfo;
import projekt.substratum.util.AOPTCheck;
import projekt.substratum.util.Root;

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
    private ThemeEntryAdapter adapter;
    private View cardView;
    private ViewGroup root;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = (ViewGroup) inflater.inflate(R.layout.home_fragment, null);

        mContext = getActivity();

        substratum_packages = new HashMap<>();
        recyclerView = (RecyclerView) root.findViewById(R.id.theme_list);
        cardView = root.findViewById(R.id.no_entry_card_view);
        cardView.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View v) {
                                            try {
                                                String playURL = getString(R.string
                                                        .search_play_store_url);
                                                Intent i = new Intent(Intent.ACTION_VIEW);
                                                i.setData(Uri.parse(playURL));
                                                startActivity(i);
                                            } catch (ActivityNotFoundException
                                                    activityNotFoundException) {
                                                //
                                            }
                                        }
                                    }
        );
        cardView.setVisibility(View.GONE);

        // Create it so it uses a recyclerView to parse substratum-based themes

        PackageManager packageManager = mContext.getPackageManager();
        list = packageManager.getInstalledApplications(PackageManager
                .GET_META_DATA);

        swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh items
                refreshLayout();
            }
        });

        refreshLayout();

        // Now we need to sort the buffered installed Layers themes
        map = new TreeMap<>(substratum_packages);

        ArrayList<ThemeInfo> themeInfos = prepareData();
        adapter = new ThemeEntryAdapter(themeInfos);

        // Assign adapter to RecyclerView
        recyclerView.setAdapter(adapter);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        return root;
    }

    private void getSubstratumPackages(Context context, String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (!References.checkOMS()) {
                    if (appInfo.metaData.getBoolean(References.metadataLegacy, false)) {
                        if (appInfo.metaData.getString(References.metadataName) != null) {
                            if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                                String[] data = {appInfo.metaData.getString
                                        (References.metadataAuthor),
                                        package_name};
                                substratum_packages.put(appInfo.metaData.getString
                                        (References.metadataName), data);
                                Log.d("Substratum Ready Theme", package_name);
                            }
                        }
                    }
                } else {
                    if (appInfo.metaData.getString(References.metadataName) != null) {
                        if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                            String[] data = {appInfo.metaData.getString
                                    (References.metadataAuthor),
                                    package_name};
                            substratum_packages.put(appInfo.metaData.getString
                                    (References.metadataName), data);
                            Log.d("Substratum Ready Theme", package_name);
                        }
                    }
                }

            }
        } catch (Exception e) {
            // Exception
        }
    }

    private ArrayList<ThemeInfo> prepareData() {

        ArrayList<ThemeInfo> themes = new ArrayList<>();
        for (int i = 0; i < map.size(); i++) {
            ThemeInfo themeInfo = new ThemeInfo();
            themeInfo.setThemeName(map.keySet().toArray()[i].toString());
            themeInfo.setThemeAuthor(map.get(map.keySet().toArray()[i].toString())[0]);
            themeInfo.setThemePackage(map.get(map.keySet().toArray()[i].toString())[1]);
            themeInfo.setThemeDrawable(
                    References.grabPackageHeroImage(mContext, map.get(map.keySet().toArray()[i]
                            .toString())[1]));
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            if (prefs.getBoolean("show_theme_ready_indicators", true)) {
                themeInfo.setThemeReadyVariable(References.grabThemeReadyVisibility(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
            }
            if (prefs.getBoolean("show_template_version", false)) {
                themeInfo.setPluginVersion(References.grabPackageTemplateVersion(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
                themeInfo.setSDKLevels(References.grabThemeAPIs(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
                themeInfo.setThemeVersion(References.grabThemeVersion(mContext,
                        map.get(map.keySet().toArray()[i].toString())[1]));
            } else {
                themeInfo.setPluginVersion(null);
                themeInfo.setSDKLevels(null);
                themeInfo.setThemeVersion(null);
            }
            themeInfo.setThemeMode(null);
            themeInfo.setContext(mContext);
            themes.add(themeInfo);
        }
        return themes;
    }

    private void refreshLayout() {
        MaterialProgressBar materialProgressBar = (MaterialProgressBar) root.findViewById(R.id
                .progress_bar_loader);
        PackageManager packageManager = mContext.getPackageManager();

        new AOPTCheck().injectAOPT(getContext());

        list.clear();
        recyclerView.setAdapter(null);
        substratum_packages = new HashMap<>();
        list = packageManager.getInstalledApplications(PackageManager
                .GET_META_DATA);
        for (ApplicationInfo packageInfo : list) {
            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                getSubstratumPackages(mContext, packageInfo.packageName);
            }
        }

        if (substratum_packages.size() == 0) {
            cardView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            cardView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        // Now we need to sort the buffered installed Layers themes
        map = new TreeMap<>(substratum_packages);
        ArrayList<ThemeInfo> themeInfos = prepareData();
        adapter = new ThemeEntryAdapter(themeInfos);
        recyclerView.setAdapter(adapter);
        new ThemeCollection().execute("");
        swipeRefreshLayout.setRefreshing(false);
        materialProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (prefs.getInt(
                "uninstalled", THEME_INFORMATION_REQUEST_CODE) == THEME_INFORMATION_REQUEST_CODE) {
            prefs.edit().putInt("uninstalled", 0).commit();
            refreshLayout();
        }
        super.onResume();
    }

    private class ThemeCollection extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... sUrl) {
            PackageManager packageManager = mContext.getPackageManager();
            List<ApplicationInfo> list = packageManager.getInstalledApplications(PackageManager
                    .GET_META_DATA);
            List<String> installed = new ArrayList<>();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            for (ApplicationInfo packageInfo : list) {
                try {
                    ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                            packageInfo.packageName, PackageManager.GET_META_DATA);
                    if (appInfo.metaData != null) {
                        if (References.checkOMS()) {
                            if (appInfo.metaData.getString(References.metadataName) != null) {
                                if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                                    installed.add(packageInfo.packageName);
                                }
                            }
                        } else {
                            if (appInfo.metaData.getString(References.metadataName) != null) {
                                if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                                    if (appInfo.metaData.getBoolean(References.metadataLegacy,
                                            false)) {
                                        installed.add(packageInfo.packageName);
                                    } else {
                                        Log.e("SubstratumCacher", "Device is non-OMS, while an " +
                                                "OMS theme is installed, aborting operation!");

                                        Intent showIntent = new Intent();
                                        PendingIntent contentIntent = PendingIntent.getActivity(
                                                mContext, 0, showIntent, 0);

                                        String parse = String.format(mContext.getString(
                                                R.string.failed_to_install_text_notification),
                                                appInfo.metaData.getString(References
                                                        .metadataName));

                                        NotificationManager notificationManager =
                                                (NotificationManager) mContext.getSystemService(
                                                        Context.NOTIFICATION_SERVICE);
                                        NotificationCompat.Builder mBuilder =
                                                new NotificationCompat.Builder(mContext)
                                                        .setContentIntent(contentIntent)
                                                        .setAutoCancel(true)
                                                        .setSmallIcon(
                                                                R.drawable
                                                                        .notification_warning_icon)
                                                        .setContentTitle(mContext.getString(
                                                                R.string.failed_to_install_title_notification))
                                                        .setContentText(parse);
                                        Notification notification = mBuilder.build();
                                        notificationManager.notify(
                                                References.notification_id, notification);

                                        String final_commands = "pm uninstall " +
                                                packageInfo.packageName;

                                        if (References.isPackageInstalled(mContext,
                                                "masquerade.substratum")) {
                                            Intent runCommand = new Intent();
                                            runCommand.addFlags(Intent
                                                    .FLAG_INCLUDE_STOPPED_PACKAGES);
                                            runCommand.setAction("masquerade.substratum.COMMANDS");
                                            runCommand.putExtra("om-commands", final_commands);
                                            mContext.sendBroadcast(runCommand);
                                        } else {
                                            Root.runCommand(final_commands);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("SubstratumLogger", "Unable to find package identifier (INDEX OUT OF " +
                            "BOUNDS)");
                }
            }

            // Check for current installed set created just now and sort it
            Set<String> installed_set = new HashSet<>();
            installed_set.addAll(installed);
            Set<String> installed_setStringSorted = new TreeSet<>();
            Iterator<String> it2 = installed_set.iterator();
            while (it2.hasNext()) {
                installed_setStringSorted.add(it2.next());
            }

            SharedPreferences.Editor edit = prefs.edit();
            edit.putStringSet("installed_themes", installed_set);
            edit.apply();
            return null;
        }
    }
}