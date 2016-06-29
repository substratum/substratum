package projekt.substratum.tabs;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;

import projekt.substratum.InformationActivityTabs;
import projekt.substratum.R;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class MainScreenTab extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_1, null);

        String theme_pid = InformationActivityTabs.getThemePID();

        TextView bootAnimTitle = (TextView) root.findViewById(R.id.bootAnimTitle);
        CardView bootAnimCard = (CardView) root.findViewById(R.id.bootAnimCard);
        TextView fontsTitle = (TextView) root.findViewById(R.id.fontsTitle);
        CardView fontsText = (CardView) root.findViewById(R.id.fontsCard);
        TextView soundsTitle = (TextView) root.findViewById(R.id.soundsTitle);
        CardView soundsCard = (CardView) root.findViewById(R.id.soundsCard);

        try {
            Context otherContext = getContext().createPackageContext(theme_pid, 0);
            AssetManager am = otherContext.getAssets();
            if (Arrays.asList(am.list("")).contains("bootanimation")) {
                bootAnimTitle.setVisibility(View.VISIBLE);
                bootAnimCard.setVisibility(View.VISIBLE);
            } else {
                bootAnimTitle.setVisibility(View.GONE);
                bootAnimCard.setVisibility(View.GONE);
            }
            if (Arrays.asList(am.list("")).contains("fonts")) {
                fontsTitle.setVisibility(View.VISIBLE);
                fontsText.setVisibility(View.VISIBLE);
            } else {
                fontsTitle.setVisibility(View.GONE);
                fontsText.setVisibility(View.GONE);
            }
            if (Arrays.asList(am.list("")).contains("audio")) {
                soundsTitle.setVisibility(View.VISIBLE);
                soundsCard.setVisibility(View.VISIBLE);
            } else {
                soundsTitle.setVisibility(View.GONE);
                soundsCard.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e("SubstratumLogger", "Could not refresh list of asset folders.");
        }
        return root;
    }
}
