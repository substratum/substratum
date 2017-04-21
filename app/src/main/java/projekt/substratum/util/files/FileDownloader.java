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

package projekt.substratum.util.files;

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

public class FileDownloader {

    /**
     * @param context           getContext() or getApplicationContext()
     * @param fileUrl           direct link to the XML file, could use PasteBin or a server XML file
     * @param outputFile        the filename of the XML file, including .xml (e.g. hello.xml)
     * @param destinationFolder the folder that encompasses this download cache (e.g. vCache)
     */
    public static void init(Context context,
                            String fileUrl,
                            String outputFile,
                            String destinationFolder) throws NetworkOnMainThreadException {

        try {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            // First create the cache folder
            File directory = new File(context.getCacheDir().getAbsolutePath() + "/" +
                    destinationFolder);
            if (!directory.exists()) {
                Boolean made = directory.mkdir();
                if (!made)
                    Log.e(References.SUBSTRATUM_LOG,
                            "Could not make " + directory.getAbsolutePath() + " directory...");
            }

            // Once the cache folder is created, start downloading the file
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

                // This will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // Download the file
                input = connection.getInputStream();

                output = new FileOutputStream(
                        context.getCacheDir().getAbsolutePath() + "/" +
                                destinationFolder + "/" + outputFile);

                // Begin writing the data into the file
                byte data[] = new byte[8192];
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
                } catch (IOException ioe) {
                    // Suppress warning
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