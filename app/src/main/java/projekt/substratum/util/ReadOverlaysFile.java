package projekt.substratum.util;

import android.util.Log;

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
            Log.e("SubstratumLogger", "Unable to load XML file correctly.");
        }
        return list;
    }
}