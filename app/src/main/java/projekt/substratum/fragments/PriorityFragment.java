package projekt.substratum.fragments;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import projekt.substratum.model.Priorities;
import projekt.substratum.model.PrioritiesItem;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class PriorityFragment extends Fragment {

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
        final ViewGroup root = (ViewGroup) inflater.inflate(R.layout.priority_fragment,
                null);

        final RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);

        final LinearLayoutManager manager = new LinearLayoutManager(getContext());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);

        CardView backCard = (CardView) root.findViewById(R.id.back_card);
        backCard.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Fragment fragment = new PriorityLoaderFragment();
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim
                        .fade_out);
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

            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            String current_header = "";
            while ((line = br.readLine()) != null) {
                if (line.length() > 0) {
                    if (line.equals(obtained_key)) {
                        current_header = line;
                    } else {
                        if (current_header.equals(obtained_key)) {
                            if (line.contains("[x]")) {
                                prioritiesList.add(new Priorities(line.substring(8), grabAppIcon
                                        (current_header)));
                            } else {
                                if (!line.contains("[ ]")) {
                                    break;
                                }

                            }
                        }
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

        final PrioritiesAdapter adapter = new PrioritiesAdapter(getContext(), R.layout.linear_item);
        adapter.setData(prioritiesList);

        recyclerView.setAdapter(adapter);

        new GestureManager.Builder(recyclerView)
                .setManualDragEnabled(true)
                .setGestureFlags(ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, ItemTouchHelper.UP
                        | ItemTouchHelper.DOWN)
                .build();

        return root;
    }
}