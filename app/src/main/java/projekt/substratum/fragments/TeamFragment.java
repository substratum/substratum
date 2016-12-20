package projekt.substratum.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import projekt.substratum.R;
import projekt.substratum.easteregg.LLandActivity;

public class TeamFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.team_fragment, container, false);

        // Begin Team Leaders

        CardView nicholas = (CardView) root.findViewById(R.id.nicholas);
        nicholas.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_nicholas_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView syko = (CardView) root.findViewById(R.id.syko);
        syko.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_syko_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView george = (CardView) root.findViewById(R.id.george);
        george.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_george_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView cory = (CardView) root.findViewById(R.id.cory);
        cory.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_cory_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        // Begin Team Themers

        CardView branden = (CardView) root.findViewById(R.id.branden);
        branden.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_branden_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView dave = (CardView) root.findViewById(R.id.dave);
        dave.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_dave_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView jeremy = (CardView) root.findViewById(R.id.jeremy);
        jeremy.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_jeremy_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView jimmy = (CardView) root.findViewById(R.id.jimmy);
        jimmy.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_jimmy_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        // Begin Contributors

        CardView ben = (CardView) root.findViewById(R.id.ben);
        ben.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.contributor_ben_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });
        ben.setOnLongClickListener(v -> {
            try {
                Intent intent = new Intent(getActivity(), LLandActivity.class);
                startActivity(intent);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
            return true;
        });

        CardView charcat = (CardView) root.findViewById(R.id.charcat);
        charcat.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.contributor_char_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView nathan = (CardView) root.findViewById(R.id.nathan);
        nathan.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.contributor_nathan_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        // Begin Development Contributors

        CardView ivan = (CardView) root.findViewById(R.id.ivan);
        ivan.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_ivan_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView jacob = (CardView) root.findViewById(R.id.jacob);
        jacob.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_jacob_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView raja = (CardView) root.findViewById(R.id.raja);
        raja.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_raja_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView surge = (CardView) root.findViewById(R.id.surge);
        surge.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.team_surge_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        // Begin Designers

        CardView idan = (CardView) root.findViewById(R.id.idan);
        idan.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.contributor_idan_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView mihir = (CardView) root.findViewById(R.id.mihir);
        mihir.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.contributor_mihir_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView sajid = (CardView) root.findViewById(R.id.sajid);
        sajid.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.contributor_sajid_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView travis = (CardView) root.findViewById(R.id.travis);
        travis.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.contributor_travis_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        // Begin Translators

        CardView chrys = (CardView) root.findViewById(R.id.chrys);
        chrys.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.translator_chrys_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView david = (CardView) root.findViewById(R.id.david);
        david.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.translator_david_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView gautham = (CardView) root.findViewById(R.id.gautham);
        gautham.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.translator_gautham_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView gediminas = (CardView) root.findViewById(R.id.gediminas);
        gediminas.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.translator_gediminas_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView jorge = (CardView) root.findViewById(R.id.jorge);
        jorge.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.translator_jorge_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView kevin = (CardView) root.findViewById(R.id.kevin);
        kevin.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.translator_kevin_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView nils = (CardView) root.findViewById(R.id.nils);
        nils.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.translator_nils_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView wasita = (CardView) root.findViewById(R.id.wasita);
        wasita.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.translator_wasita_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        // Begin Layers Crew

        CardView reinhard = (CardView) root.findViewById(R.id.reinhard);
        reinhard.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.contributor_reinhard_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView brian = (CardView) root.findViewById(R.id.brian);
        brian.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.contributor_brian_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView aldrin = (CardView) root.findViewById(R.id.aldrin);
        aldrin.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.contributor_aldrin_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView steve = (CardView) root.findViewById(R.id.steve);
        steve.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.contributor_steve_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView niklas = (CardView) root.findViewById(R.id.niklas);
        niklas.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.contributor_niklas_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView andrzej = (CardView) root.findViewById(R.id.andrzej);
        andrzej.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.contributor_andrzej_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        CardView denis = (CardView) root.findViewById(R.id.denis);
        denis.setOnClickListener(v -> {
            try {
                String playURL = getString(R.string.contributor_denis_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            } catch (ActivityNotFoundException activityNotFoundException) {
                //
            }
        });

        return root;
    }
}