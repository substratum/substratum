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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
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
import projekt.substratum.common.platform.ThemeManager;

public class PriorityLoaderFragment extends Fragment {

    private List<PrioritiesInterface> prioritiesList;
    private List<String> app_list;
    private PriorityAdapter adapter;
    private RelativeLayout emptyView;
    private RecyclerView recyclerView;
    private ProgressBar materialProgressBar;

    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ViewGroup root = (ViewGroup)
                inflater.inflate(R.layout.priority_loader_fragment, container, false);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        final PriorityAdapter empty_adapter = new PriorityAdapter(
                this.getContext(), R.layout.priority_loader_item);
        this.recyclerView = root.findViewById(R.id.recycler_view);
        this.recyclerView.setAdapter(empty_adapter);

        final LinearLayoutManager manager = new LinearLayoutManager(this.getContext());
        this.recyclerView.setHasFixedSize(true);
        this.recyclerView.setLayoutManager(manager);

        this.materialProgressBar = root.findViewById(R.id.loading_priorities);
        this.emptyView = root.findViewById(R.id.no_priorities_found);

        // Begin loading up list
        this.prioritiesList = new ArrayList<>();
        this.app_list = new ArrayList<>();
        this.adapter = new PriorityAdapter(this.getContext(), R.layout.priority_loader_item);

        final LoadPrioritizedOverlays loadPrioritizedOverlays = new LoadPrioritizedOverlays(this);
        loadPrioritizedOverlays.execute("");

        this.recyclerView.addOnItemTouchListener(new RecyclerItemTouchListener(this.getActivity(),
                new DefaultItemClickListener() {
                    @Override
                    public boolean onItemClick(final View view, final int position) {
                        final Fragment fragment = new PriorityListFragment();

                        final Bundle bundle = new Bundle();
                        bundle.putString("package_name", PriorityLoaderFragment.this.app_list.get(position));
                        fragment.setArguments(bundle);
                        final FragmentManager fm = PriorityLoaderFragment.this.getActivity().getSupportFragmentManager();
                        final FragmentTransaction transaction = fm.beginTransaction();
                        transaction.setCustomAnimations(
                                android.R.anim.fade_in, android.R.anim.fade_out);
                        transaction.replace(R.id.main, fragment);
                        transaction.commit();
                        return false;
                    }
                }));
        return root;
    }

    private static class LoadPrioritizedOverlays extends AsyncTask<String, Integer, String> {

        private final WeakReference<PriorityLoaderFragment> ref;

        LoadPrioritizedOverlays(final PriorityLoaderFragment priorityLoaderFragment) {
            super();
            this.ref = new WeakReference<>(priorityLoaderFragment);
        }

        @Override
        protected void onPostExecute(final String result) {
            super.onPostExecute(result);
            final PriorityLoaderFragment fragment = this.ref.get();
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
                        .setGestureFlags(
                                ItemTouchHelper.LEFT |
                                        ItemTouchHelper.RIGHT,
                                ItemTouchHelper.UP |
                                        ItemTouchHelper.DOWN)
                        .build();
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final PriorityLoaderFragment fragment = this.ref.get();
            if (fragment != null) {
                final List<String> targets =
                        ThemeManager.listTargetWithMultipleOverlaysEnabled(fragment.getContext());

                for (final String t : targets) {
                    fragment.prioritiesList.add(new PrioritiesItem(t, Packages.getAppIcon(
                            fragment.getContext(),
                            t)));
                    fragment.app_list.add(t);
                }
            }
            return null;
        }
    }
}