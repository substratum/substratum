/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
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
import android.content.res.Configuration;
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
import android.preference.PreferenceManager;
import android.support.annotation.RestrictTo;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Lunchbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.gordonwong.materialsheetfab.DimOverlayFrameLayout;
import com.gordonwong.materialsheetfab.MaterialSheetFab;
import com.gordonwong.materialsheetfab.MaterialSheetFabEventListener;
import com.theartofdev.edmodo.cropper.CropImage;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ooo.oxo.library.widget.PullBackLayout;
import projekt.substratum.adapters.tabs.InformationTabsAdapter;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Resources;
import projekt.substratum.common.Systems;
import projekt.substratum.common.Theming;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.services.system.SamsungPackageService;
import projekt.substratum.tabs.BootAnimations;
import projekt.substratum.tabs.WallpapersManager;
import projekt.substratum.util.files.Root;
import projekt.substratum.util.views.FloatingActionMenu;
import projekt.substratum.util.views.SheetDialog;

import static android.graphics.Bitmap.CompressFormat.JPEG;
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
import static projekt.substratum.common.Internal.SHEET_COMMAND;
import static projekt.substratum.common.Internal.START_JOB_ACTION;
import static projekt.substratum.common.Internal.THEME_MODE;
import static projekt.substratum.common.Internal.THEME_NAME;
import static projekt.substratum.common.Internal.THEME_PID;
import static projekt.substratum.common.Internal.THEME_WALLPAPER;
import static projekt.substratum.common.Packages.getOverlayMetadata;
import static projekt.substratum.common.Packages.getPackageHeroImage;
import static projekt.substratum.common.References.ACTIVITY_FINISHER;
import static projekt.substratum.common.References.BYPASS_SUBSTRATUM_BUILDER_DELETION;
import static projekt.substratum.common.References.MANAGER_REFRESH;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.References.bootAnimationsFragment;
import static projekt.substratum.common.References.fontsFragment;
import static projekt.substratum.common.References.getView;
import static projekt.substratum.common.References.metadataHeroOverride;
import static projekt.substratum.common.References.metadataOverlayParent;
import static projekt.substratum.common.References.metadataWallpapers;
import static projekt.substratum.common.References.overlaysFragment;
import static projekt.substratum.common.References.shutdownAnimationsFragment;
import static projekt.substratum.common.References.soundsFragment;
import static projekt.substratum.common.References.wallpaperFragment;
import static projekt.substratum.common.Systems.isSamsung;

public class InformationActivity extends AppCompatActivity implements PullBackLayout.Callback {

    private static final int LUNCHBAR_DISMISS_FAB_CLICK_DELAY = 200;
    public static Lunchbar currentShownLunchBar;
    public static Boolean compilingProcess = false;
    public static Boolean shouldRestartActivity = false;
    private static List<String> tab_checker;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.gradientView)
    View gradientView;
    @BindView(R.id.collapsing_toolbar_tabbed_layout)
    CollapsingToolbarLayout collapsingToolbar;
    @BindView(R.id.viewpager)
    ViewPager viewPager;
    @BindView(R.id.appbar)
    AppBarLayout appBarLayout;
    @BindView(R.id.fab_sheet)
    CardView sheetView;
    @BindView(R.id.overlay)
    DimOverlayFrameLayout overlay;
    @BindView(R.id.enable_swap)
    Switch enable_swap;
    @BindView(R.id.compile_enable_selected)
    TextView compile_enable_selected;
    @BindView(R.id.compile_update_selected)
    TextView compile_update_selected;
    @BindView(R.id.disable_selected)
    TextView disable_selected;
    @BindView(R.id.apply_fab)
    FloatingActionMenu floatingActionButton;
    @BindView(R.id.fab_menu_divider)
    View fab_menu_divider;
    @BindView(R.id.enable)
    LinearLayout enable_zone;
    @BindView(R.id.enable_selected)
    TextView enable_selected;
    @BindView(R.id.enable_disable_selected)
    TextView enable_disable_selected;
    @BindView(R.id.heroImage)
    ImageView heroImage;
    @BindView(R.id.puller)
    PullBackLayout puller;

    private String theme_name;
    private String theme_pid;
    private String theme_mode;
    private Boolean uninstalled = false;
    private byte[] byteArray;
    private Bitmap heroImageBitmap;
    private SharedPreferences prefs;
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
    private Context mContext;

    private int mInitialOrientation;
    private int mOrientation;

    /**
     * Function to get the dominant color out of a specific image
     *
     * @param bitmap Bitmap to collect data from
     * @return Returns the color that is dominant
     */
    private static int getDominantColor(Bitmap bitmap) {
        try {
            Palette palette = Palette.from(bitmap).generate();
            return palette.getDominantColor(Color.TRANSPARENT);
        } catch (IllegalArgumentException ignored) {
            // Suppress warning
        }
        return Color.TRANSPARENT;
    }

    /**
     * Depending on the hero image, change the actionbar color buttons
     *
     * @param activity  Activity to change it in
     * @param dark_mode True if dark, false if light
     */
    private static void setOverflowButtonColor(Activity activity, Boolean dark_mode) {
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
                    overflow.setImageResource(dark_mode ? R.drawable
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

    @Override
    public void onPullComplete() {
        supportFinishAfterTransition();
    }

    @Override
    public void onPullStart() {
    }

    @Override
    public void onPullCancel() {
    }

    @Override
    public void onPull(float progress) {
    }

    /**
     * Automatically set the toolbar icons based on whether the hero image is dark or not
     *
     * @param dynamicActionBarColors True, if dynamic (based on hero), false if forced
     */
    private void autoSetToolbarIcons(boolean dynamicActionBarColors) {
        if (InformationActivity.checkColorDarkness(dominantColor) && dynamicActionBarColors) {
            setDarkToolbarIcons();
        } else {
            setLightToolbarIcons();
        }
    }

    /**
     * Sets the toolbar icons to be dark
     */
    private void setDarkToolbarIcons() {
        collapsingToolbar.setCollapsedTitleTextColor(
                getColor(R.color.information_activity_dark_icon_mode));
        collapsingToolbar.setExpandedTitleColor(
                getColor(R.color.information_activity_dark_icon_mode));
        tabLayout.setTabTextColors(
                getColor(R.color.information_activity_dark_text_mode),
                getColor(R.color.information_activity_dark_text_mode));

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
        collapsingToolbar.setCollapsedTitleTextColor(
                getColor(R.color.information_activity_light_icon_mode));
        collapsingToolbar.setExpandedTitleColor(
                getColor(R.color.information_activity_light_icon_mode));
        tabLayout.setTabTextColors(
                getColor(R.color.information_activity_light_text_mode),
                getColor(R.color.information_activity_light_text_mode));

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
                                mContext,
                                resultUri.toString().substring(7),
                                "home");
                        editor.putString("home_wallpaper_applied", theme_pid);
                        currentShownLunchBar = Lunchbar.make(getView(this),
                                getString(R.string.wallpaper_homescreen_success),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    } catch (IOException e) {
                        currentShownLunchBar = Lunchbar.make(getView(this),
                                getString(R.string.wallpaper_homescreen_error),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                        e.printStackTrace();
                    }
                } else if (resultUri.toString().contains("lockscreen_wallpaper")) {
                    try {
                        WallpapersManager.setWallpaper(
                                mContext,
                                resultUri.toString().substring(7),
                                "lock");
                        editor.putString("lock_wallpaper_applied", theme_pid);
                        currentShownLunchBar = Lunchbar.make(getView(this),
                                getString(R.string.wallpaper_lockscreen_success),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    } catch (IOException e) {
                        currentShownLunchBar = Lunchbar.make(getView(this),
                                getString(R.string.wallpaper_lockscreen_error),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                        e.printStackTrace();
                    }
                } else if (resultUri.toString().contains("all_wallpaper")) {
                    try {
                        WallpapersManager.setWallpaper(
                                mContext,
                                resultUri.toString().substring(7),
                                "all");
                        editor.putString("home_wallpaper_applied", theme_pid);
                        editor.putString("lock_wallpaper_applied", theme_pid);
                        currentShownLunchBar = Lunchbar.make(getView(this),
                                getString(R.string.wallpaper_allscreen_success),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    } catch (IOException e) {
                        currentShownLunchBar = Lunchbar.make(getView(this),
                                getString(R.string.wallpaper_allscreen_error),
                                Lunchbar.LENGTH_LONG);
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
        super.onCreate(savedInstanceState);

        // Postpone the shared element enter transition.
        postponeEnterTransition();

        requestWindowFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        requestWindowFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);

        setContentView(R.layout.information_activity);
        ButterKnife.bind(this);

        mInitialOrientation = getResources().getConfiguration().orientation;
        mOrientation = mInitialOrientation;

        mContext = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        localBroadcastManager = LocalBroadcastManager.getInstance(mContext);

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new RefreshReceiver();
        localBroadcastManager.registerReceiver(refreshReceiver,
                new IntentFilter(MANAGER_REFRESH));

        // Activity finisher is for when the activity should be closed by a specific intent
        activityFinisher = new ActivityFinisher();
        localBroadcastManager.registerReceiver(activityFinisher,
                new IntentFilter(ACTIVITY_FINISHER));

        // If the device is Andromeda based, we must take account for the disconnection phase
        if (Systems.isAndromedaDevice(mContext)) {
            andromedaReceiver = new InformationActivity.AndromedaReceiver();
            localBroadcastManager.registerReceiver(andromedaReceiver,
                    new IntentFilter(ANDROMEDA_RECEIVER));
        }

        // Themer's booleans to adjust actionbar and navbar colors
        boolean dynamicActionBarColors = getResources().getBoolean(R.bool.dynamicActionBarColors);
        boolean dynamicNavBarColors = getResources().getBoolean(R.bool.dynamicNavigationBarColors);

        // Check if we should apply the new cute font from Google
        boolean bottomBarUi = !prefs.getBoolean("advanced_ui", false);
        if (bottomBarUi) {
            setTheme(R.style.AppTheme_SpecialUI);
        }

        // Obtain the current intent to receive the intent data out of it
        Intent currentIntent = getIntent();
        theme_name = currentIntent.getStringExtra(THEME_NAME);
        theme_pid = currentIntent.getStringExtra(THEME_PID);
        theme_mode = currentIntent.getStringExtra("theme_mode");
        byte[] encryption_key = currentIntent.getByteArrayExtra(ENCRYPTION_KEY_EXTRA);
        byte[] iv_encrypt_key = currentIntent.getByteArrayExtra(IV_ENCRYPTION_KEY_EXTRA);
        String wallpaperUrl = getOverlayMetadata(mContext, theme_pid,
                metadataWallpapers);

        // Package the intent data into a new bundle
        Bundle bundle = new Bundle();
        bundle.putString(THEME_NAME, theme_name);
        bundle.putString(THEME_PID, theme_pid);
        bundle.putString(THEME_MODE, theme_mode);
        bundle.putByteArray(ENCRYPTION_KEY_EXTRA, encryption_key);
        bundle.putByteArray(IV_ENCRYPTION_KEY_EXTRA, iv_encrypt_key);
        bundle.putString(THEME_WALLPAPER, wallpaperUrl);

        // By default, if the mode is null, we will launch with all tabs
        if (theme_mode == null) theme_mode = "";

        // Configure the views
        toolbar.setTitle(theme_name);
        collapsingToolbar.setTitle(theme_name);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Hero Image
        Drawable heroImage = getPackageHeroImage(
                getApplicationContext(),
                theme_pid,
                false);
        if (heroImage != null) {
            try {
                heroImageBitmap = ((BitmapDrawable) heroImage).getBitmap();
            } catch (Exception e) {
                // Suppress warning
            }
        }
        dominantColor = heroImageBitmap == null ?
                Color.TRANSPARENT : getDominantColor(heroImageBitmap);

        // Set the background of the pulldown
        getWindow().setBackgroundDrawable(new ColorDrawable(dominantColor));

        // Set the AppBarLayout to have the background color of the dominant color in hero
        appBarLayout.setBackgroundColor(dominantColor);

        // Set the actionbar colors to dominant color
        if (dynamicActionBarColors) {
            collapsingToolbar.setStatusBarScrimColor(dominantColor);
            collapsingToolbar.setContentScrimColor(dominantColor);
        }

        // Set the navbar colors to dominant color
        if (dynamicNavBarColors) {
            getWindow().setNavigationBarColor(dominantColor);
            if (InformationActivity.checkColorDarkness(dominantColor)) {
                getWindow().setNavigationBarColor(
                        getColor(R.color.theme_information_background));
            }
        }

        // Show the FAB
        floatingActionButton.show();

        // The fab needs to have some special colors
        int sheetColor = mContext.getColor(R.color.fab_menu_background_card);
        int fabColor = mContext.getColor(R.color.fab_background_color);

        // Create material sheet FAB
        materialSheetFab = new MaterialSheetFab<>(
                floatingActionButton,
                sheetView,
                overlay,
                sheetColor,
                fabColor);

        // Okay, time for the meat of the reloader
        new LayoutLoader(this).execute("");

        // First, take account for whether the theme was launched normally
        if (theme_mode != null && theme_mode.isEmpty()) {
            try {
                Context otherContext = mContext.createPackageContext
                        (theme_pid, 0);
                AssetManager am = otherContext.getAssets();
                List found_folders = Arrays.asList(am.list(""));
                tab_checker = new ArrayList<>();
                if (!Systems.checkOMS(mContext)) {
                    for (int i = 0; i < found_folders.size(); i++) {
                        if (Resources.allowedForLegacy
                                (found_folders.get(i).toString())) {
                            tab_checker.add(found_folders.get(i).toString());
                        }
                    }
                } else {
                    tab_checker = Arrays.asList(am.list(""));
                }
                boolean isWallpaperOnly = true;
                if (tab_checker.contains(overlaysFragment)) {
                    isWallpaperOnly = false;
                    tabLayout.addTab(tabLayout.newTab().setText(getString(R
                            .string
                            .theme_information_tab_one)));
                }
                if (tab_checker.contains(bootAnimationsFragment) &&
                        Resources.isBootAnimationSupported(mContext)) {
                    isWallpaperOnly = false;
                    tabLayout.addTab(tabLayout.newTab().setText(getString(R
                            .string
                            .theme_information_tab_two)));
                }
                if (tab_checker.contains(shutdownAnimationsFragment) &&
                        Resources.isShutdownAnimationSupported(mContext)) {
                    isWallpaperOnly = false;
                    tabLayout.addTab(tabLayout.newTab().setText(getString(R
                            .string
                            .theme_information_tab_six)));
                }
                if (tab_checker.contains(fontsFragment) && Resources.isFontsSupported(mContext)) {
                    isWallpaperOnly = false;
                    tabLayout.addTab(tabLayout.newTab().setText(getString(R
                            .string
                            .theme_information_tab_three)));
                }
                if (tab_checker.contains(soundsFragment) &&
                        Resources.isSoundsSupported(mContext)) {
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
        } else {
            // At this point, theme was launched in their tab specific sections
            switch (theme_mode) {
                case overlaysFragment:
                    tabLayout.addTab(tabLayout.newTab().setText(
                            getString(R.string.theme_information_tab_one)));
                    break;
                case bootAnimationsFragment:
                    tabLayout.addTab(tabLayout.newTab().setText(
                            getString(R.string.theme_information_tab_two)));
                    break;
                case shutdownAnimationsFragment:
                    tabLayout.addTab(tabLayout.newTab().setText(
                            getString(R.string.theme_information_tab_six)));
                    break;
                case fontsFragment:
                    tabLayout.addTab(tabLayout.newTab().setText(
                            getString(R.string.theme_information_tab_three)));
                    break;
                case soundsFragment:
                    tabLayout.addTab(tabLayout.newTab().setText(
                            getString(R.string.theme_information_tab_four)));
                    break;
                case wallpaperFragment:
                    tabLayout.addTab(tabLayout.newTab().setText(
                            getString(R.string.theme_information_tab_five)));
                    break;
            }
        }

        // Set the tabs to move in a certain way
        tabLayout.setTabGravity(TabLayout.MODE_SCROLLABLE);
        if (dynamicActionBarColors)
            tabLayout.setBackgroundColor(dominantColor);

        String toOverrideHero =
                Packages.getOverlayMetadata(
                        getApplicationContext(),
                        theme_pid,
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
                    autoSetToolbarIcons(dynamicActionBarColors);
                    break;
            }
        } else {
            autoSetToolbarIcons(dynamicActionBarColors);
        }

        HashMap<String, Boolean> extra_hidden_tabs = new HashMap<>();
        // Boot animation visibility
        extra_hidden_tabs.put(bootAnimationsFragment, Resources.isBootAnimationSupported(mContext));
        // Shutdown animation visibility
        extra_hidden_tabs.put(shutdownAnimationsFragment,
                Resources.isShutdownAnimationSupported(mContext));
        // Fonts visibility
        extra_hidden_tabs.put(fontsFragment, Resources.isFontsSupported(mContext));
        // Sounds visibility
        extra_hidden_tabs.put(soundsFragment, Resources.isSoundsSupported(mContext));

        // Set up the tabs
        InformationTabsAdapter adapter = new InformationTabsAdapter(
                getSupportFragmentManager(),
                tabLayout.getTabCount(),
                theme_mode,
                tab_checker,
                wallpaperUrl,
                extra_hidden_tabs,
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
                                    .getClass().getSimpleName()) {
                                case "Overlays":
                                    floatingActionButton.show();
                                    floatingActionButton.setImageResource(
                                            R.drawable.floating_action_button_icon);
                                    break;
                                case "BootAnimations":
                                case "Fonts":
                                case "Sounds":
                                    floatingActionButton.show();
                                    floatingActionButton.setImageResource(
                                            R.drawable.floating_action_button_icon_check);
                                    break;
                                case "Wallpapers":
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
                LocalBroadcastManager.getInstance(mContext);
        floatingActionButton.setOnClickListener(v -> {
            try {
                boolean isLunchbarOpen = InformationActivity.closeAllLunchBars();
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    Intent intent;
                    if (adapt != null) {
                        Object obj = adapt.instantiateItem(viewPager, tabPosition);
                        switch (obj.getClass().getSimpleName()) {
                            case "Overlays":
                                materialSheetFab.showSheet();
                                break;
                            case "BootAnimations":
                                boolean isShutdownTab = ((BootAnimations) obj)
                                        .isShutdownTab();
                                intent = new Intent((isShutdownTab ? "ShutdownAnimations" :
                                        "BootAnimations") + START_JOB_ACTION);
                                localBroadcastManager.sendBroadcast(intent);
                                break;
                            case "Fonts":
                                intent = new Intent("Fonts" + START_JOB_ACTION);
                                localBroadcastManager.sendBroadcast(intent);
                                break;
                            case "Sounds":
                                intent = new Intent("Sounds" + START_JOB_ACTION);
                                localBroadcastManager.sendBroadcast(intent);
                                break;
                        }
                    }
                }, (long) (isLunchbarOpen ? LUNCHBAR_DISMISS_FAB_CLICK_DELAY : 0));
            } catch (NullPointerException npe) {
                // Suppress warning
            }
        });

        // This is for the Floating Action Menu actions
        Intent intent = new Intent("Overlays" + START_JOB_ACTION);
        if (!Systems.checkOMS(this) && !Systems.isSamsung(mContext)) {
            enable_swap.setText(getString(R.string.fab_menu_swap_toggle_legacy));
        } else if (Systems.isSamsung(mContext)) {
            fab_menu_divider.setVisibility(View.GONE);
            enable_swap.setVisibility(View.GONE);
        }
        boolean enabled = prefs.getBoolean("enable_swapping_overlays", true);
        intent.putExtra(SHEET_COMMAND, MIX_AND_MATCH);
        intent.putExtra(MIX_AND_MATCH_IA_TO_OVERLAYS, enabled);
        localBroadcastManager.sendBroadcast(intent);
        enable_swap.setChecked(enabled);
        enable_swap.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("enable_swapping_overlays", isChecked).apply();
            intent.putExtra(SHEET_COMMAND, MIX_AND_MATCH);
            intent.putExtra(MIX_AND_MATCH_IA_TO_OVERLAYS, isChecked);
            localBroadcastManager.sendBroadcast(intent);
        });

        if (!Systems.checkOMS(this)) compile_enable_selected.setVisibility(View.GONE);
        compile_enable_selected.setOnClickListener(v -> {
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
            compile_update_selected.setText(getString(R.string.fab_menu_compile_install));
        }
        compile_update_selected.setOnClickListener(v -> {
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
            disable_selected.setText(getString(R.string.fab_menu_uninstall));
        }
        disable_selected.setOnClickListener(v -> {
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

        if (!Systems.checkOMS(this)) enable_zone.setVisibility(View.GONE);
        enable_selected.setOnClickListener(v -> {
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
            enable_disable_selected.setVisibility(View.GONE);
        enable_disable_selected.setOnClickListener(v -> {
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

        Boolean shouldShowSamsungWarning =
                !prefs.getBoolean("show_dangerous_samsung_overlays", false);
        if (Systems.isSamsung(mContext) &&
                !Packages.isSamsungTheme(mContext, theme_pid) &&
                shouldShowSamsungWarning) {
            currentShownLunchBar = Lunchbar.make(
                    getView(this),
                    R.string.toast_samsung_prototype_alert,
                    Lunchbar.LENGTH_SHORT);
            currentShownLunchBar.show();
        }
        if (Systems.isSamsung(mContext)) {
            startService(new Intent(getBaseContext(), SamsungPackageService.class));
        }

        puller.setCallback(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mOrientation = newConfig.orientation;
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

        // Start formalizing a check for dark icons
        boolean dynamicActionBarColors = getResources().getBoolean(R.bool
                .dynamicActionBarColors);
        shouldDarken = InformationActivity.checkColorDarkness(dominantColor) &&
                dynamicActionBarColors;

        // Start dynamically showing menu items
        boolean isOMS = Systems.checkOMS(mContext);
        boolean isMR1orHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;

        if (!isMR1orHigher) menu.findItem(R.id.favorite).setVisible(false);
        if (isMR1orHigher) {
            favorite = menu.findItem(R.id.favorite);
            if (prefs.contains("app_shortcut_theme")) {
                if (prefs.getString("app_shortcut_theme", "").equals(theme_pid)) {
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
        if (Packages.getThemeChangelog(mContext, theme_pid) != null) {
            MenuItem changelog = menu.findItem(R.id.changelog);
            changelog.setVisible(true);
            if (shouldDarken) changelog.getIcon().setColorFilter(
                    getColor(R.color.information_activity_dark_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
        }

        if (Systems.checkAndromeda(mContext) ||
                (!isOMS && !Root.checkRootAccess())) {
            menu.findItem(R.id.restart_systemui).setVisible(false);
        }
        if (!isOMS) {
            menu.findItem(R.id.disable).setVisible(false);
            menu.findItem(R.id.enable).setVisible(false);
        }
        if (isOMS || isSamsung(mContext)) {
            if (isSamsung(mContext)) menu.findItem(R.id.clean).setVisible(false);
            menu.findItem(R.id.reboot_device).setVisible(false);
            menu.findItem(R.id.soft_reboot).setVisible(false);
            menu.findItem(R.id.uninstall).setVisible(false);
        }

        if (!Packages.isUserApp(mContext, theme_pid)) {
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
                    if (prefs.getString("app_shortcut_theme", "").equals(theme_pid)) {
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
                        .inflate(R.layout.changelog_sheet_dialog, null);

                LinearLayout titleBox = sheetView.findViewById(R.id.title_box);
                TextView title = titleBox.findViewById(R.id.title);
                String format_me = String.format(
                        getString(R.string.changelog_title),
                        theme_name);
                title.setText(format_me);

                LinearLayout textBox = sheetView.findViewById(R.id.text_box);
                TextView text = textBox.findViewById(R.id.text);

                String[] changelog_parsing =
                        Packages.getThemeChangelog(mContext, theme_pid);
                StringBuilder to_show = new StringBuilder();
                if (changelog_parsing != null) {
                    for (String aChangelog_parsing : changelog_parsing) {
                        to_show.append("\u2022 ").append(aChangelog_parsing).append('\n');
                    }
                }
                text.setText(to_show.toString());
                sheetDialog.setCanceledOnTouchOutside(true);
                sheetDialog.setContentView(sheetView);
                sheetDialog.show();
                return true;
            case R.id.clean:
                // This cleans all the installed overlays for this theme
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setTitle(theme_name);
                builder1.setIcon(Packages.getAppIcon(mContext, theme_pid));
                builder1.setMessage(R.string.clean_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id18) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all installed overlays
                            List<String> stateAll = ThemeManager.listAllOverlays(mContext);

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = mContext
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if ((appInfo.metaData != null) &&
                                            (appInfo.metaData.getString(
                                                    metadataOverlayParent) != null)) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if ((parent != null) && parent.equals(theme_pid)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }

                            // Begin uninstalling overlays for this package
                            ThemeManager.uninstallOverlay(
                                    mContext,
                                    all_overlays
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
                builder3.setTitle(theme_name);
                builder3.setIcon(Packages.getAppIcon(mContext, theme_pid));
                builder3.setMessage(R.string.disable_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id16) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all enabled overlays
                            List<String> stateAll = ThemeManager.listOverlays(
                                    mContext, ThemeManager.STATE_ENABLED);

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = mContext
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if ((appInfo.metaData != null) &&
                                            (appInfo.metaData.getString(
                                                    metadataOverlayParent) != null)) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if ((parent != null) && parent.equals(theme_pid)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }
                            currentShownLunchBar = Lunchbar.make(getView(this),
                                    getString(R.string.disable_completion),
                                    Lunchbar.LENGTH_LONG);
                            currentShownLunchBar.show();
                            // Begin disabling overlays
                            ThemeManager.disableOverlay(mContext, all_overlays);
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
                builder4.setTitle(theme_name);
                builder4.setIcon(Packages.getAppIcon(mContext, theme_pid));
                builder4.setMessage(R.string.enable_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id14) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all disabled overlays
                            List<String> stateAll = ThemeManager.listOverlays(
                                    mContext, ThemeManager.STATE_DISABLED);

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = mContext
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if ((appInfo.metaData != null) &&
                                            (appInfo.metaData.getString(
                                                    metadataOverlayParent) != null)) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if ((parent != null) && parent.equals(theme_pid)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }
                            currentShownLunchBar = Lunchbar.make(getView(this),
                                    getString(R.string.enable_completion),
                                    Lunchbar.LENGTH_LONG);
                            currentShownLunchBar.show();

                            // Begin enabling overlays
                            ThemeManager.enableOverlay(mContext, all_overlays);
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
                builder5.setTitle(theme_name);
                builder5.setIcon(Packages.getAppIcon(mContext, theme_pid));
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
                ThemeManager.restartSystemUI(mContext);
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
            if (uninstalled) {
                Broadcasts.sendRefreshMessage(mContext);
            }
            supportFinishAfterTransition();
        }
    }

    @Override
    public void supportFinishAfterTransition() {
        if (mInitialOrientation != mOrientation) {
            finish();
        } else {
            super.supportFinishAfterTransition();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Close the active compiling notification if the app was closed from recents
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(References.notification_id_compiler);
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

        if (Systems.isAndromedaDevice(mContext)) {
            try {
                localBroadcastManager.unregisterReceiver(andromedaReceiver);
            } catch (Exception e) {
                // Unregistered already
            }
        }

        if (!BYPASS_SUBSTRATUM_BUILDER_DELETION) {
            String workingDirectory =
                    mContext.getCacheDir().getAbsolutePath();
            File deleted = new File(workingDirectory);
            FileOperations.delete(mContext, deleted.getAbsolutePath());
            if (!deleted.exists()) Log.d(References.SUBSTRATUM_BUILDER,
                    "Successfully cleared Substratum cache!");
        }
    }

    /**
     * Class to reload the whole activity and the resources
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private static class LayoutLoader extends AsyncTask<String, Integer, String> {
        private WeakReference<InformationActivity> ref;

        LayoutLoader(InformationActivity informationActivity) {
            super();
            ref = new WeakReference<>(informationActivity);
            ViewCompat.setTransitionName(
                    informationActivity.heroImage,
                    informationActivity.theme_pid);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                if (!informationActivity.prefs.getBoolean("complexion", false)) {
                    informationActivity.gradientView.setVisibility(View.GONE);
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
                } else {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(
                            informationActivity.byteArray, 0, informationActivity.byteArray.length);
                    informationActivity.heroImage.setImageBitmap(bitmap);
                }
                if (!tab_checker.contains(overlaysFragment)) {
                    informationActivity.startPostponedEnterTransition();
                    MainActivity.themeCardProgressBar.setVisibility(View.GONE);
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null && informationActivity.heroImageBitmap != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                informationActivity.heroImageBitmap.compress(JPEG, 100, stream);
                informationActivity.byteArray = stream.toByteArray();
            }
            return null;
        }
    }

    /**
     * Class to create the app shortcut on the launcher
     */
    private static class AppShortcutCreator extends AsyncTask<String, Integer, String> {
        private WeakReference<InformationActivity> ref;

        AppShortcutCreator(InformationActivity informationActivity) {
            super();
            ref = new WeakReference<>(informationActivity);
        }

        @Override
        protected void onPostExecute(String result) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                informationActivity.prefs.edit().
                        putString("app_shortcut_theme", informationActivity.theme_pid).apply();
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
                        References.getView(informationActivity),
                        format,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                References.createShortcut(
                        informationActivity.mContext,
                        informationActivity.theme_pid,
                        informationActivity.theme_name);
                return informationActivity.theme_name;
            }
            return null;
        }
    }

    /**
     * Class to remove all app shortcuts on the launcher
     */
    private static class AppShortcutClearer extends AsyncTask<String, Integer, String> {
        private WeakReference<InformationActivity> ref;

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
                        References.getView(informationActivity),
                        format,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                References.clearShortcut(informationActivity.mContext);
                return informationActivity.theme_name;
            }
            return null;
        }
    }

    /**
     * Class to uninstall the current theme, of which closes out the theme just to end gracefully
     */
    private static class uninstallTheme extends AsyncTask<String, Integer, String> {
        private WeakReference<InformationActivity> ref;

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
                        informationActivity.theme_name);

                informationActivity.mProgressDialog = new ProgressDialog(informationActivity);
                informationActivity.mProgressDialog.setMessage(parseMe);
                informationActivity.mProgressDialog.setIndeterminate(true);
                informationActivity.mProgressDialog.setCancelable(false);
                informationActivity.mProgressDialog.show();
                // Clear the notification of building theme if shown
                NotificationManager manager = (NotificationManager)
                        informationActivity.mContext
                                .getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.cancel(References.notification_id_compiler);
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
                        informationActivity.mContext,
                        informationActivity.theme_pid);
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
            if (!Packages.isPackageInstalled(context, theme_pid)) {
                Log.d("ThemeUninstaller",
                        "The theme was uninstalled, so the activity is now closing!");
                Broadcasts.sendRefreshMessage(context);
                supportFinishAfterTransition();
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
            supportFinishAfterTransition();
        }
    }

    /**
     * Receiver to kill the activity and relaunch it when the theme was updated
     */
    class ActivityFinisher extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ((intent != null) && !compilingProcess) {
                String package_name = intent.getStringExtra("theme_pid");
                if (package_name != null &&
                        package_name.equals(theme_pid)) {
                    String to_format = String.format(
                            getString(R.string.toast_activity_finished), theme_name);
                    Log.d(SUBSTRATUM_LOG,
                            theme_name + " was just updated, now closing InformationActivity...");
                    Toast.makeText(context, to_format, Toast.LENGTH_LONG).show();
                    supportFinishAfterTransition();
                    Handler handler = new Handler();
                    handler.postDelayed(() ->
                            Theming.launchTheme(mContext, theme_pid, theme_mode), 500L);
                }
            } else if (compilingProcess) {
                Log.d(SUBSTRATUM_LOG,
                        "Tried to restart activity but theme was compiling, delaying...");
                shouldRestartActivity = true;
            }
        }
    }
}
