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

package projekt.substratum.util.helpers;

import android.content.Context;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import projekt.substratum.common.References;

import static projekt.substratum.common.Internal.BYTE_ACCESS_RATE;

public enum FileDownloader {
    ;

    /**
     * @param context                 getContext() or getApplicationContext()
     * @param fileUrl                 direct link to the XML file, could use PasteBin or a
     *                                server XML file
     * @param outputFile              the filename of the XML file, including .xml
     *                                (e.g. hello.xml)
     * @param destinationFileOrFolder the folder that encompasses this download cache
     *                                (e.g. vCache)
     */
    public static void init(Context context,
                            String fileUrl,
                            String outputFile,
                            String destinationFileOrFolder) throws
            NetworkOnMainThreadException {

        try {
            // First create the cache folder
            File directory = new File(context.getCacheDir().getAbsolutePath() + '/' +
                    destinationFileOrFolder);
            if (!destinationFileOrFolder.endsWith(".png") &&
                    !destinationFileOrFolder.endsWith(".jpg") &&
                    !destinationFileOrFolder.endsWith(".xml") &&
                    !directory.exists()) {
                if (!directory.mkdir())
                    Log.e(References.SUBSTRATUM_LOG,
                            "Could not make " + directory.getAbsolutePath() + " directory...");
            } else if (destinationFileOrFolder.endsWith(".xml") &&
                    !directory.isDirectory() &&
                    directory.exists()) {
                if (!directory.delete()) {
                    Log.e(References.SUBSTRATUM_LOG, "Could not delete file: " +
                            directory.getAbsolutePath());
                }
            }

            // Once the cache folder is created, start downloading the file
            HttpURLConnection connection = null;
            OutputStream output = null;
            InputStream input = null;
            try {
                URL url = new URL(fileUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // Expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e("Server returned HTTP", connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                }

                // Download the file
                input = connection.getInputStream();

                String outputDir = context.getCacheDir().getAbsolutePath() + '/' +
                        destinationFileOrFolder +
                        (outputFile != null && !outputFile.isEmpty() ? '/' + outputFile : "");

                Log.d(References.SUBSTRATUM_LOG, "Placing file in: " + outputDir);

                output = new FileOutputStream(outputDir);

                // Begin writing the data into the file
                byte[] data = new byte[BYTE_ACCESS_RATE];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
                Log.d("FileDownloader",
                        "File download function has concluded for '" + fileUrl + "'.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}