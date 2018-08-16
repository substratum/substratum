/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.ColorUtils;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.palette.graphics.Palette;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.gordonwong.materialsheetfab.DimOverlayFrameLayout;
import com.gordonwong.materialsheetfab.MaterialSheetFab;
import com.gordonwong.materialsheetfab.MaterialSheetFabEventListener;
import com.theartofdev.edmodo.cropper.CropImage;
import org.apache.commons.io.output.ByteArrayOutputStream;
import projekt.substratum.adapters.activities.IATabsAdapter;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Internal;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Resources;
import projekt.substratum.common.Systems;
import projekt.substratum.common.Theming;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.databinding.InformationActivityBinding;
import projekt.substratum.tabs.BootAnimations;
import projekt.substratum.tabs.Overlays;
import projekt.substratum.tabs.Wallpapers;
import projekt.substratum.tabs.WallpapersManager;
import projekt.substratum.util.helpers.LocaleHelper;
import projekt.substratum.util.helpers.Root;
import projekt.substratum.util.views.FloatingActionMenu;
import projekt.substratum.util.views.Lunchbar;
import projekt.substratum.util.views.SheetDialog;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static android.graphics.Bitmap.CompressFormat.PNG;
import static projekt.substratum.common.Internal.ANDROMEDA_RECEIVER;
import static projekt.substratum.common.Internal.COMPILE_ENABLE;
import static projekt.substratum.common.Internal.COMPILE_UPDATE;
import static projekt.substratum.common.Internal.DISABLE;
import static projekt.substratum.common.Internal.ENABLE;
import static projekt.substratum.common.Internal.ENABLE_DISABLE;
import static projekt.substratum.common.Internal.ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.IV_ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.MIX_AND_MATCH;
import static projekt.substratum.common.Internal.MIX_AND_MATCH_IA_TO_OVERLAYS;
import static projekt.substratum.common.Internal.SCROLL_UP;
import static projekt.substratum.common.Internal.SHEET_COMMAND;
import static projekt.substratum.common.Internal.START_JOB_ACTION;
import static projekt.substratum.common.Internal.THEME_NAME;
import static projekt.substratum.common.Internal.THEME_PID;
import static projekt.substratum.common.Internal.THEME_WALLPAPER;
import static projekt.substratum.common.Packages.getOverlayMetadata;
import static projekt.substratum.common.Packages.getPackageHeroImage;
import static projekt.substratum.common.References.ACTIVITY_FINISHER;
import static projekt.substratum.common.References.BYPASS_SUBSTRATUM_BUILDER_DELETION;
import static projekt.substratum.common.References.MANAGER_REFRESH;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.References.bootAnimationsFolder;
import static projekt.substratum.common.References.bootAnimationsFragment;
import static projekt.substratum.common.References.fontsFolder;
import static projekt.substratum.common.References.fontsFragment;
import static projekt.substratum.common.References.metadataHeroOverride;
import static projekt.substratum.common.References.metadataOverlayParent;
import static projekt.substratum.common.References.metadataWallpapers;
import static projekt.substratum.common.References.overlaysFolder;
import static projekt.substratum.common.References.overlaysFragment;
import static projekt.substratum.common.References.shutdownAnimationsFolder;
import static projekt.substratum.common.References.soundsFolder;
import static projekt.substratum.common.References.soundsFragment;
import static projekt.substratum.common.References.wallpaperFragment;
import static projekt.substratum.common.Systems.checkPackageSupport;
import static projekt.substratum.common.Systems.isSamsung;

public class InformationActivity extends AppCompatActivity {

    private static final int LUNCHBAR_DISMISS_FAB_CLICK_DELAY = 200;
    public static Snackbar currentShownLunchBar;
    public static boolean compilingProcess = false;
    public static boolean shouldRestartActivity = false;
    private static List<String> tabChecker;
    private TabLayout tabLayout;
    private CollapsingToolbarLayout collapsingToolbar;
    private TextView toolbarCollapsedTitle;
    private ViewPager viewPager;
    private AppBarLayout appBarLayout;
    private FloatingActionMenu floatingActionButton;
    private ImageView heroImage;

    private String themeName;
    private String themePid;
    private boolean uninstalled = false;
    private byte[] byteArray;
    private Bitmap heroImageBitmap;
    private SharedPreferences prefs = Substratum.getPreferences();
    private ProgressDialog mProgressDialog;
    private MenuItem favorite;
    private boolean shouldDarken;
    private MaterialSheetFab materialSheetFab;
    private int tabPosition;
    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver refreshReceiver;
    private AndromedaReceiver andromedaReceiver;
    private ActivityFinisher activityFinisher;
    private int dominantColor;
    private Context context;

    /**
     * Function to get the dominant color out of a specific image
     *
     * @param bitmap Bitmap to collect data from
     * @return Returns the color that is dominant
     */
    private static int getDominantColor(Context context, Bitmap bitmap) {
        try {
            int transparency = ((bitmap.getPixel(0, 0) & 0xff000000) >> 24);
            if (transparency != 0) {
                Palette palette = Palette.from(bitmap).generate();
                return palette.getDominantColor(
                        context.getColor(R.color.main_screen_card_background));
            } else {
                return context.getColor(R.color.colorPrimary);
            }
        } catch (IllegalArgumentException ignored) {
        }
        return context.getColor(R.color.main_screen_card_background);
    }

    /**
     * Depending on the hero image, change the actionbar color buttons
     *
     * @param activity Activity to change it in
     * @param darkMode True if dark, false if light
     */
    private static void setOverflowButtonColor(Activity activity, boolean darkMode) {
        @SuppressLint("PrivateResource") String overflowDescription =
                activity.getString(R.string.abc_action_menu_overflow_description);
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(() -> {
            ArrayList<View> outViews = new ArrayList<>();
            decorView.findViewsWithText(
                    outViews,
                    overflowDescription,
                    View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
            if (outViews.isEmpty()) {
                return;
            }
            for (View view : outViews) {
                if (view instanceof AppCompatImageView) {
                    AppCompatImageView overflow = (AppCompatImageView) view;
                    overflow.setImageResource(darkMode ? R.drawable
                            .information_activity_overflow_dark :
                            R.drawable.information_activity_overflow_light);
                }
            }
        });
    }

    /**
     * Simple function to close all the screen displayed LunchBars
     *
     * @return True, if all LunchBars are closed.
     */
    private static boolean closeAllLunchBars() {
        if (currentShownLunchBar != null) {
            currentShownLunchBar.dismiss();
            currentShownLunchBar = null;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determine whether the color specified is dark or not
     *
     * @param color Color to be checked
     * @return True, if dark
     */
    private static boolean checkColorDarkness(int color) {
        double darkness =
                1.0 - (((0.299 * (double) Color.red(color)) +
                        (0.587 * (double) Color.green(color)) +
                        (0.114 * (double) Color.blue(color))) / 255.0);
        return darkness < 0.5;
    }

    private static View getLunchbarView(InformationActivity activity) {
        View coordinatorLayout = References.getCoordinatorLayoutView(activity);
        if (coordinatorLayout != null) {
            return coordinatorLayout;
        }
        return References.getView(activity);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    /**
     * Automatically set the toolbar icons based on whether the hero image is dark or not
     */
    private void autoSetToolbarIcons() {
        if (InformationActivity.checkColorDarkness(dominantColor)) {
            shouldDarken = true;
            setDarkToolbarIcons();
        } else {
            shouldDarken = false;
            setLightToolbarIcons();
        }
    }

    /**
     * Sets the toolbar icons to be dark
     */
    private void setDarkToolbarIcons() {
        toolbarCollapsedTitle.setTextColor(
                getColor(R.color.information_activity_dark_icon_mode));
        collapsingToolbar.setCollapsedTitleTextColor(
                getColor(R.color.information_activity_dark_icon_mode));
        collapsingToolbar.setExpandedTitleColor(
                getColor(R.color.information_activity_dark_icon_mode));
        tabLayout.setTabTextColors(
                getColor(R.color.information_activity_dark_text_mode),
                getColor(R.color.information_activity_dark_text_mode));

        View v = getWindow().getDecorView();
        int flags = v.getSystemUiVisibility();
        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        } else {
            // we can't make the nav icons dark, so we'll just make the background darker for a bit
            getWindow().setNavigationBarColor(ColorUtils.blendARGB(dominantColor, 0x000, 0.8f));
        }
        v.setSystemUiVisibility(flags);

        Drawable upArrow = getDrawable(R.drawable.information_activity_back_dark);
        if (upArrow != null)
            upArrow.setColorFilter(getColor(R.color.information_activity_dark_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
        }
        setOverflowButtonColor(this, true);
    }

    /**
     * Sets the toolbar icons to be light
     */
    private void setLightToolbarIcons() {
        toolbarCollapsedTitle.setTextColor(
                getColor(R.color.information_activity_light_icon_mode));
        collapsingToolbar.setCollapsedTitleTextColor(
                getColor(R.color.information_activity_light_icon_mode));
        collapsingToolbar.setExpandedTitleColor(
                getColor(R.color.information_activity_light_icon_mode));
        tabLayout.setTabTextColors(
                getColor(R.color.information_activity_light_text_mode),
                getColor(R.color.information_activity_light_text_mode));

        View v = getWindow().getDecorView();
        int flags = v.getSystemUiVisibility();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        v.setSystemUiVisibility(flags);

        Drawable upArrow = getDrawable(R.drawable.information_activity_back_light);
        if (upArrow != null)
            upArrow.setColorFilter(getColor(R.color.information_activity_light_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
        }
        setOverflowButtonColor(this, false);
    }

    /**
     * Handle all the startActivityWithResult calls
     *
     * @param requestCode Code used to feed into activity start
     * @param resultCode  Code that was fed to this activity after succession
     * @param data        Result from the received startActivityWithResult
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Image Cropper Request Capture
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                SharedPreferences.Editor editor = prefs.edit();
                Uri resultUri = result.getUri();
                if (resultUri.toString().contains("homescreen_wallpaper")) {
                    try {
                        WallpapersManager.setWallpaper(
                                context,
                                resultUri.toString().substring(7),
                                "home");
                        editor.putString("home_wallpaper_applied", themePid);
                        currentShownLunchBar = Lunchbar.make(getLunchbarView(this),
                                getString(R.string.wallpaper_homescreen_success),
                                Snackbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    } catch (IOException e) {
                        currentShownLunchBar = Lunchbar.make(getLunchbarView(this),
                                getString(R.string.wallpaper_homescreen_error),
                                Snackbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                        e.printStackTrace();
                    }
                } else if (resultUri.toString().contains("lockscreen_wallpaper")) {
                    try {
                        WallpapersManager.setWallpaper(
                                context,
                                resultUri.toString().substring(7),
                                "lock");
                        editor.putString("lock_wallpaper_applied", themePid);
                        currentShownLunchBar = Lunchbar.make(getLunchbarView(this),
                                getString(R.string.wallpaper_lockscreen_success),
                                Snackbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    } catch (IOException e) {
                        currentShownLunchBar = Lunchbar.make(getLunchbarView(this),
                                getString(R.string.wallpaper_lockscreen_error),
                                Snackbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                        e.printStackTrace();
                    }
                } else if (resultUri.toString().contains("all_wallpaper")) {
                    try {
                        WallpapersManager.setWallpaper(
                                context,
                                resultUri.toString().substring(7),
                                "all");
                        editor.putString("home_wallpaper_applied", themePid);
                        editor.putString("lock_wallpaper_applied", themePid);
                        currentShownLunchBar = Lunchbar.make(getLunchbarView(this),
                                getString(R.string.wallpaper_allscreen_success),
                                Snackbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    } catch (IOException e) {
                        currentShownLunchBar = Lunchbar.make(getLunchbarView(this),
                                getString(R.string.wallpaper_allscreen_error),
                                Snackbar.LENGTH_LONG);
                        currentShownLunchBar.show();
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
        context = getApplicationContext();

        super.onCreate(savedInstanceState);
        InformationActivityBinding binding =
                DataBindingUtil.setContentView(this, R.layout.information_activity);

        Toolbar toolbar = binding.toolbar;
        tabLayout = binding.tabs;
        collapsingToolbar = binding.collapsingToolbarTabbedLayout;
        toolbarCollapsedTitle = binding.toolbarCollapsedTitle;
        viewPager = binding.viewpager;
        appBarLayout = binding.appbar;
        CardView sheetView = binding.fabSheet;
        DimOverlayFrameLayout overlay = binding.overlay;
        Switch enableSwap = binding.enableSwap;
        TextView compileEnableSelected = binding.compileEnableSelected;
        TextView compileUpdateSelected = binding.compileUpdateSelected;
        TextView disableSelected = binding.disableSelected;
        floatingActionButton = binding.applyFab;
        View fabMenuDivider = binding.fabMenuDivider;
        LinearLayout enableZone = binding.enable;
        TextView enableSelected = binding.enableSelected;
        TextView enableDisableSelected = binding.enableDisableSelected;
        heroImage = binding.heroImage;

        localBroadcastManager = LocalBroadcastManager.getInstance(context);

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new RefreshReceiver();
        localBroadcastManager.registerReceiver(refreshReceiver,
                new IntentFilter(MANAGER_REFRESH));

        // Activity finisher is for when the activity should be closed by a specific intent
        activityFinisher = new ActivityFinisher();
        localBroadcastManager.registerReceiver(activityFinisher,
                new IntentFilter(ACTIVITY_FINISHER));

        // If the device is Andromeda based, we must take account for the disconnection phase
        if (Systems.isAndromedaDevice(context)) {
            andromedaReceiver = new InformationActivity.AndromedaReceiver();
            localBroadcastManager.registerReceiver(andromedaReceiver,
                    new IntentFilter(ANDROMEDA_RECEIVER));
        }

        // Themer's booleans to adjust actionbar and navbar colors
        boolean dynamicActionBarColors = getResources().getBoolean(R.bool.dynamicActionBarColors);
        boolean dynamicNavBarColors = getResources().getBoolean(R.bool.dynamicNavigationBarColors);

        // Obtain the current intent to receive the intent data out of it
        Intent currentIntent = getIntent();
        themeName = currentIntent.getStringExtra(THEME_NAME);
        themePid = currentIntent.getStringExtra(THEME_PID);
        byte[] encryptionKey = currentIntent.getByteArrayExtra(ENCRYPTION_KEY_EXTRA);
        byte[] ivEncryptKey = currentIntent.getByteArrayExtra(IV_ENCRYPTION_KEY_EXTRA);
        String wallpaperUrl = getOverlayMetadata(context, themePid,
                metadataWallpapers);

        // Package the intent data into a new bundle
        Bundle bundle = new Bundle();
        bundle.putString(THEME_NAME, themeName);
        bundle.putString(THEME_PID, themePid);
        bundle.putByteArray(ENCRYPTION_KEY_EXTRA, encryptionKey);
        bundle.putByteArray(IV_ENCRYPTION_KEY_EXTRA, ivEncryptKey);
        bundle.putString(THEME_WALLPAPER, wallpaperUrl);

        // Configure the views
        toolbar.setTitle(themeName);
        collapsingToolbar.setTitle(themeName);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Hero Image
        try {
            Drawable hero = getPackageHeroImage(getApplicationContext(), themePid, false);
            heroImageBitmap = ((BitmapDrawable) hero).getBitmap();
        } catch (Exception ignored) {
        }
        dominantColor = heroImageBitmap == null ?
                Color.TRANSPARENT : getDominantColor(getApplicationContext(), heroImageBitmap);

        if (prefs.getBoolean("lite_mode", false)) {
            heroImage.setVisibility(View.GONE);
            appBarLayout.setExpanded(false, false);
            CoordinatorLayout.LayoutParams lp =
                    (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
            lp.height = (int) getResources().getDimension(R.dimen.toolbar_height);
            toolbarCollapsedTitle.setVisibility(View.VISIBLE);
            toolbarCollapsedTitle.setText(themeName);
        } else {
            heroImage.setContentDescription(themeName);
        }

        // Set the AppBarLayout to have the background color of the dominant color in hero
        appBarLayout.setBackgroundColor(dominantColor);

        // Set the actionbar colors to dominant color
        if (dynamicActionBarColors) {
            collapsingToolbar.setStatusBarScrimColor(dominantColor);
            collapsingToolbar.setContentScrimColor(dominantColor);
        }

        // Set the navbar colors to dominant color
        if (dynamicNavBarColors) {
            getWindow().setStatusBarColor(dominantColor);
            getWindow().setNavigationBarColor(dominantColor);
        }

        if (!dynamicActionBarColors || !dynamicNavBarColors) {
            heroImage.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
            appBarLayout.setBackgroundColor(getWindow().getStatusBarColor());
        }

        // Show the FAB
        floatingActionButton.show();

        // The fab needs to have some special colors
        int sheetColor = context.getColor(R.color.fab_menu_background_card);
        int fabColor = context.getColor(R.color.fab_background_color);

        // Create material sheet FAB
        materialSheetFab = new MaterialSheetFab<>(
                floatingActionButton, sheetView, overlay, sheetColor, fabColor);

        // Okay, time for the meat of the reloader
        new LayoutLoader(this).execute("");

        try {
            Context otherContext = context.createPackageContext
                    (themePid, 0);
            AssetManager am = otherContext.getAssets();
            String[] foundFolders = am.list("");
            tabChecker = new ArrayList<>();
            if (!Systems.checkOMS(context)) {
                for (String foundFolder : foundFolders) {
                    if (Resources.allowedForLegacy
                            (foundFolder)) {
                        tabChecker.add(foundFolder);
                    }
                }
            } else {
                tabChecker = Arrays.asList(am.list(""));
            }
            boolean isWallpaperOnly = true;
            if (tabChecker.contains(overlaysFolder)) {
                isWallpaperOnly = false;
                tabLayout.addTab(tabLayout.newTab().setText(getString(R
                        .string
                        .theme_information_tab_one)));
            }
            if (tabChecker.contains(bootAnimationsFolder) &&
                    Resources.isBootAnimationSupported(context)) {
                isWallpaperOnly = false;
                tabLayout.addTab(tabLayout.newTab().setText(getString(R
                        .string
                        .theme_information_tab_two)));
            }
            if (tabChecker.contains(shutdownAnimationsFolder) &&
                    Resources.isShutdownAnimationSupported(context)) {
                isWallpaperOnly = false;
                tabLayout.addTab(tabLayout.newTab().setText(getString(R
                        .string
                        .theme_information_tab_six)));
            }
            if (tabChecker.contains(fontsFolder) && Resources.isFontsSupported(context)) {
                isWallpaperOnly = false;
                tabLayout.addTab(tabLayout.newTab().setText(getString(R
                        .string
                        .theme_information_tab_three)));
            }
            if (tabChecker.contains(soundsFolder) &&
                    Resources.isSoundsSupported(context)) {
                isWallpaperOnly = false;
                tabLayout.addTab(tabLayout.newTab().setText(getString(R
                        .string
                        .theme_information_tab_four)));
            }
            if ((wallpaperUrl != null) && !wallpaperUrl.isEmpty()) {
                tabLayout.addTab(tabLayout.newTab().setText(getString(R
                        .string
                        .theme_information_tab_five)));
            }
            if (isWallpaperOnly && (wallpaperUrl != null) && !wallpaperUrl.isEmpty()) {
                Handler handler = new Handler();
                handler.postDelayed(() ->
                        runOnUiThread(floatingActionButton::hide), 500L);
            }
        } catch (Exception e) {
            Log.e(References.SUBSTRATUM_LOG, "Could not refresh list of asset folders.");
        }

        // Set the tabs to move in a certain way
        tabLayout.setTabGravity(TabLayout.MODE_SCROLLABLE);
        if (dynamicActionBarColors)
            tabLayout.setBackgroundColor(dominantColor);

        String toOverrideHero =
                Packages.getOverlayMetadata(
                        getApplicationContext(),
                        themePid,
                        metadataHeroOverride);
        if (toOverrideHero != null) {
            switch (toOverrideHero) {
                case "dark":
                    setDarkToolbarIcons();
                    break;
                case "light":
                    setLightToolbarIcons();
                    break;
                default:
                    autoSetToolbarIcons();
                    break;
            }
        } else {
            autoSetToolbarIcons();
        }

        HashMap<String, Boolean> extraHiddenTabs = new HashMap<>();
        // Boot animation visibility
        extraHiddenTabs.put(bootAnimationsFolder, Resources.isBootAnimationSupported(context));
        // Shutdown animation visibility
        extraHiddenTabs.put(shutdownAnimationsFolder,
                Resources.isShutdownAnimationSupported(context));
        // Fonts visibility
        extraHiddenTabs.put(fontsFolder, Resources.isFontsSupported(context));
        // Sounds visibility
        extraHiddenTabs.put(soundsFolder, Resources.isSoundsSupported(context));

        // If there are no tabs, then the theme is completely empty. Show toast and quit.
        if (extraHiddenTabs.size() == 0 || tabLayout.getTabCount() == 0) {
            String format = String.format(
                    getString(R.string.information_activity_theme_improperly_setup),
                    themeName);
            Toast.makeText(this, format, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Set up the tabs
        IATabsAdapter adapter = new IATabsAdapter(
                getSupportFragmentManager(),
                tabLayout.getTabCount(),
                tabChecker,
                wallpaperUrl,
                extraHiddenTabs,
                bundle);
        viewPager.setOffscreenPageLimit(tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(
                new TabLayout.TabLayoutOnPageChangeListener(tabLayout) {
                    @Override
                    public void onPageSelected(int position) {
                        tabPosition = position;
                        if (viewPager.getAdapter() != null) {
                            switch (viewPager.getAdapter().instantiateItem(viewPager, tabPosition)
                                    .getClass().getSimpleName().toLowerCase(Locale.US)) {
                                case overlaysFragment:
                                    floatingActionButton.show();
                                    floatingActionButton.setImageResource(
                                            R.drawable.floating_action_button_icon);
                                    break;
                                case bootAnimationsFragment:
                                case fontsFragment:
                                case soundsFragment:
                                    floatingActionButton.show();
                                    floatingActionButton.setImageResource(
                                            R.drawable.floating_action_button_icon_check);
                                    break;
                                case wallpaperFragment:
                                    floatingActionButton.hide();
                                    break;
                            }
                        }
                    }
                });
        tabLayout.addOnTabSelectedListener(
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
                        viewPager.setCurrentItem(tab.getPosition());
                    }
                });

        PagerAdapter adapt = viewPager.getAdapter();
        LocalBroadcastManager localBroadcastManager =
                LocalBroadcastManager.getInstance(context);
        floatingActionButton.setOnClickListener(v -> {
            try {
                boolean isLunchbarOpen = InformationActivity.closeAllLunchBars();
                new Handler().postDelayed(() -> {
                    Intent intent;
                    if (adapt != null) {
                        Object obj = adapt.instantiateItem(viewPager, tabPosition);
                        switch (obj.getClass().getSimpleName().toLowerCase(Locale.US)) {
                            case overlaysFragment:
                                materialSheetFab.showSheet();
                                break;
                            case bootAnimationsFragment:
                                boolean isShutdownTab = ((BootAnimations) obj).isShutdownTab();
                                intent = new Intent((isShutdownTab ? "ShutdownAnimations" :
                                        "BootAnimations") + START_JOB_ACTION);
                                localBroadcastManager.sendBroadcast(intent);
                                break;
                            case fontsFragment:
                                intent = new Intent("Fonts" + START_JOB_ACTION);
                                localBroadcastManager.sendBroadcast(intent);
                                break;
                            case soundsFragment:
                                intent = new Intent("Sounds" + START_JOB_ACTION);
                                localBroadcastManager.sendBroadcast(intent);
                                break;
                        }
                    }
                }, (long) (isLunchbarOpen ? LUNCHBAR_DISMISS_FAB_CLICK_DELAY : 0));
            } catch (NullPointerException ignored) {
            }
        });

        toolbar.setOnClickListener(v -> {
            if (adapt != null) {
                Intent intent = new Intent();
                Object obj = adapt.instantiateItem(viewPager, tabPosition);
                switch (obj.getClass().getSimpleName().toLowerCase(Locale.US)) {
                    case overlaysFragment:
                        intent = new Intent("Overlays" + START_JOB_ACTION);
                        intent.putExtra(SHEET_COMMAND, SCROLL_UP);
                        break;
                    case wallpaperFragment:
                        intent = new Intent("Wallpapers" + START_JOB_ACTION);
                        break;
                }
                localBroadcastManager.sendBroadcast(intent);
            }
        });

        // This is for the Floating Action Menu actions
        Intent intent = new Intent("Overlays" + START_JOB_ACTION);
        if (!Systems.checkOMS(this) && !Systems.isSamsungDevice(context)) {
            enableSwap.setText(getString(R.string.fab_menu_swap_toggle_legacy));
        } else if (Systems.isSamsung(context)) {
            fabMenuDivider.setVisibility(View.GONE);
            enableSwap.setVisibility(View.GONE);
        }
        boolean enabled = prefs.getBoolean("enable_swapping_overlays", true);
        intent.putExtra(SHEET_COMMAND, MIX_AND_MATCH);
        intent.putExtra(MIX_AND_MATCH_IA_TO_OVERLAYS, enabled);
        localBroadcastManager.sendBroadcast(intent);
        enableSwap.setChecked(enabled);
        enableSwap.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("enable_swapping_overlays", isChecked).apply();
            intent.putExtra(SHEET_COMMAND, MIX_AND_MATCH);
            intent.putExtra(MIX_AND_MATCH_IA_TO_OVERLAYS, isChecked);
            localBroadcastManager.sendBroadcast(intent);
        });

        if (!Systems.checkOMS(this)) compileEnableSelected.setVisibility(View.GONE);
        compileEnableSelected.setOnClickListener(v -> {
            materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                @Override
                public void onSheetHidden() {
                    super.onSheetHidden();
                    intent.putExtra(SHEET_COMMAND, COMPILE_ENABLE);
                    localBroadcastManager.sendBroadcast(intent);
                    materialSheetFab.setEventListener(null);
                }
            });
            materialSheetFab.hideSheet();
        });

        if (!Systems.checkOMS(this)) {
            compileUpdateSelected.setText(getString(R.string.fab_menu_compile_install));
        }
        compileUpdateSelected.setOnClickListener(v -> {
            materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                @Override
                public void onSheetHidden() {
                    super.onSheetHidden();
                    intent.putExtra(SHEET_COMMAND, COMPILE_UPDATE);
                    localBroadcastManager.sendBroadcast(intent);
                    materialSheetFab.setEventListener(null);
                }
            });
            materialSheetFab.hideSheet();
        });

        if (!Systems.checkOMS(this)) {
            disableSelected.setText(getString(R.string.fab_menu_uninstall));
        }
        disableSelected.setOnClickListener(v -> {
            materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                @Override
                public void onSheetHidden() {
                    super.onSheetHidden();
                    intent.putExtra(SHEET_COMMAND, DISABLE);
                    localBroadcastManager.sendBroadcast(intent);
                    materialSheetFab.setEventListener(null);
                }
            });
            materialSheetFab.hideSheet();
        });

        if (!Systems.checkOMS(this)) enableZone.setVisibility(View.GONE);
        enableSelected.setOnClickListener(v -> {
            materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                @Override
                public void onSheetHidden() {
                    super.onSheetHidden();
                    intent.putExtra(SHEET_COMMAND, ENABLE);
                    localBroadcastManager.sendBroadcast(intent);
                    materialSheetFab.setEventListener(null);
                }
            });
            materialSheetFab.hideSheet();
        });

        if (!Systems.checkOMS(this))
            enableDisableSelected.setVisibility(View.GONE);
        enableDisableSelected.setOnClickListener(v -> {
            materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                @Override
                public void onSheetHidden() {
                    super.onSheetHidden();
                    intent.putExtra(SHEET_COMMAND, ENABLE_DISABLE);
                    localBroadcastManager.sendBroadcast(intent);
                    materialSheetFab.setEventListener(null);
                }
            });
            materialSheetFab.hideSheet();
        });

        boolean shouldShowSamsungWarning =
                !prefs.getBoolean("show_dangerous_samsung_overlays", false);
        if (Systems.isSamsung(context) &&
                Packages.isSamsungTheme(context, themePid) &&
                shouldShowSamsungWarning) {
            currentShownLunchBar = Lunchbar.make(
                    getLunchbarView(this),
                    getString(R.string.toast_samsung_prototype_alert),
                    Snackbar.LENGTH_SHORT);
            currentShownLunchBar.show();
        }
        Thread currentThread = Substratum.currentThread;
        if ((currentThread == null || !currentThread.isAlive()) &&
                (Systems.isSamsung(context) ||
                        (Systems.IS_OREO &&
                                Systems.isNewSamsungDevice()))) {
            Substratum.startSamsungPackageMonitor(context);
        }
    }

    /**
     * Creating the options menu (3dot overflow menu)
     *
     * @param menu Menu object
     * @return True if success
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.theme_information_menu, menu);

        // Start dynamically showing menu items
        boolean isOMS = Systems.checkOMS(context);
        boolean isMR1orHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;

        if (!isMR1orHigher) menu.findItem(R.id.favorite).setVisible(false);
        if (isMR1orHigher) {
            favorite = menu.findItem(R.id.favorite);
            if (prefs.contains("app_shortcut_theme")) {
                if (prefs.getString("app_shortcut_theme", "").equals(themePid)) {
                    favorite.setIcon(getDrawable(R.drawable.toolbar_favorite));
                } else {
                    favorite.setIcon(getDrawable(R.drawable.toolbar_not_favorite));
                }
            } else {
                favorite.setIcon(getDrawable(R.drawable.toolbar_not_favorite));
            }
            if (shouldDarken) favorite.getIcon().setColorFilter(
                    getColor(R.color.information_activity_dark_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
        }
        if (Packages.getThemeChangelog(context, themePid) != null) {
            MenuItem changelog = menu.findItem(R.id.changelog);
            changelog.setVisible(true);
            if (shouldDarken) changelog.getIcon().setColorFilter(
                    getColor(R.color.information_activity_dark_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
        }

        if (Systems.checkAndromeda(context) ||
                (!isOMS && !Root.checkRootAccess())) {
            menu.findItem(R.id.restart_systemui).setVisible(false);
        }
        if (Systems.isNewSamsungDeviceAndromeda(context) || !isOMS) {
            menu.findItem(R.id.disable).setVisible(false);
            menu.findItem(R.id.enable).setVisible(false);
        }
        if (isOMS || isSamsung(context)) {
            if (isSamsung(context)) menu.findItem(R.id.clean).setVisible(false);
            menu.findItem(R.id.reboot_device).setVisible(false);
            menu.findItem(R.id.soft_reboot).setVisible(false);
            menu.findItem(R.id.uninstall).setVisible(false);
        }

        if (!Packages.isUserApp(context, themePid)) {
            menu.findItem(R.id.uninstall).setVisible(false);
        }
        return true;
    }

    /**
     * Assign actions to every option when they are selected
     *
     * @param item Object of menu item
     * @return True, if something has changed.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.favorite:
                if (prefs.contains("app_shortcut_theme")) {
                    if (prefs.getString("app_shortcut_theme", "").equals(themePid)) {
                        new AppShortcutClearer(this).execute("");
                    } else {
                        new AppShortcutCreator(this).execute("favorite");
                    }
                } else {
                    new AppShortcutCreator(this).execute("favorite");
                }
                return true;
            case R.id.changelog:
                // This opens the changelog bottom sheet dialog for themers
                SheetDialog sheetDialog = new SheetDialog(this);
                @SuppressLint("InflateParams") View sheetView = getLayoutInflater()
                        .inflate(R.layout.information_activity_changelog_sheet_dialog, null);

                LinearLayout titleBox = sheetView.findViewById(R.id.title_box);
                TextView title = titleBox.findViewById(R.id.title);
                String formatMe = String.format(
                        getString(R.string.changelog_title),
                        themeName);
                title.setText(formatMe);

                LinearLayout textBox = sheetView.findViewById(R.id.text_box);
                TextView text = textBox.findViewById(R.id.text);

                String[] themeChangelog =
                        Packages.getThemeChangelog(context, themePid);
                StringBuilder parsedThemeChangelog = new StringBuilder();
                if (themeChangelog != null) {
                    for (String changelogEntry : themeChangelog) {
                        parsedThemeChangelog.append("\u2022 ").append(changelogEntry).append('\n');
                    }
                }
                text.setText(parsedThemeChangelog.toString());
                sheetDialog.setCanceledOnTouchOutside(true);
                sheetDialog.setContentView(sheetView);
                sheetDialog.show();
                return true;
            case R.id.clean:
                // This cleans all the installed overlays for this theme
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setTitle(themeName);
                builder1.setIcon(Packages.getAppIcon(context, themePid));
                builder1.setMessage(R.string.clean_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id18) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all installed overlays
                            List<String> stateAll = ThemeManager.listAllOverlays(context);

                            ArrayList<String> allOverlays = new ArrayList<>();
                            for (String state : stateAll) {
                                try {
                                    ApplicationInfo appInfo = context
                                            .getPackageManager().getApplicationInfo(
                                                    state, PackageManager.GET_META_DATA);
                                    if ((appInfo.metaData != null) &&
                                            (appInfo.metaData.getString(
                                                    metadataOverlayParent) != null)) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if ((parent != null) && parent.equals(themePid)) {
                                            allOverlays.add(state);
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            }

                            // Begin uninstalling overlays for this package
                            ThemeManager.uninstallOverlay(
                                    context,
                                    allOverlays
                            );
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialog, id19) -> {
                            // User cancelled the dialog
                        });
                // Create the AlertDialog object and return it
                builder1.create();
                builder1.show();
                return true;
            case R.id.disable:
                // This disables all the installed overlays for this theme
                AlertDialog.Builder builder3 = new AlertDialog.Builder(InformationActivity.this);
                builder3.setTitle(themeName);
                builder3.setIcon(Packages.getAppIcon(context, themePid));
                builder3.setMessage(R.string.disable_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id16) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all enabled overlays
                            List<String> stateAll = ThemeManager.listOverlays(
                                    context, ThemeManager.STATE_ENABLED);

                            ArrayList<String> allOverlays = new ArrayList<>();
                            for (String state : stateAll) {
                                try {
                                    ApplicationInfo appInfo = context
                                            .getPackageManager().getApplicationInfo(
                                                    state, PackageManager.GET_META_DATA);
                                    if ((appInfo.metaData != null) &&
                                            (appInfo.metaData.getString(
                                                    metadataOverlayParent) != null)) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if ((parent != null) && parent.equals(themePid)) {
                                            allOverlays.add(state);
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                            currentShownLunchBar = Lunchbar.make(getLunchbarView(this),
                                    getString(R.string.disable_completion),
                                    Snackbar.LENGTH_LONG);
                            currentShownLunchBar.show();
                            // Begin disabling overlays
                            ThemeManager.disableOverlay(context, allOverlays);
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialog, id15) -> {
                            // User cancelled the dialog
                        });
                // Create the AlertDialog object and return it
                builder3.create();
                builder3.show();
                return true;
            case R.id.enable:
                // This enables all the installed overlays for this theme
                AlertDialog.Builder builder4 = new AlertDialog.Builder(InformationActivity
                        .this);
                builder4.setTitle(themeName);
                builder4.setIcon(Packages.getAppIcon(context, themePid));
                builder4.setMessage(R.string.enable_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id14) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all disabled overlays
                            List<String> stateAll = ThemeManager.listOverlays(
                                    context, ThemeManager.STATE_DISABLED);

                            ArrayList<String> allOverlays = new ArrayList<>();
                            for (String state : stateAll) {
                                try {
                                    ApplicationInfo appInfo = context
                                            .getPackageManager().getApplicationInfo(
                                                    state, PackageManager.GET_META_DATA);
                                    if ((appInfo.metaData != null) &&
                                            (appInfo.metaData.getString(
                                                    metadataOverlayParent) != null)) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if ((parent != null) && parent.equals(themePid)) {
                                            allOverlays.add(state);
                                        }
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                            currentShownLunchBar = Lunchbar.make(getLunchbarView(this),
                                    getString(R.string.enable_completion),
                                    Snackbar.LENGTH_LONG);
                            currentShownLunchBar.show();

                            // Begin enabling overlays
                            ThemeManager.enableOverlay(context, allOverlays);
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialog, id13) ->
                                dialog
                                        .cancel());
                // Create the AlertDialog object and return it
                builder4.create();
                builder4.show();
                return true;
            case R.id.uninstall:
                AlertDialog.Builder builder5 = new AlertDialog.Builder(InformationActivity
                        .this);
                builder5.setTitle(themeName);
                builder5.setIcon(Packages.getAppIcon(context, themePid));
                builder5.setMessage(R.string.uninstall_dialog_text)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id12) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            new uninstallTheme(this).execute("");
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialog, id1) -> {
                            // User cancelled the dialog
                        });
                // Create the AlertDialog object and return it
                builder5.create();
                builder5.show();
                return true;
            case R.id.restart_systemui:
                ThemeManager.restartSystemUI(context);
                return true;
            case R.id.reboot_device:
                ElevatedCommands.reboot();
                return true;
            case R.id.soft_reboot:
                ElevatedCommands.softReboot();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Always end the activity gracefully.
     */
    @Override
    public void onBackPressed() {
        if (materialSheetFab.isSheetVisible()) {
            materialSheetFab.hideSheet();
        } else {
            if (uninstalled)
                Broadcasts.sendRefreshMessage(context);
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Destroy all asynchronous tasks if available, executed in each heavy fragment
        if (Overlays.mainLoader != null) {
            Overlays.mainLoader.cancel(true);
            Overlays.mainLoader = null;
        }
        if (Wallpapers.mainLoader != null) {
            Wallpapers.mainLoader.cancel(true);
            Wallpapers.mainLoader = null;
        }

        // Close the active Samsung package monitor if applicable
        Thread currentThread = Substratum.currentThread;
        if (currentThread != null) {
            Substratum.stopSamsungPackageMonitor();
        }

        // Close the active compiling notification if the app was closed from recents
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(References.NOTIFICATION_ID_COMPILER);
        }

        try {
            localBroadcastManager.unregisterReceiver(refreshReceiver);
        } catch (Exception e) {
            // Unregistered already
        }

        try {
            localBroadcastManager.unregisterReceiver(activityFinisher);
        } catch (Exception e) {
            // Unregistered already
        }

        if (Systems.isAndromedaDevice(context)) {
            try {
                localBroadcastManager.unregisterReceiver(andromedaReceiver);
            } catch (Exception e) {
                // Unregistered already
            }
        }

        if (!BYPASS_SUBSTRATUM_BUILDER_DELETION) {
            String workingDirectory =
                    context.getCacheDir().getAbsolutePath();
            File deleted = new File(workingDirectory);
            FileOperations.delete(context, deleted.getAbsolutePath());
            if (!deleted.exists()) Substratum.log(References.SUBSTRATUM_BUILDER,
                    "Successfully cleared Substratum cache!");
        }
    }

    /**
     * Class to reload the whole activity and the resources
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private static class LayoutLoader extends AsyncTask<String, Integer, String> {
        private final WeakReference<InformationActivity> ref;

        LayoutLoader(InformationActivity informationActivity) {
            super();
            ref = new WeakReference<>(informationActivity);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                if (checkPackageSupport(informationActivity.getApplicationContext(), false)) {
                    informationActivity.heroImage.
                            setBackgroundColor(Color.parseColor("#ffff00"));
                    informationActivity.collapsingToolbar.
                            setStatusBarScrimColor(Color.parseColor("#ffff00"));
                    informationActivity.collapsingToolbar.
                            setContentScrimColor(Color.parseColor("#ffff00"));
                    informationActivity.appBarLayout.
                            setBackgroundColor(Color.parseColor("#ffff00"));
                    informationActivity.tabLayout.
                            setBackgroundColor(Color.parseColor("#ffff00"));
                    informationActivity.getWindow().
                            setNavigationBarColor(Color.parseColor("#ffff00"));
                    informationActivity.getWindow().
                            setStatusBarColor(Color.parseColor("#ffff00"));
                    informationActivity.floatingActionButton.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#ffff00")));
                } else if (!informationActivity.prefs.getBoolean("lite_mode", false)) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(
                            informationActivity.byteArray, 0, informationActivity.byteArray.length);
                    informationActivity.heroImage.setImageBitmap(bitmap);
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null &&
                    informationActivity.heroImageBitmap != null &&
                    !informationActivity.prefs.getBoolean("lite_mode", false)) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                informationActivity.heroImageBitmap.compress(PNG, 100, stream);
                informationActivity.byteArray = stream.toByteArray();
            }
            return null;
        }
    }

    /**
     * Class to create the app shortcut on the launcher
     */
    private static class AppShortcutCreator extends AsyncTask<String, Integer, String> {
        private final WeakReference<InformationActivity> ref;

        AppShortcutCreator(InformationActivity informationActivity) {
            super();
            ref = new WeakReference<>(informationActivity);
        }

        @Override
        protected void onPostExecute(String result) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                informationActivity.prefs.edit().
                        putString("app_shortcut_theme", informationActivity.themePid).apply();
                informationActivity.favorite.setIcon(
                        informationActivity.getDrawable(R.drawable.toolbar_favorite));
                if (informationActivity.shouldDarken)
                    informationActivity.favorite.getIcon().setColorFilter(
                            informationActivity.getColor(R.color
                                    .information_activity_dark_icon_mode),
                            PorterDuff.Mode.SRC_ATOP);
                String format = String.format(
                        informationActivity.getString(R.string.menu_favorite_snackbar), result);
                currentShownLunchBar = Lunchbar.make(
                        getLunchbarView(informationActivity),
                        format,
                        Snackbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                References.createShortcut(
                        informationActivity.context,
                        informationActivity.themePid,
                        informationActivity.themeName);
                return informationActivity.themeName;
            }
            return null;
        }
    }

    /**
     * Class to remove all app shortcuts on the launcher
     */
    private static class AppShortcutClearer extends AsyncTask<String, Integer, String> {
        private final WeakReference<InformationActivity> ref;

        AppShortcutClearer(InformationActivity informationActivity) {
            super();
            ref = new WeakReference<>(informationActivity);
        }

        @Override
        protected void onPostExecute(String result) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                informationActivity.prefs.edit().remove("app_shortcut_theme").apply();
                informationActivity.favorite.setIcon(
                        informationActivity.getDrawable(R.drawable.toolbar_not_favorite));
                if (informationActivity.shouldDarken)
                    informationActivity.favorite.getIcon().setColorFilter(
                            informationActivity.getColor(R.color
                                    .information_activity_dark_icon_mode),
                            PorterDuff.Mode.SRC_ATOP);
                String format = String.format(
                        informationActivity.getString(R.string.menu_favorite_snackbar_cleared),
                        result);
                currentShownLunchBar = Lunchbar.make(
                        getLunchbarView(informationActivity),
                        format,
                        Snackbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                References.clearShortcut(informationActivity.context);
                return informationActivity.themeName;
            }
            return null;
        }
    }

    /**
     * Class to uninstall the current theme, of which closes out the theme just to end gracefully
     */
    private static class uninstallTheme extends AsyncTask<String, Integer, String> {
        private final WeakReference<InformationActivity> ref;

        uninstallTheme(InformationActivity informationActivity) {
            super();
            ref = new WeakReference<>(informationActivity);
        }

        @Override
        protected void onPreExecute() {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                String parseMe = String.format(
                        informationActivity.getString(R.string.adapter_uninstalling),
                        informationActivity.themeName);

                informationActivity.mProgressDialog = new ProgressDialog(informationActivity);
                informationActivity.mProgressDialog.setMessage(parseMe);
                informationActivity.mProgressDialog.setIndeterminate(true);
                informationActivity.mProgressDialog.setCancelable(false);
                informationActivity.mProgressDialog.show();
                // Clear the notification of building theme if shown
                NotificationManager manager = (NotificationManager)
                        informationActivity.context
                                .getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.cancel(References.NOTIFICATION_ID_COMPILER);
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                informationActivity.mProgressDialog.cancel();
                informationActivity.uninstalled = true;
                informationActivity.onBackPressed();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                Packages.uninstallPackage(
                        informationActivity.context,
                        informationActivity.themePid);
            }
            return null;
        }
    }

    /**
     * Receiver to kill the activity
     */
    class RefreshReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Packages.isPackageInstalled(context, themePid)) {
                Substratum.log("ThemeUninstaller",
                        "The theme was uninstalled, so the activity is now closing!");
                Broadcasts.sendRefreshMessage(context);
                finish();
            }
        }
    }

    /**
     * Receiver to close all the stacked activities and show the RootRequester dialog with an
     * Andromeda warning to connect to the PC.
     */
    class AndromedaReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }

    /**
     * Receiver to kill the activity and relaunch it when the theme was updated
     */
    class ActivityFinisher extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ((intent != null) && !compilingProcess) {
                String packageName = intent.getStringExtra(Internal.THEME_PID);
                if (packageName != null &&
                        packageName.equals(themePid)) {
                    String themeUpdatedToastText = String.format(
                            getString(R.string.toast_activity_finished), themeName);
                    Substratum.log(SUBSTRATUM_LOG,
                            themeName + " was just updated, now closing InformationActivity...");
                    Toast.makeText(context, themeUpdatedToastText, Toast.LENGTH_LONG).show();
                    finish();
                    new Handler().postDelayed(() ->
                            Theming.launchTheme(context, themePid), 500L);
                }
            } else if (compilingProcess) {
                Substratum.log(SUBSTRATUM_LOG,
                        "Tried to restart activity but theme was compiling, delaying...");
                shouldRestartActivity = true;
            }
        }
    }
}
