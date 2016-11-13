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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import projekt.substrate.LetsGetStarted;
import projekt.substratum.config.References;
import projekt.substratum.fragments.ThemeFragment;
import projekt.substratum.services.ThemeService;
import projekt.substratum.util.Root;

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PERMISSIONS_REQUEST_READ_PHONE_STATE = 2;
    private Drawer drawer;
    private int drawerSelected;
    private int permissionCheck, permissionCheck2;
    private ProgressDialog mProgressDialog;
    private SharedPreferences prefs;

    private void switchFragment(String title, String fragment) {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.main, Fragment.instantiate(MainActivity.this, "projekt.substratum" +
                ".fragments." + fragment));
        tx.commit();
    }

    private void switchThemeFragment(String title, String home_type) {
        Fragment fragment = new ThemeFragment();
        Bundle bundle = new Bundle();
        bundle.putString("home_type", home_type);
        fragment.setArguments(bundle);

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.main, fragment);
        tx.commit();
    }

    private void switchFragmentToLicenses(String title, LibsSupportFragment fragment) {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
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
        if (!destParentDir.exists()) {
            Boolean made = destParentDir.mkdir();
            if (!made) Log.e("SubstratumLogger",
                    "Unable to create directories for rescue archive dumps.");
        }

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
            out.flush();
            out.close();
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

        References.setAndCheckOMS(getApplicationContext());
        startService(new Intent(this, ThemeService.class));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(false);
            }
        }

        new LetsGetStarted().kissMe();

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

        prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        if (toolbar != null) drawerBuilder.withToolbar(toolbar);
        drawerBuilder.withSavedInstance(savedInstanceState);
        drawerBuilder.withActionBarDrawerToggleAnimated(true);
        if (prefs.getBoolean("alternate_drawer_design", false)) {
            drawerBuilder.withRootView(R.id.drawer_container);
            drawerBuilder.withHeaderHeight(DimenHolder.fromDp(0));
        }
        drawerBuilder.withAccountHeader(header);
        if (References.checkOMS(getApplicationContext())) {
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
                            R.drawable.nav_drawer_profiles).withEnabled(false),
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
                                    switchThemeFragment(((References.checkOMS(
                                            getApplicationContext())) ?
                                                    getString(R.string.app_name) :
                                                    getString(R.string.legacy_app_name)),
                                            References.homeFragment);
                                    drawerSelected = 1;
                                }
                                break;
                            case 2:
                                if (drawerSelected != position) {
                                    switchThemeFragment(getString(R.string.nav_overlays),
                                            References.overlaysFragment);
                                    drawerSelected = 2;
                                }
                                break;
                            case 3:
                                if (drawerSelected != position) {
                                    switchThemeFragment(getString(R.string.nav_bootanim),
                                            References.bootAnimationsFragment);
                                    drawerSelected = 3;
                                }
                                break;
                            case 4:
                                if (drawerSelected != position) {
                                    switchThemeFragment(getString(R.string.nav_fonts),
                                            References.fontsFragment);
                                    drawerSelected = 4;
                                }
                                break;
                            case 5:
                                if (drawerSelected != position) {
                                    switchThemeFragment(getString(R.string.nav_sounds),
                                            References.soundsFragment);
                                    drawerSelected = 5;
                                }
                                break;
                            case 6:
                                if (drawerSelected != position) {
                                    switchThemeFragment(getString(R.string.nav_wallpapers),
                                            References.wallpaperFragment);
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
                                switchFragment(getString(R.string.nav_priorities),
                                        "PriorityLoaderFragment");
                                drawerSelected = 10;
                                break;
                            case 11:
                                switchFragment(getString(R.string.nav_backup_restore),
                                        "ProfileFragment");
                                drawerSelected = 11;
                                break;
                            case 13:
                                if (drawerSelected != position) {
                                    switchFragment(getString(R.string.nav_troubleshooting),
                                            "TroubleshootingFragment");
                                    drawerSelected = 13;
                                }
                                break;
                            case 14:
                                if (drawerSelected != position) {
                                    switchFragment(getString(R.string.nav_team),
                                            "TeamFragment");
                                    drawerSelected = 14;
                                }
                                break;
                            case 15:
                                if (drawerSelected != position) {
                                    switchFragmentToLicenses(getString(R.string.nav_opensource),
                                            fragment);
                                    drawerSelected = 15;
                                }
                                break;
                            case 16:
                                if (drawerSelected != position) {
                                    switchFragment(getString(R.string.nav_settings),
                                            "SettingsFragment");
                                    drawerSelected = 16;
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
                Class<?> cls = Class.forName("android.graphics.Typeface");
                cls.getDeclaredMethod("getSystemFontDirLocation");
                cls.getDeclaredMethod("getThemeFontConfigLocation");
                cls.getDeclaredMethod("getThemeFontDirLocation");
                Log.e("SubstratumLogger", "This device on the legacy system fully supports font " +
                        "hotswapping.");
                fonts_allowed = true;
            } catch (Exception ex) {
                // Suppress Fonts
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
                                        R.drawable.nav_drawer_profiles).withEnabled(false),
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
                                        switchThemeFragment(((References.checkOMS(
                                                getApplicationContext())) ?
                                                        getString(R.string.app_name) :
                                                        getString(R.string
                                                                .legacy_app_name)),
                                                References.homeFragment);
                                        drawerSelected = 1;
                                    }
                                    break;
                                case 2:
                                    if (drawerSelected != position) {
                                        switchThemeFragment(getString(R.string.nav_overlays),
                                                References.overlaysFragment);
                                        drawerSelected = 2;
                                    }
                                    break;
                                case 3:
                                    if (drawerSelected != position) {
                                        switchThemeFragment(getString(R.string.nav_bootanim),
                                                References.bootAnimationsFragment);
                                        drawerSelected = 3;
                                    }
                                    break;
                                case 4:
                                    if (drawerSelected != position) {
                                        switchThemeFragment(getString(R.string.nav_fonts),
                                                References.fontsFragment);
                                        drawerSelected = 4;
                                    }
                                    break;
                                case 5:
                                    if (drawerSelected != position) {
                                        switchThemeFragment(getString(R.string.nav_sounds),
                                                References.soundsFragment);
                                        drawerSelected = 5;
                                    }
                                    break;
                                case 6:
                                    if (drawerSelected != position) {
                                        switchThemeFragment(getString(R.string.nav_wallpapers),
                                                References.wallpaperFragment);
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
                                    if (drawerSelected != position) {
                                        switchFragmentToLicenses(getString(R.string
                                                        .nav_opensource),
                                                fragment);
                                        drawerSelected = 14;
                                    }
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
                                        .drawable.nav_drawer_profiles).withEnabled(false),

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
                                        switchThemeFragment(((References.checkOMS(
                                                getApplicationContext())) ?
                                                        getString(R.string.app_name) :
                                                        getString(R.string
                                                                .legacy_app_name)),
                                                References.homeFragment);
                                        drawerSelected = 1;
                                    }
                                    break;
                                case 2:
                                    if (drawerSelected != position) {
                                        switchThemeFragment(getString(R.string.nav_overlays),
                                                References.overlaysFragment);
                                        drawerSelected = 2;
                                    }
                                    break;
                                case 3:
                                    if (drawerSelected != position) {
                                        switchThemeFragment(getString(R.string.nav_bootanim),
                                                References.bootAnimationsFragment);
                                        drawerSelected = 3;
                                    }
                                    break;
                                case 4:
                                    if (drawerSelected != position) {
                                        switchThemeFragment(getString(R.string.nav_sounds),
                                                References.soundsFragment);
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
                                    if (drawerSelected != position) {
                                        switchFragmentToLicenses(getString(R.string
                                                        .nav_opensource),
                                                fragment);
                                        drawerSelected = 12;
                                    }
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

        permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionCheck2 = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_PHONE_STATE);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // permission already granted, allow the program to continue running
            File directory = new File(Environment.getExternalStorageDirectory(),
                    "/.substratum/");
            if (!directory.exists()) {
                Boolean made = directory.mkdirs();
                if (!made) Log.e("SubstratumLogger", "Unable to create directory");
            }
            File cacheDirectory = new File(getCacheDir(),
                    "/SubstratumBuilder/");
            if (!cacheDirectory.exists()) {
                Boolean made = cacheDirectory.mkdirs();
                if (!made) Log.e("SubstratumLogger", "Unable to create cache directory");
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
                if (References.spreadYourWingsAndFly(getApplicationContext())) {
                    LetsGetStarted.kissMe();
                }
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

        printFCMtoken();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && !References.getProp("ro" +
                ".substratum.verified").equals("true")) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.api25_warning_title)
                    .setMessage(R.string.api25_warning_content)
                    .setPositiveButton(R.string.dialog_ok, new DialogInterface
                            .OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .show();
        }

        mProgressDialog = new ProgressDialog(this, R.style.SubstratumBuilder_ActivityTheme);
        new RootRequester().execute("");
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
        if (References.checkOMS(getApplicationContext())) {
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
                if (f != null)
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
            case R.id.restart_systemui:
                prefs.edit().clear().apply();
                References.restartSystemUI();
                return true;

            // Begin RRO based options
            case R.id.reboot_device:
                prefs.edit().clear().apply();
                References.reboot();
                return true;
            case R.id.soft_reboot:
                prefs.edit().clear().apply();
                References.softReboot();
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
                switchThemeFragment(getString(R.string.app_name), References.homeFragment);
                drawer.setSelectionAtPosition(1);
                drawerSelected = 1;
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission already granted, allow the program to continue running
                    File directory = new File(Environment.getExternalStorageDirectory(),
                            "/.substratum/");
                    if (!directory.exists()) {
                        Boolean made = directory.mkdirs();
                        if (!made) Log.e("SubstratumLogger",
                                "Could not make internal substratum directory.");
                    }
                    File cacheDirectory = new File(getCacheDir(),
                            "/SubstratumBuilder/");
                    if (!cacheDirectory.exists()) {
                        Boolean made = cacheDirectory.mkdirs();
                        if (!made) Log.e("SubstratumLogger", "Could not create cache directory.");
                    }
                    File[] fileList = new File(getCacheDir().getAbsolutePath() +
                            "/SubstratumBuilder/").listFiles();
                    for (File file : fileList) {
                        References.delete(getCacheDir().getAbsolutePath() +
                                "/SubstratumBuilder/" + file.getName());
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

    private class RootRequester extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
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
                        .blurAlgorithm(new RenderScriptBlur(getApplicationContext(), true))
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
            } else {
                // Now, let's grab root on the helper
                Intent rootIntent = new Intent(Intent.ACTION_MAIN);
                rootIntent.setAction("masquerade.substratum.INITIALIZE");
                try {
                    startActivity(rootIntent);
                } catch (RuntimeException re) {
                    // Exception: At this point, Masquerade is not installed at all.
                }
            }
            super.onPostExecute(result);
        }

        @Override
        protected Boolean doInBackground(String... sUrl) {
            Boolean receivedRoot = Root.requestRootAccess();
            if (receivedRoot) {
                if (References.checkOMS(getApplicationContext())) {
                    if (!prefs.getBoolean("substratum_oms", true)) {
                        if (!new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/.substratum/").exists()) {
                            References.delete(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/.substratum/");
                        }
                        if (!new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/substratum/").exists()) {
                            References.delete(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/substratum/");
                        }
                        File directory = new File(Environment.getExternalStorageDirectory(),
                                "/.substratum/");
                        if (!directory.exists()) {
                            Boolean made = directory.mkdirs();
                            if (!made) Log.e("SubstratumLogger",
                                    "Could not make substratum directory on internal storage.");
                        }
                    }
                }
            }
            return receivedRoot;
        }
    }
}