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

package projekt.substratum.adapters.fragments.troubleshooting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import projekt.substratum.R;

public class TroubleshootingAdapter extends BaseAdapter {

    private final int[] tQues;
    private final int[] tAns;
    private final Context context;

    public TroubleshootingAdapter(int[] tQues, int[] tAns, Context context) {
        super();
        this.tQues = tQues;
        this.tAns = tAns;
        this.context = context;
    }

    @Override
    public int getCount() {
        return this.tQues.length;
    }

    @Override
    public Object getItem(int i) {
        return this.tQues[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) this.context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View v = null;
        if (inflater != null) {
            v = inflater.inflate(R.layout.troubleshooting_row, viewGroup, false);

            TextView questionsTextView = v.findViewById(R.id.trouble_ques);
            TextView answersTextView = v.findViewById(R.id.trouble_ans);

            String question = getStringFromResource(this.tQues[i]);
            String answer = getStringFromResource(this.tAns[i]);

            questionsTextView.setText(question);
            answersTextView.setText(answer);
        }
        return v;
    }

    private String getStringFromResource(int stringId) {
        return this.context.getResources().getString(stringId);
    }
}