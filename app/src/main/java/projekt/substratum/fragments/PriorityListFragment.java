/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.thesurix.gesturerecycler.GestureAdapter;
import com.thesurix.gesturerecycler.GestureManager;

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.adapters.fragments.priorities.PrioritiesInterface;
import projekt.substratum.adapters.fragments.priorities.PrioritiesItem;
import projekt.substratum.adapters.fragments.priorities.PriorityAdapter;
import projekt.substratum.common.Packages;
import projekt.substratum.common.Systems;
import projekt.substratum.common.platform.ThemeManager;

import static projekt.substratum.common.References.REFRESH_WINDOW_DELAY;

public class PriorityListFragment extends Fragment {

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.restore_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.restore_info) {
            this.showPriorityInstructions();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showPriorityInstructions() {
        new AlertDialog.Builder(this.getContext())
                .setTitle(R.string.priority_instructions_title)
                .setMessage(R.string.priority_instructions_content)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) ->
                        dialogInterface.dismiss())
                .show();
    }

    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ViewGroup root = (ViewGroup)
                inflater.inflate(R.layout.priority_list_fragment, container, false);

        this.setHasOptionsMenu(true);

        final RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
        final FloatingActionButton applyFab = root.findViewById(R.id.profile_apply_fab);
        final LinearLayoutManager manager = new LinearLayoutManager(this.getContext());
        final ProgressBar headerProgress = root.findViewById(R.id.priority_header_loading_bar);
        headerProgress.setVisibility(View.GONE);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);

        final Toolbar toolbar = root.findViewById(R.id.action_bar_toolbar);
        toolbar.setTitle(this.getString(R.string.priority_back_title));
        toolbar.setNavigationIcon(this.getContext().getDrawable(R.drawable.priorities_back_button));
        toolbar.setNavigationOnClickListener(v -> {
            final Fragment fragment = new PriorityLoaderFragment();
            final FragmentManager fm = this.getActivity().getSupportFragmentManager();
            final FragmentTransaction transaction = fm.beginTransaction();
            transaction.setCustomAnimations(
                    android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            transaction.replace(R.id.main, fragment);
            transaction.commit();
        });

        // Begin loading up list
        String obtained_key = "";
        final Bundle bundle = this.getArguments();
        if (bundle != null) {
            obtained_key = bundle.getString("package_name", null);
        }

        final List<PrioritiesInterface> prioritiesList = new ArrayList<>();
        final ArrayList<String> workable_list = new ArrayList<>();
        final List<String> overlays =
                ThemeManager.listEnabledOverlaysForTarget(this.getContext(), obtained_key);
        for (final String o : overlays) {
            prioritiesList.add(new PrioritiesItem(o,
                    Packages.getOverlayParentIcon(this.getContext(), o)));
            workable_list.add(o);
        }

        final PriorityAdapter adapter = new PriorityAdapter(
                this.getContext(), R.layout.priority_overlay_item);
        adapter.setData(prioritiesList);

        recyclerView.setAdapter(adapter);

        new GestureManager.Builder(recyclerView)
                .setManualDragEnabled(true)
                .setGestureFlags(
                        ItemTouchHelper.LEFT |
                                ItemTouchHelper.RIGHT,
                        ItemTouchHelper.UP |
                                ItemTouchHelper.DOWN)
                .build();

        adapter.setDataChangeListener(
                new GestureAdapter.OnDataChangeListener<PrioritiesInterface>() {
                    @Override
                    public void onItemRemoved(final PrioritiesInterface item, final int position) {
                    }

                    @Override
                    public void onItemReorder(
                            final PrioritiesInterface item,
                            final int fromPos,
                            final int toPos) {
                        /*
                        ==========================================================================
                        A detailed explanation of the OMS "om set-priority PACKAGE PARENT" command
                        ==========================================================================

                        1. The command accepts two variables, PACKAGE and PARENT.

                        2. PARENT can also be "highest" or "lowest" to ensure it is on top of the
                        list

                        3. When you specify a PACKAGE (e.g. com.android.systemui.Beltz), you want to
                        shift it HIGHER than the parent.

                        4. The PARENT will always be a specified value that will be an index below
                        the final result of PACKAGE, for example
                        (om set-priority com.android.systemui.Beltz com.android.systemui.Domination)

                        5. com.android.systemui.Beltz will be a HIGHER priority than
                        com.android.systemui.Domination

                        */

                        if (fromPos != toPos) {
                            final String move_package = workable_list.get(fromPos);
                            // As workable list is a simulation of the priority list without object
                            // values, we have to simulate the events such as adding above parents
                            workable_list.remove(fromPos);
                            workable_list.add(toPos, move_package);
                            applyFab.show();
                        }
                    }
                });

        applyFab.setOnClickListener(v -> {
            applyFab.hide();
            if (this.getView() != null) {
                Lunchbar.make(this.getView(),
                        this.getString(R.string.priority_success_toast),
                        Lunchbar.LENGTH_INDEFINITE)
                        .show();
            }
            headerProgress.setVisibility(View.VISIBLE);
            ThemeManager.setPriority(this.getContext(), workable_list);
            if (Packages.needsRecreate(this.getContext(), workable_list)) {
                final Handler handler = new Handler();
                handler.postDelayed(() -> {
                    // OMS may not have written all the changes so
                    // quickly just yet so we may need to have a small delay
                    try {
                        this.getActivity().recreate();
                    } catch (final Exception e) {
                        // Consume window refresh
                    }
                }, (Systems.checkAndromeda(this.getContext()) ?
                        REFRESH_WINDOW_DELAY :
                        (REFRESH_WINDOW_DELAY << 1)));
            }
        });
        return root;
    }
}