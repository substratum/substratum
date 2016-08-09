package projekt.substratum.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.thesurix.gesturerecycler.GestureAdapter;
import com.thesurix.gesturerecycler.GestureManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.adapters.PrioritiesAdapter;
import projekt.substratum.config.References;
import projekt.substratum.model.Priorities;
import projekt.substratum.model.PrioritiesItem;
import projekt.substratum.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class PriorityListFragment extends Fragment {

    private String commands;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ViewGroup root = (ViewGroup) inflater.inflate(R.layout.priority_list_fragment,
                null);
        final RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        final FloatingActionButton applyFab = (FloatingActionButton) root.findViewById(R.id
                .profile_apply_fab);
        final LinearLayoutManager manager = new LinearLayoutManager(getContext());
        final ProgressBar headerProgress = (ProgressBar) root.findViewById(R.id
                .priority_header_loading_bar);
        headerProgress.setVisibility(View.GONE);

        applyFab.hide();

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);

        Toolbar toolbar = (Toolbar) root.findViewById(R.id.action_bar_toolbar);
        toolbar.setTitle(getString(R.string.priority_back_title));
        toolbar.setNavigationIcon(getContext().getDrawable(R.drawable.priorities_back_button));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = new PriorityLoaderFragment();
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim
                        .slide_out_right);
                transaction.replace(R.id.main, fragment);
                transaction.commit();
            }
        });

        // Begin loading up list

        String obtained_key = "";
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            obtained_key = bundle.getString("package_name", null);
        }

        final List<PrioritiesItem> prioritiesList = new ArrayList<>();
        final List<String> workable_list = new ArrayList<>();
        commands = "";
        Process nativeApp = null;
        try {
            nativeApp = Runtime.getRuntime().exec("om list");

            try (OutputStream stdin = nativeApp.getOutputStream();
                 InputStream stderr = nativeApp.getErrorStream();
                 InputStream stdout = nativeApp.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(stdout))) {
                String line;

                stdin.write(("ls\n").getBytes());
                stdin.write("exit\n".getBytes());

                String current_header = "";
                while ((line = br.readLine()) != null) {
                    if (line.length() > 0) {
                        if (line.equals(obtained_key)) {
                            current_header = line;
                        } else {
                            if (current_header.equals(obtained_key)) {
                                if (line.contains("[x]")) {
                                    prioritiesList.add(new Priorities(line.substring(8),
                                            References.grabAppIcon(getContext(),
                                                    current_header)));
                                    workable_list.add(line.substring(8));
                                } else if (!line.contains("[ ]")) {
                                    break;
                                }
                            }
                        }
                    }
                }

                try (BufferedReader br1 = new BufferedReader(new InputStreamReader(stderr))) {
                    while ((line = br1.readLine()) != null) {
                        Log.e("PriorityListFragment", line);
                    }
                }
            }
        } catch (IOException ioe) {
            Log.e("PriorityListFragment", "There was an issue regarding loading the priorities of" +
                    " " +
                    "each overlay.");
        } finally {
            if (nativeApp != null) {
                // destroy the Process explicitly
                nativeApp.destroy();
            }
        }

        final PrioritiesAdapter adapter = new PrioritiesAdapter(getContext(), R.layout.linear_item);
        adapter.setData(prioritiesList);

        recyclerView.setAdapter(adapter);

        new GestureManager.Builder(recyclerView)
                .setManualDragEnabled(true)
                .setGestureFlags(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, ItemTouchHelper.UP
                        | ItemTouchHelper.DOWN)
                .build();

        adapter.setDataChangeListener(new GestureAdapter.OnDataChangeListener<PrioritiesItem>() {
            @Override
            public void onItemRemoved(final PrioritiesItem item, final int position) {
            }

            @Override
            public void onItemReorder(final PrioritiesItem item, final int fromPos, final int
                    toPos) {
                /*
                ==========================================================================
                A detailed explanation of the OMS "om set-priority PACKAGE PARENT" command
                ==========================================================================

                1. The command accepts two variables, PACKAGE and PARENT.

                2. PARENT can also be "highest" or "lowest" to ensure it is on top of the list

                3. When you specify a PACKAGE (e.g. com.android.systemui.Beltz), you want to shift
                it HIGHER than the parent.

                4. The PARENT will always be a specified value that will be an index below the final
                result of PACKAGE, for example (om set-priority com.android.systemui.Beltz
                com.android.systemui.Domination)

                5. com.android.systemui.Beltz will be a HIGHER priority than
                com.android.systemui.Domination

                */

                if (commands.length() == 0) {
                    String move_package = workable_list.get(fromPos);
                    commands = "om set-priority " + move_package + " " + workable_list.get(toPos);
                    // As workable list is a simulation of the priority list without object
                    // values, we have to simulate the events such as adding above parents
                    workable_list.remove(fromPos);
                    workable_list.add(toPos, move_package);
                    applyFab.show();
                } else {
                    String move_package = workable_list.get(fromPos);
                    commands = commands + " && om set-priority " + move_package + " " +
                            workable_list.get(toPos);
                    // As workable list is a simulation of the priority list without object
                    // values, we have to simulate the events such as adding above parents
                    workable_list.remove(fromPos);
                    workable_list.add(toPos, move_package);
                }
            }
        });

        applyFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast toast = Toast.makeText(getContext(), getString(R.string
                                .priority_success_toast),
                        Toast.LENGTH_SHORT);
                toast.show();
                headerProgress.setVisibility(View.VISIBLE);
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                if (References.isPackageInstalled(getContext(),
                                        "masquerade.substratum")) {
                                    final SharedPreferences prefs =
                                            PreferenceManager.getDefaultSharedPreferences(
                                                    getContext());
                                    if (!prefs.getBoolean("systemui_recreate", false)) {
                                        if (commands.contains("systemui")) {
                                            commands = commands + " && pkill -f com.android" +
                                                    ".systemui";
                                        }
                                    }
                                    Intent runCommand = new Intent();
                                    runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                                    runCommand.setAction("masquerade.substratum.COMMANDS");
                                    runCommand.putExtra("om-commands", commands);
                                    getContext().sendBroadcast(runCommand);
                                } else {
                                    Root.runCommand(commands);
                                }
                            }
                        },
                        500
                );
            }
        });

        return root;
    }
}