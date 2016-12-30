package projekt.substratum.config;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

public class MasqueradeService {

    private static final String MASQUERADE_TOKEN = "masquerade_token";
    private static final String PRIMARY_COMMAND_KEY = "primary_command_key";
    private static final String JOB_TIME_KEY = "job_time_key";
    private static final String INSTALL_LIST_KEY = "install_list";
    private static final String UNINSTALL_LIST_KEY = "uninstall_list";
    private static final String WITH_RESTART_UI_KEY = "with_restart_ui";
    private static final String BOOTANIMATION_PID_KEY = "bootanimation_pid";
    private static final String BOOTANIMATION_FILE_NAME = "bootanimation_file_name";
    private static final String FONTS_RESET = "fonts_reset";
    private static final String COMMAND_VALUE_INSTALL = "install";
    private static final String COMMAND_VALUE_UNINSTALL = "uninstall";
    private static final String COMMAND_VALUE_RESTART_UI = "restart_ui";
    private static final String COMMAND_VALUE_CONFIGURATION_SHIM = "configuration_shim";
    private static final String COMMAND_VALUE_BOOTANIMATION = "bootanimation";
    private static final String COMMAND_VALUE_FONTS = "fonts";
    private static final String COMMAND_VALUE_AUDIO = "audio";

    public static Intent getMasquerade(Context context) {
        Intent intent = new Intent();
        intent.setComponent(
                new ComponentName(
                        "masquerade.substratum",
                        "masquerade.substratum.services.JobService"));
        PendingIntent pending = PendingIntent.getActivity(context, 0, new Intent(), 0);
        intent.putExtra(MASQUERADE_TOKEN, pending);
        intent.putExtra(JOB_TIME_KEY, System.currentTimeMillis());
        return intent;
    }

    public static void installOverlays(Context context, ArrayList<String> overlays) {
        Intent masqIntent = getMasquerade(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_INSTALL);
        masqIntent.putExtra(INSTALL_LIST_KEY, overlays);
        context.startService(masqIntent);
    }

    public static void uninstallOverlays(Context context, ArrayList<String> overlays) {
        Intent masqIntent = getMasquerade(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_UNINSTALL);
        masqIntent.putExtra(UNINSTALL_LIST_KEY, overlays);
        // only need to set if true, will restart SystemUI when done processing packages
        masqIntent.putExtra(WITH_RESTART_UI_KEY, true);
        context.startService(masqIntent);
    }

    public static void restartSystemUI(Context context) {
        Intent masqIntent = getMasquerade(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_RESTART_UI);
        context.startService(masqIntent);
    }

    public static void configurationChangeShim(Context context) {
        Intent masqIntent = getMasquerade(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_CONFIGURATION_SHIM);
        context.startService(masqIntent);
    }

    public static void setBootAnimation(Context context, String bootanimation_location) {
        Intent masqIntent = getMasquerade(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_BOOTANIMATION);
        boolean useThemed = true;
        if (useThemed) {
            // going to set a themed bootanim
            String fileName = bootanimation_location;
            masqIntent.putExtra(BOOTANIMATION_FILE_NAME, fileName);
        } else {
            // nothing. to reset to stock, just don't add PID and FILE
        }
        context.startService(masqIntent);
    }

    public static void setFonts(Context context) {
        Intent masqIntent = getMasquerade(context);
        // will automatically load prepared font from Substratum font working folder
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_FONTS);
        // set true to restore to stock
        masqIntent.putExtra(FONTS_RESET, true);
        context.startService(masqIntent);
    }

}