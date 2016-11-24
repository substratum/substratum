package projekt.substratum;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.adapters.IconPackAdapter;
import projekt.substratum.config.References;
import projekt.substratum.model.IconInfo;
import projekt.substratum.util.ReadOverlays;
import projekt.substratum.util.SubstratumIconBuilder;

import static projekt.substratum.config.References.DEBUG;
import static projekt.substratum.config.References.grabPackageName;
import static projekt.substratum.util.MapUtils.sortMapByValues;

public class StudioSystemActivity extends AppCompatActivity {

    private String current_pack;
    private ArrayList<IconInfo> icons;
    private ProgressBar progressBar;
    private MaterialProgressBar progressCircle;
    private FloatingActionButton floatingActionButton;
    private ProgressDialog mProgressDialog;
    private String current_icon = "";
    private double current_amount = 0;
    private double total_amount = 0;
    private String final_commands;
    private ArrayList<String> final_runner;
    private ArrayList<String> disable_me;
    private ArrayList<String> enable_me;
    private ArrayList<String> disabled_icon_overlays;

    private XmlPullParser getAppFilter(String packageName) {
        // Get the corresponding icon pack from the res/raw/appfilter.xml directory in the icon pack
        try {
            Context otherContext = getApplicationContext().createPackageContext(packageName, 0);
            Resources resources = otherContext.getResources();
            int i = resources.getIdentifier("appfilter", "xml", packageName);
            return resources.getXml(i);
        } catch (Exception e) {
            // Suppress warning
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private HashMap parseXML(XmlPullParser parser) throws XmlPullParserException, IOException {
        // Now let's quickly read all the entries inside the specified XML

        // HashMap is a nice way to store key and values for specified objects, especially since
        // an item in the app_filter.xml contains both component info and drawable info, translating
        // this into a Java readable way is favored in our situation.
        HashMap launcherIcons = new HashMap();
        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String name;
            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    launcherIcons = new HashMap();
                    break;
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if (name.equals("item")) {
                        String component = parser.getAttributeValue(null, "component");
                        String drawable = parser.getAttributeValue(null, "drawable");
                        if (component != null && drawable != null) {
                            launcherIcons.put(component, drawable);
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    name = parser.getName();
                    if (name.equals("item")) {
                        String component = parser.getAttributeValue(null, "component");
                        String drawable = parser.getAttributeValue(null, "drawable");
                        if (component != null && drawable != null) {
                            launcherIcons.put(component, drawable);
                        }
                    }
                    break;
            }
            eventType = parser.next();
        }
        return launcherIcons;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(current_pack);
        if (launchIntent != null) {
            getMenuInflater().inflate(R.menu.studio_activity_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.studio_activity_nd_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.refresh:
                this.recreate();
                return true;
            case R.id.open:
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(current_pack);
                if (launchIntent != null) {
                    startActivity(launchIntent);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.studio_activity);

        Intent currentIntent = getIntent();
        current_pack = currentIntent.getStringExtra("icon_pack");
        progressCircle = (MaterialProgressBar) findViewById(R.id.progress_bar_loader);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(false);
                getSupportActionBar().setTitle(References.grabPackageName(
                        getApplicationContext(), current_pack));
            }
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        floatingActionButton = (FloatingActionButton) findViewById(R.id.apply_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new IconPackInstaller().execute("");
            }
        });

        References.getIconState(getApplicationContext(), current_pack);
        mProgressDialog = new ProgressDialog(this, R.style.SubstratumBuilder_BlurView);

        new loadIconPack().execute("");
    }

    public class IconPackInstaller extends AsyncTask<String, Integer, String> {


        @Override
        protected void onPreExecute() {
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            mProgressDialog.setContentView(R.layout.compile_icon_dialog_loader);

            final float radius = 5;
            final View decorView = getWindow().getDecorView();
            final View rootView = decorView.findViewById(android.R.id.content);
            final Drawable windowBackground = decorView.getBackground();

            BlurView blurView = (BlurView) mProgressDialog.findViewById(R.id.blurView);

            blurView.setupWith(rootView)
                    .windowBackground(windowBackground)
                    .blurAlgorithm(new RenderScriptBlur(getApplicationContext(), true))
                    .blurRadius(radius);

            progressBar = (ProgressBar) mProgressDialog.findViewById(R.id.loading_bar);
            progressBar.setProgressTintList(ColorStateList.valueOf(getColor(
                    R.color.compile_dialog_wave_color)));
            progressBar.setIndeterminate(false);

            final_runner = new ArrayList<>();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            TextView textView = (TextView) mProgressDialog.findViewById(R.id.current_object);
            textView.setText(current_icon);
            double progress = (current_amount / total_amount) * 100;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressBar.setProgress((int) progress, true);
            } else {
                progressBar.setProgress((int) progress);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // Disable all the icons, then enable all the new icons

            // We have to switch Locales by reflecting to the framework methods
            // The reasoning behind this is because the Locale configuration change has a higher
            // bit-code than most others, such as Fonts, so it would affect all applications at the
            // same time, to simulate something like a assetSeq on OMS3

            // This would cause a window refresh on either Framework or Substratum
            if (final_runner.size() > 0 || disable_me.size() > 0 || enable_me.size() > 0) {
                final_commands = "";
                if (final_runner.size() > 0) {
                    for (int i = 0; i < final_runner.size(); i++) {
                        if (final_commands.length() <= 0) {
                            final_commands = final_runner.get(i);
                        } else {
                            final_commands = final_commands + " && " + final_runner.get(i);
                        }
                    }
                }
                if (disable_me.size() > 0) {
                    if (final_commands.length() > 0) final_commands = final_commands + " && " +
                            References.disableOverlay();
                    for (int j = 0; j < disable_me.size(); j++) {
                        if (final_commands.length() <= 0) {
                            final_commands = References.disableOverlay() + " " +
                                    disable_me.get(j);
                        } else {
                            final_commands = final_commands + " " + disable_me.get(j);
                        }
                    }
                }
                if (enable_me.size() > 0) {
                    if (final_commands.length() > 0) final_commands = final_commands + " && " +
                            References.enableOverlay();
                    for (int h = 0; h < enable_me.size(); h++) {
                        if (final_commands.length() <= 0) {
                            final_commands = References.enableOverlay() + " " +
                                    enable_me.get(h);
                        } else {
                            final_commands = final_commands + " " + enable_me.get(h);
                        }
                    }
                }

                if (References.isPackageInstalled(getApplicationContext(),
                        "masquerade.substratum")) {
                    if (DEBUG)
                        Log.e(References.SUBSTRATUM_ICON_BUILDER,
                                "Initializing the Masquerade theme provider...");
                    if (final_commands.contains("pm install") &&
                            References.checkOMSVersion(getApplicationContext()) == 3) {
                        final_commands = final_commands +
                                " && " + References.refreshWindows();
                    }
                    Intent runCommand = new Intent();
                    runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    runCommand.setAction("masquerade.substratum.COMMANDS");
                    ArrayList<String> final_array = new ArrayList<>();
                    final_array.add(0, References.grabPackageName(
                            getApplicationContext(), current_pack));
                    final_array.add(1, final_commands);
                    runCommand.putExtra("icon-handler", final_array);
                    getApplicationContext().sendBroadcast(runCommand);
                } else {
                    Log.e(References.SUBSTRATUM_ICON_BUILDER,
                            "Cannot apply icon pack on a non OMS7 ROM");
                }

            }

            // Disable the window
            mProgressDialog.dismiss();
        }

        @Override
        protected String doInBackground(String... Params) {
            total_amount = icons.size();
            disable_me = new ArrayList<>();

            List<String> state4 = ReadOverlays.main(4, getApplicationContext());
            List<String> state5 = ReadOverlays.main(5, getApplicationContext());
            ArrayList<String> disabled_overlays = new ArrayList<>(state4);
            disabled_icon_overlays = new ArrayList<>();
            // Buffer the list of icons that are disabled on the system
            for (int disabled = 0; disabled < disabled_overlays.size(); disabled++) {
                if (disabled_overlays.get(disabled).contains(".icon")) {
                    disabled_icon_overlays.add(disabled_overlays.get(disabled));
                }
            }
            ArrayList<String> activated_overlays = new ArrayList<>(state5);
            ArrayList<String> icons_list = new ArrayList<>();
            // Buffer the list of icons that are going to be loaded
            for (int obj = 0; obj < icons.size(); obj++) {
                icons_list.add(icons.get(obj).getPackageName() + ".icon");
            }

            enable_me = new ArrayList<>();
            for (int j = 0; j < activated_overlays.size(); j++) {
                if (activated_overlays.get(j).contains(".icon")) {
                    if (icons_list.contains(activated_overlays.get(j))) {
                        // At this point, we know that the overlay is already enabled, so with
                        // the power of OMS7, we can just compile and window refresh
                    } else {
                        // These guys are from the old icon pack, we'll have to disable them!
                        Log.e(References.SUBSTRATUM_ICON_BUILDER, "Sent the icon for disabling : " +
                                activated_overlays.get(j));
                        disable_me.add(
                                References.disableOverlay() + " " + activated_overlays.get(j));
                    }
                }
            }

            for (int i = 0; i < icons.size(); i++) {
                current_amount = i + 1;

                // Dynamically check whether the icon is disabled
                enable_me.add(icons.get(i).getPackageName() + ".icon");

                String iconName = icons.get(i).getParsedName();
                String iconNameParsed = iconName + " " +
                        getString(R.string.icon_pack_entry);
                String iconPackage = icons.get(i).getPackageName();
                String iconDrawable = icons.get(i).getPackageDrawable();
                String iconDrawableName = References.getPackageIconName(
                        getApplicationContext(), iconPackage);

                current_icon = "\"" + iconName + "\"";
                publishProgress((int) current_amount);

                Log.d(References.SUBSTRATUM_ICON_BUILDER, "Currently building : " +
                        iconPackage);
                HashMap hashMap = References.getIconState(
                        getApplicationContext(), iconPackage);

                Boolean update_bool = true;

                // The only two window refreshing icons would be Android System and Substratum as
                // these are the resources currently loaded on top of the current view
                if (icons.get(i).getPackageName().equals("android") ||
                        icons.get(i).getPackageName().equals("projekt.substratum")) {
                    Log.d("SubstratumLogger", "The flag to update this " +
                            "overlay has been triggered.");
                    update_bool = false;
                }

                Iterator it = null;
                if (hashMap != null) it = hashMap.entrySet().iterator();
                if (it != null && it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next(); // The Activity Key

                    // Filter out the icons that already have the current applied pack applied
                    if (References.isPackageInstalled(
                            getApplicationContext(), iconPackage + ".icon") &&
                            References.grabIconPack(
                                    getApplicationContext(), iconPackage + ".icon", current_pack)) {
                        Log.d(References.SUBSTRATUM_ICON_BUILDER, "'" + iconPackage +
                                "' already contains the " +
                                grabPackageName(getApplicationContext(), current_pack) +
                                " attribute, skip recompile...");
                    } else {
                        try {
                            Log.d(References.SUBSTRATUM_ICON_BUILDER,
                                    "Fusing drawable from icon pack to system : " + iconDrawable);
                            Context context = createPackageContext(current_pack, 0);
                            Resources resources = context.getResources();
                            int drawable = resources.getIdentifier(
                                    iconDrawable, // Drawable name explicitly defined
                                    "drawable", // Declared icon is a drawable, indeed.
                                    current_pack); // Icon pack package name

                            SubstratumIconBuilder sib = new SubstratumIconBuilder();
                            sib.beginAction(
                                    getApplicationContext(),
                                    current_pack,
                                    iconPackage,
                                    References.grabThemeVersion(getApplicationContext(),
                                            current_pack),
                                    true,
                                    hashMap,
                                    pair.getKey().toString(),
                                    drawable,
                                    iconDrawableName,
                                    iconNameParsed,
                                    update_bool);
                            if (sib.has_errored_out) {
                                Log.e(References.SUBSTRATUM_ICON_BUILDER,
                                        "Could not instantiate icon (" + iconPackage +
                                                ") for SubstratumIconBuilder!");
                            }

                            if (sib.no_install.length() > 0) {
                                final_runner.add(sib.no_install);
                            }
                        } catch (Exception e) {
                            Log.e(References.SUBSTRATUM_ICON_BUILDER,
                                    "Could not instantiate icon (" + iconPackage +
                                            ") for SubstratumIconBuilder!");
                        }
                    }
                }
            }
            return null;
        }
    }

    public class loadIconPack extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            progressCircle.setVisibility(View.VISIBLE);
            floatingActionButton.hide();
        }

        @Override
        protected void onPostExecute(String result) {
            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.icon_pack_recycler);
            GridLayoutManager linearLayout = new GridLayoutManager(getApplicationContext(), 4);

            recyclerView.setHasFixedSize(true);
            recyclerView.setLayoutManager(linearLayout);

            IconPackAdapter iconPackAdapter = new IconPackAdapter(icons);
            recyclerView.setAdapter(iconPackAdapter);

            floatingActionButton.show();
            progressCircle.setVisibility(View.GONE);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected String doInBackground(String... sUrl) {
            // Buffer the hash map of all the values in the XML
            HashMap hashMap = null;
            try {
                hashMap = parseXML(getAppFilter(current_pack));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Create a bare list to store each of the values necessary to add into the
            // RecyclerView
            icons = new ArrayList<>();
            // This list will make sure that the multiple ComponentInfo objects will be filtered
            ArrayList<String> packages = new ArrayList<>();

            HashMap unsortedMap = new HashMap();

            // Load up all the launcher activities to set proper icons
            final PackageManager pm = getPackageManager();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> appList = pm.queryIntentActivities(mainIntent, 0);
            ArrayList<String> appListExposed = new ArrayList<>();
            Collections.sort(appList, new ResolveInfo.DisplayNameComparator(pm));

            for (ResolveInfo temp : appList) {
                appListExposed.add(temp.activityInfo.name);
            }

            // Filter out the icon packs, we don't need launcher packs to have a dummy overlay
            List<ResolveInfo> appListIconPacks = References.getIconPacks(getApplicationContext());
            ArrayList<String> appListIconPackageNames = new ArrayList<>();
            // Quickly buffer all the package names of the icon packs
            for (int i = 0; i < appListIconPacks.size(); i++) {
                appListIconPackageNames.add(appListIconPacks.get(i).activityInfo.packageName);
            }

            // Quickly buffer all the packages in the key set to know which packages are installed
            if (hashMap != null) {
                for (Object object : hashMap.keySet()) {
                    String f = (String) object;
                    if (f.length() > 13) {
                        String drawable = hashMap.get(f).toString();
                        String parse = f.substring(13).replaceAll("[{}]", ""); // Remove brackets

                        String[] component = parse.split("/"); // Remove the dash
                        if (component.length == 2) {
                            // Only if the ComponentInfo is parsed properly, for example:
                            // com.android.quicksearchbox/com.android.quicksearchbox.SearchActivity
                            if (!packages.contains(component[0]) &&
                                    !appListIconPackageNames.contains(component[0]) &&
                                    appListExposed.contains(component[1])) {
                                // Second parameter appListExposed checks whether the activity is
                                // assigned to a launcher icon. If not, then ignore.
                                if (References.isPackageInstalled(getApplicationContext(),
                                        component[0])) {
                                    Log.d(References.SUBSTRATUM_ICON_BUILDER,
                                            "Loaded drawable from icon pack : " + drawable);
                                    unsortedMap.put(component[0] + "|" + drawable,
                                            References.grabPackageName(getApplicationContext(),
                                                    component[0]));
                                }
                            }
                        }
                    }
                }
            }

            // Sort the values list
            List<Pair<String, String>> sortedMap = sortMapByValues(unsortedMap);

            // After sorting, we should be buffering the proper sorted list to show packs
            // asciibetically
            for (Pair<String, String> entry : sortedMap) {
                String package_attributes = entry.first;
                String[] attrs = package_attributes.split("\\|");
                if (!packages.contains(attrs[0])) {
                    IconInfo iconInfo = new IconInfo(
                            getApplicationContext(),
                            attrs[0],
                            attrs[1],
                            current_pack);
                    icons.add(iconInfo);
                    packages.add(attrs[0]);
                }
            }
            return null;
        }
    }
}