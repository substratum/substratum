/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.helpers;

import android.content.Context;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import static projekt.substratum.common.Internal.BYTE_ACCESS_RATE;

public class FileDownloader {

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
            fileUrl = fileUrl.replace("http://", "https://");
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
            URL url = new URL(fileUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            String outputDir = context.getCacheDir().getAbsolutePath() + '/' +
                    destinationFileOrFolder +
                    (outputFile != null && !outputFile.isEmpty() ? '/' + outputFile : "");
            Substratum.log(References.SUBSTRATUM_LOG, "Placing file in: " + outputDir);
            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                Log.e("Server returned HTTP", connection.getResponseCode()
                        + " " + connection.getResponseMessage());
            }
            connection.connect();
            try (OutputStream output = new FileOutputStream(outputDir);
                 InputStream input = connection.getInputStream()) {

                // Expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file

                // Download the file

                // Begin writing the data into the file
                byte[] data = new byte[BYTE_ACCESS_RATE];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                connection.disconnect();
                Substratum.log("FileDownloader",
                        "File download function has concluded for '" + fileUrl + "'.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}