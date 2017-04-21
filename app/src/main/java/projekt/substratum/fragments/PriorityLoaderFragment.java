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
import android.widget.RelativeLayout;

import com.thesurix.gesturerecycler.DefaultItemClickListener;
import com.thesurix.gesturerecycler.GestureManager;
import com.thesurix.gesturerecycler.RecyclerItemTouchListener;

import java.util.ArrayList;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.R;
import projekt.substratum.adapters.fragments.priorities.PrioritiesInterface;
import projekt.substratum.adapters.fragments.priorities.PrioritiesItem;
import projekt.substratum.adapters.fragments.priorities.PriorityAdapter;
import projekt.substratum.common.References;
import projekt.substratum.common.platform.ThemeManager;

public class PriorityLoaderFragment extends Fragment {

    private List<PrioritiesInterface> prioritiesList;
    private List<String> app_list;
    private PriorityAdapter adapter;
    private RelativeLayout emptyView;
    private RecyclerView recyclerView;
    private MaterialProgressBar materialProgressBar;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ViewGroup root = (ViewGroup)
                inflater.inflate(R.layout.priority_loader_fragment, container, false);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        PriorityAdapter empty_adapter = new PriorityAdapter(
                getContext(), R.layout.linear_loader_item);
        recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        recyclerView.setAdapter(empty_adapter);

        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);

        materialProgressBar = (MaterialProgressBar) root.findViewById(R.id.loading_priorities);
        emptyView = (RelativeLayout) root.findViewById(R.id.no_priorities_found);

        // Begin loading up list
        prioritiesList = new ArrayList<>();
        app_list = new ArrayList<>();
        adapter = new PriorityAdapter(getContext(), R.layout.linear_loader_item);

        LoadPrioritizedOverlays loadPrioritizedOverlays = new LoadPrioritizedOverlays();
        loadPrioritizedOverlays.execute("");

        recyclerView.addOnItemTouchListener(new RecyclerItemTouchListener(getActivity(),
                new DefaultItemClickListener() {
                    @Override
                    public boolean onItemClick(final View view, final int position) {
                        Fragment fragment = new PriorityListFragment();

                        Bundle bundle = new Bundle();
                        bundle.putString("package_name", app_list.get(position));
                        fragment.setArguments(bundle);
                        FragmentManager fm = getActivity().getSupportFragmentManager();
                        FragmentTransaction transaction = fm.beginTransaction();
                        transaction.setCustomAnimations(
                                android.R.anim.fade_in, android.R.anim.fade_out);
                        transaction.replace(R.id.main, fragment);
                        transaction.commit();
                        return false;
                    }
                }));
        return root;
    }

    private class LoadPrioritizedOverlays extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (prioritiesList.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                materialProgressBar.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                materialProgressBar.setVisibility(View.GONE);
            }

            adapter.setData(prioritiesList);
            recyclerView.setAdapter(adapter);

            new GestureManager.Builder(recyclerView)
                    .setGestureFlags(
                            ItemTouchHelper.LEFT |
                                    ItemTouchHelper.RIGHT,
                            ItemTouchHelper.UP |
                                    ItemTouchHelper.DOWN)
                    .build();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            List<String> targets = ThemeManager.listTargetWithMultipleOverlaysEnabled();
            for (String t : targets) {
                prioritiesList.add(new PrioritiesItem(t, References.grabAppIcon(getContext(), t)));
                app_list.add(t);
            }
            return null;
        }
    }
}