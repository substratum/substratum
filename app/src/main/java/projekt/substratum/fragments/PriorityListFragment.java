/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.fragments;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.thesurix.gesturerecycler.GestureAdapter;
import com.thesurix.gesturerecycler.GestureManager;
import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.adapters.fragments.priorities.PrioritiesInterface;
import projekt.substratum.adapters.fragments.priorities.PrioritiesItem;
import projekt.substratum.adapters.fragments.priorities.PriorityAdapter;
import projekt.substratum.common.Internal;
import projekt.substratum.common.Packages;
import projekt.substratum.common.Systems;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.databinding.PriorityListFragmentBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static projekt.substratum.common.References.REFRESH_WINDOW_DELAY;
import static projekt.substratum.common.platform.ThemeManager.listEnabledOverlaysForTarget;

public class PriorityListFragment extends Fragment {

    private FloatingActionButton applyFab;
    private ProgressBar headerProgress;
    private Context context;
    private MainActivity activity;

    /**
     * Creating the options menu (3dot overflow menu)
     *
     * @param menu     Menu object
     * @param inflater The inflated menu object
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.restore_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Assign actions to every option when they are selected
     *
     * @param item Object of menu item
     * @return True, if something has changed.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.restore_info) {
            showPriorityInstructions();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Show dialog with instructions on adjusting priorities
     */
    private void showPriorityInstructions() {
        new AlertDialog.Builder(context)
                .setTitle(R.string.priority_instructions_title)
                .setMessage(R.string.priority_instructions_content)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) ->
                        dialogInterface.dismiss())
                .show();
    }

    /**
     * Go back in the fragment stack of the priority system
     */
    private void onBackPressed() {
        Fragment fragment = new PriorityLoaderFragment();
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        transaction.replace(R.id.main, fragment);
        transaction.commit();
        activity.switchToDefaultToolbarText();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();

        PriorityListFragmentBinding viewBinding =
                DataBindingUtil.inflate(inflater, R.layout.priority_list_fragment, container, false);
        View view = viewBinding.getRoot();
        headerProgress = viewBinding.priorityHeaderLoadingBar;
        RecyclerView recyclerView = viewBinding.recyclerView;
        applyFab = viewBinding.profileApplyFab;

        setHasOptionsMenu(true);
        LinearLayoutManager manager = new LinearLayoutManager(context);
        headerProgress.setVisibility(View.GONE);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);

        // Modify the toolbar
        if ((getActivity()) != null) {
            ((MainActivity) getActivity()).switchToPriorityListToolbar(v -> onBackPressed());
        }

        // Begin loading up list
        String obtainedKey = "";
        Bundle bundle = getArguments();
        if (bundle != null) {
            obtainedKey = bundle.getString(Internal.THEME_PACKAGE, null);
        }

        List<PrioritiesInterface> prioritiesList = new ArrayList<>();
        ArrayList<String> workableList = new ArrayList<>();
        List<String> overlays = listEnabledOverlaysForTarget(context, obtainedKey);
        for (String o : overlays) {
            prioritiesList.add(new PrioritiesItem(o,
                    Packages.getOverlayParentIcon(context, o)));
            workableList.add(o);
        }
        Collections.reverse(workableList);
        Collections.reverse(prioritiesList);
        PriorityAdapter adapter = new PriorityAdapter(
                context,
                R.layout.priority_overlay_item);
        adapter.setData(prioritiesList);
        recyclerView.setAdapter(adapter);

        new GestureManager.Builder(recyclerView)
                .setManualDragEnabled(true)
                .setSwipeFlags(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT)
                .setDragFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN)
                .build();

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
         the result of PACKAGE, for example
         (om set-priority com.android.systemui.Beltz com.android.systemui.Domination)

         5. com.android.systemui.Beltz will be HIGHER than com.android.systemui.Domination
         */
        adapter.setDataChangeListener(
                new GestureAdapter.OnDataChangeListener<PrioritiesInterface>() {
                    @Override
                    public void onItemReorder(
                            PrioritiesInterface item,
                            int fromPos,
                            int toPos) {
                        if (fromPos != toPos) {
                            String move_package = workableList.get(fromPos);
                            // As workable list is a simulation of the priority list without object
                            // values, we have to simulate the events such as adding above parents
                            workableList.remove(fromPos);
                            workableList.add(toPos, move_package);
                            applyFab.show();
                        }
                    }

                    @Override
                    public void onItemRemoved(PrioritiesInterface item, int position) {
                    }
                });

        applyFab.setOnClickListener(v -> {
            try {
                applyFab.setImageDrawable(context.getDrawable(R.drawable.save_to_checkmark));
                ((Animatable) applyFab.getDrawable()).start();
            } catch (Exception e) {
                // The themer broke the animation, skip!
            }

            int colorFrom = context.getColor(R.color.colorAccent);
            int colorTo = context.getColor(R.color.fab_icon_success);
            ValueAnimator colorAnimation = ValueAnimator.ofObject(
                    new ArgbEvaluator(), colorFrom, colorTo);
            colorAnimation.setDuration(
                    context.getResources().getInteger(R.integer.priority_fab_hide_delay));
            colorAnimation.addUpdateListener(animator ->
                    applyFab.setBackgroundTintList(
                            ColorStateList.valueOf(((int) animator.getAnimatedValue()))));
            colorAnimation.start();

            headerProgress.setVisibility(View.VISIBLE);
            Collections.reverse(workableList);
            ThemeManager.setPriority(context, workableList);
            if (Packages.needsRecreate(context, workableList)) {
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    // OMS may not have written all the changes so
                    // quickly just yet so we may need to have a small delay
                    try {
                        onBackPressed();
                    } catch (Exception e) {
                        // Consume window refresh
                    }
                }, (long) (Systems.checkAndromeda(context) ||
                        Systems.checkSubstratumService(context) ?
                        REFRESH_WINDOW_DELAY : (REFRESH_WINDOW_DELAY << 1)));
            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (MainActivity) context;
    }
}