/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.helpers;

import android.content.Context;
import android.widget.Toast;
import projekt.substratum.BuildConfig;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.FileOperations;

public class MagiskHelper {

    private static final String MAGISK_MIRROR_MOUNT_POINT = "/sbin/.core/mirror/system";
    private static final String MAGISK_MIRROR_MOUNT_POINT_AFTER_174 = "/sbin/.magisk/mirror/system";
    private static final String TAG = MagiskHelper.class.getSimpleName();
    private static final String CHECK_MAGISK_CMD = "if [ -d %s ] || [ -d %s ]; then echo '0'; fi";
    private static final String[] mountPoints = new String[]{MAGISK_MIRROR_MOUNT_POINT, MAGISK_MIRROR_MOUNT_POINT_AFTER_174};

    public static void migrateToModule(final Context context) {
        if (!checkMagisk())
            return;
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
            for (String mountPoint : mountPoints) {
                FileOperations.mountRW(mountPoint);
            }
            Root.runCommand(command);
            for (String mountPoint : mountPoints) {
                FileOperations.mountRO(mountPoint);
            }
            Toast.makeText(context, R.string.module_placed_reboot_message, Toast.LENGTH_LONG).show();
        }
    }

    private static boolean checkMagisk() {
        return Root.runCommand(String.format(CHECK_MAGISK_CMD, MAGISK_MIRROR_MOUNT_POINT, MAGISK_MIRROR_MOUNT_POINT_AFTER_174)).equals("0");
    }

    public static void forceRemoveOverlays() {
        for (String mountPoint : mountPoints) {
            FileOperations.mountRW(mountPoint);
            Root.runCommand(String.format("rm -rf %s/app/_*.apk", mountPoint));
            FileOperations.mountRO(mountPoint);
        }
    }
}
