package projekt.substratum;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.DimenHolder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import projekt.substratum.config.References;
import projekt.substratum.services.ThemeService;
import projekt.substratum.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PERMISSIONS_REQUEST_READ_PHONE_STATE = 2;
    private Drawer drawer;
    private int drawerSelected;
    private int permissionCheck, permissionCheck2;

    private void switchFragment(String title, String fragment) {
        getSupportActionBar().setTitle(title);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.main, Fragment.instantiate(MainActivity.this, "projekt.substratum" +
                ".fragments." + fragment));
        tx.commit();
    }

    private void switchFragmentToLicenses(String title, LibsSupportFragment fragment) {
        getSupportActionBar().setTitle(title);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.main, fragment);
        tx.commit();
    }

    private void printFCMtoken() {
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d("SubstratumLogger", "FCM Registration Token: " + token);
    }

    private boolean copyRescueFile(Context context, String sourceFileName, String destFileName) {
        AssetManager assetManager = context.getAssets();

        File destFile = new File(destFileName);
        File destParentDir = destFile.getParentFile();
        destParentDir.mkdir();

        InputStream in;
        OutputStream out;
        try {
            in = assetManager.open(sourceFileName);
            out = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        startService(new Intent(this, ThemeService.class));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(false);

        AccountHeader header = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.material_drawer_header_background)
                .withProfileImagesVisible(false)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(
                        new ProfileDrawerItem().withName(getString(R.string.drawer_name)).withEmail
                                (BuildConfig.VERSION_NAME))
                .withCurrentProfileHiddenInList(true)
                .build();

        final LibsSupportFragment fragment = new LibsBuilder().supportFragment();

        DrawerBuilder drawerBuilder = new DrawerBuilder();
        drawerBuilder.withActivity(this);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        drawerBuilder.withToolbar(toolbar);
        drawerBuilder.withSavedInstance(savedInstanceState);
        drawerBuilder.withActionBarDrawerToggleAnimated(true);
        if (prefs.getBoolean("alternate_drawer_design", false)) {
            drawerBuilder.withRootView(R.id.drawer_container);
            drawerBuilder.withHeaderHeight(DimenHolder.fromDp(0));
        }
        drawerBuilder.withAccountHeader(header);
        if (References.checkOMS()) {
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem().withName(R.string.nav_home).withIcon(
                            R.drawable.nav_theme_packs),
                    new PrimaryDrawerItem().withName(R.string.nav_overlays).withIcon(
                            R.drawable.nav_overlays),
                    new PrimaryDrawerItem().withName(R.string.nav_bootanim).withIcon(
                            R.drawable.nav_bootanim),
                    new PrimaryDrawerItem().withName(R.string.nav_fonts).withIcon(
                            R.drawable.nav_fonts),
                    new PrimaryDrawerItem().withName(R.string.nav_sounds).withIcon(
                            R.drawable.nav_sounds),
                    new PrimaryDrawerItem().withName(R.string.nav_wallpapers).withIcon(
                            R.drawable.nav_wallpapers),
                    new SectionDrawerItem().withName(R.string.nav_section_header_utilities),
                    new PrimaryDrawerItem().withName(R.string.nav_overlay_manager)
                            .withIcon(R.drawable.nav_overlay_manager),
                    new PrimaryDrawerItem().withName(R.string.nav_manage).withIcon(
                            R.drawable.nav_manage),
                    new PrimaryDrawerItem().withName(R.string.nav_priorities).withIcon(
                            R.drawable.nav_drawer_priorities),
                    new PrimaryDrawerItem().withName(R.string.nav_backup_restore).withIcon(
                            R.drawable.nav_drawer_profiles),
                    new SectionDrawerItem().withName(R.string.nav_section_header_more),
                    new SecondaryDrawerItem().withName(R.string.nav_troubleshooting)
                            .withIcon(
                                    R.drawable.nav_troubleshooting),
                    new SecondaryDrawerItem().withName(R.string.nav_team).withIcon(
                            R.drawable.nav_drawer_team),
                    new SecondaryDrawerItem().withName(getString(R.string.nav_opensource))
                            .withIcon(
                                    R.drawable.nav_drawer_licenses),
                    new SecondaryDrawerItem().withName(R.string.nav_settings).withIcon(
                            R.drawable.nav_drawer_settings)
            );
            drawerBuilder.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                @Override
                public boolean onItemClick(View view, int position, IDrawerItem
                        drawerItem) {
                    if (drawerItem != null) {
                        switch (position) {
                            case 1:
                                if (drawerSelected != position) {
                                    switchFragment(((References.checkOMS()) ?
                                                    getString(R.string.app_name) :
                                                    getString(R.string.legacy_app_name)),
                                            "HomeFragment");
                                    drawerSelected = 1;
                                }
                                break;
                            case 2:
                                if (drawerSelected != position) {
                                    switchFragment(getString(R.string.nav_overlays),
                                            "OverlaysFragment");
                                    drawerSelected = 2;
                                }
                                break;
                            case 3:
                                if (drawerSelected != position) {
                                    switchFragment(getString(R.string.nav_bootanim),
                                            "BootAnimationsFragment");
                                    drawerSelected = 3;
                                }
                                break;
                            case 4:
                                if (drawerSelected != position) {
                                    switchFragment(getString(R.string.nav_fonts),
                                            "FontsFragment");
                                    drawerSelected = 4;
                                }
                                break;
                            case 5:
                                if (drawerSelected != position) {
                                    switchFragment(getString(R.string.nav_sounds),
                                            "SoundsFragment");
                                    drawerSelected = 5;
                                }
                                break;
                            case 6:
                                if (drawerSelected != position) {
                                    switchFragment(getString(R.string.nav_wallpapers),
                                            "WallpaperFragment");
                                    drawerSelected = 6;
                                }
                                break;
                            case 8:
                                switchFragment(getString(R.string.nav_overlay_manager),
                                        "AdvancedManagerFragment");
                                drawerSelected = 7;
                                break;
                            case 9:
                                switchFragment(getString(R.string.nav_manage),
                                        "ManageFragment");
                                drawerSelected = 8;
                                break;
                            case 10:
                                switchFragment(getString(R.string.nav_priorities),
                                        "PriorityLoaderFragment");
                                drawerSelected = 9;
                                break;
                            case 11:
                                switchFragment(getString(R.string.nav_backup_restore),
                                        "ProfileFragment");
                                drawerSelected = 10;
                                break;
                            case 13:
                                if (drawerSelected != position) {
                                    switchFragment(getString(R.string.nav_troubleshooting),
                                            "TroubleshootingFragment");
                                    drawerSelected = 12;
                                }
                                break;
                            case 14:
                                if (drawerSelected != position) {
                                    switchFragment(getString(R.string.nav_team),
                                            "TeamFragment");
                                    drawerSelected = 13;
                                }
                                break;
                            case 15:
                                switchFragmentToLicenses(getString(R.string.nav_opensource),
                                        fragment);
                                drawerSelected = 14;
                                break;
                            case 16:
                                if (drawerSelected != position) {
                                    switchFragment(getString(R.string.nav_settings),
                                            "SettingsFragment");
                                    drawerSelected = 15;
                                }
                                break;
                        }
                    }
                    return false;
                }
            });
        } else {
            Boolean fonts_allowed = false;
            try {
                Class cls = Class.forName("android.graphics.Typeface");
                cls.getDeclaredMethod("getSystemFontDirLocation");
                cls.getDeclaredMethod("getThemeFontConfigLocation");
                cls.getDeclaredMethod("getThemeFontDirLocation");
                Log.e("SubstratumLogger", "This device on the legacy system fully supports font " +
                        "hotswapping.");
                fonts_allowed = true;
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (fonts_allowed) {
                drawerBuilder.addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.nav_home).withIcon(
                                R.drawable.nav_theme_packs),
                        new PrimaryDrawerItem().withName(R.string.nav_overlays).withIcon(
                                R.drawable.nav_overlays),
                        new PrimaryDrawerItem().withName(R.string.nav_bootanim).withIcon(
                                R.drawable.nav_bootanim),
                        new PrimaryDrawerItem().withName(R.string.nav_fonts).withIcon(
                                R.drawable.nav_fonts),
                        new PrimaryDrawerItem().withName(R.string.nav_sounds).withIcon(
                                R.drawable.nav_sounds),
                        new PrimaryDrawerItem().withName(R.string.nav_wallpapers).withIcon(
                                R.drawable.nav_wallpapers),
                        new SectionDrawerItem().withName(R.string
                                .nav_section_header_utilities),
                        new PrimaryDrawerItem().withName(R.string.nav_overlay_manager)
                                .withIcon(R.drawable.nav_overlay_manager),
                        new PrimaryDrawerItem().withName(R.string.nav_manage).withIcon(
                                R.drawable.nav_manage),
                        new PrimaryDrawerItem().withName(R.string.nav_backup_restore)
                                .withIcon(
                                        R.drawable.nav_drawer_profiles),
                        new SectionDrawerItem().withName(R.string.nav_section_header_more),
                        new SecondaryDrawerItem().withName(R.string.nav_troubleshooting)
                                .withIcon(
                                        R.drawable.nav_troubleshooting),
                        new SecondaryDrawerItem().withName(R.string.nav_team).withIcon(
                                R.drawable.nav_drawer_team),
                        new SecondaryDrawerItem().withName(getString(R.string
                                .nav_opensource))
                                .withIcon(
                                        R.drawable.nav_drawer_licenses),
                        new SecondaryDrawerItem().withName(R.string.nav_settings).withIcon(
                                R.drawable.nav_drawer_settings)
                );
                drawerBuilder.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem
                            drawerItem) {
                        if (drawerItem != null) {
                            switch (position) {
                                case 1:
                                    if (drawerSelected != position) {
                                        switchFragment(((References.checkOMS()) ?
                                                        getString(R.string.app_name) :
                                                        getString(R.string
                                                                .legacy_app_name)),
                                                "HomeFragment");
                                        drawerSelected = 1;
                                    }
                                    break;
                                case 2:
                                    if (drawerSelected != position) {
                                        switchFragment(getString(R.string.nav_overlays),
                                                "OverlaysFragment");
                                        drawerSelected = 2;
                                    }
                                    break;
                                case 3:
                                    if (drawerSelected != position) {
                                        switchFragment(getString(R.string.nav_bootanim),
                                                "BootAnimationsFragment");
                                        drawerSelected = 3;
                                    }
                                    break;
                                case 4:
                                    if (drawerSelected != position) {
                                        switchFragment(getString(R.string.nav_fonts),
                                                "FontsFragment");
                                        drawerSelected = 4;
                                    }
                                    break;
                                case 5:
                                    if (drawerSelected != position) {
                                        switchFragment(getString(R.string.nav_sounds),
                                                "SoundsFragment");
                                        drawerSelected = 5;
                                    }
                                    break;
                                case 6:
                                    if (drawerSelected != position) {
                                        switchFragment(getString(R.string.nav_wallpapers),
                                                "WallpaperFragment");
                                        drawerSelected = 6;
                                    }
                                    break;
                                case 8:
                                    switchFragment(getString(R.string.nav_overlay_manager),
                                            "AdvancedManagerFragment");
                                    drawerSelected = 8;
                                    break;
                                case 9:
                                    switchFragment(getString(R.string.nav_manage),
                                            "ManageFragment");
                                    drawerSelected = 9;
                                    break;
                                case 10:
                                    switchFragment(getString(R.string.nav_backup_restore),
                                            "ProfileFragment");
                                    drawerSelected = 10;
                                    break;
                                case 12:
                                    if (drawerSelected != position) {
                                        switchFragment(getString(R.string
                                                        .nav_troubleshooting),
                                                "TroubleshootingFragment");
                                        drawerSelected = 12;
                                    }
                                    break;
                                case 13:
                                    if (drawerSelected != position) {
                                        switchFragment(getString(R.string.nav_team),
                                                "TeamFragment");
                                        drawerSelected = 13;
                                    }
                                    break;
                                case 14:
                                    switchFragmentToLicenses(getString(R.string
                                                    .nav_opensource),
                                            fragment);
                                    drawerSelected = 14;
                                    break;
                                case 15:
                                    if (drawerSelected != position) {
                                        switchFragment(getString(R.string.nav_settings),
                                                "SettingsFragment");
                                        drawerSelected = 15;
                                    }
                                    break;
                            }
                        }
                        return false;
                    }
                });
            } else {
                drawerBuilder.addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.nav_home).withIcon(R
                                .drawable
                                .nav_theme_packs),
                        new PrimaryDrawerItem().withName(R.string.nav_overlays).withIcon(R
                                .drawable
                                .nav_overlays),
                        new PrimaryDrawerItem().withName(R.string.nav_bootanim).withIcon(
                                R.drawable.nav_bootanim),
                        new PrimaryDrawerItem().withName(R.string.nav_sounds).withIcon(R
                                .drawable
                                .nav_sounds),

                        new SectionDrawerItem().withName(R.string
                                .nav_section_header_utilities),
                        new PrimaryDrawerItem().withName(R.string.nav_overlay_manager)
                                .withIcon(R
                                        .drawable
                                        .nav_overlay_manager),
                        new PrimaryDrawerItem().withName(R.string.nav_manage).withIcon(R
                                .drawable.nav_manage),
                        new PrimaryDrawerItem().withName(R.string.nav_backup_restore)
                                .withIcon(R
                                        .drawable.nav_drawer_profiles),

                        new SectionDrawerItem().withName(R.string.nav_section_header_more),
                        new SecondaryDrawerItem().withName(R.string.nav_troubleshooting)
                                .withIcon(R
                                        .drawable.nav_troubleshooting),
                        new SecondaryDrawerItem().withName(R.string.nav_team).withIcon(R
                                .drawable.nav_drawer_team),
                        new SecondaryDrawerItem().withName(getString(R.string
                                .nav_opensource))
                                .withIcon(R
                                        .drawable.nav_drawer_licenses),
                        new SecondaryDrawerItem().withName(R.string.nav_settings).withIcon(R
                                .drawable.nav_drawer_settings)
                );
                drawerBuilder.withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem
                            drawerItem) {
                        if (drawerItem != null) {
                            switch (position) {
                                case 1:
                                    if (drawerSelected != position) {
                                        switchFragment(((References.checkOMS()) ?
                                                        getString(R.string.app_name) :
                                                        getString(R.string
                                                                .legacy_app_name)),
                                                "HomeFragment");
                                        drawerSelected = 1;
                                    }
                                    break;
                                case 2:
                                    if (drawerSelected != position) {
                                        switchFragment(getString(R.string.nav_overlays),
                                                "OverlaysFragment");
                                        drawerSelected = 2;
                                    }
                                    break;
                                case 3:
                                    if (drawerSelected != position) {
                                        switchFragment(getString(R.string.nav_bootanim),
                                                "BootAnimationsFragment");
                                        drawerSelected = 3;
                                    }
                                    break;
                                case 4:
                                    if (drawerSelected != position) {
                                        switchFragment(getString(R.string.nav_sounds),
                                                "SoundsFragment");
                                        drawerSelected = 4;
                                    }
                                    break;
                                case 6:
                                    switchFragment(getString(R.string.nav_overlay_manager),
                                            "AdvancedManagerFragment");
                                    drawerSelected = 6;
                                    break;
                                case 7:
                                    switchFragment(getString(R.string.nav_manage),
                                            "ManageFragment");
                                    drawerSelected = 7;
                                    break;
                                case 8:
                                    switchFragment(getString(R.string.nav_backup_restore),
                                            "ProfileFragment");
                                    drawerSelected = 8;
                                    break;
                                case 10:
                                    if (drawerSelected != position) {
                                        switchFragment(getString(R.string
                                                        .nav_troubleshooting),
                                                "TroubleshootingFragment");
                                        drawerSelected = 10;
                                    }
                                    break;
                                case 11:
                                    if (drawerSelected != position) {
                                        switchFragment(getString(R.string.nav_team),
                                                "TeamFragment");
                                        drawerSelected = 11;
                                    }
                                    break;
                                case 12:
                                    switchFragmentToLicenses(getString(R.string
                                                    .nav_opensource),
                                            fragment);
                                    drawerSelected = 12;
                                    break;
                                case 13:
                                    if (drawerSelected != position) {
                                        switchFragment(getString(R.string.nav_settings),
                                                "SettingsFragment");
                                        drawerSelected = 13;
                                    }
                                    break;
                            }
                        }
                        return false;
                    }
                });
            }

        }
        drawerBuilder.withSelectedItem(1);
        drawerBuilder.withSelectedItemByPosition(1);
        drawer = drawerBuilder.build();

        if (!Root.requestRootAccess()) {
            final ProgressDialog mProgressDialog = new ProgressDialog(this, R.style
                    .SubstratumBuilder_ActivityTheme);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            mProgressDialog.setContentView(R.layout.root_rejected_loader);

            final float radius = 5;
            final View decorView = getWindow().getDecorView();
            final View rootView = decorView.findViewById(android.R.id.content);
            final Drawable windowBackground = decorView.getBackground();

            BlurView blurView = (BlurView) mProgressDialog.findViewById(R.id.blurView);

            blurView.setupWith(rootView)
                    .windowBackground(windowBackground)
                    .blurAlgorithm(new RenderScriptBlur(this, true))
                    .blurRadius(radius);

            final TextView textView = (TextView) mProgressDialog.findViewById(R.id.timer);
            CountDownTimer Count = new CountDownTimer(5000, 1000) {
                public void onTick(long millisUntilFinished) {
                    if ((millisUntilFinished / 1000) > 1) {
                        textView.setText(String.format(
                                getString(R.string.root_rejected_timer_plural),
                                (millisUntilFinished / 1000) + ""));
                    } else {
                        textView.setText(String.format(
                                getString(R.string.root_rejected_timer_singular),
                                (millisUntilFinished / 1000) + ""));
                    }

                }

                public void onFinish() {
                    mProgressDialog.dismiss();
                    finish();
                }
            };
            Count.start();
        }

        permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionCheck2 = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_PHONE_STATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(getApplicationContext())) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + MainActivity.this.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Log.d("SubstratumLogger", "Substratum was granted " +
                        "'android.permission.WRITE_SETTINGS' " +
                        "permissions for system runtime code execution.");
            }
        }

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // permission already granted, allow the program to continue running
            File directory = new File(Environment.getExternalStorageDirectory(),
                    "/.substratum/");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File cacheDirectory = new File(getCacheDir(),
                    "/SubstratumBuilder/");
            if (!cacheDirectory.exists()) {
                cacheDirectory.mkdirs();
            }
            File rescueFile = new File(Environment.getExternalStorageDirectory() +
                    java.io.File.separator + "SubstratumRescue.zip");
            File rescueFileLegacy = new File(Environment.getExternalStorageDirectory() +
                    java.io.File.separator + "SubstratumRescue_Legacy.zip");
            if (!rescueFile.exists()) {
                copyRescueFile(getApplicationContext(), "rescue.dat",
                        Environment.getExternalStorageDirectory() +
                                java.io.File.separator + "substratum" +
                                java.io.File.separator + "SubstratumRescue.zip");
            }
            if (!rescueFileLegacy.exists()) {
                copyRescueFile(getApplicationContext(), "rescue_legacy.dat",
                        Environment.getExternalStorageDirectory() +
                                java.io.File.separator + "substratum" +
                                java.io.File.separator + "SubstratumRescue_Legacy.zip");
            }
            if (permissionCheck2 == PackageManager.PERMISSION_GRANTED) {
                // permission already granted, allow the program to continue running
                // Set the first option to start at app boot
                drawer.setSelectionAtPosition(1);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        PERMISSIONS_REQUEST_READ_PHONE_STATE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        // Now, let's grab root on the helper
        Intent rootIntent = new Intent(Intent.ACTION_MAIN);
        rootIntent.setAction("masquerade.substratum.INITIALIZE");
        try {
            startActivity(rootIntent);
        } catch (RuntimeException re) {
            // Exception: At this point, Masquerade is not installed at all.
        }

        if (References.checkOMS()) {
            if (!prefs.getBoolean("substratum_oms", true)) {
                if (!new File(Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + "/.substratum/").exists()) {
                    Root.runCommand("rm -r " + Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/.substratum/");
                }
                if (!new File(Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + "/substratum/").exists()) {
                    Root.runCommand("rm -r " + Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/substratum/");
                }
                File directory = new File(Environment.getExternalStorageDirectory(),
                        "/.substratum/");
                if (!directory.exists()) {
                    directory.mkdirs();
                }
            }
        }

        printFCMtoken();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (permissionCheck == PackageManager.PERMISSION_GRANTED && permissionCheck2 ==
                PackageManager.PERMISSION_GRANTED) {
            //add the values which need to be saved from the drawer to the bundle
            outState = drawer.saveInstanceState(outState);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (References.checkOMS()) {
            getMenuInflater().inflate(R.menu.activity_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.activity_menu_legacy, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                "substratum_state", Context.MODE_PRIVATE);
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.refresh:
                Fragment f = getSupportFragmentManager().findFragmentById(R.id.main);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                if (f instanceof Fragment)
                    ft.detach(f).attach(f).commit();
                Toast toast = Toast.makeText(getApplicationContext(),
                        getApplicationContext().getString(R.string.refresh_fragment),
                        Toast.LENGTH_SHORT);
                toast.show();
                return true;
            case R.id.search:
                Intent intent = new Intent(this, ShowcaseActivity.class);
                startActivity(intent);
                return true;

            // Begin OMS based options
            case R.id.refresh_windows:
                prefs.edit().clear().apply();
                if (References.isPackageInstalled(getApplicationContext(),
                        "masquerade.substratum")) {
                    Intent runCommand = new Intent();
                    runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    runCommand.setAction("masquerade.substratum.COMMANDS");
                    runCommand.putExtra("om-commands", "om refresh");
                    getApplicationContext().sendBroadcast(runCommand);
                } else {
                    Root.runCommand("om refresh");
                }
                return true;
            case R.id.restart_systemui:
                prefs.edit().clear().apply();
                Root.runCommand("pkill -f com.android.systemui");
                return true;

            // Begin RRO based options
            case R.id.reboot_device:
                prefs.edit().clear().apply();
                Root.runCommand("reboot");
                return true;
            case R.id.soft_reboot:
                prefs.edit().clear().apply();
                Root.runCommand("pkill -f zygote");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else {
            if (drawerSelected != 1) {
                switchFragment(getString(R.string.app_name), "HomeFragment");
                drawer.setSelectionAtPosition(1);
                drawerSelected = 1;
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission already granted, allow the program to continue running
                    File directory = new File(Environment.getExternalStorageDirectory(),
                            "/.substratum/");
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    File cacheDirectory = new File(getCacheDir(),
                            "/SubstratumBuilder/");
                    if (!cacheDirectory.exists()) {
                        cacheDirectory.mkdirs();
                    }
                    File[] fileList = new File(getCacheDir().getAbsolutePath() +
                            "/SubstratumBuilder/").listFiles();
                    for (int i = 0; i < fileList.length; i++) {
                        Root.runCommand(
                                "rm -r " + getCacheDir().getAbsolutePath() +
                                        "/SubstratumBuilder/" + fileList[i].getName());
                    }
                    Log.d("SubstratumBuilder", "The cache has been flushed!");
                    if (permissionCheck2 == PackageManager.PERMISSION_GRANTED) {
                        // permission already granted, allow the program to continue running
                        // Set the first option to start at app boot
                        drawer.setSelectionAtPosition(1);
                    } else {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.READ_PHONE_STATE},
                                PERMISSIONS_REQUEST_READ_PHONE_STATE);
                    }
                } else {
                    // permission was not granted, show closing dialog
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.permission_not_granted_dialog_title)
                            .setMessage(R.string.permission_not_granted_dialog_message1)
                            .setPositiveButton(R.string.dialog_ok, new DialogInterface
                                    .OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    MainActivity.this.finish();
                                }
                            })
                            .show();
                    return;
                }
                break;
            }
            case PERMISSIONS_REQUEST_READ_PHONE_STATE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission already granted, allow the program to continue running
                    // Set the first option to start at app boot
                    drawer.setSelectionAtPosition(1);
                } else {
                    // permission was not granted, show closing dialog
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.permission_not_granted_dialog_title)
                            .setMessage(R.string.permission_not_granted_dialog_message2)
                            .setPositiveButton(R.string.dialog_ok, new DialogInterface
                                    .OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    MainActivity.this.finish();
                                }
                            })
                            .show();
                    return;
                }
                break;
            }
        }
    }
}
