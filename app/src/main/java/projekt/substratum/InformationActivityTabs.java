package projekt.substratum;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.flaviofaria.kenburnsview.KenBurnsView;

import java.util.Arrays;

import projekt.substratum.adapters.PagerAdapter;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class InformationActivityTabs extends AppCompatActivity {

    public static String theme_name, theme_pid;

    public static int theme_mode;

    public static String getThemeName() {
        return theme_name;
    }

    public static String getThemePID() {
        return theme_pid;
    }

    public static int getThemeMode() {
        return theme_mode;
    }

    public static int getDominantColor(Bitmap bitmap) {
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        final int color = newBitmap.getPixel(0, 0);
        newBitmap.recycle();
        return color;
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
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information_activity_tabs);

        boolean dynamicActionBarColors = getResources().getBoolean(R.bool.dynamicActionBarColors);

        Intent currentIntent = getIntent();
        theme_name = currentIntent.getStringExtra("theme_name");
        theme_pid = currentIntent.getStringExtra("theme_pid");
        theme_mode = Integer.parseInt(currentIntent.getStringExtra("theme_mode"));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setTitle(theme_name);

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById
                (R.id.collapsing_toolbar_tabbed_layout);
        if (collapsingToolbarLayout != null) collapsingToolbarLayout.setTitle(theme_name);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        if (toolbar != null) toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Drawable heroImage = grabPackageHeroImage(theme_pid);
        Bitmap heroImageBitmap = ((BitmapDrawable) heroImage).getBitmap();

        int dominantColor = getDominantColor(heroImageBitmap);

        KenBurnsView kenBurnsView = (KenBurnsView) findViewById(R.id.kenburnsView);
        if (kenBurnsView != null) kenBurnsView.setImageDrawable(heroImage);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        if (tabLayout != null) {
            tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                    .theme_information_tab_one)));
            tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                    .theme_information_tab_two)));
            try {
                Context otherContext = getApplicationContext().createPackageContext(theme_pid, 0);
                AssetManager am = otherContext.getAssets();
                if (Arrays.asList(am.list("")).contains("bootanimation")) {
                    tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                            .theme_information_tab_three)));
                }
            } catch (Exception e) {
                Log.e("SubstratumLogger", "Could not refresh list of asset folders.");
            }
            tabLayout.setTabGravity(TabLayout.MODE_SCROLLABLE);
            if (dynamicActionBarColors) tabLayout.setBackgroundColor(dominantColor);
        }

        if (collapsingToolbarLayout != null && dynamicActionBarColors) {
            collapsingToolbarLayout.setStatusBarScrimColor(dominantColor);
            collapsingToolbarLayout.setContentScrimColor(dominantColor);
        }

        final PagerAdapter adapter = new PagerAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount());
        if (viewPager != null) {
            viewPager.setOffscreenPageLimit(5);
            viewPager.setAdapter(adapter);
            viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener
                    (tabLayout));
            tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    viewPager.setCurrentItem(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.theme_information_menu, menu);
        return true;
    }

}