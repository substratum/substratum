package projekt.substratum.config;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import static projekt.substratum.config.References.checkThemeInterfacer;
import static projekt.substratum.config.References.getDeviceEncryptionStatus;

public class BootAnimationManager {

    public static void setBootAnimation(Context context, String themeDirectory) {
        String location = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/.substratum/bootanimation.zip";
        // Check to see if device is decrypted with theme interface
        if (getDeviceEncryptionStatus(context) <= 1 && checkThemeInterfacer(context)) {
            Log.d("BootAnimationUtils",
                    "No-root option has been enabled with the inclusion of " +
                            "theme interface...");
            ThemeInterfacerService.setBootAnimation(context, location);
            // Otherwise, fall back to rooted operations
        } else {
            // We will mount system, make our directory, copy the bootanimation
            // zip into place, set proper permissions, then unmount
            Log.d("BootAnimationUtils", "Root option has been enabled");
            FileOperations.mountRW();
            FileOperations.mountRWData();
            FileOperations.setPermissions(755, themeDirectory);
            FileOperations.move(context, location, themeDirectory + "/bootanimation.zip");
            FileOperations.setPermissions(644, themeDirectory + "/bootanimation.zip");
            FileOperations.setContext(themeDirectory);
            FileOperations.mountROData();
            FileOperations.mountRO();
        }
    }

    public static void clearBootAnimation(Context context) {
        if (getDeviceEncryptionStatus(context) <= 1 && checkThemeInterfacer(context)) {
            // OMS with theme interface
            ThemeInterfacerService.clearBootAnimation(context);
        } else if (getDeviceEncryptionStatus(context) <= 1 && !References.checkOMS(context)) {
            // Legacy decrypted
            FileOperations.delete(context, "/data/system/theme/bootanimation.zip");
        } else {
            // Encrypted OMS and legacy
            FileOperations.mountRW();
            FileOperations.move(context, "/system/media/bootanimation-backup.zip",
                    "/system/media/bootanimation.zip");
            FileOperations.delete(context, "/system/addon.d/81-subsboot.sh");
        }
    }
}
