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

package projekt.substratum.util.files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import projekt.substratum.common.References;

public enum Root {
    ;

    private static SU su;

    public static boolean checkRootAccess() {
        final StringBuilder check = References.runShellCommand("which su");
        return check != null && !check.toString().isEmpty();
    }

    public static boolean requestRootAccess() {
        final SU su = getSU();
        su.runCommand("echo /testRoot/");
        return !su.denied;
    }

    public static String runCommand(final String command) {
        return getSU().runCommand(command);
    }

    private static SU getSU() {
        if (su == null || su.closed || su.denied)
            su = new SU();
        return su;
    }

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
                this.firstTry = true;
                this.process = Runtime.getRuntime().exec("su");
                this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(this.process
                        .getOutputStream()));
                this.bufferedReader = new BufferedReader(new InputStreamReader(this.process.getInputStream
                        ()));
            } catch (final IOException e) {
                this.denied = true;
                this.closed = true;
            }
        }

        synchronized String runCommand(final String command) {
            try {
                final StringBuilder sb = new StringBuilder();
                final String callback = "/shellCallback/";
                this.bufferedWriter.write(command + "\necho " + callback + "\n");
                this.bufferedWriter.flush();

                int i;
                final char[] buffer = new char[256];
                while (true) {
                    sb.append(buffer, 0, this.bufferedReader.read(buffer));
                    if ((i = sb.indexOf(callback)) > -1) {
                        sb.delete(i, i + callback.length());
                        break;
                    }
                }
                this.firstTry = false;
                return sb.toString().trim();
            } catch (final IOException e) {
                this.closed = true;
                if (this.firstTry) this.denied = true;
            } catch (final Exception e) {
                this.denied = true;
            }
            return null;
        }

        public void close() {
            try {
                if (this.bufferedWriter != null) {
                    this.bufferedWriter.write("exit\n");
                    this.bufferedWriter.flush();

                    this.bufferedWriter.close();
                }

                if (this.bufferedReader != null)
                    this.bufferedReader.close();

                if (this.process != null) {
                    this.process.waitFor();
                    this.process.destroy();
                }

                this.closed = true;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }
}