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

import android.os.AsyncTask;

import projekt.substratum.util.files.Root;

import static projekt.substratum.common.References.INTERFACER_PACKAGE;

public class ElevatedCommands {

    // Run the specified commands in a root shell
    public static void runCommands(final String commands) {
        Root.runCommand(commands);
    }

    // Reboot the device
    public static void reboot() {
        Root.runCommand("reboot");
    }

    // Kill the Theme Interface package to force it to restart
    public static void restartInterfacer() {
        Root.runCommand("pkill -f " + INTERFACER_PACKAGE);
    }

    // Kill zygote to force a soft rboot
    public static void softReboot() {
        Root.runCommand("pkill -f zygote");
    }

    public static class ThreadRunner extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... sUrl) {
            try {
                Root.runCommand(sUrl[0]);
            } catch (Exception e) {
                // Consume window refresh
            }
            return null;
        }
    }
}