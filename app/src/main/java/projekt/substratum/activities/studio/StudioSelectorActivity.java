/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.activities.studio;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Lunchbar;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.adapters.studio.PackAdapter;
import projekt.substratum.adapters.studio.PackInfo;
import projekt.substratum.common.References;
import projekt.substratum.common.platform.ThemeInterfacerService;
import projekt.substratum.common.platform.ThemeManager;

import static android.content.om.OverlayInfo.STATE_APPROVED_ENABLED;
import static projekt.substratum.common.References.DEBUG;
import static projekt.substratum.common.References.FIRST_WINDOW_REFRESH_DELAY;
import static projekt.substratum.common.References.MAIN_WINDOW_REFRESH_DELAY;
import static projekt.substratum.common.References.SECOND_WINDOW_REFRESH_DELAY;
import static projekt.substratum.util.files.MapUtils.sortMapByValues;

public class StudioSelectorActivity extends AppCompatActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.studio_selector_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.refresh:
                this.recreate();
                return true;
            case R.id.search:
                try {
                    String playURL = getString(R.string.search_play_store_url_icon_packs);
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(playURL));
                    startActivity(i);
                } catch (ActivityNotFoundException activityNotFoundException) {
                    Lunchbar.make(findViewById(android.R.id.content),
                            getString(R.string.activity_missing_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.studio_selector_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(false);
                getSupportActionBar().setTitle(getString(R.string.studio));
            }
            toolbar.setNavigationOnClickListener((view) -> onBackPressed());
        }

        RelativeLayout none_found = (RelativeLayout) findViewById(R.id.pack_placeholder);
        none_found.setVisibility(View.GONE);

        // Create a bare list to store each of the values necessary to add into the RecyclerView
        ArrayList<PackInfo> packs = new ArrayList<>();

        // Quickly buffer all the packages in the key set to know which packages are installed
        List<ResolveInfo> iconPacks = References.getIconPacks(getApplicationContext());
        HashMap unsortedMap = new HashMap();
        // Quickly buffer all the package names of the icon packs
        for (int i = 0; i < iconPacks.size(); i++) {
            try {
                ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo
                        (iconPacks.get(i).activityInfo.packageName, 0);
                String packageTitle = getPackageManager().getApplicationLabel
                        (applicationInfo).toString();
                unsortedMap.put(iconPacks.get(i).activityInfo.packageName, packageTitle);
            } catch (Exception e) {
                // Suppress warning
            }
        }

        // Sort the values list
        List<Pair<String, String>> sortedMap = sortMapByValues(unsortedMap);

        // After sorting, we should be buffering the proper sorted list to show packs asciibetically
        for (Pair<String, String> entry : sortedMap) {
            String package_identifier = entry.first;
            PackInfo packInfo = new PackInfo(getApplicationContext(),
                    package_identifier);
            packs.add(packInfo);
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        PackAdapter packAdapter = new PackAdapter(packs);
        recyclerView.setAdapter(packAdapter);

        if (sortedMap.size() <= 0) {
            recyclerView.setVisibility(View.GONE);
            none_found.setVisibility(View.VISIBLE);
        }

        CardView update_configuration = (CardView) findViewById(R.id.studio_update);
        update_configuration.setOnClickListener((view) -> {
            if (References.isPackageInstalled(getApplicationContext(),
                    References.INTERFACER_PACKAGE)) {
                if (DEBUG)
                    Log.e(References.SUBSTRATUM_ICON_BUILDER,
                            "Initializing the Theme Interface...");
                Intent runCommand = ThemeInterfacerService.getInterfacer(getApplicationContext());
                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                runCommand.setAction("projekt.themeinterface.COMMANDS");
                ArrayList<String> final_array = new ArrayList<>();
                final_array.add(0, null);
                final_array.add(1, null);
                final_array.add(2, String.valueOf(0));
                final_array.add(3, String.valueOf(FIRST_WINDOW_REFRESH_DELAY));
                final_array.add(4, String.valueOf(SECOND_WINDOW_REFRESH_DELAY));
                final_array.add(5, References.SUBSTRATUM_ICON_BUILDER);
                runCommand.putExtra("icon-handler", final_array);
                getApplicationContext().sendBroadcast(runCommand);
            } else {
                Log.e(References.SUBSTRATUM_ICON_BUILDER,
                        "Cannot apply icon pack on a non OMS7 ROM");
            }
        });

        CardView system_card = (CardView) findViewById(R.id.studio_system);
        system_card.setOnClickListener((view) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(StudioSelectorActivity.this);
            builder.setTitle(getString(R.string.studio_system));
            builder.setIcon(References.grabAppIcon(getApplicationContext(), "android"));
            builder.setMessage(R.string.studio_system_reset_dialog);
            builder.setPositiveButton(R.string.uninstall_dialog_okay, (dialog, id) -> {
                // Begin disabling themes
                List<String> state5 = ThemeManager.listOverlays(STATE_APPROVED_ENABLED);
                ArrayList<String> all = new ArrayList<>(state5);

                // ValidatorFilter out icon pack overlays from all overlays
                String final_commands = ThemeManager.disableOverlay;
                if (all.size() > 0) {
                    for (int i = 0; i < all.size(); i++) {
                        if (all.get(i).endsWith(".icon")) {
                            final_commands += " " + all.get(i);
                        }
                    }
                    if (References.isPackageInstalled(getApplicationContext(),
                            References.INTERFACER_PACKAGE)) {
                        if (DEBUG)
                            Log.e(References.SUBSTRATUM_ICON_BUILDER,
                                    "Initializing the Theme Interface...");

                        Intent runCommand = ThemeInterfacerService.getInterfacer
                                (getApplicationContext());
                        runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        runCommand.setAction("projekt.themeinterface.COMMANDS");
                        ArrayList<String> final_array = new ArrayList<>();
                        final_array.add(0, getString(R.string.studio_system)
                                .toLowerCase());
                        final_array.add(1, final_commands);
                        final_array.add(2,
                                String.valueOf(MAIN_WINDOW_REFRESH_DELAY));
                        final_array.add(3,
                                String.valueOf(FIRST_WINDOW_REFRESH_DELAY));
                        final_array.add(4,
                                String.valueOf(SECOND_WINDOW_REFRESH_DELAY));
                        final_array.add(5, null);
                        runCommand.putExtra("icon-handler", final_array);
                        getApplicationContext().sendBroadcast(runCommand);
                    } else {
                        Log.e(References.SUBSTRATUM_ICON_BUILDER,
                                "Cannot apply icon pack on a non OMS7 ROM");
                    }
                } else {
                    Lunchbar.make(findViewById(android.R.id.content),
                            getString(R.string.studio_system_reset_dialog_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            });
            builder.setNegativeButton(R.string.restore_dialog_cancel, (dialog, id) -> dialog
                    .dismiss());
            builder.create();
            builder.show();
        });
    }
}