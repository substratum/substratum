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

package projekt.substratum.util.readers;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.common.commands.FileOperations;

import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;

public class ReadOverlaysFile {

    public static List<String> main(Context context, String argv[]) {
        // Copy provided overlays xml path
        File current_overlays = new File(Environment
                .getExternalStorageDirectory().getAbsolutePath() +
                EXTERNAL_STORAGE_CACHE + "current_overlays.xml");
        if (current_overlays.exists()) {
            FileOperations.delete(context, current_overlays.getAbsolutePath());
        }
        FileOperations.copy(context, argv[0], current_overlays.getAbsolutePath());

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
                EXTERNAL_STORAGE_CACHE + "current_overlays.xml");
        if (current_overlays.exists()) {
            FileOperations.delete(context, current_overlays.getAbsolutePath());
        }
        FileOperations.copy(context, argv[0], current_overlays.getAbsolutePath());

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