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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.R;
import projekt.substratum.adapters.studio.IconInfo;
import projekt.substratum.adapters.studio.IconPackAdapter;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.platform.ThemeInterfacerService;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.compilers.SubstratumIconBuilder;

import static android.content.om.OverlayInfo.STATE_APPROVED_ENABLED;
import static projekt.substratum.common.References.DEBUG;
import static projekt.substratum.common.References.FIRST_WINDOW_REFRESH_DELAY;
import static projekt.substratum.common.References.MAIN_WINDOW_REFRESH_DELAY;
import static projekt.substratum.common.References.SECOND_WINDOW_REFRESH_DELAY;
import static projekt.substratum.common.References.SUBSTRATUM_ICON_BUILDER;
import static projekt.substratum.common.References.grabPackageName;
import static projekt.substratum.util.files.MapUtils.sortMapByValues;

public class StudioPreviewActivity extends AppCompatActivity {

    private String current_pack;
    private ArrayList<IconInfo> icons;
    private ProgressBar progressBar;
    private MaterialProgressBar progressCircle;
    private FloatingActionButton floatingActionButton;
    private ProgressDialog mProgressDialog;
    private String current_icon = "";
    private double current_amount = 0;
    private double total_amount = 0;
    private ArrayList<String> final_runner;
    private ArrayList<String> disable_me;
    private ArrayList<String> enable_me;
    private Boolean iconBack = false;
    private String iconBackValue = "";
    private Boolean iconUpon = false;
    private String iconUponValue = "";
    private Boolean iconMask = false;
    private String iconMaskValue = "";
    private Boolean iconScale = false;
    private float iconScaleValue = 1;

    public static Bitmap combineLayers(Bitmap bottom, Bitmap front) {
        Bitmap bmOverlay = Bitmap.createBitmap(
                bottom.getWidth(),
                bottom.getHeight(),
                bottom.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bottom, new Matrix(), null);
        canvas.drawBitmap(front,
                (canvas.getHeight() / 2) - (front.getHeight() / 2),
                (canvas.getWidth() / 2) - (front.getWidth() / 2),
                null);
        return bmOverlay;
    }

    private XmlPullParser getAppFilter(String packageName) {
        // Get the corresponding icon pack from the res/raw/appfilter.xml directory in the icon pack
        try {
            Context otherContext = getApplicationContext().createPackageContext(packageName, 0);
            Resources resources = otherContext.getResources();
            int i = resources.getIdentifier("appfilter", "xml", packageName);
            return resources.getXml(i);
        } catch (Exception e) {
            // Suppress warning
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private HashMap parseXML(XmlPullParser parser) throws XmlPullParserException, IOException {
        // Now let's quickly read all the entries inside the specified XML

        // HashMap is a nice way to store key and values for specified objects, especially since
        // an item in the app_filter.xml contains both component info and drawable info, translating
        // this into a Java readable way is favored in our situation.
        HashMap launcherIcons = new HashMap();
        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String name;
            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    launcherIcons = new HashMap();
                    break;
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    switch (name) {
                        case "item":
                            String component = parser.getAttributeValue(null, "component");
                            String drawable = parser.getAttributeValue(null, "drawable");
                            if (component != null && drawable != null) {
                                launcherIcons.put(component, drawable);
                            }
                            break;
                        case "iconback":
                            // Icon Back Drawable
                            if (parser.getAttributeValue(null, "img0") != null) {
                                // Validate that the filter is not lying
                                Boolean validated = References.validateResource(
                                        getApplicationContext(), current_pack,
                                        parser.getAttributeValue(null, "img0"), "drawable");
                                if (validated) {
                                    iconBack = true;
                                    iconBackValue = parser.getAttributeValue(null, "img0");
                                }
                            } else if (parser.getAttributeValue(null, "img1") != null) {
                                // Validate that the filter is not lying
                                Boolean validated = References.validateResource(
                                        getApplicationContext(), current_pack,
                                        parser.getAttributeValue(null, "img1"), "drawable");
                                if (validated) {
                                    iconBack = true;
                                    iconBackValue = parser.getAttributeValue(null, "img1");
                                }
                            } else if (parser.getAttributeValue(null, "img2") != null) {
                                // Validate that the filter is not lying
                                Boolean validated = References.validateResource(
                                        getApplicationContext(), current_pack,
                                        parser.getAttributeValue(null, "img2"), "drawable");
                                if (validated) {
                                    iconBack = true;
                                    iconBackValue = parser.getAttributeValue(null, "img2");
                                }
                            } else if (parser.getAttributeValue(null, "img3") != null) {
                                // Validate that the filter is not lying
                                Boolean validated = References.validateResource(
                                        getApplicationContext(), current_pack,
                                        parser.getAttributeValue(null, "img3"), "drawable");
                                if (validated) {
                                    iconBack = true;
                                    iconBackValue = parser.getAttributeValue(null, "img3");
                                }
                            }
                            break;
                        case "iconupon":
                            // Icon Upon Drawable
                            if (parser.getAttributeValue(null, "img0") != null) {
                                // Validate that the filter is not lying
                                Boolean validated = References.validateResource(
                                        getApplicationContext(), current_pack,
                                        parser.getAttributeValue(null, "img0"), "drawable");
                                if (validated) {
                                    iconUpon = true;
                                    iconUponValue = parser.getAttributeValue(null, "img0");
                                }
                            } else if (parser.getAttributeValue(null, "img1") != null) {
                                // Validate that the filter is not lying
                                Boolean validated = References.validateResource(
                                        getApplicationContext(), current_pack,
                                        parser.getAttributeValue(null, "img1"), "drawable");
                                if (validated) {
                                    iconUpon = true;
                                    iconUponValue = parser.getAttributeValue(null, "img1");
                                }
                            } else if (parser.getAttributeValue(null, "img2") != null) {
                                // Validate that the filter is not lying
                                Boolean validated = References.validateResource(
                                        getApplicationContext(), current_pack,
                                        parser.getAttributeValue(null, "img2"), "drawable");
                                if (validated) {
                                    iconUpon = true;
                                    iconUponValue = parser.getAttributeValue(null, "img2");
                                }
                            } else if (parser.getAttributeValue(null, "img3") != null) {
                                // Validate that the filter is not lying
                                Boolean validated = References.validateResource(
                                        getApplicationContext(), current_pack,
                                        parser.getAttributeValue(null, "img3"), "drawable");
                                if (validated) {
                                    iconUpon = true;
                                    iconUponValue = parser.getAttributeValue(null, "img3");
                                }
                            }
                            break;
                        case "iconmask":
                            // Icon Mask Drawable
                            if (parser.getAttributeValue(null, "img0") != null) {
                                // Validate that the filter is not lying
                                Boolean validated = References.validateResource(
                                        getApplicationContext(), current_pack,
                                        parser.getAttributeValue(null, "img0"), "drawable");
                                if (validated) {
                                    iconMask = true;
                                    iconMaskValue = parser.getAttributeValue(null, "img0");
                                }
                            } else if (parser.getAttributeValue(null, "img1") != null) {
                                // Validate that the filter is not lying
                                Boolean validated = References.validateResource(
                                        getApplicationContext(), current_pack,
                                        parser.getAttributeValue(null, "img1"), "drawable");
                                if (validated) {
                                    iconMask = true;
                                    iconMaskValue = parser.getAttributeValue(null, "img1");
                                }
                            } else if (parser.getAttributeValue(null, "img2") != null) {
                                // Validate that the filter is not lying
                                Boolean validated = References.validateResource(
                                        getApplicationContext(), current_pack,
                                        parser.getAttributeValue(null, "img2"), "drawable");
                                if (validated) {
                                    iconMask = true;
                                    iconMaskValue = parser.getAttributeValue(null, "img2");
                                }
                            } else if (parser.getAttributeValue(null, "img3") != null) {
                                // Validate that the filter is not lying
                                Boolean validated = References.validateResource(
                                        getApplicationContext(), current_pack,
                                        parser.getAttributeValue(null, "img3"), "drawable");
                                if (validated) {
                                    iconMask = true;
                                    iconMaskValue = parser.getAttributeValue(null, "img3");
                                }
                            }
                            break;
                        case "scale":
                            // Icon Scale
                            iconScale = true;
                            iconScaleValue =
                                    Float.parseFloat(parser.getAttributeValue(null, "factor"));
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    name = parser.getName();
                    if (name.equals("item")) {
                        String component = parser.getAttributeValue(null, "component");
                        String drawable = parser.getAttributeValue(null, "drawable");
                        if (component != null && drawable != null) {
                            launcherIcons.put(component, drawable);
                        }
                    }
                    break;
            }
            eventType = parser.next();
        }
        return launcherIcons;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(current_pack);
        if (launchIntent != null) {
            getMenuInflater().inflate(R.menu.studio_activity_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.studio_activity_nd_menu, menu);
        }
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
            case R.id.open:
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(current_pack);
                if (launchIntent != null) {
                    startActivity(launchIntent);
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
        setContentView(R.layout.studio_activity);

        Intent currentIntent = getIntent();
        current_pack = currentIntent.getStringExtra("icon_pack");
        progressCircle = (MaterialProgressBar) findViewById(R.id.progress_bar_loader);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(false);
                getSupportActionBar().setTitle(References.grabPackageName(
                        getApplicationContext(), current_pack));
            }
            toolbar.setNavigationOnClickListener((view) -> onBackPressed());
        }

        floatingActionButton = (FloatingActionButton) findViewById(R.id.apply_fab);
        floatingActionButton.setOnClickListener((view) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(StudioPreviewActivity.this);
            builder.setTitle(References.grabPackageName(getApplicationContext(), current_pack));
            builder.setIcon(References.grabAppIcon(getApplicationContext(), current_pack));
            String formatter = String.format(getString(R.string.studio_apply_confirmation),
                    References.grabPackageName(getApplicationContext(), current_pack));
            builder.setMessage(formatter);
            builder.setPositiveButton(R.string.dialog_ok,
                    (dialog, id) -> new IconPackInstaller().execute(""));
            builder.setNegativeButton(R.string.restore_dialog_cancel, (dialog, id) -> dialog
                    .dismiss());
            builder.create();
            builder.show();
        });

        References.getIconState(getApplicationContext(), current_pack);
        mProgressDialog = new ProgressDialog(this, R.style.SubstratumBuilder_BlurView);

        new loadIconPack().execute("");
    }

    private class IconPackInstaller extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            mProgressDialog.setContentView(R.layout.compile_icon_dialog_loader);
            if (mProgressDialog.getWindow() != null)
                mProgressDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            final float radius = 5;
            final View decorView = getWindow().getDecorView();
            final ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);
            final Drawable windowBackground = decorView.getBackground();

            BlurView blurView = (BlurView) mProgressDialog.findViewById(R.id.blurView);

            blurView.setupWith(rootView)
                    .windowBackground(windowBackground)
                    .blurAlgorithm(new RenderScriptBlur(getApplicationContext()))
                    .blurRadius(radius);

            progressBar = (ProgressBar) mProgressDialog.findViewById(R.id.loading_bar);
            progressBar.setProgressTintList(ColorStateList.valueOf(getColor(
                    R.color.compile_dialog_wave_color)));
            progressBar.setIndeterminate(false);

            final_runner = new ArrayList<>();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            TextView textView = (TextView) mProgressDialog.findViewById(R.id.current_object);
            textView.setText(current_icon);
            double progress = (current_amount / total_amount) * 100;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressBar.setProgress((int) progress, true);
            } else {
                progressBar.setProgress((int) progress);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // Disable all the icons, then enable all the new icons

            // We have to switch Locales by reflecting to the framework methods
            // The reasoning behind this is because the Locale configuration change has a higher
            // bit-code than most others, such as Fonts, so it would affect all applications at the
            // same time, to simulate something like a assetSeq on OMS3

            // This would cause a window refresh on either Framework or Substratum
            if (final_runner.size() > 0 || disable_me.size() > 0 || enable_me.size() > 0) {
                String final_commands = "";
                if (final_runner.size() > 0) {
                    for (int i = 0; i < final_runner.size(); i++) {
                        if (final_commands.length() <= 0) {
                            final_commands = final_runner.get(i);
                        } else {
                            final_commands = final_commands + " && " + final_runner.get(i);
                        }
                    }
                }
                if (disable_me.size() > 0) {
                    if (final_commands.length() > 0) final_commands = final_commands + " && " +
                            ThemeManager.disableOverlay;
                    for (int j = 0; j < disable_me.size(); j++) {
                        if (final_commands.length() <= 0) {
                            final_commands = ThemeManager.disableOverlay + " " +
                                    disable_me.get(j);
                        } else {
                            final_commands = final_commands + " " + disable_me.get(j);
                        }
                    }
                }
                if (enable_me.size() > 0) {
                    if (final_commands.length() > 0) final_commands = final_commands + " && " +
                            ThemeManager.enableOverlay;
                    for (int h = 0; h < enable_me.size(); h++) {
                        if (final_commands.length() <= 0) {
                            final_commands = ThemeManager.enableOverlay + " " +
                                    enable_me.get(h);
                        } else {
                            final_commands = final_commands + " " + enable_me.get(h);
                        }
                    }
                }

                final String final_runner = final_commands;
                new android.app.AlertDialog.Builder(StudioPreviewActivity.this)
                        .setTitle(R.string.studio_refresh_dialog_title)
                        .setMessage(R.string.studio_refresh_dialog_content)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_ok, (dialog2, i2) -> {
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                                    getApplicationContext());
                            prefs.edit().putString("installed_icon_pack", current_pack).apply();

                            if (References.isPackageInstalled(getApplicationContext(),
                                    References.INTERFACER_PACKAGE)) {
                                ElevatedCommands.restartInterfacer();
                                if (DEBUG)
                                    Log.e(References.SUBSTRATUM_ICON_BUILDER,
                                            "Initializing the Theme Interface...");
                                Intent runCommand = ThemeInterfacerService.getInterfacer
                                        (getApplicationContext());
                                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                runCommand.setAction("projekt.themeinterface.COMMANDS");
                                ArrayList<String> final_array = new ArrayList<>();
                                final_array.add(0, References.grabPackageName(
                                        getApplicationContext(), current_pack));
                                final_array.add(1, final_runner);
                                final_array.add(2, (final_runner.contains("projekt.substratum") ?
                                        String.valueOf(MAIN_WINDOW_REFRESH_DELAY) :
                                        String.valueOf(0)));
                                final_array.add(3, String.valueOf(FIRST_WINDOW_REFRESH_DELAY));
                                final_array.add(4, String.valueOf(SECOND_WINDOW_REFRESH_DELAY));
                                final_array.add(5, null);
                                runCommand.putExtra("icon-handler", final_array);
                                getApplicationContext().sendBroadcast(runCommand);
                            } else {
                                Log.e(References.SUBSTRATUM_ICON_BUILDER,
                                        "Cannot apply icon pack on a non OMS7 ROM");
                            }
                        })
                        .show();
            }

            // Disable the window
            mProgressDialog.dismiss();
        }

        @Override
        protected String doInBackground(String... Params) {
            total_amount = icons.size();
            disable_me = new ArrayList<>();

            List<String> state5 = ThemeManager.listOverlays(STATE_APPROVED_ENABLED);
            ArrayList<String> activated_overlays = new ArrayList<>(state5);
            ArrayList<String> icons_list = new ArrayList<>();
            // Buffer the list of icons that are going to be loaded
            for (int obj = 0; obj < icons.size(); obj++) {
                icons_list.add(icons.get(obj).getPackageName() + ".icon");
            }

            enable_me = new ArrayList<>();
            for (int j = 0; j < activated_overlays.size(); j++) {
                if (activated_overlays.get(j).contains(".icon")) {
                    if (!icons_list.contains(activated_overlays.get(j))) {
                        // These guys are from the old icon pack, we'll have to disable them!
                        Log.e(References.SUBSTRATUM_ICON_BUILDER, "Sent the icon for disabling : " +
                                activated_overlays.get(j));
                        disable_me.add(
                                ThemeManager.disableOverlay + " " + activated_overlays.get(j));
                    }
                }
            }

            for (int i = 0; i < icons.size(); i++) {
                current_amount = i + 1;

                Bitmap iconification = null;

                if ((iconBack || iconUpon || iconMask) &&
                        icons.get(i).getPackageDrawable().equals("null")) {
                    try {
                        // We must disable the icon from any further resource withdrawals
                        ElevatedCommands.runCommands(ThemeManager.disableOverlay + " " +
                                icons.get(i).getPackageName() + ".icon");
                        Thread.sleep(1500);

                        Context context = createPackageContext(current_pack, 0);
                        Resources resources = context.getResources();

                        if (iconBack) {
                            // Validation
                            Boolean validated = References.validateResource(
                                    getApplicationContext(), current_pack,
                                    iconBackValue, "drawable");
                            if (validated) {
                                int drawableBack = resources.getIdentifier(
                                        iconBackValue, // Drawable name explicitly defined
                                        "drawable", // Declared icon is a drawable, indeed.
                                        current_pack);

                                Bitmap bBack = BitmapFactory.decodeResource(resources,
                                        drawableBack);

                                Drawable appIcon = References.grabAppIcon(getApplicationContext(),
                                        icons.get(i).getPackageName());
                                Bitmap applicationIcon = ((BitmapDrawable) appIcon).getBitmap();
                                if (iconScale) {
                                    // Take account for the icon pack designer's scale value
                                    float height = applicationIcon.getHeight() * iconScaleValue;
                                    float width = applicationIcon.getWidth() * iconScaleValue;
                                    applicationIcon =
                                            Bitmap.createScaledBitmap(
                                                    applicationIcon,
                                                    (int) height,
                                                    (int) width,
                                                    false);
                                }
                                iconification = combineLayers(bBack, applicationIcon);
                            } else {
                                iconBack = false;
                            }
                        }
                        if (iconMask) {
                            // Validation
                            Boolean validated = References.validateResource(
                                    getApplicationContext(), current_pack,
                                    iconMaskValue, "drawable");
                            if (validated) {
                                int drawableMask = resources.getIdentifier(
                                        iconMaskValue, // Drawable name explicitly defined
                                        "drawable", // Declared icon is a drawable, indeed.
                                        current_pack);
                                Bitmap mask = BitmapFactory.decodeResource(
                                        resources,
                                        drawableMask);

                                // We have to check whether the iconMask object is a cropper, or a
                                // simple overlaying drawable
                                Boolean is_cropper = false;
                                int[] pixelCheck = new int[mask.getHeight() *
                                        mask.getWidth()];
                                mask.getPixels(pixelCheck, 0, mask.getWidth(), 0, 0,
                                        mask.getWidth(), mask.getHeight());
                                int colorPixelTopLeft = mask.getPixel(0, 0);
                                int colorPixelTopRight = mask.getPixel(mask.getWidth() - 1, 0);
                                int colorPixelBottomLeft = mask.getPixel(0, mask.getHeight() - 1);
                                int colorPixelBottomRight = mask.getPixel(mask.getWidth() - 1,
                                        mask.getHeight() - 1);
                                if (colorPixelTopLeft == colorPixelTopRight &&
                                        colorPixelBottomLeft == colorPixelBottomRight &&
                                        colorPixelTopLeft == colorPixelBottomRight) {
                                    Log.d(SUBSTRATUM_ICON_BUILDER, "Mask pixels all match, " +
                                            "checking if mask is cropper or overlay...");
                                    if (colorPixelTopLeft == Color.BLACK) {
                                        Log.d(SUBSTRATUM_ICON_BUILDER,
                                                "Mask pixels denote the template is using a " +
                                                        "cropping guideline, " +
                                                        "ensuring proper parameters...");
                                        is_cropper = true;
                                    } else {
                                        Log.d(SUBSTRATUM_ICON_BUILDER,
                                                "Mask pixels denote the template is using a " +
                                                        "overlaying guideline, " +
                                                        "ensuring proper parameters...");
                                    }
                                }

                                if (is_cropper) {
                                    Log.d(SUBSTRATUM_ICON_BUILDER,
                                            "This icon is now being processed with the " +
                                                    "cropping algorithm...");
                                    Bitmap original;
                                    if (iconification != null) {
                                        original = iconification;
                                    } else {
                                        Drawable icon = References.grabAppIcon(
                                                getApplicationContext(),
                                                icons.get(i).getPackageName());
                                        original = ((BitmapDrawable) icon).getBitmap();
                                        if (iconScale) {
                                            // Take account for the icon pack designer's scale value
                                            float height = original.getHeight() * iconScaleValue;
                                            float width = original.getWidth() * iconScaleValue;
                                            original = Bitmap.createScaledBitmap(
                                                    original,
                                                    (int) height,
                                                    (int) width,
                                                    false);
                                        }
                                    }
                                    Bitmap bMask = BitmapFactory.decodeResource(
                                            resources,
                                            drawableMask);

                                    /*
                                      Begin process of the mask icon
                                     */
                                    // Create an image with the same size as the base
                                    Bitmap imageWithBG = Bitmap.createBitmap(
                                            bMask.getWidth(),
                                            bMask.getHeight(),
                                            bMask.getConfig());

                                    // Set the background to white (erase doesn't mean erase)
                                    imageWithBG.eraseColor(Color.WHITE);

                                    // Create a canvas to draw on the new image
                                    Canvas canvas = new Canvas(imageWithBG);

                                    // Draw the mask on the background
                                    canvas.drawBitmap(bMask, 0f, 0f, null);

                                    // Erase all of the black so we have the core bleed area
                                    // ready for bitmap mutation
                                    int[] pixels = new int[
                                            imageWithBG.getHeight() *
                                                    imageWithBG.getWidth()];
                                    imageWithBG.getPixels(pixels, 0,
                                            imageWithBG.getWidth(), 0, 0,
                                            imageWithBG.getWidth(), imageWithBG.getHeight());
                                    for (int count = 0; count < pixels.length; count++) {
                                        if (pixels[count] == Color.BLACK) {
                                            pixels[count] = Color.TRANSPARENT;
                                        }
                                    }
                                    imageWithBG.setPixels(pixels, 0, imageWithBG.getWidth(),
                                            0, 0, imageWithBG.getWidth(),
                                            imageWithBG.getHeight());

                                    /*
                                      Then finally perform the masking on the processed mask
                                      overlay
                                     */
                                    Bitmap result = Bitmap.createBitmap(
                                            bMask.getWidth(),
                                            bMask.getHeight(),
                                            Bitmap.Config.ARGB_8888);
                                    Canvas tempCanvas = new Canvas(result);
                                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                    paint.setXfermode(
                                            new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                                    tempCanvas.drawBitmap(original, 0, 0, null);
                                    tempCanvas.drawBitmap(imageWithBG, 0, 0, paint);
                                    paint.setXfermode(null);

                                    // Draw result after performing masking
                                    canvas.drawBitmap(result, 0, 0, new Paint());

                                    // Conclude bitmap mutation
                                    iconification = result;

                                    // Check if there's an iconBack that needs to be replaced
                                    if (iconBack) {
                                        int drawableBack = resources.getIdentifier(
                                                iconBackValue, // Drawable name explicitly defined
                                                "drawable", // Declared icon is a drawable, indeed.
                                                current_pack);

                                        Bitmap bBack = BitmapFactory.decodeResource(resources,
                                                drawableBack);
                                        iconification = combineLayers(bBack, iconification);
                                    }
                                } else {
                                    // At this point, this object is a simple semi-transparent
                                    // overlay image
                                    Bitmap bMask = BitmapFactory.decodeResource(resources,
                                            drawableMask);

                                    Drawable icon = References.grabAppIcon(getApplicationContext(),
                                            icons.get(i).getPackageName());
                                    Bitmap app = ((BitmapDrawable) icon).getBitmap();
                                    if (iconScale) {
                                        // Take account for the icon pack designer's scale value
                                        float height = app.getHeight() * iconScaleValue;
                                        float width = app.getWidth() * iconScaleValue;
                                        app = Bitmap.createScaledBitmap(
                                                app,
                                                (int) height,
                                                (int) width,
                                                false);
                                    }
                                    if (iconification == null) {
                                        iconification = combineLayers(bMask, app);
                                    } else {
                                        iconification = combineLayers(iconification, bMask);
                                    }
                                }
                            } else {
                                iconMask = false;
                            }
                        }
                        if (iconUpon) {
                            // Validation
                            Boolean validated = References.validateResource(
                                    getApplicationContext(), current_pack,
                                    iconUponValue, "drawable");
                            if (validated) {
                                int drawableBack = resources.getIdentifier(
                                        iconUponValue, // Drawable name explicitly defined
                                        "drawable", // Declared icon is a drawable, indeed.
                                        current_pack);

                                Bitmap bFront = BitmapFactory.decodeResource(resources,
                                        drawableBack);

                                Drawable appIcon = References.grabAppIcon(getApplicationContext(),
                                        icons.get(i).getPackageName());
                                Bitmap applicationIcon = ((BitmapDrawable) appIcon).getBitmap();
                                if (iconScale) {
                                    // Take account for the icon pack designer's scale value
                                    float height = applicationIcon.getHeight() * iconScaleValue;
                                    float width = applicationIcon.getWidth() * iconScaleValue;
                                    applicationIcon =
                                            Bitmap.createScaledBitmap(
                                                    applicationIcon,
                                                    (int) height,
                                                    (int) width,
                                                    false);
                                }
                                if (iconification == null) {
                                    iconification = combineLayers(bFront, applicationIcon);
                                } else {
                                    iconification = combineLayers(iconification, bFront);
                                }
                            } else {
                                iconUpon = false;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Suppress warnings
                    }
                }

                // Dynamically check whether the icon is disabled
                enable_me.add(icons.get(i).getPackageName() + ".icon");

                String iconName = icons.get(i).getParsedName();
                String iconNameParsed = iconName + " " +
                        getString(R.string.icon_pack_entry);
                String iconPackage = icons.get(i).getPackageName();
                String iconDrawable = icons.get(i).getPackageDrawable();
                String iconDrawableName = References.getPackageIconName(
                        getApplicationContext(), iconPackage);

                current_icon = "\"" + iconName + "\"";
                publishProgress((int) current_amount);

                Log.d(References.SUBSTRATUM_ICON_BUILDER, "Currently building : " +
                        iconPackage);
                HashMap hashMap = References.getIconState(
                        getApplicationContext(), iconPackage);

                Boolean update_bool = true;

                // The only two window refreshing icons would be Android System and Substratum as
                // these are the resources currently loaded on top of the current view
                if (icons.get(i).getPackageName().equals("android") ||
                        icons.get(i).getPackageName().equals("projekt.substratum")) {
                    Log.d(References.SUBSTRATUM_LOG, "The flag to update this " +
                            "overlay has been triggered.");
                    update_bool = false;
                }

                Iterator it = null;
                if (hashMap != null) it = hashMap.entrySet().iterator();
                if (it != null && it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next(); // The Activity Key

                    // ValidatorFilter out the icons that already have the current applied pack
                    // applied
                    if (References.isPackageInstalled(
                            getApplicationContext(), iconPackage + ".icon") &&
                            References.grabIconPack(
                                    getApplicationContext(), iconPackage + ".icon", current_pack)) {
                        Log.d(References.SUBSTRATUM_ICON_BUILDER, "'" + iconPackage +
                                "' already contains the " +
                                grabPackageName(getApplicationContext(), current_pack) +
                                " attribute, skip recompile...");
                    } else {
                        try {
                            Log.d(References.SUBSTRATUM_ICON_BUILDER,
                                    "Fusing drawable from icon pack to system : " + iconDrawable);
                            Context context = createPackageContext(current_pack, 0);
                            Resources resources = context.getResources();
                            int drawable = resources.getIdentifier(
                                    iconDrawable, // Drawable name explicitly defined
                                    "drawable", // Declared icon is a drawable, indeed.
                                    current_pack); // Icon pack package name

                            SubstratumIconBuilder sib = new SubstratumIconBuilder();
                            sib.beginAction(
                                    getApplicationContext(),
                                    current_pack,
                                    iconPackage,
                                    References.grabThemeVersion(getApplicationContext(),
                                            current_pack),
                                    true,
                                    hashMap,
                                    pair.getKey().toString(),
                                    drawable,
                                    iconDrawableName,
                                    iconNameParsed,
                                    update_bool,
                                    (iconification != null) ? iconification : null);
                            if (sib.has_errored_out) {
                                Log.e(References.SUBSTRATUM_ICON_BUILDER,
                                        "Could not instantiate icon (" + iconPackage +
                                                ") for SubstratumIconBuilder!");
                            }

                            if (sib.no_install.length() > 0) {
                                final_runner.add(sib.no_install);
                            }
                        } catch (Exception e) {
                            Log.e(References.SUBSTRATUM_ICON_BUILDER,
                                    "Could not instantiate icon (" + iconPackage +
                                            ") for SubstratumIconBuilder!");
                        }
                    }
                }
            }
            return null;
        }
    }

    private class loadIconPack extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            progressCircle.setVisibility(View.VISIBLE);
            floatingActionButton.hide();
        }

        @Override
        protected void onPostExecute(String result) {
            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.icon_pack_recycler);
            GridLayoutManager linearLayout = new GridLayoutManager(getApplicationContext(), 4);

            recyclerView.setHasFixedSize(true);
            recyclerView.setLayoutManager(linearLayout);

            IconPackAdapter iconPackAdapter = new IconPackAdapter(getApplicationContext(), icons);
            recyclerView.setAdapter(iconPackAdapter);

            floatingActionButton.show();
            progressCircle.setVisibility(View.GONE);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected String doInBackground(String... sUrl) {
            // Buffer the hash map of all the values in the XML
            HashMap hashMap = null;
            try {
                hashMap = parseXML(getAppFilter(current_pack));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Create a bare list to store each of the values necessary to add into the
            // RecyclerView
            icons = new ArrayList<>();
            // This list will make sure that the multiple ComponentInfo objects will be filtered
            ArrayList<String> packages = new ArrayList<>();

            HashMap unsortedMap = new HashMap();

            // Load up all the launcher activities to set proper icons
            final PackageManager pm = getPackageManager();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> appList = pm.queryIntentActivities(mainIntent, 0);
            HashMap hmap = new HashMap();

            Collections.sort(appList, new ResolveInfo.DisplayNameComparator(pm));
            for (ResolveInfo temp : appList) {
                hmap.put(temp.activityInfo.packageName, temp.activityInfo.name);
            }

            // First filter out the icon pack dashboard applications
            List<ResolveInfo> iconPacks =
                    References.getIconPacks(getApplicationContext());
            ArrayList<String> iconPacksExposed = new ArrayList<>();
            for (int ip = 0; ip < iconPacks.size(); ip++) {
                iconPacksExposed.add(iconPacks.get(ip).activityInfo.packageName);
            }

            // Quickly buffer all the packages in the key set to know which packages are installed
            if (hashMap != null) {
                for (Object object : hashMap.keySet()) {
                    String f = (String) object;
                    if (f.length() > 13) {
                        // Check if the drawable is valid and themed by the icon pack designer
                        String drawable = hashMap.get(f).toString();
                        Boolean validated = References.validateResource(
                                getApplicationContext(), current_pack, drawable, "drawable");
                        if (!validated) {
                            drawable = null;
                        }

                        String parse = f.substring(13).replaceAll("[{}]", ""); // Remove brackets

                        String[] component = parse.split("/"); // Remove the dash
                        if (component.length == 2) {
                            /*
                              Icon Pack Studio -- Best Match System

                              Only if the ComponentInfo is parsed properly, for example:
                              com.android.quicksearchbox/com.android.quicksearchbox.SearchActivity

                              Check if the HashMap from the active intents list match up with the
                              one in the app filter, and only accept it ONCE!

                              This also filters out icon packs from appearing on the list forcibly
                             */
                            if (hmap.get(component[0]) != null &&
                                    hmap.get(component[0]).equals(component[1]) &&
                                    !iconPacksExposed.contains(component[0])) {
                                if (References.isPackageInstalled(getApplicationContext(),
                                        component[0])) {
                                    if (!References.checkIconPackNotAllowed(component[0])) {
                                        if (drawable != null && !drawable.equals("null"))
                                            Log.d(References.SUBSTRATUM_ICON_BUILDER,
                                                    "Loaded drawable from icon pack : " + drawable);
                                        unsortedMap.put(component[0] + "|" + drawable,
                                                References.grabPackageName(getApplicationContext(),
                                                        component[0]));
                                    }
                                }
                            }
                        }
                    }
                }

                // Attach all the unthemed icons to the list as well
                if (iconBack || iconUpon || iconMask) {
                    for (int i = 0; i < appList.size(); i++) {
                        if (!iconPacksExposed.contains(appList.get(i).activityInfo.packageName)) {
                            if (!unsortedMap.values().contains(
                                    References.grabPackageName(
                                            getApplicationContext(),
                                            appList.get(i).activityInfo.packageName))) {
                                if (!References.checkIconPackNotAllowed(
                                        appList.get(i).activityInfo.packageName)) {
                                    String packageName = appList.get(i).activityInfo.packageName;
                                    Log.d(References.SUBSTRATUM_ICON_BUILDER,
                                            "Attaching unthemed icon : " +
                                                    packageName);
                                    unsortedMap.put(packageName + "|null",
                                            References.grabPackageName(
                                                    getApplicationContext(), packageName));
                                }
                            }
                        }
                    }
                }
            }

            // Sort the values list
            List<Pair<String, String>> sortedMap = sortMapByValues(unsortedMap);

            // After sorting, we should be buffering the proper sorted list to show packs
            // asciibetically
            for (Pair<String, String> entry : sortedMap) {
                String package_attributes = entry.first;
                String[] attrs = package_attributes.split("\\|");
                if (!packages.contains(attrs[0])) {
                    IconInfo iconInfo = new IconInfo(
                            getApplicationContext(),
                            attrs[0],
                            attrs[1],
                            current_pack,
                            References.grabPackageName(getApplicationContext(), attrs[0]));
                    icons.add(iconInfo);
                    packages.add(attrs[0]);
                }
            }
            return null;
        }
    }
}