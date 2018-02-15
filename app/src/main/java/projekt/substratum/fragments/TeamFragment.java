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

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.databinding.TeamFragmentBinding;

import static projekt.substratum.common.Activities.launchActivityUrl;

public class TeamFragment extends Fragment {

    CardView nicholasCard;
    CardView sykoCard;
    CardView ivanCard;
    CardView surgeCard;
    CardView georgeCard;
    CardView nathanCard;
    CardView charCard;
    CardView harshCard;
    Button developmentContributors;
    Button contribute;
    Button layers;
    Button translators;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TeamFragmentBinding viewBinding =
                DataBindingUtil.inflate(inflater, R.layout.team_fragment, container, false);
        View view = viewBinding.getRoot();
        Context context = getContext();
        nicholasCard = viewBinding.nicholasCard;
        sykoCard = viewBinding.sykoCard;
        ivanCard = viewBinding.ivanCard;
        surgeCard = viewBinding.surgeCard;
        georgeCard = viewBinding.georgeCard;
        nathanCard = viewBinding.nathanCard;
        charCard = viewBinding.charCard;
        harshCard = viewBinding.harshCard;
        developmentContributors = viewBinding.listButtonContributors;
        contribute = viewBinding.listButtonTranslatorsContribute;
        layers = viewBinding.listButtonLayers;
        translators = viewBinding.listButtonTranslators;

        if (context != null) {
            // Nicholas
            nicholasCard.setOnClickListener(v -> {
                if (getActivity() != null) {
                    launchActivityUrl(
                            context,
                            References.getView(getActivity()),
                            R.string.team_nicholas_link);
                }
            });

            // Mike
            sykoCard.setOnClickListener(v -> {
                if (getActivity() != null) {
                    launchActivityUrl(
                            context,
                            References.getView(getActivity()),
                            R.string.team_syko_link);
                }
            });

            // Ivan
            ivanCard.setOnClickListener(v -> {
                if (getActivity() != null) {
                    launchActivityUrl(
                            context,
                            References.getView(getActivity()),
                            R.string.team_ivan_link);
                }
            });

            // Surge
            surgeCard.setOnClickListener(v -> {
                if (getActivity() != null) {
                    launchActivityUrl(
                            context,
                            References.getView(getActivity()),
                            R.string.team_surge_link);
                }
            });

            // George
            georgeCard.setOnClickListener(v -> {
                if (getActivity() != null) {
                    launchActivityUrl(
                            context,
                            References.getView(getActivity()),
                            R.string.team_george_link);
                }
            });

            // Nathan
            nathanCard.setOnClickListener(v -> {
                if (getActivity() != null) {
                    launchActivityUrl(
                            context,
                            References.getView(getActivity()),
                            R.string.team_nathan_link);
                }
            });

            // Char
            charCard.setOnClickListener(v -> {
                if (getActivity() != null) {
                    launchActivityUrl(
                            context,
                            References.getView(getActivity()),
                            R.string.team_char_link);
                }
            });

            // Harsh
            harshCard.setOnClickListener(v -> {
                if (getActivity() != null) {
                    launchActivityUrl(
                            context,
                            References.getView(getActivity()),
                            R.string.team_harsh_link);
                }
            });

            // Development contributors
            developmentContributors.setOnClickListener(v -> {
                assert getActivity() != null;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setItems(getResources().getStringArray(R.array.substratum_contributors),
                        (dialog, item) -> {
                        });
                AlertDialog alert = builder.create();
                alert.show();
            });

            // Translators
            contribute.setOnClickListener(v -> {
                if (getActivity() != null) {
                    launchActivityUrl(
                            context,
                            References.getView(getActivity()),
                            R.string.crowdin_url);
                }
            });

            // Layers contributors
            layers.setOnClickListener(v -> {
                assert getActivity() != null;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setItems(getResources().getStringArray(R.array.layers_contributors),
                        (dialog, item) -> {
                        });
                AlertDialog alert = builder.create();
                alert.show();
            });

            translators.setOnClickListener(v -> {
                assert getActivity() != null;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setItems(getResources().getStringArray(R.array.translations),
                        (dialog, item) -> {
                            dialog.cancel();
                            AlertDialog.Builder builder2 = new AlertDialog.Builder(getActivity());
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
        }
        return view;
    }
}