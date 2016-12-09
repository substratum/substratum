package projekt.substratum.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import projekt.substratum.R;
import projekt.substratum.config.References;
import projekt.substratum.model.IconEntry;
import projekt.substratum.model.IconInfo;

public class IconPackAdapter extends RecyclerView.Adapter<IconEntry> {

    private ArrayList<IconInfo> itemList;
    private Context mContext;

    public IconPackAdapter(Context mContext, ArrayList<IconInfo> itemList) {
        this.mContext = mContext;
        this.itemList = itemList;
    }

    @Override
    public IconEntry onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(
                parent.getContext()).inflate(R.layout.icon_entry_card, null);
        return new IconEntry(mContext, layoutView);
    }

    @Override
    public void onBindViewHolder(IconEntry holder, int position) {
        // Get the Resources first
        if (itemList.get(position).getDrawable() != null) {
            holder.iconName.setText(itemList.get(position).getParsedName());
            Glide.with(itemList.get(position).getContext())
                    .load(itemList.get(position).getDrawable())
                    .centerCrop()
                    .into(holder.iconDrawable);
        } else {
            try {
                if (itemList.get(position).getThemePackage() == null) {
                    // Package name on the RecyclerView item
                    String packageName = References.grabPackageName(
                            itemList.get(position).getContext(),
                            itemList.get(position).getPackageName());
                    itemList.get(position).setParsedName(packageName);
                    holder.iconName.setText(packageName);

                    // Load the newly added icon into the RecyclerView item
                    Drawable drawable_icon = itemList.get(position).getContext().getPackageManager()
                            .getApplicationIcon(itemList.get(position).getPackageName());
                    Bitmap bitmap = ((BitmapDrawable) drawable_icon).getBitmap();
                    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] bitmapData = stream.toByteArray();
                        itemList.get(position).setDrawable(bitmapData);

                        Glide.with(itemList.get(position).getContext())
                                .load(bitmapData)
                                .centerCrop()
                                .into(holder.iconDrawable);
                    }
                } else {
                    Context context = itemList.get(position).getContext().createPackageContext(
                            itemList.get(position).getThemePackage(), 0);
                    Resources resources = context.getResources();
                    int drawable = resources.getIdentifier(
                            itemList.get(position).getPackageDrawable(), // Drawable name
                            "drawable",
                            itemList.get(position).getThemePackage()); // Icon Pack

                    // Package name on the RecyclerView item
                    String packageName = References.grabPackageName(
                            itemList.get(position).getContext(),
                            itemList.get(position).getPackageName());
                    itemList.get(position).setParsedName(packageName);
                    holder.iconName.setText(packageName);

                    if (drawable != 0) {
                        // Load the newly added icon into the RecyclerView item
                        Drawable drawable_icon = context.getDrawable(drawable);
                        if (drawable_icon != null) {
                            Bitmap bitmap = ((BitmapDrawable) drawable_icon).getBitmap();
                            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream);
                                byte[] bitmapData = stream.toByteArray();
                                itemList.get(position).setDrawable(bitmapData);

                                Glide.with(itemList.get(position).getContext())
                                        .load(bitmapData)
                                        .centerCrop()
                                        .into(holder.iconDrawable);
                            }
                        }
                    } else {
                        // Load the newly added icon into the RecyclerView item
                        Drawable drawable_icon = itemList.get(position).getContext()
                                .getPackageManager().getApplicationIcon(
                                        itemList.get(position).getPackageName());
                        Bitmap bitmap = ((BitmapDrawable) drawable_icon).getBitmap();
                        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream);
                            byte[] bitmapData = stream.toByteArray();
                            itemList.get(position).setDrawable(bitmapData);

                            Glide.with(itemList.get(position).getContext())
                                    .load(bitmapData)
                                    .centerCrop()
                                    .into(holder.iconDrawable);
                        }
                    }
                }
            } catch (Exception e) {
                // Suppress warning
            }
        }
        if (itemList.get(position).getPackageDrawable().equals("null")) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            holder.iconDrawable.setColorFilter(filter);
            holder.setDisabled(true);
        }
    }

    @Override
    public int getItemCount() {
        return this.itemList.size();
    }
}