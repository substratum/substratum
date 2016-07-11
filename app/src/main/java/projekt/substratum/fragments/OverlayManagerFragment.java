package projekt.substratum.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gordonwong.materialsheetfab.MaterialSheetFab;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.adapters.OverlayManagerAdapter;
import projekt.substratum.model.OverlayManager;
import projekt.substratum.util.FloatingActionMenu;
import projekt.substratum.util.ReadOverlaysFile;
import projekt.substratum.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class OverlayManagerFragment extends Fragment {

    private RecyclerView.Adapter mAdapter;
    private MaterialSheetFab materialSheetFab;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.overlay_manager, null);
        RelativeLayout relativeLayout = (RelativeLayout) root.findViewById(R.id
                .no_overlays_enabled);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getContext());

        List<OverlayManager> overlaysList = new ArrayList<>();

        File current_overlays = new File(Environment
                .getExternalStorageDirectory().getAbsolutePath() +
                "/.substratum/current_overlays.xml");
        if (current_overlays.exists()) {
            Root.runCommand("rm " + Environment
                    .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");
        }
        Root.runCommand("cp /data/system/overlays.xml " +
                Environment
                        .getExternalStorageDirectory().getAbsolutePath() +
                "/.substratum/current_overlays.xml");
        String[] commands = {Environment.getExternalStorageDirectory()
                .getAbsolutePath() +
                "/.substratum/current_overlays.xml", "4"};
        String[] commands1 = {Environment.getExternalStorageDirectory()
                .getAbsolutePath() +
                "/.substratum/current_overlays.xml", "5"};

        ArrayList<String> activated_overlays;

        if (prefs.getBoolean("manager_disabled_overlays", true)) {
            List<String> state4 = ReadOverlaysFile.main(commands);
            List<String> state5 = ReadOverlaysFile.main(commands1);
            activated_overlays = new ArrayList<>(state4);
            activated_overlays.addAll(state5);

            Collections.sort(activated_overlays);

            for (int i = 0; i < activated_overlays.size(); i++) {
                if (state4.contains(activated_overlays.get(i))) {
                    OverlayManager st = new OverlayManager(getContext(), activated_overlays.get(i),
                            false);
                    overlaysList.add(st);
                } else {
                    if (state5.contains(activated_overlays.get(i))) {
                        OverlayManager st = new OverlayManager(getContext(), activated_overlays.get
                                (i), true);
                        overlaysList.add(st);
                    }
                }
            }
        } else {
            List<String> state5 = ReadOverlaysFile.main(commands1);
            activated_overlays = new ArrayList<>(state5);

            Collections.sort(activated_overlays);

            for (int i = 0; i < activated_overlays.size(); i++) {
                if (state5.contains(activated_overlays.get(i))) {
                    OverlayManager st = new OverlayManager(getContext(), activated_overlays.get
                            (i), true);
                    overlaysList.add(st);
                }
            }
        }

        RecyclerView mRecyclerView = (RecyclerView) root.findViewById(R.id.overlays_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new OverlayManagerAdapter(overlaysList);
        mRecyclerView.setAdapter(mAdapter);

        View sheetView = root.findViewById(R.id.fab_sheet);
        View overlay = root.findViewById(R.id.overlay);
        int sheetColor = getContext().getColor(R.color.fab_menu_background_card);
        int fabColor = getContext().getColor(R.color.colorAccent);

        final FloatingActionMenu floatingActionButton = (FloatingActionMenu) root.findViewById(R
                .id.apply_fab);
        floatingActionButton.show();

        // Create material sheet FAB
        if (sheetView != null && overlay != null) {
            materialSheetFab = new MaterialSheetFab<>(floatingActionButton, sheetView, overlay,
                    sheetColor, fabColor);
        }

        if (activated_overlays.size() == 0) {
            floatingActionButton.hide();
            relativeLayout.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            floatingActionButton.show();
            relativeLayout.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }

        TextView disable_selected = (TextView) root.findViewById(R.id.disable_selected);
        if (disable_selected != null)
            disable_selected.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    materialSheetFab.hideSheet();
                    String data = "om disable";
                    List<OverlayManager> overlayList = ((OverlayManagerAdapter) mAdapter)
                            .getOverlayManagerList();
                    for (int i = 0; i < overlayList.size(); i++) {
                        OverlayManager overlay = overlayList.get(i);
                        if (overlay.isSelected()) {
                            data = data + " " + overlay.getName();
                        }
                    }
                    Toast toast = Toast.makeText(getContext(), getString(R
                                    .string.toast_disabled),
                            Toast.LENGTH_LONG);
                    toast.show();
                    if (isPackageInstalled("projekt.substratum.helper")) {
                        Intent runCommand = new Intent();
                        runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        runCommand.setAction("projekt.substratum.helper.COMMANDS");
                        runCommand.putExtra("om-commands", data);
                        getContext().sendBroadcast(runCommand);
                    } else {
                        Root.runCommand(data);
                    }
                }
            });

        TextView enable_selected = (TextView) root.findViewById(R.id.enable_selected);
        if (enable_selected != null) enable_selected.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                materialSheetFab.hideSheet();
                String data = "om enable";
                List<OverlayManager> overlayList = ((OverlayManagerAdapter) mAdapter)
                        .getOverlayManagerList();
                for (int i = 0; i < overlayList.size(); i++) {
                    OverlayManager overlay = overlayList.get(i);
                    if (overlay.isSelected()) {
                        data = data + " " + overlay.getName();
                    }
                }
                Toast toast = Toast.makeText(getContext(), getString(R
                                .string.toast_enabled),
                        Toast.LENGTH_LONG);
                toast.show();
                if (isPackageInstalled("projekt.substratum.helper")) {
                    Intent runCommand = new Intent();
                    runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    runCommand.setAction("projekt.substratum.helper.COMMANDS");
                    runCommand.putExtra("om-commands", data);
                    getContext().sendBroadcast(runCommand);
                } else {
                    Root.runCommand(data);
                }
            }
        });
        if (!prefs.getBoolean("manager_disabled_overlays", true)) {
            LinearLayout enable_view = (LinearLayout) root.findViewById(R.id.enable);
            enable_view.setVisibility(View.GONE);
        }
        return root;
    }

    private boolean isPackageInstalled(String package_name) {
        PackageManager pm = getContext().getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(package_name)) {
                return true;
            }
        }
        return false;
    }

}