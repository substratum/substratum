package projekt.substratum.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadOverlaysFile {

    public static List<String> main(String argv[]) {
        // Current overlay list was copied in advance, outside this class
        File file = new File(argv[0]);
        int state_count = Integer.parseInt(argv[1]);

        List<String> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (String line; (line = br.readLine()) != null; ) {
                if (line.contains("state=\"" + state_count + "\"")) {
                    String[] split = line.substring(22).split("\\s+");
                    list.add(split[0].substring(1, split[0].length() - 1));
                }
            }
        } catch (IOException ioe) {
            // Exception
        }
        return list;
    }

    public static List<List<String>> withTargetPackage(String argv[]) {
        // Current overlay list was copied in advance, outside this class
        File file = new File(argv[0]);
        int state_count = Integer.parseInt(argv[1]);
        List<List<String>> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (String line; (line = br.readLine()) != null; ) {
                List<String> overlays = new ArrayList<>();
                if (line.contains("state=\"" + state_count + "\"")) {
                    String[] split = line.substring(22).split("\\s+");
                    String packageName = split[0].substring(1, split[0].length() - 1);
                    String targetPackage = split[2].substring(19, split[2].length() - 1);

                    overlays.add(packageName);
                    overlays.add(targetPackage);
                    list.add(overlays);
                }
            }
        } catch (IOException ioe) {
            // Exception
        }
        return list;
    }
}