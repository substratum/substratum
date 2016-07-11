package projekt.substratum.adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import projekt.substratum.R;
import projekt.substratum.model.ThemeInfo;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {
    private ArrayList<ThemeInfo> information;
    private Context context;

    public DataAdapter(Context context, ArrayList<ThemeInfo> information) {
        this.context = context;
        this.information = information;

    }

    public Drawable grabPackageHeroImage(String package_name) {
        Resources res;
        Drawable hero = null;
        try {
            res = context.getPackageManager().getResourcesForApplication(package_name);
            int resourceId = res.getIdentifier(package_name + ":drawable/heroimage", null, null);
            if (0 != resourceId) {
                hero = context.getPackageManager().getDrawable(package_name, resourceId, null);
            }
            return hero;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return hero;
    }

    @Override
    public DataAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.theme_entry_card,
                viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {

        viewHolder.theme_name.setText(information.get(i).getThemeName());
        viewHolder.theme_author.setText(information.get(i).getThemeAuthor());

        if (information.get(i).getThemeDrawable() != null) {
            viewHolder.imageView.setImageDrawable(information.get(i).getThemeDrawable());
        } else {
            information.get(i).setDrawable(grabPackageHeroImage(information.get(i)
                    .getThemePackage()));
            viewHolder.imageView.setImageDrawable(information.get(i).getThemeDrawable());
        }

    }

    @Override
    public int getItemCount() {
        return information.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView theme_name;
        TextView theme_author;
        ImageView imageView;

        public ViewHolder(View view) {
            super(view);
            theme_name = (TextView) view.findViewById(R.id.theme_name);
            theme_author = (TextView) view.findViewById(R.id.theme_author);
            imageView = (ImageView) view.findViewById(R.id.theme_preview_image);
        }
    }
}