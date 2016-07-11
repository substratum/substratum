package projekt.substratum.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import projekt.substratum.R;

/**
 * @author Nicholas Chum (nicholaschum)yea
 */

public class TroubleshootingFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        return inflater.inflate(R.layout.troubleshooting_fragment, null);
    }
}