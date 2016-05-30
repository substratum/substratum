package projekt.substratum;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;


/**
 * @author Nicholas Chum (nicholaschum)
 */
public class ThemeInformation extends AppCompatActivity {

    public ListView listView;
    public String theme_name, theme_pid;
    public AssetManager am;
    public String[] values;

    public Drawable grabPackageHeroImage(String package_name) {
        Resources res;
        Drawable hero = null;
        try {
            //I want to use the clear_activities string in Package com.android.settings
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

        // Handle collapsible toolbar with theme name

        Intent currentIntent = getIntent();
        theme_name = currentIntent.getStringExtra("theme_name");
        theme_pid = currentIntent.getStringExtra("theme_pid");

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

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, values);

        if (listView != null) {
            listView.setNestedScrollingEnabled(true);
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            listView.setAdapter(adapter);
        }

        // Handle the logic for selecting all overlays or not

        Switch toggle_overlays = (Switch) findViewById(R.id.toggle_all_overlays);
        if (toggle_overlays != null) {
            toggle_overlays.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            SparseBooleanArray checked = listView.getCheckedItemPositions();
                            for (int i = 0; i < listView.getAdapter().getCount(); i++) {
                                if (checked.get(i)) {
                                    if (!isChecked == listView.isItemChecked(i)) {
                                        listView.setItemChecked(i, false);
                                    } else {
                                        listView.setItemChecked(i, true);
                                    }
                                } else {
                                    if (!isChecked == listView.isItemChecked(i)) {
                                        listView.setItemChecked(i, true);
                                    } else {
                                        listView.setItemChecked(i, false);
                                    }
                                }
                            }
                        }
                    });
        }

        Button button = (Button) findViewById(R.id.btnSelection);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    SparseBooleanArray checked = listView.getCheckedItemPositions();
                    Log.e("==================", "=============================");
                    for (int i = 0; i < listView.getAdapter().getCount(); i++) {
                        if (checked.get(i)) {
                            Log.e("You have selected", listView.getItemAtPosition(i).toString());
                        }
                    }
                    Log.e("==================", "=============================");
                }
            });
        }
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
        super.onBackPressed();
    }
}
