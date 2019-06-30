/*
 * Copyright (c) 2016-2019 Projekt Substratum
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

public class MagiskHelper {

    private static final String TAG = MagiskHelper.class.getSimpleName();
    private static final String MAGISK_DIR = References.getMagiskDirectory();

    private static void installModule(final Context context) {
        // Return if not using Magisk, when directory is "/" it means the version
        // is unsupported and we're falling back to modifying system.
        if (!checkMagisk() || MAGISK_DIR.equals("/"))
            return;

        Substratum.log(TAG, "Magisk module does not exist, creating!");
        String command = "set -ex \n" +
                String.format("mkdir -p %s; ", MAGISK_DIR) +
                String.format(
                        "printf 'name=substratum\nversion=%s\nversionCode=%s\nauthor=substratum development team\ndescription=Systemless overlays for Substratum\nminMagisk=1500\n' > %s/module.prop; ",
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                        MAGISK_DIR
                ) +
                String.format("touch %s/auto_mount; ", MAGISK_DIR) +
                String.format("mkdir -p %s/system/app; ", MAGISK_DIR);
        Root.runCommand(command);
        Toast.makeText(context, R.string.module_placed_reboot_message, Toast.LENGTH_LONG).show();
    }

    public static void handleModule(final Context context) {
        if (!moduleExists())
            installModule(context);
    }

    private static boolean checkMagisk() {
        return Root.runCommand("su --version").contains("MAGISKSU");
    }

    private static boolean moduleExists() {
        return Root.runCommand(String.format("test -d %s && echo '1'", MAGISK_DIR)).equals("1");
    }

}
