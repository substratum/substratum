package projekt.substratum.adapters;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import projekt.substratum.R;
import projekt.substratum.config.References;
import projekt.substratum.model.ThemeInfo;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ThemeEntryAdapter extends RecyclerView.Adapter<ThemeEntryAdapter.ViewHolder> {
    private ArrayList<ThemeInfo> information;

    public ThemeEntryAdapter(ArrayList<ThemeInfo> information) {
        this.information = information;
    }

    @Override
    public ThemeEntryAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.theme_entry_card,
                viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int i) {
        viewHolder.theme_name.setText(information.get(i).getThemeName());
        viewHolder.theme_author.setText(information.get(i).getThemeAuthor());
        if (information.get(i).getPluginVersion() != null) {
            viewHolder.plugin_version.setText(information.get(i).getPluginVersion());
        } else {
            viewHolder.plugin_version.setVisibility(View.INVISIBLE);
        }
        if (information.get(i).getSDKLevels() != null) {
            viewHolder.theme_apis.setText(information.get(i).getSDKLevels());
        } else {
            viewHolder.theme_apis.setVisibility(View.INVISIBLE);
        }
        if (information.get(i).getThemeVersion() != null) {
            viewHolder.theme_version.setText(information.get(i).getThemeVersion());
        } else {
            viewHolder.theme_version.setVisibility(View.INVISIBLE);
        }
        if (information.get(i).getThemeReadyVariable() == null) {
            viewHolder.divider.setVisibility(View.GONE);
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.GONE);
        } else if (information.get(i).getThemeReadyVariable().equals("all")) {
            viewHolder.divider.setVisibility(View.VISIBLE);
            viewHolder.tbo.setVisibility(View.VISIBLE);
            viewHolder.two.setVisibility(View.VISIBLE);
        } else if (information.get(i).getThemeReadyVariable().equals("ready")) {
            viewHolder.divider.setVisibility(View.VISIBLE);
            viewHolder.tbo.setVisibility(View.VISIBLE);
            viewHolder.two.setVisibility(View.GONE);
        } else if (information.get(i).getThemeReadyVariable().equals("stock")) {
            viewHolder.divider.setVisibility(View.VISIBLE);
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.VISIBLE);
        } else {
            viewHolder.divider.setVisibility(View.GONE);
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.GONE);
        }

        viewHolder.cardView.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        SharedPreferences prefs = information.get(i).getContext()
                                .getSharedPreferences(
                                        "substratum_state", Context.MODE_PRIVATE);
                        if (!prefs.contains("is_updating")) prefs.edit()
                                .putBoolean("is_updating", false).apply();
                        if (!prefs.getBoolean("is_updating", true)) {
                            // Process fail case if user uninstalls an app and goes back an activity
                            if (References.isPackageInstalled(information.get(i).getContext(),
                                    information.get(i).getThemePackage())) {

                                File checkSubstratumVerity = new File(information.get(i)
                                        .getContext().getCacheDir()
                                        .getAbsoluteFile() + "/SubstratumBuilder/" +
                                        information.get(i).getThemeName().replaceAll("\\s+", "")
                                                .replaceAll("[^a-zA-Z0-9]+", "") +
                                        "/substratum.xml");
                                if (checkSubstratumVerity.exists()) {
                                    References.launchTheme(information.get(i).getContext(),
                                            information.get(i).getThemeName(), information.get(i)
                                                    .getThemePackage(), information.get(i)
                                                    .getThemeMode());
                                } else {
                                    new References.SubstratumThemeUpdate(
                                            information.get(i).getContext(),
                                            information.get(i).getThemePackage(),
                                            information.get(i).getThemeName(),
                                            information.get(i).getThemeMode())
                                            .execute();
                                }
                            } else {
                                Toast toast = Toast.makeText(information.get(i).getContext(),
                                        information.get(i).getContext().getString(R.string
                                                .toast_uninstalled),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                                notifyDataSetChanged();
                            }
                        } else {
                            Toast toast = Toast.makeText(information.get(i).getContext(),
                                    information.get(i).getContext().getString(R.string
                                            .background_updating_toast),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                });

        viewHolder.tbo.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        new AlertDialog.Builder(information.get(i)
                                .getContext())
                                .setMessage(R.string.tbo_description)
                                .setPositiveButton(R.string.tbo_dialog_proceed,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                try {
                                                    String playURL =
                                                            information.get(i).getContext()
                                                                    .getString(R.string
                                                                            .tbo_theme_ready_url);
                                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                                    intent.setData(Uri.parse(playURL));
                                                    information.get(i).getContext()
                                                            .startActivity(intent);
                                                } catch (ActivityNotFoundException
                                                        activityNotFoundException) {
                                                    //
                                                }
                                            }
                                        })
                                .setNegativeButton(android.R.string.cancel,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        })
                                .setCancelable(true)
                                .show();
                    }
                }
        );

        viewHolder.two.setOnClickListener(new View.OnClickListener() {
                                              public void onClick(View v) {
                                                  new AlertDialog.Builder(information.get(i)
                                                          .getContext())
                                                          .setMessage(R.string.two_description)
                                                          .setCancelable(true)
                                                          .show();
                                              }
                                          }
        );

        viewHolder.theme_author.setText(information.get(i).getThemeAuthor());
        viewHolder.imageView.setImageDrawable(information.get(i).getThemeDrawable());
    }

    @Override
    public int getItemCount() {
        return information.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView theme_name;
        TextView theme_author;
        TextView theme_apis;
        TextView theme_version;
        TextView plugin_version;
        ImageView imageView;
        View divider;
        ImageView tbo;
        ImageView two;

        public ViewHolder(View view) {
            super(view);
            cardView = (CardView) view.findViewById(R.id.theme_card);
            theme_name = (TextView) view.findViewById(R.id.theme_name);
            theme_author = (TextView) view.findViewById(R.id.theme_author);
            theme_apis = (TextView) view.findViewById(R.id.api_levels);
            theme_version = (TextView) view.findViewById(R.id.theme_version);
            plugin_version = (TextView) view.findViewById(R.id.plugin_version);
            imageView = (ImageView) view.findViewById(R.id.theme_preview_image);
            divider = view.findViewById(R.id.theme_ready_divider);
            tbo = (ImageView) view.findViewById(R.id.theme_ready_indicator);
            two = (ImageView) view.findViewById(R.id.theme_unready_indicator);
        }
    }
}