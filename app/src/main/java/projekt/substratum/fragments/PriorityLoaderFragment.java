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

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.thesurix.gesturerecycler.DefaultItemClickListener;
import com.thesurix.gesturerecycler.GestureManager;
import com.thesurix.gesturerecycler.RecyclerItemTouchListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.adapters.fragments.priorities.PrioritiesInterface;
import projekt.substratum.adapters.fragments.priorities.PrioritiesItem;
import projekt.substratum.adapters.fragments.priorities.PriorityAdapter;
import projekt.substratum.common.Packages;
import projekt.substratum.databinding.PriorityLoaderFragmentBinding;

import static projekt.substratum.common.platform.ThemeManager.listTargetWithMultipleOverlaysEnabled;

public class PriorityLoaderFragment extends Fragment {

    RecyclerView recyclerView;
    ProgressBar materialProgressBar;
    RelativeLayout emptyView;
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

        private WeakReference<PriorityLoaderFragment> ref;

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