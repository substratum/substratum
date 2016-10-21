package projekt.substratum.adapters;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.model.ShowcaseItem;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ShowcaseItemAdapter extends RecyclerView.Adapter<ShowcaseItemAdapter.ViewHolder> {
    private ArrayList<ShowcaseItem> information;

    public ShowcaseItemAdapter(ArrayList<ShowcaseItem> information) {
        this.information = information;
    }

    @Override
    public ShowcaseItemAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout
                .showcase_entry_card, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int i) {
        Glide.with(information.get(i).getContext())
                .load(information.get(i).getThemeIcon())
                .centerCrop()
                .crossFade()
                .into(viewHolder.imageView);

        viewHolder.themeName.setText(information.get(i).getThemeName());
        viewHolder.themeAuthor.setText(information.get(i).getThemeAuthor());

        if (information.get(i).getThemePricing().toLowerCase().equals("paid")) {
            viewHolder.themePricing.setVisibility(View.VISIBLE);
        } else {
            viewHolder.themePricing.setVisibility(View.GONE);
        }

        String[] supported = information.get(i).getThemeSupport().split("\\|");
        List supported_array = Arrays.asList(supported);
        if (supported_array.contains("wallpapers")) {
            viewHolder.wallpaper.setAlpha((float) 1.0);
        } else {
            viewHolder.wallpaper.setAlpha((float) 0.2);
        }
        if (supported_array.contains("sounds")) {
            viewHolder.sounds.setAlpha((float) 1.0);
        } else {
            viewHolder.sounds.setAlpha((float) 0.2);
        }
        if (supported_array.contains("fonts")) {
            viewHolder.fonts.setAlpha((float) 1.0);
        } else {
            viewHolder.fonts.setAlpha((float) 0.2);
        }
        if (supported_array.contains("bootanimations")) {
            viewHolder.bootanimations.setAlpha((float) 1.0);
        } else {
            viewHolder.bootanimations.setAlpha((float) 0.2);
        }
        if (supported_array.contains("overlays")) {
            viewHolder.overlays.setAlpha((float) 1.0);
        } else {
            viewHolder.overlays.setAlpha((float) 0.2);
        }

        viewHolder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(information.get(i).getThemeLink()));
                    information.get(i).getContext().startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast toaster = Toast.makeText(information.get(i).getContext(),
                            information.get(i).getContext().getString(R.string
                                    .activity_missing_toast),
                            Toast.LENGTH_SHORT);
                    toaster.show();
                }

            }
        });
    }

    @Override
    public int getItemCount() {
        return information.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView themeName, themeAuthor;
        ImageView themePricing;
        ImageView imageView, wallpaper, sounds, fonts, bootanimations, overlays;

        public ViewHolder(View view) {
            super(view);
            cardView = (CardView) view.findViewById(R.id.theme_card);
            themeName = (TextView) view.findViewById(R.id.theme_name);
            themeAuthor = (TextView) view.findViewById(R.id.theme_author);
            themePricing = (ImageView) view.findViewById(R.id.theme_pricing);
            imageView = (ImageView) view.findViewById(R.id.theme_icon);
            wallpaper = (ImageView) view.findViewById(R.id.theme_wallpapers);
            sounds = (ImageView) view.findViewById(R.id.theme_sounds);
            fonts = (ImageView) view.findViewById(R.id.theme_fonts);
            bootanimations = (ImageView) view.findViewById(R.id.theme_bootanimations);
            overlays = (ImageView) view.findViewById(R.id.theme_overlays);
        }
    }
}