package projekt.substratum.fragments;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.thesurix.gesturerecycler.DefaultItemClickListener;
import com.thesurix.gesturerecycler.GestureManager;
import com.thesurix.gesturerecycler.RecyclerItemTouchListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.adapters.PrioritiesAdapter;
import projekt.substratum.model.Priorities;
import projekt.substratum.model.PrioritiesItem;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class PriorityLoaderFragment extends Fragment {

    public Drawable grabAppIcon(String package_name) {
        Drawable icon = null;
        try {
            icon = getContext().getPackageManager().getApplicationIcon(package_name);
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e("SubstratumLogger", "Could not grab the application icon for \"" + package_name
                    + "\"");
        }
        return icon;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ViewGroup root = (ViewGroup) inflater.inflate(R.layout.priority_loader_fragment,
                null);

        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);

        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);

        RelativeLayout emptyView = (RelativeLayout) root.findViewById(R.id.no_priorities_found);

        // Begin loading up list

        final List<PrioritiesItem> prioritiesList = new ArrayList<>();
        final List<String> app_list = new ArrayList<>();

        try {
            String line;
            Process nativeApp = Runtime.getRuntime().exec("om list");

            OutputStream stdin = nativeApp.getOutputStream();
            InputStream stderr = nativeApp.getErrorStream();
            InputStream stdout = nativeApp.getInputStream();
            stdin.write(("ls\n").getBytes());
            stdin.write("exit\n".getBytes());
            stdin.flush();
            stdin.close();

            int checked_count = 0;
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            String current_header = "";
            while ((line = br.readLine()) != null) {
                if (line.length() > 0) {
                    if (!line.contains("[")) {
                        if (checked_count > 1) {
                            prioritiesList.add(new Priorities(current_header, grabAppIcon
                                    (current_header)));
                            app_list.add(current_header);
                            current_header = line;
                            checked_count = 0;
                        } else {
                            current_header = line;
                            checked_count = 0;
                        }
                    } else {
                        if (line.contains("[x]")) {
                            checked_count += 1;
                        }
                    }
                } else {
                    if (checked_count > 1) {
                        prioritiesList.add(new Priorities(current_header, grabAppIcon
                                (current_header)));
                        app_list.add(current_header);
                        current_header = line;
                        checked_count = 0;
                    } else {
                        current_header = line;
                        checked_count = 0;
                    }
                }
            }
            br.close();
            br = new BufferedReader(new InputStreamReader(stderr));
            while ((line = br.readLine()) != null) {
                Log.e("SubstratumLogger", line);
            }
            br.close();
        } catch (IOException ioe) {
            Log.e("SubstratumLogger", "There was an issue regarding loading the priorities of " +
                    "each overlay.");
        }

        final PrioritiesAdapter adapter = new PrioritiesAdapter(getContext(), R.layout
                .linear_loader_item);

        if (prioritiesList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }

        adapter.setData(prioritiesList);
        recyclerView.setAdapter(adapter);

        new GestureManager.Builder(recyclerView)
                .setGestureFlags(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, ItemTouchHelper.UP
                        | ItemTouchHelper.DOWN)
                .build();

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
}