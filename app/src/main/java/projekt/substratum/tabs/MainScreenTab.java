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
            if (Arrays.asList(am.list("")).contains("system_sounds")) {
                soundsTitle.setVisibility(View.VISIBLE);
                soundsCard.setVisibility(View.VISIBLE);
            } else {
                soundsTitle.setVisibility(View.GONE);
                soundsCard.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e("SubstratumLogger", "Could not refresh list of asset folders.");
        }

        /*
        try {
            Class typeface = Class.forName("android.graphics.Typeface");
            Method recreateDefaults = typeface.getMethod("recreateDefaults");
            recreateDefaults.invoke(null, null);

            Class canvas = Class.forName("android.graphics.Canvas");
            Method freeTextLayoutCaches = canvas.getMethod("freeTextLayoutCaches");
            freeTextLayoutCaches.invoke(null, null);

            Log.e("Reflection", "Mom, I have reflected on my decisions and have decided to be a " +
                    "good kid from now on.");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Reflection", "I'm sorry, I could not reflect on this object at the given " +
                    "time...");
        }*/

        return root;
    }
}
