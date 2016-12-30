package projekt.substratum.fragments;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.TimePicker;

import java.util.Calendar;

import projekt.substratum.R;
import projekt.substratum.config.References;

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    public static final int FLAG_START_TIME = 0;
    public static final int FLAG_END_TIME = 1;

    private static int flag = 0;

    public static void setFlag(int f) {
        flag = f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if (flag == FLAG_START_TIME) {
            final Button startTime = (Button) getActivity().findViewById(R.id.night_start_time);

            startTime.setText(References.parseTime(getActivity(), hourOfDay, minute));
            ProfileFragment.setNightProfileStart(hourOfDay, minute);
        } else if (flag == FLAG_END_TIME) {
            final Button endTime = (Button) getActivity().findViewById((R.id.night_end_time));

            endTime.setText(References.parseTime(getActivity(), hourOfDay, minute));
            ProfileFragment.setDayProfileStart(hourOfDay, minute);
        }
    }
}
