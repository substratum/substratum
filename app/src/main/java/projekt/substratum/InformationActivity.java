package projekt.substratum;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.gordonwong.materialsheetfab.MaterialSheetFab;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.util.FloatingActionMenu;
import projekt.substratum.util.ReadOverlaysFile;
import projekt.substratum.util.SubstratumBuilder;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class InformationActivity extends AppCompatActivity {

    private final int THEME_INFORMATION_REQUEST_CODE = 1;
    public ListView listView;
    public String theme_name, theme_pid;
    public AssetManager am;
    public ArrayList<String> values;
    public Boolean has_extracted_cache;
    public SubstratumBuilder lb;
    public List<String> listStrings, problematicOverlays, mixAndMatch;
    public Switch toggle_overlays;
    public ProgressBar progressBar;
    private ArrayList<String> enabled_overlays;
    private ArrayAdapter<String> adapter;
    private String final_commands;
    private List<String> unapproved_overlays;
    private SharedPreferences prefs;
    private String current_mode;
    private Boolean mixAndMatchMode;
    private MaterialSheetFab materialSheetFab;
    private String mixAndMatchCommands;
    private int overlayCount;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private Boolean is_building = false;
    private Boolean app_paused = false;
    private Boolean app_resumed = false;
    private int id = 1;

    private boolean isPackageInstalled(Context context, String package_name) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public Drawable grabAppIcon(String package_name) {
        Drawable icon = null;
        try {
            icon = getPackageManager().getApplicationIcon(package_name);
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e("SubstratumLogger", "Could not grab the application icon for \"" + package_name
                    + "\"");
        }
        return icon;
    }

    public Drawable grabPackageHeroImage(String package_name) {
        Resources res;
        Drawable hero = null;
        try {
            res = getPackageManager().getResourcesForApplication(package_name);
            int resourceId = res.getIdentifier(package_name + ":drawable/heroimage", null, null);
            if (0 != resourceId) {
                hero = getPackageManager().getDrawable(package_name, resourceId, null);
            }
            return hero;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information_activity);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        enabled_overlays = new ArrayList<>();
        has_extracted_cache = false;

        // Handle collapsible toolbar with theme name

        Intent currentIntent = getIntent();
        theme_name = currentIntent.getStringExtra("theme_name");
        theme_pid = currentIntent.getStringExtra("theme_pid");

        View sheetView = findViewById(R.id.fab_sheet);
        View overlay = findViewById(R.id.overlay);
        int sheetColor = getColor(R.color.fab_menu_background_card);
        int fabColor = getColor(R.color.colorAccent);

        final FloatingActionMenu floatingActionButton = (FloatingActionMenu) findViewById(R
                .id.apply_fab);
        floatingActionButton.hide();

        // Create material sheet FAB
        if (floatingActionButton != null && sheetView != null && overlay != null) {
            materialSheetFab = new MaterialSheetFab<>(floatingActionButton,
                    sheetView, overlay,
                    sheetColor, fabColor);
        }

        Switch enable_swap = (Switch) findViewById(R.id.enable_swap);
        if (enable_swap != null) {
            if (prefs.getBoolean("enable_swapping_overlays", true)) {
                mixAndMatchMode = false;
                enable_swap.setChecked(false);
            } else {
                mixAndMatchMode = true;
                enable_swap.setChecked(true);
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

        TextView compile_enable_selected = (TextView) findViewById(R.id.compile_enable_selected);
        if (compile_enable_selected != null)
            compile_enable_selected.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            getString(R.string
                                    .toast_updating),
                            Toast.LENGTH_LONG);
                    toast.show();
                    eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays.xml " +
                            Environment
                                    .getExternalStorageDirectory().getAbsolutePath() +
                            "/.substratum/current_overlays.xml");
                    String[] commands0 = {Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/.substratum/current_overlays.xml", "4"};
                    String[] commands1 = {Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/.substratum/current_overlays.xml", "5"};

                    List<String> state4 = ReadOverlaysFile.main(commands0);
                    List<String> state5 = ReadOverlaysFile.main(commands1);

                    unapproved_overlays = new ArrayList<>(state4);
                    unapproved_overlays.addAll(state5);

                    current_mode = "compile_enable";
                    progressBar.setVisibility(View.VISIBLE);
                    SparseBooleanArray checked = listView.getCheckedItemPositions();
                    listStrings = new ArrayList<>();
                    for (int i = 0; i < listView.getAdapter()
                            .getCount(); i++) {
                        if (checked.get(i)) {
                            listStrings.add(listView
                                    .getItemAtPosition(i)
                                    .toString());
                        }
                    }
                    // Run through phase two - initialize the cache for the specific theme
                    Phase2_InitializeCache phase2_initializeCache = new Phase2_InitializeCache();
                    phase2_initializeCache.execute("");
                }
            });

        TextView disable_selected = (TextView) findViewById(R.id.disable_selected);
        if (disable_selected != null)
            disable_selected.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    current_mode = "disable";
                    progressBar.setVisibility(View.VISIBLE);
                    SparseBooleanArray checked = listView.getCheckedItemPositions();
                    listStrings = new ArrayList<>();
                    for (int i = 0; i < listView.getAdapter()
                            .getCount(); i++) {
                        if (checked.get(i)) {
                            listStrings.add(listView
                                    .getItemAtPosition(i)
                                    .toString());
                        }
                    }
                    // Run through phase two - initialize the cache for the specific theme
                    Phase2_InitializeCache phase2_initializeCache = new Phase2_InitializeCache();
                    phase2_initializeCache.execute("");
                }
            });

        TextView enable_selected = (TextView) findViewById(R.id.enable_selected);
        if (enable_selected != null) enable_selected.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                current_mode = "enable";
                progressBar.setVisibility(View.VISIBLE);
                SparseBooleanArray checked = listView.getCheckedItemPositions();
                listStrings = new ArrayList<>();
                for (int i = 0; i < listView.getAdapter()
                        .getCount(); i++) {
                    if (checked.get(i)) {
                        listStrings.add(listView
                                .getItemAtPosition(i)
                                .toString());
                    }
                }
                // Run through phase two - initialize the cache for the specific theme
                Phase2_InitializeCache phase2_initializeCache = new Phase2_InitializeCache();
                phase2_initializeCache.execute("");
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.loading_bar);
        if (progressBar != null) progressBar.setVisibility(View.GONE);

        ImageView imageView = (ImageView) findViewById(R.id.preview_image);
        if (imageView != null) imageView.setImageDrawable(grabPackageHeroImage(theme_pid));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setTitle(theme_name);

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById
                (R.id.collapsingToolbarLayout);
        if (collapsingToolbarLayout != null) collapsingToolbarLayout.setTitle(theme_name);

        // From now on parse the theme_name as a package_identifier ready version
        String parse1_themeName = theme_name.replaceAll("\\s+", "");
        theme_name = parse1_themeName.replaceAll("[^a-zA-Z0-9]+", "");

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        if (toolbar != null) toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Handle all overlays that are located in the APK
        listView = (ListView) findViewById(R.id.overlay_picker);

        LoadOverlays loadOverlays = new LoadOverlays();
        loadOverlays.execute("");

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int counter = 0;
                SparseBooleanArray checked = listView.getCheckedItemPositions();
                for (int i = 0; i < listView.getAdapter()
                        .getCount(); i++) {
                    if (checked.get(i)) {
                        counter += 1;
                    }
                }
                if (counter > 0) {
                    if (counter == listView.getCount()) {
                        if (!toggle_overlays.isChecked()) {
                            toggle_overlays.setChecked(true);
                        }
                    }
                    if (floatingActionButton != null) {
                        floatingActionButton.show();
                    }
                } else {
                    if (toggle_overlays.isChecked()) {
                        toggle_overlays.setChecked(false);
                    }
                    if (floatingActionButton != null) {
                        floatingActionButton.hide();
                    }
                }
            }
        });

        // Handle the logic for selecting all overlays or not

        toggle_overlays = (Switch) findViewById(R.id.toggle_all_overlays);
        if (toggle_overlays != null) {
            toggle_overlays.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            try {
                                SparseBooleanArray checked = listView.getCheckedItemPositions();
                                for (int i = 0; i < listView.getAdapter().getCount(); i++) {
                                    if (checked.get(i)) {
                                        if (!isChecked == listView.isItemChecked(i)) {
                                            if (floatingActionButton != null) {
                                                floatingActionButton.hide();
                                            }
                                            listView.setItemChecked(i, false);
                                        } else {
                                            if (floatingActionButton != null) {
                                                floatingActionButton.show();
                                            }
                                            listView.setItemChecked(i, true);
                                        }
                                    } else {
                                        if (!isChecked == listView.isItemChecked(i)) {
                                            if (floatingActionButton != null) {
                                                floatingActionButton.show();
                                            }
                                            listView.setItemChecked(i, true);
                                        } else {
                                            if (floatingActionButton != null) {
                                                floatingActionButton.hide();
                                            }
                                            listView.setItemChecked(i, false);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // Deal with app force closing after too many window refreshes
                                Log.e("SubstratumLogger", "Information window refreshed too " +
                                        "many times, restarting current activity to preserve app " +
                                        "integrity.");
                                Intent intent = getIntent();
                                finish();
                                startActivity(intent);
                            }
                        }
                    });
        }

        // Run through phase one - checking whether aapt exists on the device
        Phase1_AAPT_Check phase1_aapt_check = new Phase1_AAPT_Check();
        phase1_aapt_check.execute("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.theme_information_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.clean) {
            AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
            builder.setTitle(theme_name);
            builder.setIcon(grabAppIcon(theme_pid));
            builder.setMessage(R.string.clean_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Quickly parse theme_name
                            String parse1_themeName = theme_name.replaceAll("\\s+", "");
                            String parse2_themeName = parse1_themeName.replaceAll
                                    ("[^a-zA-Z0-9]+", "");

                            // Begin uninstalling all overlays based on this package
                            eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays" +
                                    ".xml " +
                                    Environment
                                            .getExternalStorageDirectory().getAbsolutePath() +
                                    "/.substratum/current_overlays.xml");

                            String[] commands = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "4"};

                            String[] commands1 = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "5"};

                            List<String> stateAll = ReadOverlaysFile.main(commands);
                            stateAll.addAll(ReadOverlaysFile.main(commands1));

                            String commands2 = "";
                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int i = 0; i < stateAll.size(); i++) {
                                String current_item = stateAll.get(i);
                                String[] packageNameParsed = current_item.split
                                        ("\\.");
                                if (packageNameParsed[packageNameParsed.length - 1]
                                        .equals(parse2_themeName)) {
                                    Log.d("OverlayDisabler", "Uninstalling overlay \"" +
                                            current_item + "\"");
                                    all_overlays.add(current_item);
                                }
                            }
                            for (int i = 0; i < all_overlays.size(); i++) {
                                if (i == 0) {
                                    commands2 = commands2 + "pm uninstall " + all_overlays
                                            .get(i);
                                } else {
                                    commands2 = commands2 + " && pm uninstall " +
                                            all_overlays.get(i);
                                }
                            }
                            if (commands2.contains("systemui")) {
                                commands2 = commands2 + " && pkill com.android.systemui";
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .clean_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            eu.chainfire.libsuperuser.Shell.SU.run(commands2);

                            // Finally refresh the listView adapter
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton(R.string.uninstall_dialog_cancel, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create();
            builder.show();
            return true;
        }
        if (id == R.id.disable) {
            AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
            builder.setTitle(theme_name);
            builder.setIcon(grabAppIcon(theme_pid));
            builder.setMessage(R.string.disable_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Quickly parse theme_name
                            String parse1_themeName = theme_name.replaceAll("\\s+", "");
                            String parse2_themeName = parse1_themeName.replaceAll
                                    ("[^a-zA-Z0-9]+", "");

                            // Begin disabling all overlays based on this package
                            eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays" +
                                    ".xml " +
                                    Environment
                                            .getExternalStorageDirectory().getAbsolutePath() +
                                    "/.substratum/current_overlays.xml");

                            String[] commands = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "4"};

                            String[] commands1 = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "5"};

                            List<String> stateAll = ReadOverlaysFile.main(commands);
                            stateAll.addAll(ReadOverlaysFile.main(commands1));

                            String commands2 = "";
                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int i = 0; i < stateAll.size(); i++) {
                                String current_item = stateAll.get(i);
                                String[] packageNameParsed = current_item.split
                                        ("\\.");
                                if (packageNameParsed[packageNameParsed.length - 1]
                                        .equals(parse2_themeName)) {
                                    Log.d("OverlayDisabler", "Disabling overlay \"" +
                                            current_item + "\"");
                                    all_overlays.add(current_item);
                                }
                            }
                            for (int i = 0; i < all_overlays.size(); i++) {
                                if (i == 0) {
                                    commands2 = commands2 + "om disable " + all_overlays
                                            .get(i);
                                } else {
                                    commands2 = commands2 + " && om disable " +
                                            all_overlays.get(i);
                                }
                            }
                            if (commands2.contains("systemui")) {
                                commands2 = commands2 + " && pkill com.android.systemui";
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .disable_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            eu.chainfire.libsuperuser.Shell.SU.run(commands2);

                            // Finally refresh the listView adapter
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton(R.string.uninstall_dialog_cancel, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create();
            builder.show();
            return true;
        }
        if (id == R.id.enable) {
            AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
            builder.setTitle(theme_name);
            builder.setIcon(grabAppIcon(theme_pid));
            builder.setMessage(R.string.enable_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Quickly parse theme_name
                            String parse1_themeName = theme_name.replaceAll("\\s+", "");
                            String parse2_themeName = parse1_themeName.replaceAll
                                    ("[^a-zA-Z0-9]+", "");

                            // Begin enabling all overlays based on this package
                            eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays" +
                                    ".xml " +
                                    Environment
                                            .getExternalStorageDirectory().getAbsolutePath() +
                                    "/.substratum/current_overlays.xml");

                            String[] commands = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "4"};

                            String[] commands1 = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "5"};

                            List<String> stateAll = ReadOverlaysFile.main(commands);
                            stateAll.addAll(ReadOverlaysFile.main(commands1));

                            String commands2 = "";
                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int i = 0; i < stateAll.size(); i++) {
                                String current_item = stateAll.get(i);
                                String[] packageNameParsed = current_item.split
                                        ("\\.");
                                if (packageNameParsed[packageNameParsed.length - 1]
                                        .equals(parse2_themeName)) {
                                    Log.d("OverlayEnabler", "Enabling overlay \"" +
                                            current_item + "\"");
                                    all_overlays.add(current_item);
                                }
                            }
                            for (int i = 0; i < all_overlays.size(); i++) {
                                if (i == 0) {
                                    commands2 = commands2 + "om enable " + all_overlays
                                            .get(i);
                                } else {
                                    commands2 = commands2 + " && om enable " +
                                            all_overlays.get(i);
                                }
                            }
                            if (commands2.contains("systemui")) {
                                commands2 = commands2 + " && pkill com.android.systemui";
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .enable_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            eu.chainfire.libsuperuser.Shell.SU.run(commands2);

                            // Finally refresh the listView adapter
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton(R.string.uninstall_dialog_cancel, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create();
            builder.show();
            return true;
        }
        if (id == R.id.rate) {
            String playURL = "https://play.google.com/store/apps/details?id=" + theme_pid;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(playURL));
            startActivity(i);
            return true;
        }
        if (id == R.id.uninstall) {
            AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
            builder.setTitle(theme_name);
            builder.setIcon(grabAppIcon(theme_pid));
            builder.setMessage(R.string.uninstall_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            eu.chainfire.libsuperuser.Shell.SU.run("pm uninstall " + theme_pid);

                            // Quickly parse theme_name
                            String parse1_themeName = theme_name.replaceAll("\\s+", "");
                            String parse2_themeName = parse1_themeName.replaceAll
                                    ("[^a-zA-Z0-9]+", "");

                            // Begin uninstalling all overlays based on this package
                            eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays" +
                                    ".xml " +
                                    Environment
                                            .getExternalStorageDirectory().getAbsolutePath() +
                                    "/.substratum/current_overlays.xml");

                            String[] commands = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "4"};

                            String[] commands1 = {Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/.substratum/current_overlays.xml", "5"};

                            List<String> stateAll = ReadOverlaysFile.main(commands);
                            stateAll.addAll(ReadOverlaysFile.main(commands1));

                            String commands2 = "";
                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int i = 0; i < stateAll.size(); i++) {
                                String current_item = stateAll.get(i);
                                String[] packageNameParsed = current_item.split
                                        ("\\.");
                                if (packageNameParsed[packageNameParsed.length - 1]
                                        .equals(parse2_themeName)) {
                                    Log.d("OverlayDisabler", "Uninstalling overlay \"" +
                                            current_item + "\"");
                                    all_overlays.add(current_item);
                                }
                            }
                            for (int i = 0; i < all_overlays.size(); i++) {
                                if (i == 0) {
                                    commands2 = commands2 + "pm uninstall " + all_overlays
                                            .get(i);
                                } else {
                                    commands2 = commands2 + " && pm uninstall " +
                                            all_overlays.get(i);
                                }
                            }
                            if (commands2.contains("systemui")) {
                                commands2 = commands2 + " && pkill com.android.systemui";
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .clean_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            eu.chainfire.libsuperuser.Shell.SU.run(commands2);

                            // Finally close out of the window
                            onBackPressed();
                        }
                    })
                    .setNegativeButton(R.string.uninstall_dialog_cancel, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create();
            builder.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (materialSheetFab.isSheetVisible()) {
            materialSheetFab.hideSheet();
        } else {
            if (!is_building) {
                Intent intent = new Intent();
                setResult(THEME_INFORMATION_REQUEST_CODE, intent);
                // Destroy the cache if the user leaves the activity
                super.onBackPressed();
                clearCache clear = new clearCache();
                clear.execute("");
            } else {
                Toast toast = Toast.makeText(getApplicationContext(),
                        getString(R.string
                                .toast_on_back_press_compiling),
                        Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    @Override
    public void onPause() {
        app_paused = true;
        super.onPause();
    }

    @Override
    public void onResume() {
        if (app_resumed) {
            if (!is_building) {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        } else {
            app_paused = false;
        }
        super.onResume();
    }

    private class LoadOverlays extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            MaterialProgressBar materialProgressBar = (MaterialProgressBar) findViewById(R.id
                    .progress_bar);
            if (materialProgressBar != null) materialProgressBar.setVisibility(View.GONE);

            TextView overlay_count = (TextView) findViewById(R.id.title_overlays);
            if (overlay_count != null)
                overlay_count.setText(getString(R.string.list_of_overlays) + " (" + overlayCount
                        + ")");

            // Let's start sorting the overlay list alphabetically with proper package names
            List<String> unsortedList = new ArrayList<String>();
            List<String> unsortedListWithNames = new ArrayList<String>();

            // Let's convert the values list to package names first
            for (int i = 0; i < values.size(); i++) {
                try {
                    ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo
                            (values.get(i), 0);
                    String packageTitle = getPackageManager().getApplicationLabel
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

            if (listView != null) {
                listView.setNestedScrollingEnabled(true);
                listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                adapter = new ArrayAdapter<String>(InformationActivity.this, android.R.layout
                        .simple_list_item_multiple_choice, values) {
                    @Override
                    public View getView(final int position, View convertView, ViewGroup parent) {
                        TextView textView = (TextView) super.getView(position, convertView, parent);
                        try {
                            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo
                                    (listView.getItemAtPosition
                                            (position).toString(), 0);
                            String packageTitle = getPackageManager().getApplicationLabel
                                    (applicationInfo).toString();
                            textView.setText(packageTitle);
                        } catch (PackageManager.NameNotFoundException nnfe) {
                            Log.e("SubstratumLogger", "Could not find explicit package identifier" +
                                    " in package manager list.");
                        }
                        if (isPackageInstalled(InformationActivity.this, listView.getItemAtPosition
                                (position).toString() + "." + theme_name)) {
                            if (enabled_overlays.contains(listView.getItemAtPosition(position)
                                    .toString() + "." + theme_name)) {
                                textView.setTextColor(getColor(R.color
                                        .overlay_installed_list_entry));
                            } else {
                                if (unapproved_overlays.contains(listView.getItemAtPosition
                                        (position).toString() + "." + theme_name)) {
                                    SpannableString string = new SpannableString(textView.getText
                                            ());
                                    string.setSpan(new StrikethroughSpan(), 0, string.length(), 0);
                                    textView.setText(string);
                                    textView.setTextColor(getColor(R.color
                                            .overlay_not_approved_list_entry));
                                } else {
                                    textView.setTextColor(getColor(R.color
                                            .overlay_not_enabled_list_entry));
                                }
                            }
                        } else {
                            textView.setTextColor(getColor(R.color
                                    .overlay_not_installed_list_entry));
                        }
                        return textView;
                    }
                };
                listView.setAdapter(adapter);
            }
            toggle_overlays.setClickable(true);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                    getApplicationContext());

            // Filter out state 0-3 overlays before enabling them

            eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays.xml " +
                    Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");
            String[] commands0 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "0"};
            String[] commands1 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "1"};
            String[] commands2 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "2"};
            String[] commands3 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "3"};

            List<String> state0 = ReadOverlaysFile.main(commands0);
            List<String> state1 = ReadOverlaysFile.main(commands1);
            List<String> state2 = ReadOverlaysFile.main(commands2);
            List<String> state3 = ReadOverlaysFile.main(commands3);

            unapproved_overlays = new ArrayList<>(state0);
            unapproved_overlays.addAll(state1);
            unapproved_overlays.addAll(state2);
            unapproved_overlays.addAll(state3);

            // Parse the list of overlay folders inside assets/overlays
            try {
                values = new ArrayList<>();
                Context otherContext = createPackageContext(theme_pid, 0);
                am = otherContext.getAssets();
                String[] am_list = am.list("overlays");

                for (String package_name : am_list) {
                    if (prefs.getBoolean("show_installed_packages", true)) {
                        if (isPackageInstalled(InformationActivity.this, package_name)) {
                            values.add(package_name);
                        }
                    } else {
                        values.add(package_name);
                    }
                }
                overlayCount = values.size();
            } catch (Exception e) {
                Log.e("SubstratumLogger", "Could not refresh list of overlay folders.");
            }
            // Check enabled overlays
            eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays.xml " +
                    Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");
            String[] commands = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "5"};
            List<String> state5 = ReadOverlaysFile.main(commands);
            for (int i = 0; i < state5.size(); i++) {
                enabled_overlays.add(state5.get(i));
            }
            return null;
        }
    }

    private class clearCache extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("SubstratumBuilder", "The cache has been flushed!");
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Superuser is used due to some files being held hostage by the system
            File cacheFolder = new File(getCacheDir().getAbsolutePath() + "/SubstratumBuilder/");
            if (cacheFolder.exists()) {
                eu.chainfire.libsuperuser.Shell.SU.run(
                        "rm -r " + getCacheDir().getAbsolutePath() +
                                "/SubstratumBuilder/");
            }
            return null;
        }
    }

    private class Phase1_AAPT_Check extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.d("Phase 1", "This phase has started it's asynchronous task.");
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Check whether device has AAPT installed
            SubstratumBuilder aaptCheck = new SubstratumBuilder();
            aaptCheck.injectAAPT(getApplicationContext());
            return null;
        }
    }

    private class Phase2_InitializeCache extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.d("Phase 2", "This phase has started it's asynchronous task.");
            int notification_priority = 2; // PRIORITY_MAX == 2

            // This is the time when the notification should be shown on the user's screen
            if (!current_mode.equals("enable") && !current_mode.equals("disable")) {
                mNotifyManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mBuilder = new NotificationCompat.Builder(InformationActivity.this);
                mBuilder.setContentTitle(getString(R.string.notification_initial_title))
                        .setProgress(100, 0, true)
                        .setSmallIcon(android.R.drawable.ic_popup_sync)
                        .setPriority(notification_priority)
                        .setOngoing(true);
                mNotifyManager.notify(id, mBuilder.build());
            }
            is_building = true;
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Phase3_mainFunction phase3_mainFunction = new Phase3_mainFunction();
            phase3_mainFunction.execute("");
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Initialize the cache for this specific app
            if (!current_mode.equals("enable") && !current_mode.equals("disable")) {
                if (!has_extracted_cache) {
                    lb = new SubstratumBuilder();
                    lb.initializeCache(getApplicationContext(), theme_pid);
                    has_extracted_cache = true;
                }
            }
            return null;
        }
    }

    private class Phase3_mainFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.d("Phase 3", "This phase has started it's asynchronous task.");

            if (!current_mode.equals("enable") && !current_mode.equals("disable")) {
                // Change title in preparation for loop to change subtext
                mBuilder.setContentTitle(getString(R.string
                        .notification_compiling_signing_installing))
                        .setContentText(getString(R.string.notification_extracting_assets_text))
                        .setProgress(100, 0, false);
                mNotifyManager.notify(id, mBuilder.build());
            }

            problematicOverlays = new ArrayList<>();
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // On non-compiling dialogs, we have the progress bar shown
            progressBar.setVisibility(View.GONE);

            if (current_mode.equals("compile_enable") && problematicOverlays.size() == 0) {
                // Closing off the persistent notification
                mBuilder.setProgress(0, 0, false);
                mBuilder.setOngoing(false);
                mBuilder.setSmallIcon(R.drawable.notification_success_icon);
                mBuilder.setContentTitle(getString(R.string.notification_done_title));
                mBuilder.setContentText(getString(R.string.notification_no_errors_found));
                mNotifyManager.notify(id, mBuilder.build());
                is_building = false;

                Toast toast = Toast.makeText(getApplicationContext(), getString(R
                                .string.toast_compiled_updated),
                        Toast.LENGTH_SHORT);
                toast.show();

                if (!app_paused) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = getIntent();
                            finish();
                            startActivity(intent);
                        }
                    }, 2000);
                } else {
                    // Named this way because it will assume the next time the app is loaded, it
                    // will most definitely restart the activity
                    app_resumed = true;
                }

            } else {
                if (problematicOverlays.size() > 0) {
                    if (current_mode.equals("compile_enable")) {
                        // Closing off the persistent notification
                        mBuilder.setProgress(0, 0, false);
                        mBuilder.setOngoing(false);
                        mBuilder.setSmallIcon(R.drawable.notification_warning_icon);
                        mBuilder.setContentTitle(getString(R.string.notification_done_title));
                        mBuilder.setContentText(getString(R.string.notification_some_errors_found));
                        mNotifyManager.notify(id, mBuilder.build());
                        is_building = false;

                        for (int i = 0; i < problematicOverlays.size(); i++) {
                            String toast_text = String.format(getApplicationContext().getResources()
                                            .getString(
                                                    R.string.failed_to_install_overlay_toast),
                                    problematicOverlays.get(i));
                            Toast toast = Toast.makeText(getApplicationContext(), toast_text,
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }

                        if (!app_paused) {
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = getIntent();
                                    finish();
                                    startActivity(intent);
                                }
                            }, 2000);
                        } else {
                            // Named this way because it will assume the next time the app is
                            // loaded, it will most definitely restart the activity
                            app_resumed = true;
                        }
                    } else {
                        for (int i = 0; i < problematicOverlays.size(); i++) {
                            String toast_text = String.format(getApplicationContext().getResources()
                                            .getString(
                                                    R.string.failed_to_install_overlay_toast),
                                    problematicOverlays.get(i));
                            Toast toast = Toast.makeText(getApplicationContext(), toast_text,
                                    Toast.LENGTH_SHORT);
                            toast.show();
                            adapter.notifyDataSetChanged();
                        }
                    }
                } else {
                    if (!current_mode.equals("disable")) {
                        if (final_commands.length() > 0) {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R
                                            .string.toast_installed),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                            final_commands = final_commands + " && cp /data/system" +
                                    "/overlays" +
                                    ".xml " +
                                    Environment
                                            .getExternalStorageDirectory().getAbsolutePath()
                                    + "/" +
                                    ".substratum" +
                                    "/overlays" +
                                    ".xml";
                            if (mixAndMatchMode && mixAndMatchCommands.length() != 0) {
                                final_commands = mixAndMatchCommands + " && " + final_commands;
                            }
                            Log.e("Substratum4", final_commands);
                            eu.chainfire.libsuperuser.Shell.SU.run(final_commands);
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R
                                            .string.toast_disabled3),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    } else {
                        if (final_commands.length() > 0) {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R
                                            .string.toast_disabled2),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                            final_commands = final_commands + " && cp /data/system" +
                                    "/overlays" +
                                    ".xml " +
                                    Environment
                                            .getExternalStorageDirectory().getAbsolutePath()
                                    + "/" +
                                    ".substratum" +
                                    "/overlays" +
                                    ".xml";
                            if (mixAndMatchMode && mixAndMatchCommands.length() != 0) {
                                final_commands = mixAndMatchCommands + " && " + final_commands;
                            }
                            Log.e("Substratum5", final_commands);
                            eu.chainfire.libsuperuser.Shell.SU.run(final_commands);
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R
                                            .string.toast_disabled4),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Filter out state 4 overlays before programming them to enable
            ArrayList<String> approved_overlays = new ArrayList<>();

            if (!current_mode.equals("enable") && !current_mode.equals("disable")) {
                for (int i = 0; i < listStrings.size(); i++) {
                    try {
                        // Process notification while compiling
                        ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo
                                (listStrings.get(i), 0);
                        String packageTitle = getPackageManager().getApplicationLabel
                                (applicationInfo).toString();
                        mBuilder.setProgress(100, (int) (((double) (i + 1) / listStrings.size()) *
                                100), false);
                        mBuilder.setContentText(getString(R.string.notification_processing) + " " +
                                "\"" +
                                packageTitle + "\"");
                        mNotifyManager.notify(id, mBuilder.build());

                        lb = new SubstratumBuilder();
                        lb.beginAction(getApplicationContext(), listStrings.get(i), theme_name,
                                current_mode.equals("compile_enable") + "");
                        if (lb.has_errored_out) {
                            problematicOverlays.add(listStrings.get(i));
                        } else {
                            approved_overlays.add(listStrings.get(i) + "." + lb.parse2_themeName);
                        }
                    } catch (PackageManager.NameNotFoundException nnfe) {
                        Log.e("SubstratumLogger", "Could not find explicit package identifier in " +
                                "package manager list.");
                    }
                }
            }
            // Process the current overlays whether they are enabled or disabled
            eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays.xml " +
                    Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");

            List<String> approved_disabled_overlays;
            if (current_mode.equals("compile_enable")) {
                String[] commands = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/current_overlays.xml", "4"};
                String[] commands1 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/current_overlays.xml", "5"};
                approved_disabled_overlays = ReadOverlaysFile.main(commands);
                approved_disabled_overlays.addAll(ReadOverlaysFile.main(commands1));
            } else {
                String[] commands = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/current_overlays.xml", "4"};
                approved_disabled_overlays = ReadOverlaysFile.main(commands);
            }

            String swap_mode_commands = "";

            if (!current_mode.equals("enable") && !current_mode.equals("disable") &&
                    !current_mode.equals("compile_enable")) {
                final_commands = "";

                for (int i = 0; i < approved_overlays.size(); i++) {
                    if (i == 0 && approved_disabled_overlays.contains(approved_overlays.get(i))) {
                        final_commands = final_commands + "om enable " + approved_overlays.get(i);
                    } else {
                        if (i != 0 && final_commands.length() == 0 && approved_disabled_overlays
                                .contains(approved_overlays.get(i))) {
                            final_commands = final_commands + "om enable " + approved_overlays
                                    .get(i);
                        } else {
                            if (approved_disabled_overlays.contains(approved_overlays.get(i))) {
                                final_commands = final_commands + " && om enable " +
                                        approved_overlays.get(i);
                            }
                        }
                    }
                }
            } else {
                // Quickly parse theme_name
                String parse1_themeName = theme_name.replaceAll("\\s+", "");
                String parse2_themeName = parse1_themeName.replaceAll
                        ("[^a-zA-Z0-9]+", "");

                // Process the current overlays whether they are enabled or disabled
                String[] disabled = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/current_overlays.xml", "4"};
                String[] enabled = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/current_overlays.xml", "5"};
                List<String> enabled_disabled_overlays = ReadOverlaysFile.main(disabled);
                enabled_disabled_overlays.addAll(ReadOverlaysFile.main(enabled));

                final_commands = "";

                if (current_mode.equals("enable")) {
                    for (int i = 0; i < listStrings.size(); i++) {
                        if (i == 0 && enabled_disabled_overlays.contains(listStrings.get(i) + "."
                                + parse2_themeName)) {
                            final_commands = final_commands + "om enable " +
                                    listStrings.get(i) + "." + parse2_themeName;
                        } else {
                            if (i != 0 && final_commands.length() == 0 &&
                                    enabled_disabled_overlays.contains(listStrings.get(i) + "." +
                                            parse2_themeName)) {
                                final_commands = final_commands + "om enable " +
                                        listStrings.get(i) + "." + parse2_themeName;
                            } else {
                                if (enabled_disabled_overlays.contains(listStrings.get(i) + "." +
                                        parse2_themeName)) {
                                    final_commands = final_commands + " && om enable " +
                                            listStrings.get(i) + "." + parse2_themeName;
                                }
                            }
                        }
                    }
                }
                if (current_mode.equals("disable")) {
                    for (int i = 0; i < listStrings.size(); i++) {
                        if (i == 0 && enabled_disabled_overlays.contains(listStrings.get(i) + "."
                                + parse2_themeName)) {
                            final_commands = final_commands + "om disable " +
                                    listStrings.get(i) + "." + parse2_themeName;
                        } else {
                            if (i != 0 && final_commands.length() == 0 &&
                                    enabled_disabled_overlays.contains(listStrings.get(i) + "." +
                                            parse2_themeName)) {
                                final_commands = final_commands + "om disable " +
                                        listStrings.get(i) + "." + parse2_themeName;
                            } else {
                                if (enabled_disabled_overlays.contains(listStrings.get(i) + "." +
                                        parse2_themeName)) {
                                    final_commands = final_commands + " && om disable " +
                                            listStrings.get(i) + "." + parse2_themeName;
                                }
                            }
                        }
                    }
                }
            }
            if (final_commands.contains("systemui")) {
                final_commands = final_commands + " && pkill com.android.systemui";
            }
            if (final_commands.length() > 0) {
                if (!mixAndMatchMode) {
                    // Begin uninstalling all overlays based on this package
                    eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays.xml " +
                            Environment
                                    .getExternalStorageDirectory().getAbsolutePath() +
                            "/.substratum/current_overlays.xml");

                    String[] commands = {Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/.substratum/current_overlays.xml", "5"};

                    List<String> state5 = ReadOverlaysFile.main(commands);

                    for (int i = 0; i < state5.size(); i++) {
                        if (i == 0) {
                            swap_mode_commands = swap_mode_commands + "om disable " +
                                    state5.get(i);
                        } else {
                            swap_mode_commands = swap_mode_commands + " && om disable " +
                                    state5.get(i);
                        }
                    }
                } else {
                    String parse1_themeName = theme_name.replaceAll("\\s+", "");
                    String parse2_themeName = parse1_themeName.replaceAll
                            ("[^a-zA-Z0-9]+", "");

                    mixAndMatch = new ArrayList<>();
                    mixAndMatchCommands = "";

                    eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays.xml " +
                            Environment
                                    .getExternalStorageDirectory().getAbsolutePath() +
                            "/.substratum/current_overlays.xml");

                    String[] commands = {Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/.substratum/current_overlays.xml", "5"};
                    List<String> mixAndMatch1 = ReadOverlaysFile.main(commands);

                    for (int i = 0; i < mixAndMatch1.size(); i++) {
                        String packageNameParsed = mixAndMatch1.get(i);
                        if (!packageNameParsed.substring(8).split
                                ("\\.")[packageNameParsed.substring(8).split
                                ("\\.").length - 1].equals(parse2_themeName)) {
                            mixAndMatch.add(packageNameParsed);
                        }
                    }
                    for (int i = 0; i < mixAndMatch.size(); i++) {
                        if (i == 0) {
                            mixAndMatchCommands = mixAndMatchCommands + "om disable " +
                                    mixAndMatch.get(i);
                        } else {
                            if (mixAndMatchCommands.length() == 0) {
                                mixAndMatchCommands = mixAndMatchCommands + "om disable " +
                                        mixAndMatch.get(i);
                            } else {
                                mixAndMatchCommands = mixAndMatchCommands + " && om disable " +
                                        mixAndMatch.get(i);
                            }
                        }
                    }
                }
            }
            return null;
        }
    }
}
