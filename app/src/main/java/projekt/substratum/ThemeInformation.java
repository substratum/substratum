package projekt.substratum;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.util.LayersBuilder;


/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ThemeInformation extends AppCompatActivity {

    public ListView listView;
    public String theme_name, theme_pid;
    public AssetManager am;
    public String[] values;
    public Boolean has_extracted_cache;
    public LayersBuilder lb;
    public List<String> listStrings, erroredOverlays;
    public Switch toggle_overlays;
    private PowerManager.WakeLock mWakeLock;
    private ArrayList<String> enabled_overlays;
    private ArrayAdapter<String> adapter;

    private boolean isPackageInstalled(Context context, String package_name) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void checkEnabledOverlays() {
        try {
            String line;
            enabled_overlays = new ArrayList<String>();
            Process nativeApp = Runtime.getRuntime().exec(
                    "om list");

            OutputStream stdin = nativeApp.getOutputStream();
            InputStream stderr = nativeApp.getErrorStream();
            InputStream stdout = nativeApp.getInputStream();
            stdin.write(("ls\n").getBytes());
            stdin.write("exit\n".getBytes());
            stdin.flush();
            stdin.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while ((line = br.readLine()) != null) {
                if (line.contains("    [x] ")) {
                    enabled_overlays.add(line.substring(8));
                }
            }
            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
                Log.e("LayersBuilder", line);
            }
            br.close();
        } catch (IOException ioe) {
        }
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
        return hero;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.theme_information);

        enabled_overlays = new ArrayList<String>();
        has_extracted_cache = false;

        // Handle collapsible toolbar with theme name

        Intent currentIntent = getIntent();
        theme_name = currentIntent.getStringExtra("theme_name");
        theme_pid = currentIntent.getStringExtra("theme_pid");

        final FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R
                .id.apply_fab);
        if (floatingActionButton != null) {
            floatingActionButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    SparseBooleanArray checked = listView.getCheckedItemPositions();
                    listStrings = new ArrayList<String>();
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
            floatingActionButton.hide();
        }

        ImageView imageView = (ImageView) findViewById(R.id.preview_image);
        imageView.setImageDrawable(grabPackageHeroImage(theme_pid));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(theme_name);

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById
                (R.id.collapsingToolbarLayout);
        collapsingToolbarLayout.setTitle(theme_name);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Handle all overlays that are located in the APK
        listView = (ListView) findViewById(R.id.overlay_picker);

        //
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
                    floatingActionButton.show();
                } else {
                    if (toggle_overlays.isChecked()) {
                        toggle_overlays.setChecked(false);
                    }
                    floatingActionButton.hide();
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
                            SparseBooleanArray checked = listView.getCheckedItemPositions();
                            for (int i = 0; i < listView.getAdapter().getCount(); i++) {
                                if (checked.get(i)) {
                                    if (!isChecked == listView.isItemChecked(i)) {
                                        floatingActionButton.hide();
                                        listView.setItemChecked(i, false);
                                    } else {
                                        floatingActionButton.show();
                                        listView.setItemChecked(i, true);
                                    }
                                } else {
                                    if (!isChecked == listView.isItemChecked(i)) {
                                        floatingActionButton.show();
                                        listView.setItemChecked(i, true);
                                    } else {
                                        floatingActionButton.hide();
                                        listView.setItemChecked(i, false);
                                    }
                                }
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

        if (id == R.id.rate) {
            String playURL = "https://play.google.com/store/apps/details?id=" + theme_pid;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(playURL));
            startActivity(i);
            return true;
        }
        if (id == R.id.uninstall) {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + theme_pid));
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Destroy the cache if the user leaves the activity
        // Superuser is used due to some files being held hostage by the system

        File[] fileList = new File(getCacheDir().getAbsolutePath() +
                "/LayersBuilder/").listFiles();
        for (int i = 0; i < fileList.length; i++) {
            eu.chainfire.libsuperuser.Shell.SU.run(
                    "rm -r " + getCacheDir().getAbsolutePath() +
                            "/LayersBuilder/" + fileList[i].getName());
        }
        Log.d("LayersBuilder", "The cache has been flushed!");

        super.onBackPressed();
    }

    private class LoadOverlays extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.d("Phase 1", "This phase has started it's asynchronous task.");
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            MaterialProgressBar materialProgressBar = (MaterialProgressBar) findViewById(R.id
                    .progress_bar);
            if (materialProgressBar != null) materialProgressBar.setVisibility(View.GONE);

            if (listView != null) {
                listView.setNestedScrollingEnabled(true);
                listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                adapter = new ArrayAdapter<String>(ThemeInformation.this, android.R.layout
                        .simple_list_item_multiple_choice, values) {
                    @Override
                    public View getView(final int position, View convertView, ViewGroup parent) {
                        TextView textView = (TextView) super.getView(position, convertView, parent);
                        if (isPackageInstalled(ThemeInformation.this, listView.getItemAtPosition
                                (position).toString() + "." + theme_name)) {
                            if (enabled_overlays.contains(listView.getItemAtPosition(position)
                                    .toString() + "." + theme_name)) {
                                textView.setTextColor(getColor(R.color
                                        .overlay_installed_list_entry));
                                textView.setTypeface(null, Typeface.BOLD_ITALIC);
                            } else {
                                textView.setTextColor(getColor(R.color
                                        .overlay_not_enabled_list_entry));
                                textView.setTypeface(null, Typeface.NORMAL);
                            }
                        } else {
                            textView.setTextColor(getColor(R.color
                                    .overlay_not_installed_list_entry));
                            textView.setTypeface(null, Typeface.NORMAL);
                        }
                        return textView;
                    }
                };
                listView.setAdapter(adapter);
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Parse the list of overlay folders inside assets/overlays
            try {
                Context otherContext = createPackageContext(theme_pid, 0);
                am = otherContext.getAssets();
                values = new String[am.list("overlays").length];
                for (int i = 0; i < am.list("overlays").length; i++) {
                    values[i] = am.list("overlays")[i];
                }
            } catch (Exception e) {
            }
            checkEnabledOverlays();
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
            LayersBuilder aaptCheck = new LayersBuilder();
            aaptCheck.injectAAPT(getApplicationContext());
            return null;
        }
    }

    private class Phase2_InitializeCache extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.d("Phase 2", "This phase has started it's asynchronous task.");
            PowerManager pm = (PowerManager)
                    getApplicationContext().getSystemService(Context.POWER_SERVICE);
            mWakeLock = null;
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
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
            if (!has_extracted_cache) {
                lb = new LayersBuilder();
                lb.initializeCache(getApplicationContext(), theme_pid);
                has_extracted_cache = true;
            }
            return null;
        }
    }

    private class Phase3_mainFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            Log.d("Phase 3", "This phase has started it's asynchronous task.");
            erroredOverlays = new ArrayList<String>();
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            mWakeLock.release();
            if (erroredOverlays.size() > 0) {
                for (int i = 0; i < erroredOverlays.size(); i++) {
                    String toast_text = String.format(getApplicationContext().getResources()
                            .getString(
                                    R.string.failed_to_install_overlay_toast), erroredOverlays
                            .get(i));
                    Toast toast = Toast.makeText(getApplicationContext(), toast_text,
                            Toast.LENGTH_SHORT);
                    toast.show();
                    adapter.notifyDataSetChanged();
                }
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string
                                .toast_installed),
                        Toast.LENGTH_SHORT);
                toast.show();
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Initialize the cache for this specific app
            for (int i = 0; i < listStrings.size(); i++) {
                lb = new LayersBuilder();
                lb.beginAction(getApplicationContext(), listStrings.get(i), theme_name);
                if (lb.has_errored_out) {
                    erroredOverlays.add(listStrings.get(i));
                }
            }
            return null;
        }
    }
}
