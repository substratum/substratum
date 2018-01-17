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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import projekt.substratum.R;
import projekt.substratum.common.References;

import static projekt.substratum.common.Activities.launchActivityUrl;

public class TeamFragment extends Fragment {

    @BindView(R.id.nicholas_card)
    CardView nicholas_card;
    @BindView(R.id.syko_card)
    CardView syko_card;
    @BindView(R.id.ivan_card)
    CardView ivan_card;
    @BindView(R.id.surge_card)
    CardView surge_card;
    @BindView(R.id.george_card)
    CardView george_card;
    @BindView(R.id.nathan_card)
    CardView nathan_card;
    @BindView(R.id.char_card)
    CardView char_card;
    @BindView(R.id.harsh_card)
    CardView harsh_card;
    @BindView(R.id.list_button_contributors)
    Button development_contributors;
    @BindView(R.id.list_button_translators_contribute)
    Button contribute;
    @BindView(R.id.list_button_layers)
    Button layers;
    @BindView(R.id.list_button_translators)
    Button translators;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.team_fragment, container, false);
        ButterKnife.bind(this, view);

        final Context context = getContext();
        final Activity activity = getActivity();
        // Nicholas
        nicholas_card.setOnClickListener(v -> {
            if (activity != null && context != null) {
                launchActivityUrl(
                    context,
                    References.getView(activity),
                    R.string.team_nicholas_link);
            }
        });

        // Mike
        syko_card.setOnClickListener(v -> {
            if (activity != null && context != null) {
                launchActivityUrl(
                        context,
                        References.getView(activity),
                        R.string.team_syko_link);
            }
        });

        // Ivan
        ivan_card.setOnClickListener(v -> {
            if (activity != null && context != null) {
                launchActivityUrl(
                        context,
                        References.getView(activity),
                        R.string.team_ivan_link);
            }
        });

        // Surge
        surge_card.setOnClickListener(v -> {
            if (activity != null && context != null) {
                launchActivityUrl(
                        context,
                        References.getView(activity),
                        R.string.team_surge_link);
            }
        });

        // George
        george_card.setOnClickListener(v -> {
            if (activity != null && context != null) {
                launchActivityUrl(
                        context,
                        References.getView(activity),
                        R.string.team_george_link);
            }
        });

        // Nathan
        nathan_card.setOnClickListener(v -> {
            if (activity != null && context != null) {
                launchActivityUrl(
                        context,
                        References.getView(activity),
                        R.string.team_nathan_link);
            }
        });

        // Char
        char_card.setOnClickListener(v -> {
            if (activity != null && context != null) {
                launchActivityUrl(
                        context,
                        References.getView(activity),
                        R.string.team_char_link);
            }
        });

        // Harsh
        harsh_card.setOnClickListener(v -> {
            if (activity != null && context != null) {
                launchActivityUrl(
                        context,
                        References.getView(activity),
                        R.string.team_harsh_link);
            }
        });

        // Development contributors
        development_contributors.setOnClickListener(v -> {
            assert activity != null;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setItems(getResources().getStringArray(R.array.substratum_contributors),
                    (dialog, item) -> {
                    });
            AlertDialog alert = builder.create();
            alert.show();
        });

        // Translators
        contribute.setOnClickListener(v -> {
            if (activity != null && context != null) {
                launchActivityUrl(
                        context,
                        References.getView(activity),
                        R.string.crowdin_url);
            }
        });

        // Layers contributors
        layers.setOnClickListener(v -> {
            assert activity != null;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setItems(getResources().getStringArray(R.array.layers_contributors),
                    (dialog, item) -> {
                    });
            AlertDialog alert = builder.create();
            alert.show();
        });

        translators.setOnClickListener(v -> {
            assert activity != null;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setItems(getResources().getStringArray(R.array.translations),
                    (dialog, item) -> {
                        dialog.cancel();
                        AlertDialog.Builder builder2 = new AlertDialog.Builder(activity);
                        switch (item) {
                            case 0:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.belarusian_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 1:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.czech_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 2:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.chinese_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 3:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.french_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 4:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.german_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 5:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.hungarian_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 6:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.italian_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 7:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.lithuanian_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 8:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.dutch_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 9:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.polish_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 10:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.portuguese_brazillian_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 11:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.portuguese_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 12:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.russian_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 13:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.slovak_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 14:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.spanish_translators),
                                        (dialog2, item2) -> {
                                        });
                                break;
                            case 15:
                                builder2.setItems(
                                        getResources().getStringArray(
                                                R.array.turkish_translators),
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
        return view;
    }
}