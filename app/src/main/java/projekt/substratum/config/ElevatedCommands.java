package projekt.substratum.config;

import android.os.AsyncTask;

import projekt.substratum.util.Root;

import static projekt.substratum.config.References.MASQUERADE_PACKAGE;

public class ElevatedCommands {

    public static void runCommands(final String commands) {
        Root.runCommand(commands);
    }

    public static void reboot() {
        Root.runCommand("reboot");
    }

    public static void restartMasquerade() {
        Root.runCommand("pkill -f " + MASQUERADE_PACKAGE);
    }

    public static void softReboot() {
        Root.runCommand("pkill -f zygote");
    }

    static class ThreadRunner extends AsyncTask<String, Integer, String> {
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
