package projekt.substratum.tabs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.util.BootAnimationHandler;
import projekt.substratum.util.FontHandler;
import projekt.substratum.util.SoundsHandler;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class MainScreenTab extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_1, null);

        final String theme_pid = InformationActivity.getThemePID();

        TextView bootAnimTitle = (TextView) root.findViewById(R.id.bootAnimTitle);
        CardView bootAnimCard = (CardView) root.findViewById(R.id.bootAnimCard);
        TextView fontsTitle = (TextView) root.findViewById(R.id.fontsTitle);
        CardView fontsCard = (CardView) root.findViewById(R.id.fontsCard);
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
                fontsCard.setVisibility(View.VISIBLE);
            } else {
                fontsTitle.setVisibility(View.GONE);
                fontsCard.setVisibility(View.GONE);
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

        // Boot Animation Dialog

        bootAnimCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(getContext());
                builderSingle.setTitle(R.string.bootanimation_default_spinner);

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(),
                        R.layout.dialog_listview);
                ArrayList<String> parsedBootAnimations = new ArrayList<>();

                try {
                    Context otherContext = getContext().createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    String[] unparsedBootAnimations = am.list("bootanimation");
                    for (int i = 0; i < unparsedBootAnimations.length; i++) {
                        parsedBootAnimations.add(unparsedBootAnimations[i].substring(0,
                                unparsedBootAnimations[i].length() - 4));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("QuickApply", "There is no bootanimation.zip found within the assets " +
                            "of this theme!");
                }
                for (int i = 0; i < parsedBootAnimations.size(); i++) {
                    arrayAdapter.add(parsedBootAnimations.get(i));
                }
                builderSingle.setNegativeButton(
                        R.string.theme_information_quick_apply_dialog_negative_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builderSingle.setAdapter(
                        arrayAdapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String strName = arrayAdapter.getItem(which);
                                new BootAnimationHandler().BootAnimationHandler(strName,
                                        getContext(), theme_pid);
                            }
                        });
                builderSingle.show();
            }
        });

        // Font Dialog

        fontsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(getContext());
                builderSingle.setTitle(R.string.font_default_spinner);

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(),
                        R.layout.dialog_listview);
                ArrayList<String> unarchivedFonts = new ArrayList<>();

                try {
                    Context otherContext = getContext().createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    String[] archivedFonts = am.list("fonts");
                    for (int i = 0; i < archivedFonts.length; i++) {
                        unarchivedFonts.add(archivedFonts[i].substring(0,
                                archivedFonts[i].length() - 4));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("QuickApply", "There is no fonts.zip found within the assets " +
                            "of this theme!");
                }
                for (int i = 0; i < unarchivedFonts.size(); i++) {
                    arrayAdapter.add(unarchivedFonts.get(i));
                }
                builderSingle.setNegativeButton(
                        R.string.theme_information_quick_apply_dialog_negative_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builderSingle.setAdapter(
                        arrayAdapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String strName = arrayAdapter.getItem(which);
                                new FontHandler().FontHandler(strName, getContext(), theme_pid);
                            }
                        });
                builderSingle.show();
            }
        });

        // Sounds Dialog

        soundsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(getContext());
                builderSingle.setTitle(R.string.sounds_default_spinner);

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(),
                        R.layout.dialog_listview);
                ArrayList<String> unarchivedSounds = new ArrayList<>();

                try {
                    Context otherContext = getContext().createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    String[] archivedSounds = am.list("audio");
                    for (int i = 0; i < archivedSounds.length; i++) {
                        unarchivedSounds.add(archivedSounds[i].substring(0,
                                archivedSounds[i].length() - 4));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("QuickApply", "There is no sounds.zip found within the assets " +
                            "of this theme!");
                }
                for (int i = 0; i < unarchivedSounds.size(); i++) {
                    arrayAdapter.add(unarchivedSounds.get(i));
                }
                builderSingle.setNegativeButton(
                        R.string.theme_information_quick_apply_dialog_negative_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builderSingle.setAdapter(
                        arrayAdapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String strName = arrayAdapter.getItem(which);
                                new SoundsHandler().SoundsHandler(strName, getContext(), theme_pid);
                            }
                        });
                builderSingle.show();
            }
        });
        return root;
    }
}