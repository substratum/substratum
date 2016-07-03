package projekt.substratum.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import projekt.substratum.R;

/**
 * Created by Nicholas on 2016-03-31.
 */
public class TroubleshootingFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.troubleshooting_fragment, null);


        return root;
    }
}