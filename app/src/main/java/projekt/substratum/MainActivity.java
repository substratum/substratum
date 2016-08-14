package projekt.substratum;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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

import com.google.firebase.iid.FirebaseInstanceId;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.io.File;

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

        if (References.checkOMS()) {
            drawer = new DrawerBuilder()
                    .withActivity(this)
                    .withToolbar(toolbar)
                    .withAccountHeader(header)
                    .withSavedInstance(savedInstanceState)
                    .withActionBarDrawerToggleAnimated(true)
                    .addDrawerItems(
                            new PrimaryDrawerItem().withName(R.string.nav_home).withIcon(R.drawable
                                    .nav_theme_packs),
                            new PrimaryDrawerItem().withName(R.string.nav_overlays).withIcon(R
                                    .drawable
                                    .nav_overlays),
                            new PrimaryDrawerItem().withName(R.string.nav_bootanim).withIcon(R
                                    .drawable
                                    .nav_bootanim),
                            new PrimaryDrawerItem().withName(R.string.nav_fonts).withIcon(R.drawable
                                    .nav_fonts),
                            new PrimaryDrawerItem().withName(R.string.nav_sounds).withIcon(R
                                    .drawable
                                    .nav_sounds),
                            new SectionDrawerItem().withName(R.string.nav_section_header_utilities),
                            new PrimaryDrawerItem().withName(R.string.nav_overlay_manager)
                                    .withIcon(R
                                            .drawable
                                            .nav_overlay_manager),
                            new PrimaryDrawerItem().withName(R.string.nav_manage).withIcon(R
                                    .drawable.nav_manage),
                            new PrimaryDrawerItem().withName(R.string.nav_priorities).withIcon(R
                                    .drawable.nav_drawer_priorities),
                            new PrimaryDrawerItem().withName(R.string.nav_backup_restore).withIcon(R
                                    .drawable.nav_drawer_profiles),
                            new SectionDrawerItem().withName(R.string.nav_section_header_more),
                            new SecondaryDrawerItem().withName(R.string.nav_troubleshooting)
                                    .withIcon(R
                                            .drawable.nav_troubleshooting),
                            new SecondaryDrawerItem().withName(R.string.nav_team).withIcon(R
                                    .drawable.nav_drawer_team),
                            new SecondaryDrawerItem().withName(getString(R.string.nav_opensource))
                                    .withIcon(R
                                            .drawable.nav_drawer_licenses),
                            new SecondaryDrawerItem().withName(R.string.nav_settings).withIcon(R
                                    .drawable.nav_drawer_settings)
                    )
                    .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
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
                                    case 7:
                                        switchFragment(getString(R.string.nav_overlay_manager),
                                                "AdvancedManagerFragment");
                                        drawerSelected = 7;
                                        break;
                                    case 8:
                                        switchFragment(getString(R.string.nav_manage),
                                                "ManageFragment");
                                        drawerSelected = 8;
                                        break;
                                    case 9:
                                        switchFragment(getString(R.string.nav_priorities),
                                                "PriorityLoaderFragment");
                                        drawerSelected = 9;
                                        break;
                                    case 10:
                                        switchFragment(getString(R.string.nav_backup_restore),
                                                "ProfileFragment");
                                        drawerSelected = 10;
                                        break;
                                    case 12:
                                        if (drawerSelected != position) {
                                            switchFragment(getString(R.string.nav_troubleshooting),
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
                                        switchFragmentToLicenses(getString(R.string.nav_opensource),
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
                    })
                    .withSelectedItem(1)
                    .withSelectedItemByPosition(1)
                    .build();
        } else {
            drawer = new DrawerBuilder()
                    .withActivity(this)
                    .withToolbar(toolbar)
                    .withAccountHeader(header)
                    .withSavedInstance(savedInstanceState)
                    .withActionBarDrawerToggleAnimated(true)
                    .addDrawerItems(
                            new PrimaryDrawerItem().withName(R.string.nav_home).withIcon(R.drawable
                                    .nav_theme_packs),
                            new PrimaryDrawerItem().withName(R.string.nav_overlays).withIcon(R
                                    .drawable
                                    .nav_overlays),
                            new PrimaryDrawerItem().withName(R.string.nav_sounds).withIcon(R
                                    .drawable
                                    .nav_sounds),

                            new SectionDrawerItem().withName(R.string.nav_section_header_utilities),
                            new PrimaryDrawerItem().withName(R.string.nav_overlay_manager)
                                    .withIcon(R
                                            .drawable
                                            .nav_overlay_manager),
                            new PrimaryDrawerItem().withName(R.string.nav_manage).withIcon(R
                                    .drawable.nav_manage),
                            new PrimaryDrawerItem().withName(R.string.nav_backup_restore).withIcon(R
                                    .drawable.nav_drawer_profiles),

                            new SectionDrawerItem().withName(R.string.nav_section_header_more),
                            new SecondaryDrawerItem().withName(R.string.nav_troubleshooting)
                                    .withIcon(R
                                            .drawable.nav_troubleshooting),
                            new SecondaryDrawerItem().withName(R.string.nav_team).withIcon(R
                                    .drawable.nav_drawer_team),
                            new SecondaryDrawerItem().withName(getString(R.string.nav_opensource))
                                    .withIcon(R
                                            .drawable.nav_drawer_licenses),
                            new SecondaryDrawerItem().withName(R.string.nav_settings).withIcon(R
                                    .drawable.nav_drawer_settings)
                    )
                    .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
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
                                            switchFragment(getString(R.string.nav_sounds),
                                                    "SoundsFragment");
                                            drawerSelected = 3;
                                        }
                                        break;
                                    case 5:
                                        switchFragment(getString(R.string.nav_overlay_manager),
                                                "AdvancedManagerFragment");
                                        drawerSelected = 5;
                                        break;
                                    case 6:
                                        switchFragment(getString(R.string.nav_manage),
                                                "ManageFragment");
                                        drawerSelected = 6;
                                        break;
                                    case 7:
                                        switchFragment(getString(R.string.nav_backup_restore),
                                                "ProfileFragment");
                                        drawerSelected = 7;
                                        break;
                                    case 9:
                                        if (drawerSelected != position) {
                                            switchFragment(getString(R.string.nav_troubleshooting),
                                                    "TroubleshootingFragment");
                                            drawerSelected = 10;
                                        }
                                        break;
                                    case 10:
                                        if (drawerSelected != position) {
                                            switchFragment(getString(R.string.nav_team),
                                                    "TeamFragment");
                                            drawerSelected = 10;
                                        }
                                        break;
                                    case 11:
                                        switchFragmentToLicenses(getString(R.string.nav_opensource),
                                                fragment);
                                        drawerSelected = 11;
                                        break;
                                    case 12:
                                        if (drawerSelected != position) {
                                            switchFragment(getString(R.string.nav_settings),
                                                    "SettingsFragment");
                                            drawerSelected = 12;
                                        }
                                        break;
                                }
                            }
                            return false;
                        }
                    })
                    .withSelectedItem(1)
                    .withSelectedItemByPosition(1)
                    .build();
        }
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

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        if (References.checkOMS()) {
            if (!prefs.getBoolean("substratum_oms", true)) {
                Root.runCommand("rm -r " + Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + "/.substratum/");
                Root.runCommand("rm -r " + Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + "/substratum/");
                File directory = new File(Environment.getExternalStorageDirectory(),
                        "/.substratum/");
                if (!directory.exists()) {
                    directory.mkdirs();
                }
            }
        } else {
            if (prefs.getBoolean("substratum_oms", true)) {
                Root.runCommand("rm -r " + Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + "/.substratum/");
                Root.runCommand("rm -r " + Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + "/substratum/");
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
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.search:
                String playURL;
                if (References.checkOMS()) {
                    playURL = getString(R.string.search_play_store_url);
                } else {
                    playURL = getString(R.string.search_play_store_url_legacy);
                }
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
                return true;

            // Begin OMS based options
            case R.id.refresh_windows:
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
                Root.runCommand("pkill -f com.android.systemui");
                return true;

            // Begin RRO based options
            case R.id.reboot_device:
                Root.runCommand("reboot");
                return true;
            case R.id.soft_reboot:
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