package projekt.substratum.tabs;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.config.References;
import projekt.substratum.util.FontHandler;
import projekt.substratum.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class FontInstaller extends Fragment {

    private String theme_pid;
    private ViewGroup root;
    private MaterialProgressBar progressBar;
    private ImageButton imageButton;
    private Spinner fontSelector;
    private ColorStateList unchecked, checked;
    private RelativeLayout font_holder;
    private RelativeLayout font_placeholder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        theme_pid = InformationActivity.getThemePID();

        root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_4, container, false);

        progressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);

        font_holder = (RelativeLayout) root.findViewById(R.id.font_holder);
        font_placeholder = (RelativeLayout) root.findViewById(R.id.font_placeholder);

        imageButton = (ImageButton) root.findViewById(R.id.checkBox);
        imageButton.setClickable(false);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FontHandler().FontHandler(fontSelector.getSelectedItem().toString(),
                        getContext(), theme_pid);
            }
        });

        unchecked = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        getContext().getColor(R.color.font_unchecked),
                        getContext().getColor(R.color.font_unchecked)
                }
        );
        checked = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        getContext().getColor(R.color.font_checked),
                        getContext().getColor(R.color.font_checked)
                }
        );

        try {
            File f = new File(getContext().getCacheDir().getAbsoluteFile() + "/SubstratumBuilder/" +
                    getThemeName(theme_pid).replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "")
                    + "/assets/fonts");
            File[] fileArray = f.listFiles();
            ArrayList<String> unparsedFonts = new ArrayList<>();
            for (int i = 0; i < fileArray.length; i++) {
                unparsedFonts.add(fileArray[i].getName());
            }
            ArrayList<String> fonts = new ArrayList<>();
            fonts.add(getString(R.string.font_default_spinner));
            for (int i = 0; i < unparsedFonts.size(); i++) {
                fonts.add(unparsedFonts.get(i).substring(0,
                        unparsedFonts.get(i).length() - 4));
            }

            ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, fonts);
            fontSelector = (Spinner) root.findViewById(R.id.fontSelection);
            fontSelector.setAdapter(adapter1);
            fontSelector.setOnItemSelectedListener(new AdapterView
                    .OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {
                    if (pos == 0) {
                        font_placeholder.setVisibility(View.VISIBLE);
                        imageButton.setClickable(false);
                        imageButton.setImageTintList(unchecked);
                        font_holder.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                    } else {
                        font_placeholder.setVisibility(View.GONE);
                        String[] commands = {arg0.getSelectedItem().toString()};
                        new FontPreview().execute(commands);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("FontHandler", "There is no font.zip found within the assets " +
                    "of this theme!");
        }

        return root;
    }

    private String getThemeName(String package_name) {
        // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
        try {
            ApplicationInfo appInfo = getContext().getPackageManager().getApplicationInfo(
                    package_name, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString(References.metadataName) != null) {
                    if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                        return appInfo.metaData.getString(References.metadataName);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("SubstratumLogger", "Unable to find package identifier (INDEX OUT OF BOUNDS)");
        }
        return null;
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
                Log.d("FontHandler", "Fonts have been loaded on the drawing panel.");

                String work_directory = getContext().getCacheDir().getAbsolutePath() +
                        "/FontCache/font_preview/";

                try {
                    Typeface normal_tf = Typeface.createFromFile(work_directory + "Roboto-Regular" +
                            ".ttf");
                    TextView normal = (TextView) root.findViewById(R.id.text_normal);
                    normal.setTypeface(normal_tf);
                } catch (Exception e) {
                    Log.e("FontHandler", "Could not load font from directory for normal template." +
                            " Maybe it wasn't themed?");
                }

                try {
                    Typeface bold_tf = Typeface.createFromFile(work_directory + "Roboto-Black.ttf");
                    TextView normal_bold = (TextView) root.findViewById(R.id.text_bold);
                    normal_bold.setTypeface(bold_tf);
                } catch (Exception e) {
                    Log.e("FontHandler", "Could not load font from directory for normal-bold " +
                            "template. Maybe it wasn't themed?");
                }

                try {
                    Typeface italics_tf = Typeface.createFromFile(work_directory + "Roboto-Italic" +
                            ".ttf");
                    TextView italics = (TextView) root.findViewById(R.id.text_normal_italics);
                    italics.setTypeface(italics_tf);
                } catch (Exception e) {
                    Log.e("FontHandler", "Could not load font from directory for italic template." +
                            " Maybe it wasn't themed?");
                }

                try {
                    Typeface italics_bold_tf = Typeface.createFromFile(work_directory +
                            "Roboto-BoldItalic.ttf");
                    TextView italics_bold = (TextView) root.findViewById(R.id
                            .text_normal_bold_italics);
                    italics_bold.setTypeface(italics_bold_tf);
                } catch (Exception e) {
                    Log.e("FontHandler", "Could not load font from directory for italic-bold " +
                            "template. Maybe it wasn't themed?");
                }

                Root.runCommand(
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
                    boolean created = cacheDirectory.mkdirs();
                    if (created) Log.d("FontHandler", "FontCache folder created");
                }
                File cacheDirectory2 = new File(getContext().getCacheDir(), "/FontCache/" +
                        "font_preview/");
                if (!cacheDirectory2.exists()) {
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("FontHandler", "FontCache work folder created");
                } else {
                    Root.runCommand(
                            "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                    "/FontCache/font_preview/");
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("FontHandler", "FontCache folder recreated");
                }

                // Copy the font.zip from assets/fonts of the theme's assets

                String source = sUrl[0] + ".zip";

                try {
                    File f = new File(getContext().getCacheDir().getAbsoluteFile() +
                            "/SubstratumBuilder/" +
                            getThemeName(theme_pid).replaceAll("\\s+", "").replaceAll
                                    ("[^a-zA-Z0-9]+", "") + "/assets/fonts/" + source);
                    InputStream inputStream = new FileInputStream(f);
                    OutputStream outputStream = new FileOutputStream(getContext().getCacheDir()
                            .getAbsolutePath() + "/FontCache/" + source);
                    CopyStream(inputStream, outputStream);
                } catch (Exception e) {
                    Log.e("FontHandler", "There is no fonts.zip found within the assets " +
                            "of this theme!");
                }

                // Unzip the fonts to get it prepared for the preview
                unzip(getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/" + source,
                        getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/font_preview/");
            } catch (Exception e) {
                Log.e("FontHandler", "Unexpectedly lost connection to the application host");
            }
            return null;
        }

        private void unzip(String source, String destination) {
            try (ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(source)))){
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
                    try(FileOutputStream outputStream = new FileOutputStream(file)) {
                        while ((count = inputStream.read(buffer)) != -1)
                            outputStream.write(buffer, 0, count);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("FontHandler",
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