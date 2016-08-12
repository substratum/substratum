package projekt.substratum.util;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ReadOverlaysFile {

    public static List<String> main(String argv[]) {

        File current_overlays = new File(Environment
                .getExternalStorageDirectory().getAbsolutePath() +
                "/.substratum/current_overlays.xml");
        if (current_overlays.exists()) {
            Root.runCommand("rm " + Environment
                    .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");
        }
        Root.runCommand("cp /data/system/overlays.xml " +
                Environment
                        .getExternalStorageDirectory().getAbsolutePath() +
                "/.substratum/current_overlays.xml");

        File file = new File(argv[0]);
        int state_count = Integer.parseInt(argv[1]);

        List<String> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (String line; (line = br.readLine()) != null; ) {
                if (line.contains("state=\"" + state_count + "\"")) {
                    String[] split = line.substring(21).split("\\s+");
                    list.add(split[0].substring(1, split[0].length() - 1));
                }
            }
        } catch (IOException ioe) {
            // Exception
        }
        return list;
    }
}
