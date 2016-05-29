package projekt.substratum;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public RecyclerView list;
    public HashMap<String, String[]> layers_packages;
    private String[] headerNamesArray, headerPreviewsArray;
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

        // TODO: Create it so it uses a recyclerView to parse substratum-based themes

        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> list = packageManager.getInstalledApplications(PackageManager
                .GET_META_DATA);
        for (ApplicationInfo packageInfo : list) {
            Log.d("Processing package", packageInfo.packageName);
            getLayersPackages(mContext, packageInfo.packageName);
        }
        Log.e("Substratum Ready Themes", Integer.toString(layers_packages.size()));

        // Test

        CardView testCard = (CardView) findViewById(R.id.theme_card);
        testCard.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        Intent myIntent = new Intent(MainActivity.this, ThemeInformation.class);
                        //myIntent.putExtra("key", value); //Optional parameters
                        myIntent.putExtra("theme_name", "Domination by Dave");
                        myIntent.putExtra("theme_pid", "com.annihilation.domination");
                        MainActivity.this.startActivity(myIntent);
                    }
                }).start();
            }
        });


    }

}
