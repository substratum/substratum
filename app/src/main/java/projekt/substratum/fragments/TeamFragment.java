package projekt.substratum.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.wangyuwei.galleryview.GalleryEntity;
import me.wangyuwei.galleryview.GalleryView;
import projekt.substratum.R;
import projekt.substratum.easteregg.LLandActivity;

public class TeamFragment extends Fragment {

    private List<GalleryView> contributors = new ArrayList<>();
    private List<GalleryView> developers = new ArrayList<>();
    private List<GalleryView> themers = new ArrayList<>();
    private List<GalleryEntity> contributorEntities = new ArrayList<>();
    private List<GalleryEntity> developerEntities = new ArrayList<>();
    private List<GalleryEntity> themerEntities = new ArrayList<>();

    private boolean flipContributors = false;
    private boolean flipDevelopers = false;
    private boolean flipThemers = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.team_fragment, container, false);

        themers.add((GalleryView) root.findViewById(R.id.designer1));
        themers.add((GalleryView) root.findViewById(R.id.designer2));
        themers.add((GalleryView) root.findViewById(R.id.designer3));

        GalleryEntity branden = new GalleryEntity();
        branden.imgUrl = getString(R.string.team_branden_link);
        branden.title = getString(R.string.team_branden);
        themerEntities.add(branden);
        GalleryEntity ivan = new GalleryEntity();
        ivan.imgUrl = getString(R.string.team_ivan_link);
        ivan.title = getString(R.string.team_ivan);
        themerEntities.add(ivan);
        themers.get(0).addGalleryData(themerEntities);
        themerEntities.clear();

        GalleryEntity dave = new GalleryEntity();
        dave.imgUrl = getString(R.string.team_dave_link);
        dave.title = getString(R.string.team_dave);
        themerEntities.add(dave);
        GalleryEntity jeremy = new GalleryEntity();
        jeremy.imgUrl = getString(R.string.team_jeremy_link);
        jeremy.title = getString(R.string.team_jeremy);
        themerEntities.add(jeremy);
        themers.get(1).addGalleryData(themerEntities);
        themerEntities.clear();

        GalleryEntity dejan = new GalleryEntity();
        dejan.imgUrl = getString(R.string.team_dejan_link);
        dejan.title = getString(R.string.team_dejan);
        themerEntities.add(dejan);
        GalleryEntity jimmy = new GalleryEntity();
        jimmy.imgUrl = getString(R.string.team_jimmy_link);
        jimmy.title = getString(R.string.team_jimmy);
        themerEntities.add(jimmy);
        themers.get(2).addGalleryData(themerEntities);
        themerEntities.clear();

        Timer timer = new Timer();
        timer.schedule(new FlipTheThemers(), 0, 3000);

        // Begin Developers

        developers.add((GalleryView) root.findViewById(R.id.developer1));
        developers.add((GalleryView) root.findViewById(R.id.developer2));
        developers.add((GalleryView) root.findViewById(R.id.developer3));

        GalleryEntity cory = new GalleryEntity();
        cory.imgUrl = getString(R.string.team_cory_link);
        cory.title = getString(R.string.team_cory);
        developerEntities.add(cory);
        ivan = new GalleryEntity();
        ivan.imgUrl = getString(R.string.team_ivan_link);
        ivan.title = getString(R.string.team_ivan);
        developerEntities.add(ivan);
        developers.get(0).addGalleryData(developerEntities);
        developerEntities.clear();

        GalleryEntity george = new GalleryEntity();
        george.imgUrl = getString(R.string.team_george_link);
        george.title = getString(R.string.team_george);
        developerEntities.add(george);
        GalleryEntity raja = new GalleryEntity();
        raja.imgUrl = getString(R.string.team_raja_link);
        raja.title = getString(R.string.team_raja);
        developerEntities.add(raja);
        developers.get(1).addGalleryData(developerEntities);
        developerEntities.clear();

        GalleryEntity jacob = new GalleryEntity();
        jacob.imgUrl = getString(R.string.team_jacob_link);
        jacob.title = getString(R.string.team_jacob);
        developerEntities.add(jacob);
        GalleryEntity surge = new GalleryEntity();
        surge.imgUrl = getString(R.string.team_surge_link);
        surge.title = getString(R.string.team_surge);
        developerEntities.add(surge);
        developers.get(2).addGalleryData(developerEntities);
        developerEntities.clear();

        Timer timer2 = new Timer();
        timer2.schedule(new FlipTheDevelopers(), 0, 3000);

        // Begin Contributors

        contributors.add((GalleryView) root.findViewById(R.id.contributor1));
        contributors.add((GalleryView) root.findViewById(R.id.contributor2));
        contributors.add((GalleryView) root.findViewById(R.id.contributor3));

        GalleryEntity ben = new GalleryEntity();
        ben.imgUrl = getString(R.string.contributor_ben_link);
        ben.title = getString(R.string.contributor_ben);
        contributorEntities.add(ben);
        GalleryEntity idan = new GalleryEntity();
        idan.imgUrl = getString(R.string.contributor_idan_link);
        idan.title = getString(R.string.contributor_idan);
        contributorEntities.add(idan);
        contributors.get(0).addGalleryData(contributorEntities);
        contributorEntities.clear();

        GalleryEntity char_g = new GalleryEntity();
        char_g.imgUrl = getString(R.string.contributor_char_link);
        char_g.title = getString(R.string.contributor_char);
        contributorEntities.add(char_g);
        GalleryEntity sajid = new GalleryEntity();
        sajid.imgUrl = getString(R.string.contributor_sajid_link);
        sajid.title = getString(R.string.contributor_sajid);
        contributorEntities.add(sajid);
        contributors.get(1).addGalleryData(contributorEntities);
        contributorEntities.clear();

        GalleryEntity nathan = new GalleryEntity();
        nathan.imgUrl = getString(R.string.contributor_nathan_link);
        nathan.title = getString(R.string.contributor_nathan);
        contributorEntities.add(nathan);
        GalleryEntity travis = new GalleryEntity();
        travis.imgUrl = getString(R.string.contributor_travis_link);
        travis.title = getString(R.string.contributor_travis);
        contributorEntities.add(travis);
        contributors.get(2).addGalleryData(contributorEntities);
        contributorEntities.clear();

        Timer timer3 = new Timer();
        timer3.schedule(new FlipTheContributors(), 0, 3000);

        Button layers = (Button) root.findViewById(R.id.list_button_layers);
        layers.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setItems(getResources().getStringArray(R.array.layers_contributors),
                    (dialog, item) -> {
                    });
            builder.setPositiveButton(R.string.dialog_ok, (dialog, item) -> dialog.cancel());
            AlertDialog alert = builder.create();
            alert.show();
        });
        layers.setOnLongClickListener(v -> {
            try {
                Intent intent = new Intent(getActivity(), LLandActivity.class);
                startActivity(intent);
            } catch (ActivityNotFoundException activityNotFoundException) {
                // Suppress warning
            }
            return true;
        });
        return root;
    }

    private class FlipTheThemers extends TimerTask {
        public void run() {
            if (flipThemers) {
                startSmooth();
            }
            flipThemers = !flipThemers;
        }

        private void startSmooth() {
            for (int i = 0; i < themers.size(); i++) {
                final int index = i;
                themers.get(i).postDelayed(() -> themers.get(index).startSmooth(), 100 * i);
            }
        }
    }

    private class FlipTheDevelopers extends TimerTask {
        public void run() {
            if (flipDevelopers) {
                startSmooth();
            }
            flipDevelopers = !flipDevelopers;
        }

        private void startSmooth() {
            for (int i = 0; i < developers.size(); i++) {
                final int index = i;
                developers.get(i).postDelayed(() -> developers.get(index).startSmooth(), 100 * i);
            }
        }
    }

    private class FlipTheContributors extends TimerTask {
        public void run() {
            if (flipContributors) {
                startSmooth();
            }
            flipContributors = !flipContributors;
        }

        private void startSmooth() {
            for (int i = 0; i < contributors.size(); i++) {
                final int index = i;
                contributors.get(i).postDelayed(() -> contributors.get(index).startSmooth(), 100 * i);
            }
        }
    }
}