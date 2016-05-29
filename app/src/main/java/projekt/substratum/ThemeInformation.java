package projekt.substratum;

import android.content.Intent;
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
import android.widget.ListView;
import android.widget.Switch;


/**
 * @author Nicholas Chum (nicholaschum)
 */
public class ThemeInformation extends AppCompatActivity {

    public ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.theme_configurator);

        // Handle collapsible toolbar with theme name

        Intent currentIntent = getIntent();
        String theme_name = currentIntent.getStringExtra("theme_name");
        String theme_pid = currentIntent.getStringExtra("theme_pid");

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

        // Defined Array values to show in ListView
        // TODO: Parse the list of overlay folders inside assets/overlays
        String[] values = new String[]{"Android List View",
                "Adapter implementation",
                "Simple List View In Android",
                "Create List View Android",
                "Android Example",
                "List View Source Code",
                "List View Array Adapter",
                "Android Example List View"
        };

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
        getMenuInflater().inflate(R.menu.theme_configuration_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
