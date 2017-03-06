package projekt.substratum.tabs;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
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
import projekt.substratum.config.ElevatedCommands;
import projekt.substratum.config.FileOperations;
import projekt.substratum.config.FontManager;
import projekt.substratum.config.References;
import projekt.substratum.util.FontUtils;

public class Fonts extends Fragment {

    private String theme_pid;
    private ViewGroup root;
    private MaterialProgressBar progressBar;
    private ImageButton imageButton;
    private Spinner fontSelector;
    private ColorStateList unchecked, checked;
    private RelativeLayout font_holder;
    private RelativeLayout font_placeholder;
    private RelativeLayout defaults;
    private ProgressDialog mProgressDialog;
    private SharedPreferences prefs;
    private AsyncTask current;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        theme_pid = InformationActivity.getThemePID();
        root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_3, container, false);
        progressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);
        defaults = (RelativeLayout) root.findViewById(R.id.restore_to_default);
        font_holder = (RelativeLayout) root.findViewById(R.id.font_holder);
        font_placeholder = (RelativeLayout) root.findViewById(R.id.font_placeholder);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        imageButton = (ImageButton) root.findViewById(R.id.checkBox);
        imageButton.setClickable(false);
        imageButton.setOnClickListener(v -> {
            if (References.checkThemeInterfacer(getContext()) ||
                    Settings.System.canWrite(getContext())) {
                if (fontSelector.getSelectedItemPosition() == 1) {
                    new FontsClearer().execute("");
                } else {
                    new FontUtils().execute(fontSelector.getSelectedItem().toString(),
                            getContext(), theme_pid);
                }
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                Toast toast = Toast.makeText(getContext(),
                        getString(R.string.fonts_dialog_permissions_grant_toast2),
                        Toast.LENGTH_LONG);
                toast.show();
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
                    theme_pid + "/assets/fonts");
            File[] fileArray = f.listFiles();
            ArrayList<String> unparsedFonts = new ArrayList<>();
            for (File file : fileArray) {
                unparsedFonts.add(file.getName());
            }
            ArrayList<String> fonts = new ArrayList<>();
            fonts.add(getString(R.string.font_default_spinner));
            fonts.add(getString(R.string.font_spinner_set_defaults));
            for (int i = 0; i < unparsedFonts.size(); i++) {
                fonts.add(unparsedFonts.get(i).substring(0, unparsedFonts.get(i).length() - 4));
            }

            ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, fonts);
            fontSelector = (Spinner) root.findViewById(R.id.fontSelection);
            fontSelector.setAdapter(adapter1);
            fontSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {
                    switch (pos) {
                        case 0:
                            if (current != null)
                                current.cancel(true);
                            font_placeholder.setVisibility(View.VISIBLE);
                            defaults.setVisibility(View.GONE);
                            imageButton.setClickable(false);
                            imageButton.setImageTintList(unchecked);
                            font_holder.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            break;

                        case 1:
                            if (current != null)
                                current.cancel(true);
                            defaults.setVisibility(View.VISIBLE);
                            font_placeholder.setVisibility(View.GONE);
                            imageButton.setImageTintList(checked);
                            imageButton.setClickable(true);
                            font_holder.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            break;

                        default:
                            if (current != null)
                                current.cancel(true);
                            defaults.setVisibility(View.GONE);
                            font_placeholder.setVisibility(View.GONE);
                            String[] commands = {arg0.getSelectedItem().toString()};
                            current = new FontPreview().execute(commands);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("FontUtils", "There is no font.zip found within the assets of this theme!");
        }
        return root;
    }

    private class FontsClearer extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(getActivity(), R.style.RestoreDialog);
            mProgressDialog.setMessage(getString(R.string.manage_dialog_performing));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mProgressDialog.dismiss();
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("fonts_applied");
            editor.apply();

            if (References.checkOMS(getContext())) {
                Toast toast = Toast.makeText(getContext(),
                        getString(R.string.manage_fonts_toast), Toast.LENGTH_SHORT);
                toast.show();
            } else {
                Toast toast = Toast.makeText(getContext(),
                        getString(R.string.manage_fonts_toast), Toast.LENGTH_SHORT);
                toast.show();
                final AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(getContext());
                alertDialogBuilder.setTitle(getString(R.string.legacy_dialog_soft_reboot_title));
                alertDialogBuilder.setMessage(getString(R.string.legacy_dialog_soft_reboot_text));
                alertDialogBuilder.setPositiveButton(android.R.string.ok,
                        (dialog, id) -> ElevatedCommands.reboot());
                alertDialogBuilder.setNegativeButton(R.string.remove_dialog_later,
                        (dialog, id) -> dialog.dismiss());
                alertDialogBuilder.setCancelable(false);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            FontManager.clearFonts(getContext());
            return null;
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
                Log.d("FontUtils", "Fonts have been loaded on the drawing panel.");

                String work_directory = getContext().getCacheDir().getAbsolutePath() +
                        "/FontCache/font_preview/";

                try {
                    Typeface normal_tf = Typeface.createFromFile(work_directory +
                            "Roboto-Regular.ttf");
                    TextView normal = (TextView) root.findViewById(R.id.text_normal);
                    normal.setTypeface(normal_tf);
                } catch (Exception e) {
                    Log.e("FontUtils", "Could not load font from directory for normal template." +
                            " Maybe it wasn't themed?");
                }

                try {
                    Typeface bold_tf = Typeface.createFromFile(work_directory + "Roboto-Black.ttf");
                    TextView normal_bold = (TextView) root.findViewById(R.id.text_bold);
                    normal_bold.setTypeface(bold_tf);
                } catch (Exception e) {
                    Log.e("FontUtils", "Could not load font from directory for normal-bold " +
                            "template. Maybe it wasn't themed?");
                }

                try {
                    Typeface italics_tf = Typeface.createFromFile(work_directory + "Roboto-Italic" +
                            ".ttf");
                    TextView italics = (TextView) root.findViewById(R.id.text_normal_italics);
                    italics.setTypeface(italics_tf);
                } catch (Exception e) {
                    Log.e("FontUtils", "Could not load font from directory for italic template." +
                            " Maybe it wasn't themed?");
                }

                try {
                    Typeface italics_bold_tf = Typeface.createFromFile(work_directory +
                            "Roboto-BoldItalic.ttf");
                    TextView italics_bold = (TextView) root.findViewById(R.id
                            .text_normal_bold_italics);
                    italics_bold.setTypeface(italics_bold_tf);
                } catch (Exception e) {
                    Log.e("FontUtils", "Could not load font from directory for italic-bold " +
                            "template. Maybe it wasn't themed?");
                }

                FileOperations.delete(getContext(), getContext().getCacheDir().getAbsolutePath() +
                        "/FontCache/font_preview/");
                imageButton.setImageTintList(checked);
                imageButton.setClickable(true);
                font_holder.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e("Fonts",
                        "Window was destroyed before AsyncTask could complete postExecute()");
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            try {
                File cacheDirectory = new File(getContext().getCacheDir(), "/FontCache/");
                if (!cacheDirectory.exists()) {
                    if (cacheDirectory.mkdirs()) Log.d("FontUtils", "FontCache folder created");
                }
                File cacheDirectory2 = new File(getContext().getCacheDir(),
                        "/FontCache/font_preview/");

                if (!cacheDirectory2.exists()) {
                    if (cacheDirectory2.mkdirs()) Log.d("FontUtils",
                            "FontCache work folder created");
                } else {
                    FileOperations.delete(getContext(),
                            getContext().getCacheDir().getAbsolutePath() +
                                    "/FontCache/font_preview/");
                    if (cacheDirectory2.mkdirs()) Log.d("FontUtils", "FontCache folder recreated");
                }

                // Copy the font.zip from assets/fonts of the theme's assets
                String source = sUrl[0] + ".zip";

                try {
                    File f = new File(getContext().getCacheDir().getAbsoluteFile() +
                            "/SubstratumBuilder/" + theme_pid + "/assets/fonts/" + source);
                    InputStream inputStream = new FileInputStream(f);
                    OutputStream outputStream = new FileOutputStream(
                            getContext().getCacheDir().getAbsolutePath() + "/FontCache/" + source);
                    CopyStream(inputStream, outputStream);
                } catch (Exception e) {
                    Log.e("FontUtils",
                            "There is no fonts.zip found within the assets of this theme!");
                }

                // Unzip the fonts to get it prepared for the preview
                unzip(getContext().getCacheDir().getAbsolutePath() + "/FontCache/" + source,
                        getContext().getCacheDir().getAbsolutePath() + "/FontCache/font_preview/");
            } catch (Exception e) {
                Log.e("FontUtils", "Unexpectedly lost connection to the application host");
            }
            return null;
        }

        private void unzip(String source, String destination) {
            try (ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(source)))) {
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
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        while ((count = inputStream.read(buffer)) != -1)
                            outputStream.write(buffer, 0, count);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("FontUtils",
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