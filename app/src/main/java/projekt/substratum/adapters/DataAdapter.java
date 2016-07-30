package projekt.substratum.adapters;

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

    public DataAdapter(ArrayList<ThemeInfo> information) {
        this.information = information;
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
        if (information.get(i).getThemeVersion() != null) {
            viewHolder.theme_version.setText(information.get(i).getThemeVersion());
        }
        viewHolder.theme_author.setText(information.get(i).getThemeAuthor());
        viewHolder.imageView.setImageDrawable(information.get(i).getThemeDrawable());
    }

    @Override
    public int getItemCount() {
        return information.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView theme_name;
        TextView theme_author;
        TextView theme_version;
        ImageView imageView;

        public ViewHolder(View view) {
            super(view);
            theme_name = (TextView) view.findViewById(R.id.theme_name);
            theme_author = (TextView) view.findViewById(R.id.theme_author);
            theme_version = (TextView) view.findViewById(R.id.theme_version);
            imageView = (ImageView) view.findViewById(R.id.theme_preview_image);
        }
    }
}