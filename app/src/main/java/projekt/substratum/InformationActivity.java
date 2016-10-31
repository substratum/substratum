package projekt.substratum;

import android.app.Activity;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.flaviofaria.kenburnsview.KenBurnsView;
import com.theartofdev.edmodo.cropper.CropImage;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import projekt.substratum.adapters.InformationTabsAdapter;
import projekt.substratum.config.References;
import projekt.substratum.util.ReadOverlays;
import projekt.substratum.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class InformationActivity extends AppCompatActivity {

    public static String theme_name, theme_pid, theme_mode;
    private static List tab_checker;
    private static String wallpaperUrl;
    private final int THEME_INFORMATION_REQUEST_CODE = 1;
    private Boolean refresh_mode = false;
    private Boolean uninstalled = false;
    private Boolean theme_legacy = false;
    private KenBurnsView kenBurnsView;
    private byte[] byteArray;
    private Bitmap heroImageBitmap;
    private SharedPreferences prefs;

    public static String getThemeName() {
        return theme_name;
    }

    public static String getThemePID() {
        return theme_pid;
    }

    public static String getWallpaperUrl() {
        return wallpaperUrl;
    }

    private static int getDominantColor(Bitmap bitmap) {
        try {
            return bitmap.getPixel(0, 0);
        } catch (IllegalArgumentException iae) {
            return Color.BLACK;
        }
    }

    public static List getListOfFolders() {
        return tab_checker;
    }

    private static void setOverflowButtonColor(final Activity activity) {
        final String overflowDescription =
                activity.getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final ArrayList<View> outViews = new ArrayList<>();
                decorView.findViewsWithText(outViews, overflowDescription,
                        View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                if (outViews.isEmpty()) {
                    return;
                }
                AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);
                overflow.setImageResource(
                        activity.getResources().getIdentifier(
                                "information_activity_overflow_dark",
                                "drawable",
                                activity.getPackageName()));
            }
        });
    }

    private boolean checkColorDarkness(int color) {
        double darkness =
                1 - (0.299 * Color.red(color) +
                        0.587 * Color.green(color) +
                        0.114 * Color.blue(color)) / 255;
        return darkness < 0.5;
    }

    private Drawable grabPackageHeroImage(String package_name) {
        Resources res;
        Drawable hero = null;
        try {
            res = getPackageManager().getResourcesForApplication(package_name);
            int resourceId = res.getIdentifier(package_name + ":drawable/heroimage", null, null);
            if (0 != resourceId) {
                hero = getPackageManager().getDrawable(package_name, resourceId, null);
            }
            return hero;
        } catch (Exception e) {
            // Exception
        }
        return null;
    }

    private int getDeviceEncryptionStatus() {
        // 0: ENCRYPTION_STATUS_UNSUPPORTED
        // 1: ENCRYPTION_STATUS_INACTIVE
        // 2: ENCRYPTION_STATUS_ACTIVATING
        // 3: ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY
        // 4: ENCRYPTION_STATUS_ACTIVE
        // 5: ENCRYPTION_STATUS_ACTIVE_PER_USER
        int status = DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED;
        final DevicePolicyManager dpm = (DevicePolicyManager)
                getApplicationContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) {
            status = dpm.getStorageEncryptionStatus();
        }
        return status;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Image Cropper Request Capture
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            WallpaperManager myWallpaperManager
                    = WallpaperManager.getInstance(getApplicationContext());
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                if (resultUri.toString().contains("homescreen_wallpaper")) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            myWallpaperManager.setStream(new FileInputStream(
                                            resultUri.toString().substring(7)), null, true,
                                    WallpaperManager.FLAG_SYSTEM);
                        } else {
                            myWallpaperManager.setStream(new FileInputStream(
                                    resultUri.toString().substring(7)));
                        }
                        Toast toast = Toast.makeText(getApplicationContext(),
                                getString(R.string.wallpaper_homescreen_success),
                                Toast.LENGTH_LONG);
                        toast.show();
                    } catch (IOException e) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                getString(R.string.wallpaper_homescreen_error),
                                Toast.LENGTH_LONG);
                        toast.show();
                        e.printStackTrace();
                    }
                } else if (resultUri.toString().contains("lockscreen_wallpaper")) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            myWallpaperManager.setStream(new FileInputStream(
                                            resultUri.toString().substring(7)), null, true,
                                    WallpaperManager.FLAG_LOCK);
                        }
                        Toast toast = Toast.makeText(getApplicationContext(),
                                getString(R.string.wallpaper_lockscreen_success),
                                Toast.LENGTH_LONG);
                        toast.show();
                    } catch (IOException e) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                getString(R.string.wallpaper_lockscreen_error),
                                Toast.LENGTH_LONG);
                        toast.show();
                        e.printStackTrace();
                    }
                } else if (resultUri.toString().contains("all_wallpaper")) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            myWallpaperManager.setStream(new FileInputStream(
                                            resultUri.toString().substring(7)), null, true,
                                    WallpaperManager.FLAG_SYSTEM);
                            myWallpaperManager.clear(WallpaperManager.FLAG_LOCK);
                        }
                        Toast toast = Toast.makeText(getApplicationContext(),
                                getString(R.string.wallpaper_allscreen_success),
                                Toast.LENGTH_LONG);
                        toast.show();
                    } catch (IOException e) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                getString(R.string.wallpaper_allscreen_error),
                                Toast.LENGTH_LONG);
                        toast.show();
                        e.printStackTrace();
                    }
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Log.e("SubstratumImageCropper",
                        "There has been an error processing the image : " + error);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information_activity);

        prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());

        boolean dynamicActionBarColors = getResources().getBoolean(R.bool.dynamicActionBarColors);
        boolean dynamicNavBarColors = getResources().getBoolean(R.bool.dynamicNavigationBarColors);

        Intent currentIntent = getIntent();
        theme_name = currentIntent.getStringExtra("theme_name");
        theme_pid = currentIntent.getStringExtra("theme_pid");
        theme_mode = currentIntent.getStringExtra("theme_mode");
        theme_legacy = currentIntent.getBooleanExtra("theme_legacy", false);
        refresh_mode = currentIntent.getBooleanExtra("refresh_mode", false);
        wallpaperUrl = null;

        try {
            ApplicationInfo appInfo = getApplicationContext()
                    .getPackageManager().getApplicationInfo(
                            theme_pid, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString("Substratum_Wallpapers") != null) {
                    wallpaperUrl = appInfo.metaData.getString("Substratum_Wallpapers");
                }
            }
        } catch (Exception e) {
            // NameNotFound
        }

        if (theme_mode == null) {
            theme_mode = "";
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setTitle(theme_name);

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById
                (R.id.collapsing_toolbar_tabbed_layout);
        if (collapsingToolbarLayout != null) collapsingToolbarLayout.setTitle(theme_name);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);

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

        Drawable heroImage = grabPackageHeroImage(theme_pid);
        heroImageBitmap = ((BitmapDrawable) heroImage).getBitmap();
        int dominantColor = getDominantColor(heroImageBitmap);

        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        appBarLayout.setBackgroundColor(dominantColor);

        if (collapsingToolbarLayout != null && dynamicActionBarColors &&
                prefs.getBoolean("dynamic_actionbar", true)) {
            collapsingToolbarLayout.setStatusBarScrimColor(dominantColor);
            collapsingToolbarLayout.setContentScrimColor(dominantColor);
        }

        if (dynamicNavBarColors && prefs.getBoolean("dynamic_navbar", true)) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setNavigationBarColor(dominantColor);
                if (checkColorDarkness(dominantColor)) {
                    getWindow().setNavigationBarColor(
                            getColor(R.color.theme_information_background));
                }
            }
        }

        new LayoutLoader().execute("");

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        if (tabLayout != null) {
            if (theme_mode.equals("")) {
                if (!theme_legacy) tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                        .theme_information_tab_one)));
                try {
                    Context otherContext = getApplicationContext().createPackageContext
                            (theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    List found_folders = Arrays.asList(am.list(""));
                    tab_checker = new ArrayList<>();
                    if (!References.checkOMS(getApplicationContext())) {
                        for (int i = 0; i < found_folders.size(); i++) {
                            if (References.allowedForLegacy(found_folders.get(i).toString())) {
                                tab_checker.add(found_folders.get(i).toString());
                            }
                        }
                    } else {
                        tab_checker = Arrays.asList(am.list(""));
                    }
                    if (tab_checker.contains("overlays") ||
                            tab_checker.contains("overlays_legacy")) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_two)));
                    }
                    if (tab_checker.contains("bootanimation")) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_three)));
                    }
                    if (tab_checker.contains("fonts")) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_four)));
                    }
                    if (tab_checker.contains("audio")) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_five)));
                    }
                    if (wallpaperUrl != null) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_six)));
                    }
                } catch (Exception e) {
                    Log.e("SubstratumLogger", "Could not refresh list of asset folders.");
                }
            } else {
                if (theme_mode.equals("overlays")) {
                    tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                            .theme_information_tab_two)));
                } else {
                    if (theme_mode.equals("bootanimation")) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_three)));
                    } else {
                        if (theme_mode.equals("fonts")) {
                            tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                    .theme_information_tab_four)));
                        } else {
                            if (theme_mode.equals("audio")) {
                                tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                        .theme_information_tab_five)));
                            } else {
                                if (theme_mode.equals("wallpapers")) {
                                    tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                            .theme_information_tab_six)));
                                }
                            }
                        }
                    }
                }
            }

            tabLayout.setTabGravity(TabLayout.MODE_SCROLLABLE);
            if (dynamicActionBarColors && prefs.getBoolean("dynamic_actionbar", true))
                tabLayout.setBackgroundColor(dominantColor);

            if (collapsingToolbarLayout != null && checkColorDarkness(dominantColor) &&
                    dynamicActionBarColors && prefs.getBoolean("dynamic_actionbar", true)) {
                collapsingToolbarLayout.setCollapsedTitleTextColor(
                        getColor(R.color.information_activity_dark_icon_mode));
                collapsingToolbarLayout.setExpandedTitleColor(
                        getColor(R.color.information_activity_dark_icon_mode));
                tabLayout.setTabTextColors(
                        getColor(R.color.information_activity_dark_icon_mode),
                        getColor(R.color.information_activity_dark_icon_mode));

                Drawable upArrow = getDrawable(R.drawable.information_activity_back_dark);
                if (upArrow != null)
                    upArrow.setColorFilter(getColor(R.color.information_activity_dark_icon_mode),
                            PorterDuff.Mode.SRC_ATOP);
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
                setOverflowButtonColor(this);
            }
        }

        final InformationTabsAdapter adapter = new InformationTabsAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount(), getApplicationContext(),
                        theme_pid, (theme_mode.equals("") && !theme_legacy), theme_mode,
                        tab_checker, wallpaperUrl);
        if (viewPager != null) {
            viewPager.setOffscreenPageLimit(tabLayout.getTabCount());
            viewPager.setAdapter(adapter);
            viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener
                    (tabLayout));
            tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    viewPager.setCurrentItem(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(getApplicationContext())) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + InformationActivity.this.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Log.d("SubstratumLogger", "Substratum was granted " +
                        "'android.permission.WRITE_SETTINGS' " +
                        "permissions for system runtime code execution.");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (References.checkOMS(getApplicationContext())) {
            getMenuInflater().inflate(R.menu.theme_information_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.theme_information_menu_legacy, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.clean) {
            AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
            builder.setTitle(theme_name);
            builder.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
            builder.setMessage(R.string.clean_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Quickly parse theme_name
                            String parse1_themeName = theme_name.replaceAll("\\s+", "");
                            String parse2_themeName = parse1_themeName.replaceAll
                                    ("[^a-zA-Z0-9]+", "");

                            // Begin uninstalling all overlays based on this package
                            List<String> stateAll = ReadOverlays.main(4, getApplicationContext());
                            stateAll.addAll(ReadOverlays.main(5, getApplicationContext()));

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = getApplicationContext()
                                            .getPackageManager()
                                            .getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null) {
                                        if (appInfo.metaData.getString("Substratum_Parent") !=
                                                null) {
                                            if (appInfo.metaData.getString("Substratum_Parent")
                                                    .equals(parse2_themeName)) {
                                                all_overlays.add(current);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .clean_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            if (References.isPackageInstalled(getApplicationContext(),
                                    "masquerade.substratum")) {
                                Intent runCommand = new Intent();
                                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                runCommand.setAction("masquerade.substratum.COMMANDS");
                                runCommand.putExtra("restart_systemui", true);
                                runCommand.putStringArrayListExtra("pm-uninstall-specific",
                                        all_overlays);
                                getApplicationContext().sendBroadcast(runCommand);
                            } else {
                                String commands2 = "";
                                for (int i = 0; i < all_overlays.size(); i++) {
                                    if (i == 0) {
                                        commands2 = commands2 + "pm uninstall " +
                                                all_overlays.get(i);
                                    } else {
                                        commands2 = commands2 + " && pm uninstall " +
                                                all_overlays.get(i);
                                    }
                                }
                                Root.runCommand(commands2);
                            }
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
            builder.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
            builder.setMessage(R.string.disable_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Quickly parse theme_name
                            String parse1_themeName = theme_name.replaceAll("\\s+", "");
                            String parse2_themeName = parse1_themeName.replaceAll
                                    ("[^a-zA-Z0-9]+", "");

                            // Begin disabling all overlays based on this package
                            File current_overlays = new File(Environment
                                    .getExternalStorageDirectory().getAbsolutePath() +
                                    "/.substratum/current_overlays.xml");
                            if (current_overlays.exists()) {
                                Root.runCommand("rm " + Environment
                                        .getExternalStorageDirectory().getAbsolutePath() +
                                        "/.substratum/current_overlays.xml");
                            }
                            Root.runCommand("cp /data/system/overlays" +
                                    ".xml " +
                                    Environment
                                            .getExternalStorageDirectory().getAbsolutePath() +
                                    "/.substratum/current_overlays.xml");

                            List<String> stateAll = ReadOverlays.main(4, getApplicationContext());
                            stateAll.addAll(ReadOverlays.main(5, getApplicationContext()));

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = getApplicationContext()
                                            .getPackageManager()
                                            .getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null
                                            && appInfo.metaData.getString("Substratum_Parent") !=
                                            null) {
                                        if (appInfo.metaData.getString("Substratum_Parent")
                                                .equals(parse2_themeName)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }

                            String commands2 = References.disableOverlay() + " ";
                            for (int i = 0; i < all_overlays.size(); i++) {
                                commands2 = commands2 + all_overlays.get(i) + " ";
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .disable_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            if (References.isPackageInstalled(getApplicationContext(),
                                    "masquerade.substratum")) {
                                Intent runCommand = new Intent();
                                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                runCommand.setAction("masquerade.substratum.COMMANDS");
                                runCommand.putExtra("om-commands", commands2);
                                getApplicationContext().sendBroadcast(runCommand);
                            } else {
                                Root.runCommand(commands2);
                            }
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
            builder.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
            builder.setMessage(R.string.enable_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Quickly parse theme_name
                            String parse1_themeName = theme_name.replaceAll("\\s+", "");
                            String parse2_themeName = parse1_themeName.replaceAll
                                    ("[^a-zA-Z0-9]+", "");

                            // Begin enabling all overlays based on this package
                            List<String> stateAll = ReadOverlays.main(4, getApplicationContext());
                            stateAll.addAll(ReadOverlays.main(5, getApplicationContext()));

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = getApplicationContext()
                                            .getPackageManager()
                                            .getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null) {
                                        if (appInfo.metaData.getString("Substratum_Parent") !=
                                                null) {
                                            if (appInfo.metaData.getString("Substratum_Parent")
                                                    .equals(parse2_themeName)) {
                                                all_overlays.add(current);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }

                            String commands2 = References.enableOverlay() + " ";
                            for (int i = 0; i < all_overlays.size(); i++) {
                                commands2 = commands2 + all_overlays.get(i) + " ";
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .enable_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            if (References.isPackageInstalled(getApplicationContext(),
                                    "masquerade.substratum")) {
                                Intent runCommand = new Intent();
                                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                runCommand.setAction("masquerade.substratum.COMMANDS");
                                runCommand.putExtra("om-commands", commands2);
                                getApplicationContext().sendBroadcast(runCommand);
                            } else {
                                Root.runCommand(commands2);
                            }
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
            try {
                String playURL = "https://play.google.com/store/apps/details?id=" + theme_pid;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string
                                .activity_missing_toast),
                        Toast.LENGTH_SHORT);
                toast.show();
            }
            return true;
        }
        if (id == R.id.uninstall) {
            final SharedPreferences.Editor editor = prefs.edit();
            AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
            builder.setTitle(theme_name);
            builder.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
            builder.setMessage(R.string.uninstall_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, new DialogInterface
                            .OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Root.runCommand("pm uninstall " + theme_pid);

                            // Quickly parse theme_name
                            String parse1_themeName = theme_name.replaceAll("\\s+", "");
                            String parse2_themeName = parse1_themeName.replaceAll
                                    ("[^a-zA-Z0-9]+", "");

                            // Begin uninstalling all overlays based on this package
                            File current_overlays = new File(Environment
                                    .getExternalStorageDirectory().getAbsolutePath() +
                                    "/.substratum/current_overlays.xml");
                            if (current_overlays.exists()) {
                                Root.runCommand("rm " + Environment
                                        .getExternalStorageDirectory().getAbsolutePath() +
                                        "/.substratum/current_overlays.xml");
                            }
                            Root.runCommand("cp /data/system/overlays" +
                                    ".xml " +
                                    Environment
                                            .getExternalStorageDirectory().getAbsolutePath() +
                                    "/.substratum/current_overlays.xml");

                            List<String> stateAll = ReadOverlays.main(4, getApplicationContext());
                            stateAll.addAll(ReadOverlays.main(5, getApplicationContext()));

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = getApplicationContext()
                                            .getPackageManager()
                                            .getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null) {
                                        if (appInfo.metaData.getString("Substratum_Parent") !=
                                                null) {
                                            if (appInfo.metaData.getString("Substratum_Parent")
                                                    .equals(parse2_themeName)) {
                                                all_overlays.add(current);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .clean_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            Root.runCommand("rm -r " + getCacheDir().getAbsolutePath() +
                                    "/SubstratumBuilder/" + getThemePID());

                            if (References.isPackageInstalled(getApplicationContext(),
                                    "masquerade.substratum")) {
                                Intent runCommand = new Intent();
                                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                runCommand.setAction("masquerade.substratum.COMMANDS");
                                runCommand.putStringArrayListExtra("pm-uninstall-specific",
                                        all_overlays);
                                getApplicationContext().sendBroadcast(runCommand);
                            } else {
                                String commands2 = "";
                                for (int i = 0; i < all_overlays.size(); i++) {
                                    if (i == 0) {
                                        commands2 = commands2 + "pm uninstall " +
                                                all_overlays.get(i);
                                    } else {
                                        commands2 = commands2 + " && pm uninstall " +
                                                all_overlays.get(i);
                                    }
                                }
                                Root.runCommand(commands2);
                            }

                            //Remove applied font, sounds, and bootanimation
                            if (prefs.getString("sounds_applied", "").equals(theme_pid)) {
                                Root.runCommand("rm -r /data/system/theme/audio/ && pkill -f com" +
                                        ".android.systemui");
                                editor.remove("sounds_applied");
                            }
                            if (prefs.getString("fonts_applied", "").equals(theme_pid)) {
                                Root.runCommand("rm -r /data/system/theme/fonts/");
                                if (References.isPackageInstalled(getApplicationContext(),
                                        "masquerade.substratum")) {
                                    Intent runCommand = new Intent();
                                    runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                    runCommand.setAction("masquerade.substratum.COMMANDS");
                                    runCommand.putExtra("om-commands", References.refreshWindows() +
                                            " && setprop sys.refresh_theme 1");
                                    getApplicationContext().sendBroadcast(runCommand);
                                } else {
                                    Root.runCommand(References.refreshWindows() +
                                            " && setprop sys.refresh_theme 1");
                                }
                                if (!prefs.getBoolean("systemui_recreate", false)) {
                                    if (References.isPackageInstalled(getApplicationContext(),
                                            "masquerade.substratum")) {
                                        Intent runCommand = new Intent();
                                        runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                        runCommand.setAction("masquerade.substratum.COMMANDS");
                                        runCommand.putExtra("om-commands", "pkill -f com.android" +
                                                ".systemui");
                                        getApplicationContext().sendBroadcast(runCommand);
                                    } else {
                                        Root.runCommand("pkill -f com.android.systemui");
                                    }
                                }
                                editor.remove("fonts_applied");
                            }
                            if (prefs.getString("bootanimation_applied", "").equals(theme_pid)) {
                                if (getDeviceEncryptionStatus() <= 1) {
                                    Root.runCommand("rm -r /data/system/theme/bootanimation.zip");
                                } else {
                                    Root.runCommand("rm -r /system/media/bootanimation-encrypted" +
                                            ".zip");
                                }
                                editor.remove("bootanimation_applied");
                            }

                            // Finally close out of the window
                            editor.apply();
                            uninstalled = true;
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

        // Begin OMS based options
        if (id == R.id.refresh_windows) {
            if (References.isPackageInstalled(getApplicationContext(),
                    "masquerade.substratum")) {
                Intent runCommand = new Intent();
                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                runCommand.setAction("masquerade.substratum.COMMANDS");
                runCommand.putExtra("om-commands", References.refreshWindows());
                getApplicationContext().sendBroadcast(runCommand);
            } else {
                Root.runCommand(References.refreshWindows());
            }
            return true;
        }
        if (id == R.id.restart_systemui) {
            Root.runCommand("pkill -f com.android.systemui");
            return true;
        }

        // Begin RRO based options
        if (id == R.id.reboot_device) {
            Root.runCommand("reboot");
            return true;
        }
        if (id == R.id.soft_reboot) {
            Root.runCommand("pkill -f zygote");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        if (uninstalled || refresh_mode) {
            prefs.edit().putInt("uninstalled", THEME_INFORMATION_REQUEST_CODE).commit();
        } else {
            prefs.edit().putInt("uninstalled", 0).commit();
        }
    }

    private class LayoutLoader extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Glide.with(getApplicationContext()).load(byteArray).centerCrop().into(kenBurnsView);

            // Now, let's grab root on the helper
            Intent rootIntent = new Intent(Intent.ACTION_MAIN);
            rootIntent.setAction("masquerade.substratum.INITIALIZE");
            try {
                startActivity(rootIntent);
            } catch (RuntimeException re) {
                // Exception: At this point, Masquerade is not installed at all.
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            kenBurnsView = (KenBurnsView) findViewById(R.id.kenburnsView);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            heroImageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byteArray = stream.toByteArray();
            return null;
        }
    }
}