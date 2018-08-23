/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.common.commands;

import projekt.substratum.util.helpers.Root;

public class ElevatedCommands {

    /**
     * Reboot the device
     */
    public static void reboot() {
        Root.runCommand("reboot");
    }

    /**
     * Kill zygote to force a soft reboot
     */
    public static void softReboot() {
        Root.runCommand("pkill -f zygote");
    }

    /**
     * Run a command to be executed and held in another thread
     *
     * @param command Command to be run
     */
    public static void runThreadedCommand(String command) {
        new Thread(() -> {
            try {
                Root.runCommand(command);
            } catch (Exception ignored) {
                // Consume window refresh
            }
        }).start();
    }
}