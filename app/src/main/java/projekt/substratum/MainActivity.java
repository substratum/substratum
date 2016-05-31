package projekt.substratum;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import projekt.substratum.adapters.DataAdapter;
import projekt.substratum.util.ThemeParser;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private HashMap<String, String[]> layers_packages;
    private RecyclerView recyclerView;
    private Map<String, String[]> map;
    private Context mContext;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<ApplicationInfo> list;
    private DataAdapter adapter;

    private boolean isPackageInstalled(Context context, String package_name) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void getLayersPackages(Context context, String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString("Layers_Name") != null) {
                    if (appInfo.metaData.getString("Layers_Developer") != null) {
                        if (appInfo.metaData.getString("Substratum_Enabled") != null) {
                            String[] data = {appInfo.metaData.getString("Layers_Developer"),
                                    package_name};
                            layers_packages.put(appInfo.metaData.getString("Layers_Name"), data);
                        }
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.search) {
            String playURL = "https://play.google.com/store/search?q=layers%20theme&c=apps";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(playURL));
            startActivity(i);
            return true;
        }

        if (id == R.id.settings) {
            Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class);
            MainActivity.this.startActivity(myIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshLayout() {
        PackageManager packageManager = getPackageManager();
        list.clear();
        recyclerView.setAdapter(null);
        layers_packages = new HashMap<String, String[]>();
        list = packageManager.getInstalledApplications(PackageManager
                .GET_META_DATA);
        for (ApplicationInfo packageInfo : list) {
            getLayersPackages(mContext, packageInfo.packageName);
        }
        Log.d("Substratum Ready Themes", Integer.toString(layers_packages.size()));

        // Now we need to sort the buffered installed Layers themes
        map = new TreeMap<String, String[]>(layers_packages);
        ArrayList<ThemeParser> themeParsers = prepareData();
        adapter = new DataAdapter(getApplicationContext(), themeParsers);
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);
        mContext = this;
        layers_packages = new HashMap<String, String[]>();
        recyclerView = (RecyclerView) findViewById(R.id.theme_list);

        // Create it so it uses a recyclerView to parse substratum-based themes

        PackageManager packageManager = getPackageManager();
        list = packageManager.getInstalledApplications(PackageManager
                .GET_META_DATA);
        for (ApplicationInfo packageInfo : list) {
            getLayersPackages(mContext, packageInfo.packageName);
        }
        Log.d("Substratum Ready Themes", Integer.toString(layers_packages.size()));

        // Now we need to sort the buffered installed Layers themes
        map = new TreeMap<String, String[]>(layers_packages);

        ArrayList<ThemeParser> themeParsers = prepareData();
        adapter = new DataAdapter(getApplicationContext(), themeParsers);

        // Assign adapter to RecyclerView
        recyclerView.setAdapter(adapter);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            GestureDetector gestureDetector = new GestureDetector(getApplicationContext(),
                    new GestureDetector.SimpleOnGestureListener() {

                        @Override
                        public boolean onSingleTapUp(MotionEvent e) {
                            return true;
                        }

                    });

            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                View child = rv.findChildViewUnder(e.getX(), e.getY());
                if (child != null && gestureDetector.onTouchEvent(e)) {
                    // RecyclerView Clicked item value
                    int position = rv.getChildAdapterPosition(child);

                    // Process fail case if user uninstalls an app and goes back an activity
                    if (isPackageInstalled(getApplicationContext(),
                            map.get(map.keySet().toArray()[position].toString())[1])) {
                        Intent myIntent = new Intent(MainActivity.this, ThemeInformation.class);
                        //myIntent.putExtra("key", value); //Optional parameters
                        myIntent.putExtra("theme_name", map.keySet().toArray()[position].toString
                                ());
                        myIntent.putExtra("theme_pid", map.get(map.keySet().toArray()[position]
                                .toString())[1]);
                        MainActivity.this.startActivity(myIntent);
                    } else {
                        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string
                                        .toast_uninstalled),
                                Toast.LENGTH_SHORT);
                        toast.show();
                        refreshLayout();
                    }
                }

                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh items
                refreshLayout();
            }
        });

        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // permission already granted, allow the program to continue running
            File directory = new File(Environment.getExternalStorageDirectory(),
                    "/substratum/");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File cacheDirectory = new File(getCacheDir(),
                    "/LayersBuilder/");
            if (!cacheDirectory.exists()) {
                cacheDirectory.mkdirs();
            }
            File[] fileList = new File(getCacheDir().getAbsolutePath() +
                    "/LayersBuilder/").listFiles();
            for (int i = 0; i < fileList.length; i++) {
                eu.chainfire.libsuperuser.Shell.SU.run(
                        "rm -r " + getCacheDir().getAbsolutePath() +
                                "/LayersBuilder/" + fileList[i].getName());
            }
            Log.d("LayersBuilder", "The cache has been flushed!");
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
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
                            "/substratum/");
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    File cacheDirectory = new File(getCacheDir(),
                            "/LayersBuilder/");
                    if (!cacheDirectory.exists()) {
                        cacheDirectory.mkdirs();
                    }
                    File[] fileList = new File(getCacheDir().getAbsolutePath() +
                            "/LayersBuilder/").listFiles();
                    for (int i = 0; i < fileList.length; i++) {
                        eu.chainfire.libsuperuser.Shell.SU.run(
                                "rm -r " + getCacheDir().getAbsolutePath() +
                                        "/LayersBuilder/" + fileList[i].getName());
                    }
                    Log.d("LayersBuilder", "The cache has been flushed!");
                } else {
                    // permission was not granted, show closing dialog
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.permission_not_granted_dialog_title)
                            .setMessage(R.string.permission_not_granted_dialog_message)
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

    private ArrayList<ThemeParser> prepareData() {

        ArrayList<ThemeParser> themes = new ArrayList<>();
        for (int i = 0; i < map.size(); i++) {
            ThemeParser themeParser = new ThemeParser();
            themeParser.setThemeName(map.keySet().toArray()[i].toString());
            themeParser.setThemeAuthor(map.get(map.keySet().toArray()[i].toString())[0]);
            themeParser.setThemePackage(map.get(map.keySet().toArray()[i].toString())[1]);
            themes.add(themeParser);
        }
        return themes;
    }

}
