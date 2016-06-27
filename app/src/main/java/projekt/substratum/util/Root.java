package projekt.substratum.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Root {

    public static SU su;

    public static boolean isRooted() {
        return isInPath("su");
    }

    public static boolean requestRootAccess() {
        SU su = getSU();
        su.runCommand("echo /testRoot/");
        return !su.denied;
    }

    public static boolean isBusyboxInstalled() {
        return isInPath("busybox");
    }

    private static boolean isInPath(String binary) {
        for (String path : System.getenv("PATH").split(":")) {
            if (!path.endsWith("/")) path += "/";
            if (new File(path + binary).exists()) return true;
        }
        return false;
    }

    public static void mount(boolean writeable, String mountpoint) {
        runCommand(writeable ? "mount -o remount,rw " + mountpoint + " " + mountpoint :
                "mount -o remount,ro " + mountpoint + " " + mountpoint);
    }

    public static void closeSU() {
        if (su != null) su.close();
        su = null;
    }

    public static String runCommand(String command) {
        return getSU().runCommand(command);
    }

    private static SU getSU() {
        if (su == null) su = new SU();
        else if (su.closed || su.denied) su = new SU();
        return su;
    }

    public static class SU {

        private Process process;
        private BufferedWriter bufferedWriter;
        private BufferedReader bufferedReader;
        private boolean closed;
        private boolean denied;
        private boolean firstTry;

        public SU() {
            try {
                firstTry = true;
                process = Runtime.getRuntime().exec("su");
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(process
                        .getOutputStream()));
                bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream
                        ()));
            } catch (IOException e) {
                denied = true;
                closed = true;
            }
        }

        public synchronized String runCommand(final String command) {
            try {
                StringBuilder sb = new StringBuilder();
                String callback = "/shellCallback/";
                bufferedWriter.write(command + "\necho " + callback + "\n");
                bufferedWriter.flush();

                int i;
                char[] buffer = new char[256];
                while (true) {
                    sb.append(buffer, 0, bufferedReader.read(buffer));
                    if ((i = sb.indexOf(callback)) > -1) {
                        sb.delete(i, i + callback.length());
                        break;
                    }
                }
                firstTry = false;
                return sb.toString().trim();
            } catch (IOException e) {
                closed = true;
                e.printStackTrace();
                if (firstTry) denied = true;
            } catch (ArrayIndexOutOfBoundsException e) {
                denied = true;
            } catch (Exception e) {
                e.printStackTrace();
                denied = true;
            }
            return null;
        }

        public void close() {
            try {
                bufferedWriter.write("exit\n");
                bufferedWriter.flush();

                process.waitFor();
                closed = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}