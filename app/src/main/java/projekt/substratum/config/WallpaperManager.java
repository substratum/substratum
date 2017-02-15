package projekt.substratum.config;

import android.content.Context;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class WallpaperManager {

    public static void setWallpaper(Context context, String path, String which) throws IOException {
        android.app.WallpaperManager wallpaperManager =
                android.app.WallpaperManager.getInstance(context);
        switch (which) {
            case "home":
                // Set home screen wallpaper
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Get current lock screen wallpaper to be applied later
                    ParcelFileDescriptor lockFile = wallpaperManager
                            .getWallpaperFile(android.app.WallpaperManager.FLAG_LOCK);
                    InputStream input = new FileInputStream(lockFile.getFileDescriptor());
                    // Now apply the wallpapers
                    wallpaperManager.setStream(new FileInputStream(path), null, true,
                            android.app.WallpaperManager.FLAG_SYSTEM);
                    // Reapply previous lock screen wallpaper
                    wallpaperManager.setStream(input, null, true, android.app.WallpaperManager
                            .FLAG_LOCK);
                } else {
                    wallpaperManager.setStream(new FileInputStream(path));
                }
                break;
            case "lock":
                // Set lock screen wallpaper
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.setStream(new FileInputStream(path), null, true,
                            android.app.WallpaperManager.FLAG_LOCK);
                }
                break;
            case "all":
                // Apply both wallpaper
                wallpaperManager.setStream(new FileInputStream(path));
                break;
        }
    }

    public static void clearWallpaper(Context context, String which) throws IOException {
        android.app.WallpaperManager wallpaperManager = android.app.WallpaperManager.getInstance
                (context);
        switch (which) {
            case "home":
                // Clear home screen wallpaper
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Get current lock screen wallpaper to be applied later
                    ParcelFileDescriptor lockFile = wallpaperManager
                            .getWallpaperFile(android.app.WallpaperManager.FLAG_LOCK);
                    InputStream input = new FileInputStream(lockFile.getFileDescriptor());
                    // Clear home wallpaper
                    wallpaperManager.clear(android.app.WallpaperManager.FLAG_SYSTEM);
                    // Reapply lock screen wallpaper
                    wallpaperManager.setStream(input, null, true, android.app.WallpaperManager
                            .FLAG_LOCK);
                } else {
                    wallpaperManager.clear();
                }
                break;
            case "lock":
                // clear lock screen wallpaper
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wallpaperManager.clear(android.app.WallpaperManager.FLAG_LOCK);
                }
                break;
            case "all":
                // Clear both wallpaper
                wallpaperManager.clear();
                break;
        }
    }
}