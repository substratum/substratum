package projekt.substratum.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ReadOverlays {

    private static String OVERLAY_MANAGER_STATE_NOT_APPROVED = "---";
    private static String OVERLAY_MANAGER_STATE_DISABLED = "[ ]";
    private static String OVERLAY_MANAGER_STATE_ENABLED = "[x]";

    public static List<String> main(int state_count) {

        String CURRENT_SELECTION = OVERLAY_MANAGER_STATE_NOT_APPROVED;
        List<String> list = new ArrayList<>();
        Process nativeApp = null;

        if (state_count == 4) {
            // At this point we are looking for disabled overlays
            CURRENT_SELECTION = OVERLAY_MANAGER_STATE_DISABLED;
        } else if (state_count == 5) {
            // At this point we are looking for enabled overlays
            CURRENT_SELECTION = OVERLAY_MANAGER_STATE_ENABLED;
        }

        try {
            nativeApp = Runtime.getRuntime().exec("om list");

            try (OutputStream stdin = nativeApp.getOutputStream();
                 InputStream stdout = nativeApp.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(stdout))) {
                String line;

                stdin.write(("ls\n").getBytes());
                stdin.write("exit\n".getBytes());

                while ((line = br.readLine()) != null) {
                    if (line.length() > 0) {
                        if (line.contains(CURRENT_SELECTION)) {
                            list.add(line.substring(8));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Exception
        } finally {
            if (nativeApp != null) {
                // Destroy the process explicitly
                nativeApp.destroy();
            }
        }
        return list;
    }
}