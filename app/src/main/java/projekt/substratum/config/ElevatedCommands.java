package projekt.substratum.config;

import android.os.AsyncTask;

import projekt.substratum.util.Root;

import static projekt.substratum.config.References.INTERFACE_PACKAGE;

public class ElevatedCommands {

    // Run the specified commands in a root shell
    public static void runCommands(final String commands) {
        Root.runCommand(commands);
    }

    // Reboot the device
    public static void reboot() {
        Root.runCommand("reboot");
    }

    // Kill the masquerade package to force it to restart
    public static void restartMasquerade() {
        Root.runCommand("pkill -f " + INTERFACE_PACKAGE);
    }

    // Kill zygote to force a soft rboot
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
