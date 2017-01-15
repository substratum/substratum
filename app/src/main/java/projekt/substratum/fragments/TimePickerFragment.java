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
import projekt.substratum.config.References;

import static projekt.substratum.fragments.ProfileFragment.dayHour;
import static projekt.substratum.fragments.ProfileFragment.nightHour;
import static projekt.substratum.fragments.ProfileFragment.dayMinute;
import static projekt.substratum.fragments.ProfileFragment.nightMinute;


public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    public static final int FLAG_START_TIME = 1;
    public static final int FLAG_END_TIME = 2;
    public static final int FLAG_GET_VALUE = 4;

    private static int flag = 0;

    public static void setFlag(int f) {
        flag = f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        if ((flag & FLAG_GET_VALUE) != 0) {
            if ((flag & FLAG_START_TIME) != 0) {
                hour = nightHour;
                minute = nightMinute;
            } else if ((flag & FLAG_END_TIME) != 0) {
                hour = dayHour;
                minute = dayMinute;
            }
        }

        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if ((flag & FLAG_START_TIME) != 0) {
            final Button startTime = (Button) getActivity().findViewById(R.id.night_start_time);
            startTime.setText(References.parseTime(getActivity(), hourOfDay, minute));
            ProfileFragment.setNightProfileStart(hourOfDay, minute);
        } else if ((flag & FLAG_END_TIME) != 0) {
            final Button endTime = (Button) getActivity().findViewById((R.id.night_end_time));
            endTime.setText(References.parseTime(getActivity(), hourOfDay, minute));
            ProfileFragment.setDayProfileStart(hourOfDay, minute);
        }
    }
}
