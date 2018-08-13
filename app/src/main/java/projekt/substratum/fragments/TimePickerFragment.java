/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.fragments;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.TimePicker;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import projekt.substratum.R;
import projekt.substratum.common.References;

import java.util.Calendar;


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