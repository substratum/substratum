package projekt.substratum;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import projekt.substrate.LetsGetStarted;
import projekt.substratum.adapters.InformationTabsAdapter;
import projekt.substratum.config.References;
import projekt.substratum.util.ReadOverlays;

import static projekt.substratum.config.References.SYSTEMUI_PAUSE;

public class InformationActivity extends AppCompatActivity {

    private static final int THEME_INFORMATION_REQUEST_CODE = 1;
    public static String theme_name, theme_pid, theme_mode;
    private static List<String> tab_checker;
    private static String wallpaperUrl;
    private Boolean refresh_mode = false;
    private Boolean uninstalled = false;
    private KenBurnsView kenBurnsView;
    private byte[] byteArray;
    private Bitmap heroImageBitmap;
    private SharedPreferences prefs;
    private AppBarLayout appBarLayout;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private View gradientView;
    private TabLayout tabLayout;
    private ProgressDialog mProgressDialog;

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

    private static void setOverflowButtonColor(final Activity activity, final Boolean dark_mode) {
        final String overflowDescription =
                activity.getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(() -> {
            final ArrayList<View> outViews = new ArrayList<>();
            decorView.findViewsWithText(outViews, overflowDescription,
                    View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
            if (outViews.isEmpty()) {
                return;
            }
            AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);
            if (dark_mode) {
                overflow.setImageResource(
                        activity.getResources().getIdentifier(
                                "information_activity_overflow_dark",
                                "drawable",
                                activity.getPackageName()));
            } else {
                overflow.setImageResource(
                        activity.getResources().getIdentifier(
                                "information_activity_overflow_light",
                                "drawable",
                                activity.getPackageName()));
            }
        });
    }

    public static int getDeviceEncryptionStatus(Context context) {
        // 0: ENCRYPTION_STATUS_UNSUPPORTED
        // 1: ENCRYPTION_STATUS_INACTIVE
        // 2: ENCRYPTION_STATUS_ACTIVATING
        // 3: ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY
        // 4: ENCRYPTION_STATUS_ACTIVE
        // 5: ENCRYPTION_STATUS_ACTIVE_PER_USER
        int status = DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED;
        final DevicePolicyManager dpm = (DevicePolicyManager)
                context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) {
            status = dpm.getStorageEncryptionStatus();
        }
        return status;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Image Cropper Request Capture
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            WallpaperManager myWallpaperManager
                    = WallpaperManager.getInstance(getApplicationContext());
            if (resultCode == RESULT_OK) {
                SharedPreferences.Editor editor = prefs.edit();
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
                        editor.putString("home_wallpaper_applied", theme_pid);
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.wallpaper_homescreen_success),
                                Snackbar.LENGTH_LONG)
                                .show();
                    } catch (IOException e) {
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.wallpaper_homescreen_error),
                                Snackbar.LENGTH_LONG)
                                .show();
                        e.printStackTrace();
                    }
                } else if (resultUri.toString().contains("lockscreen_wallpaper")) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            myWallpaperManager.setStream(new FileInputStream(
                                            resultUri.toString().substring(7)), null, true,
                                    WallpaperManager.FLAG_LOCK);
                        }
                        editor.putString("lock_wallpaper_applied", theme_pid);
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.wallpaper_lockscreen_success),
                                Snackbar.LENGTH_LONG)
                                .show();
                    } catch (IOException e) {
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.wallpaper_lockscreen_error),
                                Snackbar.LENGTH_LONG)
                                .show();
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
                        editor.putString("home_wallpaper_applied", theme_pid);
                        editor.putString("lock_wallpaper_applied", theme_pid);
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.wallpaper_allscreen_success),
                                Snackbar.LENGTH_LONG)
                                .show();
                    } catch (IOException e) {
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.wallpaper_allscreen_error),
                                Snackbar.LENGTH_LONG)
                                .show();
                        e.printStackTrace();
                    }
                }
                editor.apply();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Log.e("SubstratumImageCropper",
                        "There has been an error processing the image...");
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
        Boolean theme_legacy = currentIntent.getBooleanExtra("theme_legacy", false);
        refresh_mode = currentIntent.getBooleanExtra("refresh_mode", false);
        wallpaperUrl = null;
        String theme_author = References.grabThemeAuthor(getApplicationContext(), theme_pid);

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

        gradientView = findViewById(R.id.gradientView);
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById
                (R.id.collapsing_toolbar_tabbed_layout);
        if (collapsingToolbarLayout != null) collapsingToolbarLayout.setTitle(theme_name);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> onBackPressed());

        Drawable heroImage = grabPackageHeroImage(theme_pid);
        if (heroImage != null) heroImageBitmap = ((BitmapDrawable) heroImage).getBitmap();
        int dominantColor = getDominantColor(heroImageBitmap);

        appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
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

        if (References.isOffensive(theme_name)) {
            References.backupDebuggableStatistics(
                    getApplicationContext(),
                    "bannable-offence",
                    References.getDeviceID(getApplicationContext()),
                    theme_name);
        } else if (
                References.isOffensive(theme_author)) {
            References.backupDebuggableStatistics(
                    getApplicationContext(),
                    "bannable-offence",
                    References.getDeviceID(getApplicationContext()),
                    theme_author);
        }

        new LayoutLoader().execute("");

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        if (tabLayout != null) {
            if (theme_mode.equals("")) {
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
                                .theme_information_tab_one)));
                    }
                    if (tab_checker.contains("bootanimation")) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_two)));
                    }
                    if (tab_checker.contains("fonts")) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_three)));
                    }
                    if (tab_checker.contains("audio")) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_four)));
                    }
                    if (wallpaperUrl != null) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_five)));
                    }
                } catch (Exception e) {
                    Log.e(References.SUBSTRATUM_LOG, "Could not refresh list of asset folders.");
                }
            } else {
                if (theme_mode.equals("overlays")) {
                    tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                            .theme_information_tab_one)));
                } else {
                    if (theme_mode.equals("bootanimation")) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_two)));
                    } else {
                        if (theme_mode.equals("fonts")) {
                            tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                    .theme_information_tab_three)));
                        } else {
                            if (theme_mode.equals("audio")) {
                                tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                        .theme_information_tab_four)));
                            } else {
                                if (theme_mode.equals("wallpapers")) {
                                    tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                            .theme_information_tab_five)));
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
                        getColor(R.color.information_activity_dark_text_mode),
                        getColor(R.color.information_activity_dark_text_mode));

                Drawable upArrow = getDrawable(R.drawable.information_activity_back_dark);
                if (upArrow != null)
                    upArrow.setColorFilter(getColor(R.color.information_activity_dark_icon_mode),
                            PorterDuff.Mode.SRC_ATOP);
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
                setOverflowButtonColor(this, true);
            } else if (collapsingToolbarLayout != null) {
                collapsingToolbarLayout.setCollapsedTitleTextColor(
                        getColor(R.color.information_activity_light_icon_mode));
                collapsingToolbarLayout.setExpandedTitleColor(
                        getColor(R.color.information_activity_light_icon_mode));
                tabLayout.setTabTextColors(
                        getColor(R.color.information_activity_light_text_mode),
                        getColor(R.color.information_activity_light_text_mode));

                Drawable upArrow = getDrawable(R.drawable.information_activity_back_light);
                if (upArrow != null)
                    upArrow.setColorFilter(getColor(R.color.information_activity_light_icon_mode),
                            PorterDuff.Mode.SRC_ATOP);
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
                setOverflowButtonColor(this, false);
            }
        }
        final InformationTabsAdapter adapter = new InformationTabsAdapter
                (getSupportFragmentManager(), (tabLayout != null) ? tabLayout.getTabCount() : 0,
                        theme_mode, tab_checker, wallpaperUrl);

        if (viewPager != null) {
            viewPager.setOffscreenPageLimit((tabLayout != null) ? tabLayout.getTabCount() : 0);
            viewPager.setAdapter(adapter);
            viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener
                    (tabLayout));
            if (tabLayout != null) tabLayout.addOnTabSelectedListener(
                    new TabLayout.OnTabSelectedListener() {
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
        new AppShortcutCreator().execute("last_opened");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (References.checkOMS(getApplicationContext())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                getMenuInflater().inflate(R.menu.theme_information_menu_n_mr1, menu);
            } else {
                getMenuInflater().inflate(R.menu.theme_information_menu, menu);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                getMenuInflater().inflate(R.menu.theme_information_menu_legacy_n_mr1, menu);
            } else {
                getMenuInflater().inflate(R.menu.theme_information_menu_legacy, menu);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.favorite) {
            new AppShortcutCreator().execute("favorite");
            return true;
        }

        if (id == R.id.clean) {
            AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
            builder.setTitle(theme_name);
            builder.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
            builder.setMessage(R.string.clean_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, (dialog, id18) -> {
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
                                if (appInfo.metaData != null &&
                                        appInfo.metaData.getString(
                                                "Substratum_Parent") != null) {
                                    String parent =
                                            appInfo.metaData.getString("Substratum_Parent");
                                    if (parent != null && parent.equals(theme_pid)) {
                                        all_overlays.add(current);
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
                            new References.ThreadRunner().execute(commands2);
                        }
                    })
                    .setNegativeButton(R.string.uninstall_dialog_cancel, (dialog, id19) -> {
                        // User cancelled the dialog
                    });
            // Create the AlertDialog object and return it
            builder.create();
            builder.show();
            return true;
        }

        if (id == R.id.clean_cache) {
            AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
            builder.setTitle(theme_name);
            builder.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
            builder.setMessage(R.string.clean_cache_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, (dialog, id110) -> {
                        References.delete(getCacheDir().getAbsolutePath() +
                                "/SubstratumBuilder/" + theme_pid + "/");
                        String format =
                                String.format(
                                        getString(R.string.cache_clear_completion), theme_name);
                        Toast toast = Toast.makeText(getApplicationContext(), format,
                                Toast.LENGTH_LONG);
                        toast.show();
                        finish();
                    })
                    .setNegativeButton(R.string.uninstall_dialog_cancel, (dialog, id17) -> dialog.cancel());
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
                    .setPositiveButton(R.string.uninstall_dialog_okay, (dialog, id16) -> {
                        // Begin disabling all overlays based on this package
                        File current_overlays = new File(Environment
                                .getExternalStorageDirectory().getAbsolutePath() +
                                "/.substratum/current_overlays.xml");
                        if (current_overlays.exists()) {
                            References.delete(Environment
                                    .getExternalStorageDirectory().getAbsolutePath() +
                                    "/.substratum/current_overlays.xml");
                        }
                        References.copy("/data/system/overlays.xml",
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
                                if (appInfo.metaData != null &&
                                        appInfo.metaData.getString(
                                                "Substratum_Parent") != null) {
                                    String parent =
                                            appInfo.metaData.getString("Substratum_Parent");
                                    if (parent != null && parent.equals(theme_pid)) {
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

                        if (!prefs.getBoolean("systemui_recreate", false) &&
                                commands2.contains("systemui")) {
                            commands2 = commands2 + " && sleep " + SYSTEMUI_PAUSE + " && " +
                                    "pkill -f com.android.systemui";
                        }

                        if (References.isPackageInstalled(getApplicationContext(),
                                "masquerade.substratum")) {
                            Intent runCommand = new Intent();
                            runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                            runCommand.setAction("masquerade.substratum.COMMANDS");
                            runCommand.putExtra("om-commands", commands2);
                            getApplicationContext().sendBroadcast(runCommand);
                        } else {
                            new References.ThreadRunner().execute(commands2);
                        }
                    })
                    .setNegativeButton(R.string.uninstall_dialog_cancel, (dialog, id15) -> {
                        // User cancelled the dialog
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
                    .setPositiveButton(R.string.uninstall_dialog_okay, (dialog, id14) -> {
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
                                if (appInfo.metaData != null &&
                                        appInfo.metaData.getString(
                                                "Substratum_Parent") != null) {
                                    String parent =
                                            appInfo.metaData.getString("Substratum_Parent");
                                    if (parent != null && parent.equals(theme_pid)) {
                                        all_overlays.add(current);
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

                        if (!prefs.getBoolean("systemui_recreate", false) &&
                                commands2.contains("systemui")) {
                            commands2 = commands2 + " && sleep " + SYSTEMUI_PAUSE + " && " +
                                    "pkill -f com.android.systemui";
                        }

                        if (References.isPackageInstalled(getApplicationContext(),
                                "masquerade.substratum")) {
                            Intent runCommand = new Intent();
                            runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                            runCommand.setAction("masquerade.substratum.COMMANDS");
                            runCommand.putExtra("om-commands", commands2);
                            getApplicationContext().sendBroadcast(runCommand);
                        } else {
                            new References.ThreadRunner().execute(commands2);
                        }
                    })
                    .setNegativeButton(R.string.uninstall_dialog_cancel, (dialog, id13) -> dialog.cancel());
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
                Snackbar.make(findViewById(android.R.id.content),
                        getString(R.string.activity_missing_toast),
                        Snackbar.LENGTH_LONG)
                        .show();
            }
            return true;
        }
        if (id == R.id.uninstall) {
            AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
            builder.setTitle(theme_name);
            builder.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
            builder.setMessage(R.string.uninstall_dialog_body)
                    .setPositiveButton(R.string.uninstall_dialog_okay, (dialog, id12) -> new uninstallTheme().execute(""))
                    .setNegativeButton(R.string.uninstall_dialog_cancel, (dialog, id1) -> {
                        // User cancelled the dialog
                    });
            // Create the AlertDialog object and return it
            builder.create();
            builder.show();
            return true;
        }

        // Begin OMS based options
        if (id == R.id.restart_systemui) {
            References.restartSystemUI();
            return true;
        }

        // Begin RRO based options
        if (id == R.id.reboot_device) {
            References.reboot();
            return true;
        }
        if (id == R.id.soft_reboot) {
            References.softReboot();
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
            prefs.edit().putInt("uninstalled", THEME_INFORMATION_REQUEST_CODE).apply();
        } else {
            prefs.edit().putInt("uninstalled", 0).apply();
        }
    }

    private class LayoutLoader extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (References.spreadYourWingsAndFly(getApplicationContext()) ||
                    LetsGetStarted.overcomeMyBeauty()) {
                gradientView.setVisibility(View.GONE);
                kenBurnsView.setBackgroundColor(Color.parseColor("#ffff00"));
                collapsingToolbarLayout.setStatusBarScrimColor(Color.parseColor("#ffff00"));
                collapsingToolbarLayout.setContentScrimColor(Color.parseColor("#ffff00"));
                appBarLayout.setBackgroundColor(Color.parseColor("#ffff00"));
                tabLayout.setBackgroundColor(Color.parseColor("#ffff00"));
                getWindow().setNavigationBarColor(Color.parseColor("#ffff00"));
                LetsGetStarted.kissMe();
            } else {
                Glide.with(getApplicationContext()).load(byteArray).centerCrop().into(kenBurnsView);
            }

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

    private class AppShortcutCreator extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... sUrl) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
                Bitmap app_icon = ((BitmapDrawable)
                        References.grabAppIcon(getApplicationContext(), theme_pid)).getBitmap();
                try {
                    Intent myIntent = new Intent(Intent.ACTION_MAIN);
                    myIntent.putExtra("theme_name", theme_name);
                    myIntent.putExtra("theme_pid", theme_pid);
                    myIntent.setComponent(
                            ComponentName.unflattenFromString(
                                    "projekt.substratum/projekt.substratum.LaunchTheme"));
                    myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    ShortcutInfo shortcut =
                            new ShortcutInfo.Builder(getApplicationContext(), sUrl[0])
                                    .setShortLabel(((sUrl[0].equals("favorite")) ? "♥ " : "") +
                                            theme_name)
                                    .setLongLabel(((sUrl[0].equals("favorite")) ? "♥ " : "") +
                                            theme_name)
                                    .setIcon(Icon.createWithBitmap(app_icon))
                                    .setIntent(myIntent)
                                    .build();
                    List<ShortcutInfo> shortcuts = new ArrayList<>();
                    shortcuts.add(shortcut);
                    shortcutManager.addDynamicShortcuts(shortcuts);
                } catch (Exception e) {
                    // Suppress warning
                }
            }
            return null;
        }
    }

    private class uninstallTheme extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            String parseMe = String.format(getString(R.string.adapter_uninstalling),
                    theme_name);
            mProgressDialog = new ProgressDialog(InformationActivity.this);
            mProgressDialog.setMessage(parseMe);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            // Clear the notification of building theme if shown
            NotificationManager manager = (NotificationManager)
                    getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(References.notification_id);
        }

        @Override
        protected void onPostExecute(String result) {
            mProgressDialog.cancel();
            Toast toast = Toast.makeText(getApplicationContext(),
                    getString(R.string.clean_completion),
                    Toast.LENGTH_LONG);
            toast.show();
            uninstalled = true;
            onBackPressed();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            final SharedPreferences.Editor editor = prefs.edit();

            References.uninstallOverlay(theme_pid);

            // Begin uninstalling all overlays based on this package
            File current_overlays = new File(Environment
                    .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");
            if (current_overlays.exists()) {
                References.delete(Environment
                        .getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/current_overlays.xml");
            }
            References.copy("/data/system/overlays.xml",
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
                    if (appInfo.metaData != null &&
                            appInfo.metaData.getString(
                                    "Substratum_Parent") != null) {
                        String parent =
                                appInfo.metaData.getString("Substratum_Parent");
                        if (parent != null && parent.equals(theme_pid)) {
                            all_overlays.add(current);
                        }
                    }
                } catch (Exception e) {
                    // NameNotFound
                }
            }

            References.delete(getCacheDir().getAbsolutePath() +
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
                References.runCommands(commands2);
            }

            //Remove applied font, sounds, and bootanimation
            if (prefs.getString("sounds_applied", "").equals(theme_pid)) {
                References.delete("/data/system/theme/audio/ && pkill -f com" +
                        ".android.systemui");
                editor.remove("sounds_applied");
            }
            if (prefs.getString("fonts_applied", "").equals(theme_pid)) {
                int version = References.checkOMSVersion(getApplicationContext());
                if (version == 3) {
                    References.delete("/data/system/theme/fonts/");
                    References.runCommands(References.refreshWindows());
                } else if (version == 7) {
                    References.delete("/data/system/theme/fonts/");
                    References.mountRWData();
                    References.copyDir("/system/fonts/", "/data/system/theme/");
                    copyAssets();
                    References.move(getApplicationContext().getCacheDir().getAbsolutePath() +
                            "/FontCache/FontCreator/fonts.xml", "/data/system/theme/fonts/");

                    // Check for correct permissions and system file context integrity.
                    References.setPermissions(755, "/data/system/theme/");
                    References.setPermissionsRecursively(747, "/data/system/theme/fonts/");
                    References.setPermissions(775, "/data/system/theme/fonts/");
                    References.setContext("/data/system/theme");
                    References.setProp("sys.refresh_theme", "1");
                    References.mountROData();
                } else if (version == 0) {
                    References.delete("/data/system/theme/fonts/");
                }
                if (!prefs.getBoolean("systemui_recreate", false)) {
                    if (References.isPackageInstalled(getApplicationContext(),
                            "masquerade.substratum")) {
                        Intent runCommand = new Intent();
                        runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        runCommand.setAction("masquerade.substratum.COMMANDS");
                        runCommand.putExtra("om-commands", "pkill -f com.android.systemui");
                        getApplicationContext().sendBroadcast(runCommand);
                    } else {
                        References.restartSystemUI();
                    }
                }
                editor.remove("fonts_applied");
            }
            if (prefs.getString("bootanimation_applied", "").equals(theme_pid)) {
                if (getDeviceEncryptionStatus(getApplicationContext()) <= 1 && References.checkOMS(
                        getApplicationContext())) {
                    References.delete("/data/system/theme/bootanimation.zip");
                } else {
                    References.mountRW();
                    References.move("/system/media/bootanimation-backup.zip",
                            "/system/media/bootanimation.zip");
                    References.delete("/system/addon.d/81-subsboot.sh");
                }
                editor.remove("bootanimation_applied");
            }
            WallpaperManager wm = WallpaperManager.getInstance(
                    getApplicationContext());
            if (prefs.getString("home_wallpaper_applied", "").equals(theme_pid)) {
                try {
                    wm.clear();
                    editor.remove("home_wallpaper_applied");
                } catch (IOException e) {
                    Log.e("InformationActivity", "Failed to restore home screen " +
                            "wallpaper!");
                }
            }
            if (prefs.getString("lock_wallpaper_applied", "").equals(theme_pid)) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wm.clear(WallpaperManager.FLAG_LOCK);
                        editor.remove("lock_wallpaper_applied");
                    }
                } catch (IOException e) {
                    Log.e("InformationActivity", "Failed to restore lock screen " +
                            "wallpaper!");
                }
            }
            editor.apply();
            return null;
        }

        private void copyFile(InputStream in, OutputStream out) throws IOException {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }

        private void copyAssets() {
            AssetManager assetManager = getApplicationContext().getAssets();
            final String filename = "fonts.xml";
            try (InputStream in = assetManager.open(filename);
                 OutputStream out = new FileOutputStream(getApplicationContext().getCacheDir()
                         .getAbsolutePath() +
                         "/FontCache/FontCreator/" + filename)) {
                copyFile(in, out);
            } catch (IOException e) {
                Log.e("FontHandler", "Failed to move font configuration file to working " +
                        "directory!");
            }
        }
    }
}