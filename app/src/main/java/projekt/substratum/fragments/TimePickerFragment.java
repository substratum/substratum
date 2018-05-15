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

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.TimePicker;

import java.util.Calendar;

import projekt.substratum.R;
import projekt.substratum.common.References;


public class TimePickerFragment extends DialogFragment implements
        TimePickerDialog.OnTimeSetListener {

    static final int FLAG_START_TIME = 1;
    static final int FLAG_END_TIME = 2;
    static final int FLAG_GET_VALUE = 4;

    private static int flag;

    public static void setFlag(int f) {
        flag = f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        if ((flag & FLAG_GET_VALUE) != 0) {
            if ((flag & FLAG_START_TIME) != 0) {
                hour = ProfileFragment.nightHour;
                minute = ProfileFragment.nightMinute;
            } else if ((flag & FLAG_END_TIME) != 0) {
                hour = ProfileFragment.dayHour;
                minute = ProfileFragment.dayMinute;
            }
        }

        return new TimePickerDialog(
                getActivity(),
                this,
                hour,
                minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(
            TimePicker view,
            int hourOfDay,
            int minute) {
        if ((flag & FLAG_START_TIME) != 0) {
            assert getActivity() != null;
            Button startTime = getActivity().findViewById(R.id.night_start_time);
            startTime.setText(References.parseTime(getActivity(), hourOfDay, minute));
            ProfileFragment.setNightProfileStart(hourOfDay, minute);
        } else if ((flag & FLAG_END_TIME) != 0) {
            assert getActivity() != null;
            Button endTime = getActivity().findViewById((R.id.night_end_time));
            endTime.setText(References.parseTime(getActivity(), hourOfDay, minute));
            ProfileFragment.setDayProfileStart(hourOfDay, minute);
        }
    }
}