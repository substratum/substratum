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

package projekt.substratum.tabs;

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

import static projekt.substratum.common.Internal.BYTE_ACCESS_RATE;
import static projekt.substratum.common.Internal.FONTS_SYSTEM_DIRECTORY;
import static projekt.substratum.common.Internal.FONTS_THEME_DIRECTORY;
import static projekt.substratum.common.Internal.FONT_CACHE;
import static projekt.substratum.common.Internal.FONT_CREATION_CACHE;
import static projekt.substratum.common.Internal.REFRESH_PROP;
import static projekt.substratum.common.Internal.REFRESH_PROP_ACTIVATED;
import static projekt.substratum.common.Internal.THEME_747;
import static projekt.substratum.common.Internal.THEME_755;
import static projekt.substratum.common.Internal.THEME_775;
import static projekt.substratum.common.Internal.THEME_DIRECTORY;
import static projekt.substratum.common.Systems.checkOMS;
import static projekt.substratum.common.Systems.checkThemeInterfacer;

public enum FontsManager {
    ;

    private static final String TAG = "FontsManager";

    /**
     * Set a font pack
     *
     * @param context   Context
     * @param theme_pid Theme package name
     * @param name      Name of font
     */
    public static void setFonts(
            Context context,
            String theme_pid,
            String name) {
        if (checkOMS(context) && checkThemeInterfacer(context)) {
            ThemeInterfacerService.setFonts(context, theme_pid, name);
        } else {
            // oms no theme interfacer or legacy
            try {
                // Move the file from assets folder to a new working area
                Log.d(TAG, "Copying over the selected fonts to working directory...");

                File cacheDirectory = new File(context.getCacheDir(), FONT_CACHE);
                if (!cacheDirectory.exists() && cacheDirectory.mkdirs()) {
                    Log.d(TAG, "Successfully created cache folder!");
                }
                File cacheDirectory2 = new File(
                        context.getCacheDir(), FONT_CREATION_CACHE);
                if (!cacheDirectory2.exists() && cacheDirectory2.mkdirs()) {
                    Log.d(TAG, "Successfully created cache folder work directory!");
                } else {
                    FileOperations.delete(context,
                            context.getCacheDir().getAbsolutePath() + FONT_CREATION_CACHE);
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d(TAG, "Successfully recreated cache folder work directory!");
                }

                // Copy the font.zip from assets/fonts of the theme's assets
                String sourceFile = name + ".zip";

                try {
                    Context otherContext = context.createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();

                    try (InputStream inputStream = am.open("fonts/" + sourceFile);
                         OutputStream outputStream = new FileOutputStream(
                                 context.getCacheDir().getAbsolutePath() +
                                         FONT_CACHE + sourceFile)) {
                        byte[] buffer = new byte[BYTE_ACCESS_RATE];
                        int length = inputStream.read(buffer);
                        while (length > 0) {
                            outputStream.write(buffer, 0, length);
                            length = inputStream.read(buffer);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "There is no fonts.zip found within the assets of this theme! " +
                            e.getMessage());
                }

                // Unzip the fonts to get it prepared for the preview
                String source =
                        context.getCacheDir().getAbsolutePath() + FONT_CACHE + sourceFile;
                String destination =
                        context.getCacheDir().getAbsolutePath() + FONTS_THEME_DIRECTORY;

                try (ZipInputStream inputStream = new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(source)))) {
                    ZipEntry zipEntry;
                    byte[] buffer = new byte[BYTE_ACCESS_RATE];
                    while ((zipEntry = inputStream.getNextEntry()) != null) {
                        File file = new File(destination, zipEntry.getName());
                        File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                        if (!dir.isDirectory() && !dir.mkdirs())
                            throw new FileNotFoundException(
                                    "Failed to ensure directory: " + dir.getAbsolutePath());
                        if (zipEntry.isDirectory())
                            continue;
                        try (FileOutputStream outputStream = new FileOutputStream(file)) {
                            int count;
                            while ((count = inputStream.read(buffer)) != -1)
                                outputStream.write(buffer, 0, count);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG,
                            "An issue has occurred while attempting to decompress this archive. " +
                                    e.getMessage());
                }

                // Copy all the system fonts to /data/system/theme/fonts
                File dataSystemThemeDir = new File(THEME_DIRECTORY);
                if (!dataSystemThemeDir.exists()) {
                    if (!checkThemeInterfacer(context)) FileOperations.mountRWData();
                    FileOperations.createNewFolder(context, THEME_DIRECTORY);
                }
                File dataSystemThemeFontsDir = new File(FONTS_THEME_DIRECTORY);
                if (!dataSystemThemeFontsDir.exists()) {
                    if (!checkThemeInterfacer(context)) FileOperations.mountRWData();
                    FileOperations.createNewFolder(context, FONTS_THEME_DIRECTORY);
                } else {
                    FileOperations.delete(context, FONTS_THEME_DIRECTORY);
                    if (!checkThemeInterfacer(context)) FileOperations.mountRWData();
                    FileOperations.createNewFolder(context, FONTS_THEME_DIRECTORY);
                }

                // Copy font configuration file (fonts.xml) to the working directory
                File fontsConfig = new File(context.getCacheDir().getAbsolutePath() +
                        FONT_CREATION_CACHE + "fonts.xml");
                if (!fontsConfig.exists()) {
                    AssetManager assetManager = context.getAssets();
                    String filename = "fonts.xml";
                    try (InputStream in = assetManager.open(filename);
                         OutputStream out = new FileOutputStream(context.getCacheDir()
                                 .getAbsolutePath() + FONT_CREATION_CACHE + filename)) {
                        byte[] buffer = new byte[BYTE_ACCESS_RATE];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to move font configuration file to working directory! " +
                                e.getMessage());
                    }
                }

                FileOperations.copy(context, FONTS_SYSTEM_DIRECTORY + "*", FONTS_THEME_DIRECTORY);

                // Copy all the files from work directory to /data/system/theme/fonts
                FileOperations.copy(context, context.getCacheDir().getAbsolutePath() +
                        FONT_CREATION_CACHE + "*", FONTS_THEME_DIRECTORY);

                // Check for correct permissions and system file context integrity.
                FileOperations.mountRWData();
                FileOperations.setPermissions(THEME_755, THEME_DIRECTORY);
                FileOperations.setPermissionsRecursively(THEME_747, FONTS_THEME_DIRECTORY);
                FileOperations.setPermissions(THEME_775, FONTS_THEME_DIRECTORY);
                FileOperations.mountROData();
                FileOperations.setContext(THEME_DIRECTORY);
                FileOperations.setProp(REFRESH_PROP, String.valueOf(REFRESH_PROP_ACTIVATED));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Clear an applied font pack
     *
     * @param context Context
     */
    public static void clearFonts(Context context) {
        if (checkOMS(context) && checkThemeInterfacer(context)) {
            ThemeInterfacerService.clearFonts(context);
        } else {
            // oms with no theme interfacer and legacy
            FileOperations.delete(context, FONTS_THEME_DIRECTORY);
            if (!checkOMS(context)) ThemeManager.restartSystemUI(context);
        }
    }
}