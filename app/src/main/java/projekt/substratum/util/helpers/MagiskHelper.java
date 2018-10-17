package projekt.substratum.util.helpers;

import android.content.Context;
import android.widget.Toast;
import projekt.substratum.BuildConfig;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;

public class MagiskHelper {

    private static String TAG = MagiskHelper.class.getSimpleName();
    private static final String MAGISK_MIRROR_MOUNT_POINT = "/sbin/.core/mirror/system";
    private static final String MAGISK_MIRROR_MOUNT_POINT_AFTER_174 = "/sbin/.magisk/mirror/system";

    public static void migrateToModule(final Context context) {
        if (Root.runCommand(String.format("test -d %s || echo '1'",
                References.MAGISK_MODULE_DIR)).equals("1")) {
            Substratum.log(TAG, "Magisk module does not exist, migrating");
            String command = "set -ex \n" +
                    String.format("mkdir -p %s; ", References.MAGISK_MODULE_DIR) +
                    String.format(
                            "printf 'name=substratum\nversion=%s\nversionCode=%s\nauthor=substratum development team\ndescription=Systemless overlays for Substratum\nminMagisk=1500\n' > %s/module.prop; ",
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE,
                            References.MAGISK_MODULE_DIR
                    ) +
                    String.format("touch %s/auto_mount; ", References.MAGISK_MODULE_DIR) +
                    String.format("mkdir -p %s/system/app; ", References.MAGISK_MODULE_DIR) +
                    String.format("mv %s/app/_*.apk %s; ", MAGISK_MIRROR_MOUNT_POINT, References.getPieDir()) +
                    String.format("mv %s/app/_*.apk %s; ", MAGISK_MIRROR_MOUNT_POINT_AFTER_174, References.getPieDir());
            Root.runCommand(command);
            Toast.makeText(context, R.string.module_placed_reboot_message, Toast.LENGTH_LONG).show();
        }
    }
}
