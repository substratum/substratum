package projekt.substratum;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

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

public class MainActivity extends AppCompatActivity {

    public HashMap<String, String[]> layers_packages;
    public RecyclerView recyclerView;
    public Map<String, String[]> map;
    private Context mContext;

    public void getLayersPackages(Context context, String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString("Layers_Name") != null) {
                    if (appInfo.metaData.getString("Layers_Developer") != null) {
                        String[] data = {appInfo.metaData.getString("Layers_Developer"),
                                package_name};
                        layers_packages.put(appInfo.metaData.getString("Layers_Name"), data);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
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
        List<ApplicationInfo> list = packageManager.getInstalledApplications(PackageManager
                .GET_META_DATA);
        for (ApplicationInfo packageInfo : list) {
            Log.d("Processing package", packageInfo.packageName);
            getLayersPackages(mContext, packageInfo.packageName);
        }
        Log.e("Substratum Ready Themes", Integer.toString(layers_packages.size()));

        // Now we need to sort the buffered installed Layers themes
        map = new TreeMap<String, String[]>(layers_packages);

        ArrayList<ThemeParser> headerParsers = prepareData();
        DataAdapter adapter = new DataAdapter(getApplicationContext(), headerParsers);
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

                    Intent myIntent = new Intent(MainActivity.this, ThemeInformation.class);
                    //myIntent.putExtra("key", value); //Optional parameters
                    myIntent.putExtra("theme_name", map.keySet().toArray()[position].toString());
                    myIntent.putExtra("theme_pid", map.get(map.keySet().toArray()[position]
                            .toString())[1]);
                    MainActivity.this.startActivity(myIntent);
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
