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
import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static projekt.substratum.common.Internal.ALL_WALLPAPER;
import static projekt.substratum.common.Internal.HOME_WALLPAPER;
import static projekt.substratum.common.Internal.LOCK_WALLPAPER;

public enum WallpapersManager {
    ;

    /**
     * Set a wallpaper
     *
     * @param context Context
     * @param path    Path of wallpaper
     * @param which   Wallpaper mode
     * @throws IOException If the wallpaper could not be set
     */
    public static void setWallpaper(Context context,
                                    String path,
                                    String which) throws IOException {
        android.app.WallpaperManager wallpaperManager =
                android.app.WallpaperManager.getInstance(context);
        switch (which) {
            case HOME_WALLPAPER:
                // Set home screen wallpaper
                // Get current lock screen wallpaper to be applied later
                ParcelFileDescriptor lockFile =
                        wallpaperManager.getWallpaperFile(android.app.WallpaperManager.FLAG_LOCK);
                if (lockFile != null) {
                    InputStream input = new FileInputStream(lockFile.getFileDescriptor());
                    // Now apply the wallpapers
                    wallpaperManager.setStream(new FileInputStream(path), null, true,
                            android.app.WallpaperManager.FLAG_SYSTEM);
                    // Reapply previous lock screen wallpaper
                    wallpaperManager.setStream(input, null, true,
                            android.app.WallpaperManager.FLAG_LOCK);
                } else {
                    // No lock screen wallpaper applied, just apply directly
                    wallpaperManager.setStream(new FileInputStream(path), null, true,
                            android.app.WallpaperManager.FLAG_SYSTEM);
                }
                break;
            case LOCK_WALLPAPER:
                // Set lock screen wallpaper
                wallpaperManager.setStream(new FileInputStream(path), null, true,
                        android.app.WallpaperManager.FLAG_LOCK);
                break;
            case ALL_WALLPAPER:
                // Apply both wallpapers
                wallpaperManager.setStream(new FileInputStream(path));
                break;
        }
    }

    /**
     * Restore the default wallpapers
     *
     * @param context Context
     * @param which   Wallpaper mode
     * @throws IOException If the wallpaper could not be reset
     */
    public static void clearWallpaper(Context context,
                                      String which) throws IOException {
        android.app.WallpaperManager wallpaperManager =
                android.app.WallpaperManager.getInstance(context);
        switch (which) {
            case HOME_WALLPAPER:
                // Clear home screen wallpaper
                // Get current lock screen wallpaper to be applied later
                ParcelFileDescriptor lockFile =
                        wallpaperManager.getWallpaperFile(android.app.WallpaperManager.FLAG_LOCK);
                if (lockFile != null) {
                    InputStream input = new FileInputStream(lockFile.getFileDescriptor());
                    // Clear home wallpaper
                    wallpaperManager.clear(android.app.WallpaperManager.FLAG_SYSTEM);
                    // Reapply lock screen wallpaper
                    wallpaperManager.setStream(input, null, true,
                            android.app.WallpaperManager.FLAG_LOCK);
                } else {
                    // No lock screen wallpaper applied, just apply directly
                    wallpaperManager.clear(android.app.WallpaperManager.FLAG_SYSTEM);
                }
                break;
            case LOCK_WALLPAPER:
                // Clear lock screen wallpaper
                wallpaperManager.clear(android.app.WallpaperManager.FLAG_LOCK);
                break;
            case ALL_WALLPAPER:
                // Clear both wallpapers
                wallpaperManager.clear();
                break;
        }
    }
}