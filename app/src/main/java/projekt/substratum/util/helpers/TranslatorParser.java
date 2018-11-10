/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.helpers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.AsyncTask;
import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.fragments.SettingsFragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TranslatorParser {
    private final InputStream inputStream;

    private TranslatorParser(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Reads a CrowdIn exported CSV file for Top Reviewers. Ensure the languages column doesn't have
     * any unescaped commas.
     *
     * @return Returns a list of translators
     * @throws RuntimeException If CSV file could not be read
     */
    @SuppressWarnings("unchecked")
    private List<Translator> read() throws RuntimeException {
        List<Translator> resultList = new ArrayList();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String csvLine;
            while ((csvLine = reader.readLine()) != null) {
                String[] row = csvLine.split(",");
                if (!row[0].equals("Name") &&
                        !row[1].equals("Languages") &&
                        Integer.parseInt(row[2]) >= 10) {
                    Translator translator = new Translator(row[0], row[1], row[2]);
                    resultList.add(translator);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("CSV file could not be read due to an exception: " + ex);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
        return resultList;
    }

    /**
     * Loads the Translator Contribution Dialog asynchronously with multiple dialogs
     */
    public static class TranslatorContributionDialog extends AsyncTask<String, Integer,
            ArrayList<String>> {

        private final WeakReference<SettingsFragment> ref;
        private WeakReference<AlertDialog.Builder> alertDialogBuilder;

        public TranslatorContributionDialog(SettingsFragment settingsFragment) {
            super();
            ref = new WeakReference<>(settingsFragment);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            SettingsFragment settingsFragment = ref.get();
            if (settingsFragment.isAdded() && settingsFragment != null) {
                if (settingsFragment.getActivity() != null) {
                    settingsFragment.dialog = new Dialog(settingsFragment.getActivity());
                    settingsFragment.dialog.setContentView(R.layout.validator_dialog);
                    settingsFragment.dialog.setCancelable(false);
                    settingsFragment.dialog.show();
                }
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            SettingsFragment settingsFragment = ref.get();
            if (settingsFragment.isAdded() && settingsFragment != null) {
                settingsFragment.dialog.cancel();
                if (alertDialogBuilder.get() != null) alertDialogBuilder.get().show();
            }
        }

        @Override
        protected ArrayList<String> doInBackground(String... strings) {
            SettingsFragment settingsFragment = ref.get();
            if (settingsFragment.isAdded() && settingsFragment != null) {
                InputStream inputStream =
                        ref.get().getResources().openRawResource(R.raw.translators);
                TranslatorParser csvFile = new TranslatorParser(inputStream);
                List<TranslatorParser.Translator> translators = csvFile.read();
                alertDialogBuilder = new WeakReference<>(
                        References.invokeTranslatorDialog(ref.get().context, translators));
            }
            return null;
        }
    }

    public class Translator {

        public final String contributorName;
        public final List<String> languages;
        public Integer translated_words = 0;

        Translator(String contributor_name,
                   String languages,
                   String translated_words) {
            this.contributorName = contributor_name;
            this.languages = Arrays.asList(languages.split("; "));
            try {
                this.translated_words = Integer.valueOf(translated_words);
            } catch (Exception ignored) {
            }
        }
    }
}