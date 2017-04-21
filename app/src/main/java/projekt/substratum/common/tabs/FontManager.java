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

package projekt.substratum.common.tabs;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeInterfacerService;
import projekt.substratum.common.platform.ThemeManager;

import static projekt.substratum.common.References.checkOMS;
import static projekt.substratum.common.References.checkThemeInterfacer;

public class FontManager {

    public static void setFonts(Context context, String theme_pid, String name) {
        if (checkOMS(context) && checkThemeInterfacer(context)) {
            ThemeInterfacerService.setFonts(context, theme_pid, name);
        } else {
            // oms no theme interfacer or legacy
            try {
                // Move the file from assets folder to a new working area
                Log.d("FontUtils",
                        "Copying over the selected fonts to working directory...");

                File cacheDirectory = new File(context.getCacheDir(), "/FontCache/");
                if (!cacheDirectory.exists() && cacheDirectory.mkdirs()) {
                    Log.d("FontUtils", "Successfully created cache folder!");
                }
                File cacheDirectory2 = new File(
                        context.getCacheDir(), "/FontCache/FontCreator/");
                if (!cacheDirectory2.exists() && cacheDirectory2.mkdirs()) {
                    Log.d("FontUtils", "Successfully created cache folder work directory!");
                } else {
                    FileOperations.delete(context,
                            context.getCacheDir().getAbsolutePath() + "/FontCache/FontCreator/");
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("FontUtils",
                            "Successfully recreated cache folder work directory!");
                }

                // Copy the font.zip from assets/fonts of the theme's assets
                String sourceFile = name + ".zip";

                try {
                    Context otherContext = context.createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    try (InputStream inputStream = am.open("fonts/" + sourceFile);
                         OutputStream outputStream = new FileOutputStream(context.getCacheDir()
                                 .getAbsolutePath() + "/FontCache/" + sourceFile)) {
                        byte[] buffer = new byte[5120];
                        int length = inputStream.read(buffer);
                        while (length > 0) {
                            outputStream.write(buffer, 0, length);
                            length = inputStream.read(buffer);
                        }
                    }
                } catch (Exception e) {
                    Log.e("FontUtils",
                            "There is no fonts.zip found within the assets of this theme! " +
                                    e.getMessage());
                }

                // Unzip the fonts to get it prepared for the preview
                String source =
                        context.getCacheDir().getAbsolutePath() + "/FontCache/" + sourceFile;
                String destination =
                        context.getCacheDir().getAbsolutePath() + "/FontCache/FontCreator/";

                try (ZipInputStream inputStream = new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(source)))) {
                    ZipEntry zipEntry;
                    int count;
                    byte[] buffer = new byte[8192];
                    while ((zipEntry = inputStream.getNextEntry()) != null) {
                        File file = new File(destination, zipEntry.getName());
                        File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                        if (!dir.isDirectory() && !dir.mkdirs())
                            throw new FileNotFoundException(
                                    "Failed to ensure directory: " + dir.getAbsolutePath());
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
                            "An issue has occurred while attempting to decompress this archive. " +
                                    e.getMessage());
                }

                // Copy all the system fonts to /data/system/theme/fonts
                File dataSystemThemeDir = new File("/data/system/theme");
                if (!dataSystemThemeDir.exists()) {
                    if (!checkThemeInterfacer(context)) {
                        FileOperations.mountRWData();
                    }
                    FileOperations.createNewFolder(context, "/data/system/theme/");
                }
                File dataSystemThemeFontsDir = new File("/data/system/theme/fonts");
                if (!dataSystemThemeFontsDir.exists()) {
                    if (!checkThemeInterfacer(context)) {
                        FileOperations.mountRWData();
                    }
                    FileOperations.createNewFolder(context, "/data/system/theme/fonts");
                } else {
                    FileOperations.delete(context, "/data/system/theme/fonts/");
                    if (!checkThemeInterfacer(context)) {
                        FileOperations.mountRWData();
                    }
                    FileOperations.createNewFolder(context, "/data/system/theme/fonts");
                }

                // Copy font configuration file (fonts.xml) to the working directory
                File fontsConfig = new File(context.getCacheDir().getAbsolutePath() +
                        "/FontCache/FontCreator/fonts.xml");
                if (!fontsConfig.exists()) {
                    AssetManager assetManager = context.getAssets();
                    final String filename = "fonts.xml";
                    try (InputStream in = assetManager.open(filename);
                         OutputStream out = new FileOutputStream(context.getCacheDir()
                                 .getAbsolutePath() + "/FontCache/FontCreator/" + filename)) {
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    } catch (IOException e) {
                        Log.e("FontUtils",
                                "Failed to move font configuration file to working directory! " +
                                        e.getMessage());
                    }
                }

                FileOperations.copy(context, "/system/fonts/*", "/data/system/theme/fonts/");

                // Copy all the files from work directory to /data/system/theme/fonts
                FileOperations.copy(context, context.getCacheDir().getAbsolutePath() +
                        "/FontCache/FontCreator/*", "/data/system/theme/fonts/");

                // Check for correct permissions and system file context integrity.
                FileOperations.mountRWData();
                FileOperations.setPermissions(755, "/data/system/theme/");
                FileOperations.setPermissionsRecursively(747, "/data/system/theme/fonts/");
                FileOperations.setPermissions(775, "/data/system/theme/fonts/");
                FileOperations.mountROData();
                FileOperations.setContext("/data/system/theme");
                FileOperations.setProp("sys.refresh_theme", "1");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void clearFonts(Context context) {
        if (checkOMS(context) && checkThemeInterfacer(context)) {
            ThemeInterfacerService.clearFonts(context);
        } else {
            // oms with no theme interfacer and legacy
            FileOperations.delete(context, "/data/system/theme/fonts/");
            if (!checkOMS(context)) ThemeManager.restartSystemUI(context);
        }
    }
}