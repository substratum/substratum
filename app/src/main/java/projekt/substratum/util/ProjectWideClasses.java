package projekt.substratum.util;

import android.os.Build;

import java.io.File;
import java.util.Arrays;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ProjectWideClasses {

    // This class is used to determine whether there the system is initiated with OMS

    public static Boolean checkOMS() {
        File om = new File("/system/bin/om");
        return om.exists();
    }

    // This class configures the new devices and their configuration of their vendor folders

    public static Boolean inNexusFilter() {
        String[] nexus_filter = {"angler", "bullhead", "flounder", "marlin", "sailfish"};
        return Arrays.asList(nexus_filter).contains(Build.DEVICE);
    }

    // This string array contains all the SystemUI acceptable overlay packs
    public static Boolean allowedSystemUIOverlay(String current) {
        String[] allowed_overlays = {
                "com.android.systemui.headers",
                "com.android.systemui.navbars"
        };
        return Arrays.asList(allowed_overlays).contains(current);
    }

    // This string array contains all the SystemUI acceptable sound files
    public static Boolean allowedUISound(String targetValue) {
        String[] allowed_themable = {
                "lock_sound",
                "unlock_sound",
                "low_battery_sound"};
        return Arrays.asList(allowed_themable).contains(targetValue);
    }

    // This int controls the notification identifier

    public static int notification_id = 2486;

    // This boolean controls the DEBUG level of the application

    public static Boolean DEBUG = false;

    // This int controls the default priority level for legacy overlays

    public static int DEFAULT_PRIORITY = 50;
}