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
import projekt.substratum.adapters.PrioritiesAdapter;
import projekt.substratum.config.References;
import projekt.substratum.config.ThemeManager;
import projekt.substratum.model.Priorities;
import projekt.substratum.model.PrioritiesItem;

public class PriorityLoaderFragment extends Fragment {

    private List<PrioritiesItem> prioritiesList;
    private List<String> app_list;
    private PrioritiesAdapter adapter;
    private RelativeLayout emptyView;
    private RecyclerView recyclerView;
    private MaterialProgressBar materialProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ViewGroup root = (ViewGroup) inflater.inflate(R.layout.priority_loader_fragment,
                container, false);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        PrioritiesAdapter empty_adapter = new PrioritiesAdapter(getContext(), R.layout
                .linear_loader_item);
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
        adapter = new PrioritiesAdapter(getContext(), R.layout
                .linear_loader_item);

        LoadPrioritizedOverlays loadPrioritizedOverlays = new LoadPrioritizedOverlays();
        loadPrioritizedOverlays.execute("");

        recyclerView.addOnItemTouchListener(new RecyclerItemTouchListener(getActivity(), new
                DefaultItemClickListener() {
                    @Override
                    public boolean onItemClick(final View view, final int position) {
                        Fragment fragment = new PriorityListFragment();

                        Bundle bundle = new Bundle();
                        bundle.putString("package_name", app_list.get(position));
                        fragment.setArguments(bundle);
                        FragmentManager fm = getActivity().getSupportFragmentManager();
                        FragmentTransaction transaction = fm.beginTransaction();
                        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim
                                .fade_out);
                        transaction.replace(R.id.main, fragment);
                        transaction.commit();

                        // return true if the event is consumed
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
                    .setGestureFlags(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                            ItemTouchHelper.UP
                                    | ItemTouchHelper.DOWN)
                    .build();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            List<String> targets = ThemeManager.listTargetWithMultipleOverlaysEnabled();
            for (String t : targets) {
                prioritiesList.add(new Priorities(t, References.grabAppIcon(getContext(), t)));
                app_list.add(t);
            }
            return null;
        }
    }
}