/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.fragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.thesurix.gesturerecycler.DefaultItemClickListener;
import com.thesurix.gesturerecycler.GestureManager;
import com.thesurix.gesturerecycler.RecyclerItemTouchListener;
import projekt.substratum.R;
import projekt.substratum.adapters.fragments.priorities.PrioritiesInterface;
import projekt.substratum.adapters.fragments.priorities.PrioritiesItem;
import projekt.substratum.adapters.fragments.priorities.PriorityAdapter;
import projekt.substratum.common.Packages;
import projekt.substratum.databinding.PriorityLoaderFragmentBinding;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static projekt.substratum.common.platform.ThemeManager.listTargetWithMultipleOverlaysEnabled;

public class PriorityLoaderFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar materialProgressBar;
    private RelativeLayout emptyView;
    private List<PrioritiesInterface> prioritiesList;
    private List<String> appList;
    private PriorityAdapter adapter;
    private Context context;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        context = getContext();
        PriorityLoaderFragmentBinding viewBinding =
                DataBindingUtil.inflate(inflater, R.layout.priority_loader_fragment, container, false);
        View view = viewBinding.getRoot();
        recyclerView = viewBinding.recyclerView;
        materialProgressBar = viewBinding.loadingPriorities;
        emptyView = viewBinding.noPrioritiesFound;

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        PriorityAdapter empty_adapter =
                new PriorityAdapter(context, R.layout.priority_loader_item);
        recyclerView.setAdapter(empty_adapter);
        LinearLayoutManager manager = new LinearLayoutManager(context);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);

        // Begin loading up list
        prioritiesList = new ArrayList<>();
        appList = new ArrayList<>();
        adapter = new PriorityAdapter(getContext(), R.layout.priority_loader_item);

        // Load prioritized overlays
        new LoadPrioritizedOverlays(this).execute("");

        assert getActivity() != null;
        recyclerView.addOnItemTouchListener(new RecyclerItemTouchListener<>(
                new DefaultItemClickListener<PrioritiesItem>() {
                    @Override
                    public boolean onItemClick(final PrioritiesItem item, final int position) {
                        Fragment fragment = new PriorityListFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("package_name", appList.get(position));
                        fragment.setArguments(bundle);
                        FragmentManager fm = getActivity().getSupportFragmentManager();
                        FragmentTransaction transaction = fm.beginTransaction();
                        transaction.setCustomAnimations(
                                R.anim.slide_in_right, R.anim.slide_out_left);
                        transaction.replace(R.id.main, fragment);
                        transaction.commit();
                        return false;
                    }
                }));
        return view;
    }

    private static class LoadPrioritizedOverlays extends AsyncTask<String, Integer, String> {

        private final WeakReference<PriorityLoaderFragment> ref;

        LoadPrioritizedOverlays(PriorityLoaderFragment priorityLoaderFragment) {
            super();
            ref = new WeakReference<>(priorityLoaderFragment);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            PriorityLoaderFragment fragment = ref.get();
            if (fragment != null) {
                if (fragment.prioritiesList.isEmpty()) {
                    fragment.emptyView.setVisibility(View.VISIBLE);
                    fragment.materialProgressBar.setVisibility(View.GONE);
                } else {
                    fragment.emptyView.setVisibility(View.GONE);
                    fragment.materialProgressBar.setVisibility(View.GONE);
                }

                fragment.adapter.setData(fragment.prioritiesList);
                fragment.recyclerView.setAdapter(fragment.adapter);

                new GestureManager.Builder(fragment.recyclerView)
                        .setSwipeFlags(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT)
                        .setDragFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN)
                        .build();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            PriorityLoaderFragment fragment = ref.get();
            if (fragment != null) {
                List<String> targets =
                        listTargetWithMultipleOverlaysEnabled(fragment.context);

                for (String t : targets) {
                    fragment.prioritiesList.add(new PrioritiesItem(t,
                            Packages.getAppIcon(fragment.context, t)));
                    fragment.appList.add(t);
                }
            }
            return null;
        }
    }
}