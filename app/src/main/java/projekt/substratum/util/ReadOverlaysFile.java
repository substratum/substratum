package projekt.substratum.util;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.config.References;

public class ReadOverlaysFile {

    public static List<String> main(Context context, String argv[]) {
        // Copy provided overlays xml path
        File current_overlays = new File(Environment
                .getExternalStorageDirectory().getAbsolutePath() +
                "/.substratum/current_overlays.xml");
        if (current_overlays.exists()) {
            References.delete(context, current_overlays.getAbsolutePath());
        }
        References.copy(context, argv[0], current_overlays.getAbsolutePath());

        // Parse provided state count
        int state_count = Integer.parseInt(argv[1]);

        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(current_overlays))) {
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

    public static List<List<String>> withTargetPackage(Context context, String argv[]) {
        // Copy provided overlays xml path
        File current_overlays = new File(Environment
                .getExternalStorageDirectory().getAbsolutePath() +
                "/.substratum/current_overlays.xml");
        if (current_overlays.exists()) {
            References.delete(context, current_overlays.getAbsolutePath());
        }
        References.copy(context, argv[0], current_overlays.getAbsolutePath());

        // Parse provided state count
        int state_count = Integer.parseInt(argv[1]);

        List<List<String>> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(current_overlays))) {
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