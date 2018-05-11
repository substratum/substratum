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

package projekt.substratum.util.helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import projekt.substratum.common.References;

public enum Root {
    ;

    private static SU su;

    /**
     * Checks whether there is root access on the device
     *
     * @return True, if root is present
     */
    public static boolean checkRootAccess() {
        StringBuilder check = References.runShellCommand("which su");
        return (check != null) && !check.toString().isEmpty();
    }

    /**
     * Request root access
     *
     * @return True, if su is granted
     */
    public static boolean requestRootAccess() {
        SU su = getSU();
        su.runCommand("echo /testRoot/");
        return !su.denied;
    }

    /**
     * Run a series of commands as su
     *
     * @return String output
     */
    public static String runCommand(String command) {
        return getSU().runCommand(command);
    }

    /**
     * Obtain a process of SU
     *
     * @return Instance of SU
     */
    private static SU getSU() {
        if ((su == null) || su.closed || su.denied)
            su = new SU();
        return su;
    }

    /**
     * Class that is the beef of obtaining superuser access and permissions
     */
    private static class SU {
        private Process process;
        private BufferedWriter bufferedWriter;
        private BufferedReader bufferedReader;
        private boolean closed;
        private boolean denied;
        private boolean firstTry;

        SU() {
            super();
            try {
                firstTry = true;
                process = Runtime.getRuntime().exec("su");
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(
                        process.getOutputStream()));
                bufferedReader = new BufferedReader(new InputStreamReader(
                        process.getInputStream()));
            } catch (IOException ignored) {
                denied = true;
                closed = true;
            }
        }

        synchronized String runCommand(String command) {
            try {
                StringBuilder sb = new StringBuilder();
                String callback = "/shellCallback/";
                bufferedWriter.write(command + "\necho " + callback + '\n');
                bufferedWriter.flush();

                char[] buffer = new char[256];
                while (true) {
                    sb.append(buffer, 0, bufferedReader.read(buffer));
                    int i;
                    if ((i = sb.indexOf(callback)) > -1) {
                        sb.delete(i, i + callback.length());
                        break;
                    }
                }
                firstTry = false;
                return sb.toString().trim();
            } catch (IOException ignored) {
                closed = true;
                if (firstTry) denied = true;
            } catch (Exception ignored) {
                denied = true;
            }
            return null;
        }

    }
}