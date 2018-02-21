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

package projekt.substratum.common.commands;

import projekt.substratum.util.helpers.Root;

public enum ElevatedCommands {
    ;

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