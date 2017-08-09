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

package projekt.substratum.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import projekt.substratum.R;

public class TeamFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.team_fragment, container, false);

        CardView nicholas_card = (CardView) root.findViewById(R.id.nicholas_card);
        nicholas_card.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_nicholas_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                if (getActivity() != null)
                    Lunchbar.make(getActivity().findViewById(android.R.id.content),
                            getString(R.string.activity_missing_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
            }
        });

        CardView syko_card = (CardView) root.findViewById(R.id.syko_card);
        syko_card.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_syko_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                if (getActivity() != null)
                    Lunchbar.make(getActivity().findViewById(android.R.id.content),
                            getString(R.string.activity_missing_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
            }
        });

        CardView ivan_card = (CardView) root.findViewById(R.id.ivan_card);
        ivan_card.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_ivan_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                if (getActivity() != null)
                    Lunchbar.make(getActivity().findViewById(android.R.id.content),
                            getString(R.string.activity_missing_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
            }
        });

        CardView surge_card = (CardView) root.findViewById(R.id.surge_card);
        surge_card.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_surge_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                if (getActivity() != null)
                    Lunchbar.make(getActivity().findViewById(android.R.id.content),
                            getString(R.string.activity_missing_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
            }
        });

        CardView george_card = (CardView) root.findViewById(R.id.george_card);
        george_card.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_george_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                if (getActivity() != null)
                    Lunchbar.make(getActivity().findViewById(android.R.id.content),
                            getString(R.string.activity_missing_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
            }
        });

        CardView nathan_card = (CardView) root.findViewById(R.id.nathan_card);
        nathan_card.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_nathan_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                if (getActivity() != null)
                    Lunchbar.make(getActivity().findViewById(android.R.id.content),
                            getString(R.string.activity_missing_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
            }
        });

        CardView char_card = (CardView) root.findViewById(R.id.char_card);
        char_card.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_char_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                if (getActivity() != null)
                    Lunchbar.make(getActivity().findViewById(android.R.id.content),
                            getString(R.string.activity_missing_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
            }
        });

        CardView harsh_card = root.findViewById(R.id.harsh_card);
        harsh_card.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_harsh_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                if (getActivity() != null)
                    Lunchbar.make(getActivity().findViewById(android.R.id.content),
                            getString(R.string.activity_missing_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
            }
        });


        Button development_contributors = (Button) root.findViewById(R.id.list_button_contributors);
        development_contributors.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setItems(getResources().getStringArray(R.array.substratum_contributors),
                    (dialog, item) -> {
                    });
            AlertDialog alert = builder.create();
            alert.show();
        });

        Button contribute = (Button) root.findViewById(R.id.list_button_translators_contribute);
        contribute.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.crowdin_url);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                if (getActivity() != null)
                    Lunchbar.make(getActivity().findViewById(android.R.id.content),
                            getString(R.string.activity_missing_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
            }
        });

        Button layers = (Button) root.findViewById(R.id.list_button_layers);
        layers.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setItems(getResources().getStringArray(R.array.layers_contributors),
                    (dialog, item) -> {
                    });
            AlertDialog alert = builder.create();
            alert.show();
        });

        Button translators = (Button) root.findViewById(R.id.list_button_translators);
        translators.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setItems(getResources().getStringArray(R.array.translations),
                    (dialog, item) -> {
                        dialog.cancel();
                        AlertDialog.Builder builder2 = new AlertDialog.Builder(getActivity());
                        switch (item) {
                            case 0:
                                builder2.setItems(
                                        getResources().getStringArray(R.array
                                                .belarusian_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 1:
                                builder2.setItems(
                                        getResources().getStringArray(R.array.czech_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 2:
                                builder2.setItems(
                                        getResources().getStringArray(R.array.chinese_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 3:
                                builder2.setItems(
                                        getResources().getStringArray(R.array.french_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 4:
                                builder2.setItems(
                                        getResources().getStringArray(R.array.german_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 5:
                                builder2.setItems(
                                        getResources().getStringArray(R.array
                                                .hungarian_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 6:
                                builder2.setItems(
                                        getResources().getStringArray(R.array.italian_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 7:
                                builder2.setItems(
                                        getResources().getStringArray(R.array
                                                .lithuanian_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 8:
                                builder2.setItems(
                                        getResources().getStringArray(R.array.dutch_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 9:
                                builder2.setItems(
                                        getResources().getStringArray(R.array.polish_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 10:
                                builder2.setItems(
                                        getResources().getStringArray(R.array
                                                .portuguese_brazillian_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 11:
                                builder2.setItems(
                                        getResources().getStringArray(R.array
                                                .portuguese_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 12:
                                builder2.setItems(
                                        getResources().getStringArray(R.array.russian_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 13:
                                builder2.setItems(
                                        getResources().getStringArray(R.array.slovak_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 14:
                                builder2.setItems(
                                        getResources().getStringArray(R.array.spanish_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 15:
                                builder2.setItems(
                                        getResources().getStringArray(R.array.turkish_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                        }
                        AlertDialog alert2 = builder2.create();
                        alert2.show();
                    });
            AlertDialog alert = builder.create();
            alert.show();
        });
        return root;
    }
}
