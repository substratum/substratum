package projekt.substratum.tabs;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import kellinwood.security.zipsigner.ZipSigner;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.InformationActivityTabs;
import projekt.substratum.R;
import projekt.substratum.util.ReadOverlaysFile;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class FontInstaller extends Fragment {

    private String theme_pid;
    private ViewGroup root;
    private List<Bitmap> images = new ArrayList<>();
    private MaterialProgressBar progressBar;
    private ImageButton imageButton;
    private Spinner bootAnimationSelector;
    private ColorStateList unchecked, checked;
    private ProgressDialog progress;
    private RelativeLayout font_holder;
    private String final_commands;

    private boolean isPackageInstalled(Context context, String package_name) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        theme_pid = InformationActivityTabs.getThemePID();

        root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_4, container, false);

        progressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);

        font_holder = (RelativeLayout) root.findViewById(R.id.font_holder);

        imageButton = (ImageButton) root.findViewById(R.id.checkBox);
        imageButton.setClickable(false);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FontHandler().execute(bootAnimationSelector.getSelectedItem()
                        .toString());
            }
        });

        unchecked = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        getContext().getColor(R.color.bootanimation_unchecked),
                        getContext().getColor(R.color.bootanimation_unchecked)
                }
        );
        checked = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        getContext().getColor(R.color.bootanimation_checked),
                        getContext().getColor(R.color.bootanimation_checked)
                }
        );


        try {
            Context otherContext = getContext().createPackageContext(theme_pid, 0);
            AssetManager am = otherContext.getAssets();
            String[] unparsedBootAnimations = am.list("fonts");
            ArrayList<String> parsedBootAnimations = new ArrayList<>();
            for (int i = 0; i < unparsedBootAnimations.length; i++) {
                parsedBootAnimations.add(unparsedBootAnimations[i].substring(0,
                        unparsedBootAnimations[i].length() - 4));
            }
            ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, parsedBootAnimations);
            bootAnimationSelector = (Spinner) root.findViewById(R.id.bootAnimationSelection);
            bootAnimationSelector.setAdapter(adapter1);
            bootAnimationSelector.setOnItemSelectedListener(new AdapterView
                    .OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {
                    String[] commands = {arg0.getSelectedItem().toString()};
                    new FontPreview().execute(commands);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    String[] commands = {arg0.getSelectedItem().toString()};
                    new FontPreview().execute(commands);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("SubstratumLogger", "There is no font.zip found within the assets " +
                    "of this theme!");
        }

        return root;
    }

    private class FontHandler extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getContext(), android.R.style
                    .Theme_DeviceDefault_Dialog_Alert);
            progress.setMessage(getString(R.string.font_dialog_apply_text));
            progress.setIndeterminate(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();
            if (result == null) {
                Toast toast = Toast.makeText(getContext(),
                        getString(R.string.font_dialog_apply_success), Toast.LENGTH_LONG);
                toast.show();
            } else {
                Toast toast = Toast.makeText(getContext(),
                        getString(R.string.font_dialog_apply_failed), Toast.LENGTH_LONG);
                toast.show();
            }

            if (final_commands.length() > 0 && result == null) {

                // Path 1: Reflect and use recreateDefaults() and freeTextLayoutCaches()

                try {
                    Class typeface = Class.forName("android.graphics.Typeface");
                    Method recreateDefaults = typeface.getMethod("recreateDefaults");
                    recreateDefaults.invoke(null, null);

                    Class canvas = Class.forName("android.graphics.Canvas");
                    Method freeTextLayoutCaches = canvas.getMethod("freeTextLayoutCaches");
                    freeTextLayoutCaches.invoke(null, null);

                    Log.e("Reflection", "Mom, I have reflected on my decisions and have decided " +
                            "to be a " +
                            "good kid from now on.");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("Reflection", "I'm sorry, I could not reflect on this object at the " +
                            "given " +
                            "time...");
                }

                // Path 2: Reflect back to Settings and updateConfiguration() run on locale change

                try {
                    Class<?> activityManagerNative = Class.forName("android.app" +
                            ".ActivityManagerNative");
                    Object am = activityManagerNative.getMethod("getDefault").invoke
                            (activityManagerNative);
                    Object config = am.getClass().getMethod("getConfiguration").invoke(am);
                    config.getClass().getDeclaredField("locale").set(config, Locale.US);
                    config.getClass().getDeclaredField("userSetLocale").setBoolean(config, true);

                    am.getClass().getMethod("updateConfiguration", android.content.res
                            .Configuration.class).invoke(am, config);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Path 3: Finally, enable/disable the SystemUI dummy overlay

                eu.chainfire.libsuperuser.Shell.SU.run(final_commands);
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {

            try {

                // Move the file from assets folder to a new working area

                Log.d("FontHandler", "Copying over the selected fonts to working " +
                        "directory...");

                File cacheDirectory = new File(getContext().getCacheDir(), "/FontCache/");
                if (!cacheDirectory.exists()) {
                    cacheDirectory.mkdirs();
                }
                File cacheDirectory2 = new File(getContext().getCacheDir(), "/FontCache/" +
                        "FontCreator/");
                if (!cacheDirectory2.exists()) {
                    cacheDirectory2.mkdirs();
                } else {
                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                    "/FontCache/FontCreator/");
                    cacheDirectory2.mkdirs();
                }

                // Copy the font.zip from assets/fonts of the theme's assets

                String source = sUrl[0] + ".zip";

                try {
                    Context otherContext = getContext().createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    InputStream inputStream = am.open("fonts/" + source);
                    OutputStream outputStream = new FileOutputStream(getContext().getCacheDir()
                            .getAbsolutePath() + "/FontCache/" + source);
                    CopyStream(inputStream, outputStream);
                } catch (Exception e) {
                    Log.e("SubstratumLogger", "There is no fonts.zip found within the assets " +
                            "of this theme!");
                }

                // Unzip the fonts to get it prepared for the preview
                unzip(getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/" + source,
                        getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/FontCreator/");

                // Copy all the system fonts to /data/system/theme/fonts

                File dataSystemThemeDir = new File("/data/system/theme");
                if (!dataSystemThemeDir.exists()) {
                    eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /data");
                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "mkdir /data/system/theme/");
                } else {
                    eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /data");
                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "rm -r /data/system/theme/");
                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "mkdir /data/system/theme/");
                }
                File dataSystemThemeFontsDir = new File("/data/system/theme/fonts");
                if (!dataSystemThemeFontsDir.exists()) {
                    eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /data");
                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "mkdir /data/system/theme/fonts");
                }

                // Copy font configuration file (fonts.xml) to the working directory
                copyAssets();

                eu.chainfire.libsuperuser.Shell.SU.run(
                        "cp /system/fonts/* /data/system/theme/fonts/");

                // Copy all the files from work directory to /data/system/theme/fonts

                eu.chainfire.libsuperuser.Shell.SU.run(
                        "cp -F " + getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/FontCreator/*" + " /data/system/theme/fonts/");

                // Check if substratum_helper overlay installed, if not, create it

                if (!isPackageInstalled(getContext(), "substratum.helper")) {
                    Log.e("FontHandler", "Substratum Helper Overlay not installed, compiling and " +
                            "installing now...");

                    File helperDirectory = new File(getContext().getCacheDir(),
                            "/FontCache/helper/");

                    if (!helperDirectory.exists()) {
                        helperDirectory.mkdirs();
                    }
                    File helperDirectoryRes = new File(getContext().getCacheDir(),
                            "/FontCache/helper/res/");
                    if (!helperDirectoryRes.exists()) {
                        helperDirectoryRes.mkdirs();
                    }
                    File helperDirectoryResDrawable =
                            new File(getContext().getCacheDir(),
                                    "/FontCache/helper/res/drawable-xxhdpi/");
                    if (!helperDirectoryResDrawable.exists()) {
                        helperDirectoryResDrawable.mkdirs();
                    }
                    File helperDirectoryResValues =
                            new File(getContext().getCacheDir(), "/FontCache/helper/res/values/");
                    if (!helperDirectoryResValues.exists()) {
                        helperDirectoryResValues.mkdirs();
                    }

                    // Create new icon in /res/drawable-xxhdpi from Substratum's mipmap-xxhdpi
                    Resources res;
                    Drawable hero = null;
                    try {
                        res = getContext().getPackageManager().getResourcesForApplication(
                                getContext().getApplicationContext().getPackageName());
                        int resourceId = res.getIdentifier(getContext().getApplicationContext()
                                .getPackageName() + ":mipmap/main_launcher", null, null);
                        if (0 != resourceId) {
                            hero = getContext().getPackageManager().getDrawable(getContext()
                                    .getApplicationContext().getPackageName(), resourceId, null);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }

                    Bitmap bmp = ((BitmapDrawable) hero).getBitmap();

                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(new File(getContext().getCacheDir(),
                                "/FontCache/helper/res/drawable-xxhdpi/app_icon.png"));
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    // Create new EXISTING boolean file based on MM6.0+ to surpass non-dangerous
                    // overlay

                    File root = new File(getContext().getCacheDir(),
                            "/FontCache/helper/res/values/bools.xml");
                    try {
                        root.createNewFile();
                        FileWriter fw = new FileWriter(root);
                        BufferedWriter bw = new BufferedWriter(fw);
                        PrintWriter pw = new PrintWriter(bw);
                        String string =
                                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                        "<resources>\n" +
                                        "        <bool name=\"enable_volume_ui\">true</bool>\n" +
                                        "</resources>";
                        pw.write(string);
                        pw.close();
                        bw.close();
                        fw.close();
                    } catch (Exception e) {
                        Log.e("FontHandler", "There was an exception creating a new bools file!");
                    }

                    // Create dummy AndroidManifest

                    File root2 = new File(getContext().getCacheDir(),
                            "/FontCache/helper/AndroidManifest.xml");
                    try {
                        root2.createNewFile();
                        FileWriter fw2 = new FileWriter(root2);
                        BufferedWriter bw2 = new BufferedWriter(fw2);
                        PrintWriter pw2 = new PrintWriter(bw2);

                        PackageInfo pInfo = getContext().getPackageManager()
                                .getPackageInfo(getContext().getPackageName(), 0);
                        String version = pInfo.versionName;

                        String manifest =
                                "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +
                                        "<manifest xmlns:android=\"http://schemas.android" +
                                        ".com/apk/res/android\" package=\"" +
                                        "substratum.helper" + "\"\n" +
                                        "        android:versionName=\"" + version + "\"> \n" +
                                        "    <overlay android:targetPackage=\"" +
                                        "com.android.systemui" +
                                        "\"/>\n" +
                                        "    <application android:label=\"" +
                                        getString(R.string.helper_name) + "\"\n" +
                                        "         android:icon=\"@drawable/app_icon\"/>\n" +
                                        "</manifest>\n";
                        pw2.write(manifest);
                        pw2.close();
                        bw2.close();
                        fw2.close();
                    } catch (Exception e) {
                        Log.e("FontHandler", "There was an exception creating a new Manifest " +
                                "file!");
                    }

                    // Begin aapt and compile the temp file

                    try {
                        String commands = "aapt p -M " + getContext().getCacheDir()
                                .getAbsolutePath() +

                                "/FontCache/helper/AndroidManifest.xml " +
                                "-S " + getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/helper/res/ " +
                                "-I " + "/system/framework/framework-res.apk " +
                                "-F " + getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/helper/unsigned_helper.apk " +
                                "-f\n";

                        String line;
                        Process nativeApp = Runtime.getRuntime().exec(commands);

                        OutputStream stdin = nativeApp.getOutputStream();
                        InputStream stderr = nativeApp.getErrorStream();
                        InputStream stdout = nativeApp.getInputStream();
                        stdin.write(("ls\n").getBytes());
                        stdin.write("exit\n".getBytes());
                        stdin.flush();
                        stdin.close();

                        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                        while ((line = br.readLine()) != null) {
                            Log.d("OverlayOptimizer", line);
                        }
                        br.close();
                        br = new BufferedReader(new InputStreamReader(stderr));
                        while ((line = br.readLine()) != null) {
                            Log.e("SubstratumBuilder", line);
                        }
                        br.close();
                        nativeApp.waitFor();
                    } catch (Exception e) {
                        Log.e("FontHandler", "Unfortunately, there was an exception trying to " +
                                "create the Substratum Overlay Helper " +
                                "APK");
                    }

                    // Sign the helper overlay

                    try {
                        // Sign with the built-in test key/certificate.
                        String sign_source = getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/helper/unsigned_helper.apk";
                        String sign_destination = Environment.getExternalStorageDirectory()
                                .getAbsolutePath() +
                                "/.substratum/" + "substratum.helper" + "-signed.apk";

                        ZipSigner zipSigner = new ZipSigner();
                        zipSigner.setKeymode("testkey");
                        zipSigner.signZip(sign_source, sign_destination);

                        Log.d("FontHandler", "Substratum Helper Overlay APK successfully signed!");
                    } catch (Throwable t) {
                        t.printStackTrace();
                        Log.e("FontHandler", "Substratum Helper Overlay APK could not be signed. " +
                                t.toString());
                    }

                    // Install the Helper

                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "pm install -r " + Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/.substratum/substratum.helper-signed" +
                                    ".apk");

                    // Bruteforce the cache folder out of here due to possible MAJOR access
                    // permissions denial

                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                    "/FontCache/");

                    final_commands = "om enable substratum.helper";
                } else {
                    eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays" +
                            ".xml " +
                            Environment
                                    .getExternalStorageDirectory().getAbsolutePath() +
                            "/.substratum/current_overlays.xml");

                    String[] commands0 = {Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/.substratum/current_overlays.xml", "4"};
                    String[] commands1 = {Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/.substratum/current_overlays.xml", "5"};

                    List<String> state4overlays = ReadOverlaysFile.main(commands0);
                    List<String> state5overlays = ReadOverlaysFile.main(commands1);

                    if (state4overlays.contains("substratum.helper")) {
                        final_commands = "om enable substratum.helper";
                    } else {
                        if (state5overlays.contains("substratum.helper")) {
                            final_commands = "om disable substratum.helper";
                        }
                    }
                }

                // Whether the user has the helper installed, we have to check for correct
                // permissions
                // and system file context integrity.

                eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /system");
                eu.chainfire.libsuperuser.Shell.SU.run("chmod 755 /data/system/theme/");

                eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /system");
                eu.chainfire.libsuperuser.Shell.SU.run("chmod -R 747 " +
                        "/data/system/theme/fonts/");

                eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /system");
                eu.chainfire.libsuperuser.Shell.SU.run("chmod 775 " +
                        "/data/system/theme/fonts/");

                eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /data");
                eu.chainfire.libsuperuser.Shell.SU.run("chcon -R u:object_r:system_file:s0 " +
                        "/data/system/theme");
            } catch (Exception e) {
                e.printStackTrace();
                return "failed";
            }
            return null;
        }

        private void copyAssets() {
            AssetManager assetManager = getContext().getAssets();
            String[] files = null;
            InputStream in = null;
            OutputStream out = null;
            String filename = "fonts.xml";
            try {
                in = assetManager.open(filename);
                out = new FileOutputStream(getContext().getCacheDir().getAbsolutePath() +
                        "/FontCache/FontCreator/" + filename);
                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            } catch (IOException e) {
                Log.e("FontHandler", "Failed to move font configuration file to working " +
                        "directory!");
            }
        }

        private void copyFile(InputStream in, OutputStream out) throws IOException {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }

        private void unzip(String source, String destination) {
            try {
                ZipInputStream inputStream = new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(source)));
                try {
                    ZipEntry zipEntry;
                    int count;
                    byte[] buffer = new byte[8192];
                    while ((zipEntry = inputStream.getNextEntry()) != null) {
                        File file = new File(destination, zipEntry.getName());
                        File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                        if (!dir.isDirectory() && !dir.mkdirs())
                            throw new FileNotFoundException("Failed to ensure directory: " +
                                    dir.getAbsolutePath());
                        if (zipEntry.isDirectory())
                            continue;
                        FileOutputStream outputStream = new FileOutputStream(file);
                        try {
                            while ((count = inputStream.read(buffer)) != -1)
                                outputStream.write(buffer, 0, count);
                        } finally {
                            outputStream.close();
                        }
                    }
                } finally {
                    inputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("SubstratumLogger",
                        "An issue has occurred while attempting to decompress this archive.");
            }
        }

        private void CopyStream(InputStream Input, OutputStream Output) throws IOException {
            byte[] buffer = new byte[5120];
            int length = Input.read(buffer);
            while (length > 0) {
                Output.write(buffer, 0, length);
                length = Input.read(buffer);
            }
        }
    }


    private class FontPreview extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            imageButton.setClickable(false);
            imageButton.setImageTintList(unchecked);
            font_holder.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                Log.d("SubstratumLogger", "Loaded boot animation contains " + images.size() + " " +
                        "frames.");

                String work_directory = getContext().getCacheDir().getAbsolutePath() +
                        "/FontCache/font_preview/";

                Typeface normal_tf = Typeface.createFromFile(work_directory + "Roboto-Regular.ttf");
                Typeface bold_tf = Typeface.createFromFile(work_directory + "Roboto-Black.ttf");
                Typeface italics_tf = Typeface.createFromFile(work_directory + "Roboto-Italic.ttf");
                Typeface italics_bold_tf = Typeface.createFromFile(work_directory +
                        "Roboto-BoldItalic.ttf");

                TextView normal = (TextView) root.findViewById(R.id.text_normal);
                normal.setTypeface(normal_tf);
                TextView normal_bold = (TextView) root.findViewById(R.id.text_bold);
                normal_bold.setTypeface(bold_tf);
                TextView italics = (TextView) root.findViewById(R.id.text_normal_italics);
                italics.setTypeface(italics_tf);
                TextView italics_bold = (TextView) root.findViewById(R.id.text_normal_bold_italics);
                italics_bold.setTypeface(italics_bold_tf);

                eu.chainfire.libsuperuser.Shell.SU.run(
                        "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/font_preview/");
                imageButton.setImageTintList(checked);
                imageButton.setClickable(true);
                font_holder.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e("FontInstaller", "Window was destroyed before AsyncTask could complete " +
                        "postExecute()");
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            try {
                File cacheDirectory = new File(getContext().getCacheDir(), "/FontCache/");
                if (!cacheDirectory.exists()) {
                    cacheDirectory.mkdirs();
                }
                File cacheDirectory2 = new File(getContext().getCacheDir(), "/FontCache/" +
                        "font_preview/");
                if (!cacheDirectory2.exists()) {
                    cacheDirectory2.mkdirs();
                } else {
                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                    "/FontCache/font_preview/");
                    cacheDirectory2.mkdirs();
                }

                // Copy the font.zip from assets/fonts of the theme's assets

                String source = sUrl[0] + ".zip";

                try {
                    Context otherContext = getContext().createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    InputStream inputStream = am.open("fonts/" + source);
                    OutputStream outputStream = new FileOutputStream(getContext().getCacheDir()
                            .getAbsolutePath() + "/FontCache/" + source);
                    CopyStream(inputStream, outputStream);
                } catch (Exception e) {
                    Log.e("SubstratumLogger", "There is no fonts.zip found within the assets " +
                            "of this theme!");
                }

                // Unzip the fonts to get it prepared for the preview
                unzip(getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/" + source,
                        getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/font_preview/");
            } catch (Exception e) {
                Log.e("FontHelper", "Unexpectedly lost connection to the application host");
            }
            return null;
        }

        private void unzip(String source, String destination) {
            try {
                ZipInputStream inputStream = new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(source)));
                try {
                    ZipEntry zipEntry;
                    int count;
                    byte[] buffer = new byte[8192];
                    while ((zipEntry = inputStream.getNextEntry()) != null) {
                        File file = new File(destination, zipEntry.getName());
                        File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                        if (!dir.isDirectory() && !dir.mkdirs())
                            throw new FileNotFoundException("Failed to ensure directory: " +
                                    dir.getAbsolutePath());
                        if (zipEntry.isDirectory())
                            continue;
                        FileOutputStream outputStream = new FileOutputStream(file);
                        try {
                            while ((count = inputStream.read(buffer)) != -1)
                                outputStream.write(buffer, 0, count);
                        } finally {
                            outputStream.close();
                        }
                    }
                } finally {
                    inputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("SubstratumLogger",
                        "An issue has occurred while attempting to decompress this archive.");
            }
        }

        private void CopyStream(InputStream Input, OutputStream Output) throws IOException {
            byte[] buffer = new byte[5120];
            int length = Input.read(buffer);
            while (length > 0) {
                Output.write(buffer, 0, length);
                length = Input.read(buffer);
            }
        }
    }
}