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
    private static final String BOOTANIMATION_FILE_NAME = "bootanimation_file_name";
    private static final String AUDIO_PID = "audio_pid";
    private static final String AUDIO_FILENAME = "audio_filename";
    private static final String FONTS_FILENAME = "fonts_filename";
    private static final String FONTS_PID = "fonts_pid";
    private static final String ENABLE_LIST_KEY = "enable_list";
    private static final String DISABLE_LIST_KEY = "disable_list";
    private static final String PRIORITY_LIST_KEY = "priority_list";
    private static final String SOURCE_FILE_KEY = "source_file";
    private static final String DESTINATION_FILE_KEY = "destination_file";
    private static final String WITH_DELETE_PARENT_KEY = "with_delete_parent";
    private static final String PROFILE_NAME_KEY = "profile_name";
    private static final String COMMAND_VALUE_INSTALL = "install";
    private static final String COMMAND_VALUE_UNINSTALL = "uninstall";
    private static final String COMMAND_VALUE_RESTART_UI = "restart_ui";
    private static final String COMMAND_VALUE_RESTART_SERVICE = "restart_service";
    private static final String COMMAND_VALUE_CONFIGURATION_SHIM = "configuration_shim";
    private static final String COMMAND_VALUE_BOOTANIMATION = "bootanimation";
    private static final String COMMAND_VALUE_FONTS = "fonts";
    private static final String COMMAND_VALUE_AUDIO = "audio";
    private static final String COMMAND_VALUE_ENABLE = "enable";
    private static final String COMMAND_VALUE_DISABLE = "disable";
    private static final String COMMAND_VALUE_PRIORITY = "priority";
    private static final String COMMAND_VALUE_COPY = "copy";
    private static final String COMMAND_VALUE_MOVE = "move";
    private static final String COMMAND_VALUE_DELETE = "delete";
    private static final String COMMAND_VALUE_PROFILE = "profile";
    private static final String COMMAND_VALUE_MKDIR = "mkdir";

    private static Intent getMasqueradeRootless(Context context) {
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

    public static Intent getMasquerade(Context context) {
        Intent intent = new Intent();
        PendingIntent pending = PendingIntent.getActivity(context, 0, new Intent(), 0);
        intent.putExtra(MASQUERADE_TOKEN, pending);
        intent.putExtra(JOB_TIME_KEY, System.currentTimeMillis());
        return intent;
    }

    static void installOverlays(Context context, ArrayList<String> overlays) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_INSTALL);
        masqIntent.putExtra(INSTALL_LIST_KEY, overlays);
        context.startService(masqIntent);
    }

    static void uninstallOverlays(Context context, ArrayList<String> overlays, boolean
            restartUi) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_UNINSTALL);
        masqIntent.putExtra(UNINSTALL_LIST_KEY, overlays);
        // only need to set if true, will restart SystemUI when done processing packages
        masqIntent.putExtra(WITH_RESTART_UI_KEY, restartUi);
        context.startService(masqIntent);
    }

    static void enableOverlays(Context context, ArrayList<String> overlays, boolean restartUi) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_ENABLE);
        masqIntent.putExtra(ENABLE_LIST_KEY, overlays);
        masqIntent.putExtra(WITH_RESTART_UI_KEY, restartUi);
        context.startService(masqIntent);
    }

    static void disableOverlays(Context context, ArrayList<String> overlays, boolean restartUi) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_DISABLE);
        masqIntent.putExtra(DISABLE_LIST_KEY, overlays);
        masqIntent.putExtra(WITH_RESTART_UI_KEY, restartUi);
        context.startService(masqIntent);
    }

    public static void restartSystemUI(Context context) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_RESTART_UI);
        context.startService(masqIntent);
    }

    public static void restartService(Context context) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_RESTART_SERVICE);
        context.startService(masqIntent);
    }

    public static void configurationChangeShim(Context context) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_CONFIGURATION_SHIM);
        context.startService(masqIntent);
    }

    static void setBootAnimation(Context context, String bootanimation_location) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_BOOTANIMATION);
        masqIntent.putExtra(BOOTANIMATION_FILE_NAME, bootanimation_location);
        context.startService(masqIntent);
    }

    static void clearBootAnimation(Context context) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_BOOTANIMATION);
        context.startService((masqIntent));
    }

    static void setFonts(Context context, String pid, String name) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_FONTS);
        masqIntent.putExtra(FONTS_FILENAME, name);
        masqIntent.putExtra(FONTS_PID, pid);
        context.startService(masqIntent);
    }

    static void clearFonts(Context context) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_FONTS);
        context.startService(masqIntent);
    }

    static void setThemedSounds(Context context, String pid, String name) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_AUDIO);
        masqIntent.putExtra(AUDIO_PID, pid);
        masqIntent.putExtra(AUDIO_FILENAME, name);
        context.startService(masqIntent);
    }

    static void clearThemedSounds(Context context) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_AUDIO);
        context.startService(masqIntent);
    }

    static void setPriority(Context context, ArrayList<String> overlays) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_PRIORITY);
        masqIntent.putExtra(PRIORITY_LIST_KEY, overlays);
        context.startService(masqIntent);
    }

    public static void copy(Context context, String source, String destination) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_COPY);
        masqIntent.putExtra(SOURCE_FILE_KEY, source);
        masqIntent.putExtra(DESTINATION_FILE_KEY, destination);
        context.startService(masqIntent);
    }

    public static void move(Context context, String source, String destination) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_MOVE);
        masqIntent.putExtra(SOURCE_FILE_KEY, source);
        masqIntent.putExtra(DESTINATION_FILE_KEY, destination);
        context.startService(masqIntent);
    }

    public static void delete(Context context, String directory, boolean deleteParent) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_DELETE);
        masqIntent.putExtra(SOURCE_FILE_KEY, directory);
        masqIntent.putExtra(WITH_DELETE_PARENT_KEY, deleteParent);
        context.startService(masqIntent);
    }

    public static void applyProfile(Context context, String name, ArrayList<String> toBeDisabled,
                                    ArrayList<String> toBeEnabled, boolean restartUi) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_PROFILE);
        masqIntent.putExtra(PROFILE_NAME_KEY, name);
        masqIntent.putExtra(DISABLE_LIST_KEY, toBeDisabled);
        masqIntent.putExtra(ENABLE_LIST_KEY, toBeEnabled);
        masqIntent.putExtra(WITH_RESTART_UI_KEY, restartUi);
        context.startService(masqIntent);
    }

    public static void createNewFolder(Context context, String destination) {
        Intent masqIntent = getMasqueradeRootless(context);
        masqIntent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_MKDIR);
        masqIntent.putExtra(DESTINATION_FILE_KEY, destination);
        context.startService(masqIntent);
    }
}
