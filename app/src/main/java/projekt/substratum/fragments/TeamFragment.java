package projekt.substratum.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import projekt.substratum.R;

/**
 * Created by Nicholas on 2016-03-31.
 */
public class TeamFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.team_fragment, null);

        CardView nicholas = (CardView) root.findViewById(R.id.nicholas);
        nicholas.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_nicholas_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView syko = (CardView) root.findViewById(R.id.syko);
        syko.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_syko_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView george = (CardView) root.findViewById(R.id.george);
        george.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_george_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView cory = (CardView) root.findViewById(R.id.cory);
        cory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_cory_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView ivan = (CardView) root.findViewById(R.id.ivan);
        ivan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_ivan_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView jacob = (CardView) root.findViewById(R.id.jacob);
        jacob.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_jacob_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView raja = (CardView) root.findViewById(R.id.raja);
        raja.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_raja_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView branden = (CardView) root.findViewById(R.id.branden);
        branden.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_branden_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView dave = (CardView) root.findViewById(R.id.dave);
        dave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_dave_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView jeremy = (CardView) root.findViewById(R.id.jeremy);
        jeremy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_jeremy_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView jimmy = (CardView) root.findViewById(R.id.jimmy);
        jimmy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.team_jimmy_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView idan = (CardView) root.findViewById(R.id.idan);
        idan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.contributor_idan_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView mihir = (CardView) root.findViewById(R.id.mihir);
        mihir.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.contributor_mihir_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView nathan = (CardView) root.findViewById(R.id.nathan);
        nathan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.contributor_nathan_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView sajid = (CardView) root.findViewById(R.id.sajid);
        sajid.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.contributor_sajid_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView travis = (CardView) root.findViewById(R.id.travis);
        travis.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.contributor_travis_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView reinhard = (CardView) root.findViewById(R.id.reinhard);
        reinhard.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.contributor_reinhard_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView brian = (CardView) root.findViewById(R.id.brian);
        brian.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.contributor_brian_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView aldrin = (CardView) root.findViewById(R.id.aldrin);
        aldrin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.contributor_aldrin_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView steve = (CardView) root.findViewById(R.id.steve);
        steve.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.contributor_steve_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView niklas = (CardView) root.findViewById(R.id.niklas);
        niklas.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.contributor_niklas_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView andrzej = (CardView) root.findViewById(R.id.andrzej);
        andrzej.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.contributor_andrzej_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        CardView denis = (CardView) root.findViewById(R.id.denis);
        denis.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String playURL = getString(R.string.contributor_denis_link);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(playURL));
                startActivity(i);
            }
        });

        return root;
    }
}