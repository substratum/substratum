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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import projekt.substratum.R;
import projekt.substratum.adapters.fragments.troubleshooting.TroubleshootingAdapter;

public class TroubleshootingFragment extends Fragment {

    ListView troubleshootListView;

    int[] troubleshootQuestions = {
            R.string.question_one,
            R.string.question_two,
            R.string.question_three,
            R.string.question_four,
            R.string.question_five,
            R.string.question_six,
            R.string.question_seven,
            R.string.question_eight,
            R.string.question_nine,
            R.string.question_ten,
            R.string.question_eleven,
            R.string.question_twelve
    };

    int[] troubleshootAnswers = {
            R.string.answer_one,
            R.string.answer_two,
            R.string.answer_three,
            R.string.answer_four,
            R.string.answer_five,
            R.string.answer_six,
            R.string.answer_seven,
            R.string.answer_eight,
            R.string.answer_nine,
            R.string.answer_ten,
            R.string.answer_eleven,
            R.string.answer_twelve
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View root = inflater.inflate(R.layout.troubleshooting_fragment, container, false);

        troubleshootListView = (ListView) root.findViewById(R.id.troubleshoot_list_view);

        // Make sure troubleshootQuestions & troubleshootAnswers are of same
        // length and then assign adapter to listView
        troubleshootListView.setAdapter(
                new TroubleshootingAdapter(
                        troubleshootQuestions,
                        troubleshootAnswers,
                        getActivity().getApplicationContext()
                ));

        // This avoids from having a janky look with the last card getting cut off
        View footer = LayoutInflater.from(
                getActivity()).inflate(R.layout.list_footer, troubleshootListView, false);
        troubleshootListView.addFooterView(footer);
        troubleshootListView.setFooterDividersEnabled(false);
        footer.setOnClickListener(null);
        return root;
    }
}