package projekt.substratum.adapters;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.thesurix.gesturerecycler.GestureAdapter;
import com.thesurix.gesturerecycler.GestureViewHolder;

import java.io.ByteArrayOutputStream;

import projekt.substratum.R;
import projekt.substratum.config.References;
import projekt.substratum.model.Priorities;
import projekt.substratum.model.PrioritiesHeader;
import projekt.substratum.model.PrioritiesItem;

public class PrioritiesAdapter extends GestureAdapter<PrioritiesItem, GestureViewHolder> {

    private final Context mContext;
    private final int mItemResId;

    public PrioritiesAdapter(final Context context, @LayoutRes final int itemResId) {
        mContext = context;
        mItemResId = itemResId;
    }

    @Override
    public GestureViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        if (viewType == PrioritiesItem.PrioritiesItemType.CONTENT.ordinal()) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(mItemResId,
                    parent, false);
            return new PrioritiesViewHolder(itemView);
        } else {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout
                    .header_item, parent, false);
            return new HeaderViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(final GestureViewHolder holder, final int position) {
        super.onBindViewHolder(holder, position);
        final PrioritiesItem prioritiesItem = getData().get(position);

        if (prioritiesItem.getType() == PrioritiesItem.PrioritiesItemType.CONTENT) {
            final PrioritiesViewHolder prioritiesViewHolder = (PrioritiesViewHolder) holder;
            final Priorities priorities = (Priorities) prioritiesItem;

            try {
                // Keep this value but do not display it to the user, instead, parse it
                try {
                    ApplicationInfo applicationInfo = mContext.getPackageManager()
                            .getApplicationInfo
                                    (priorities.getName(), PackageManager.GET_META_DATA);
                    String packageTitle = mContext.getPackageManager().getApplicationLabel
                            (applicationInfo).toString();
                    if (applicationInfo.metaData != null) {
                        if (applicationInfo.metaData.getString("Substratum_Device") != null) {
                            prioritiesViewHolder.mCardText.setText(priorities.getName());
                        } else {
                            prioritiesViewHolder.mCardText.setText(packageTitle + " (" +
                                    priorities.getName() + ")");
                        }
                    } else {
                        prioritiesViewHolder.mCardText.setText(packageTitle + " (" + priorities
                                .getName() + ")");
                    }

                } catch (Exception e) {
                    Log.e(References.SUBSTRATUM_LOG, "Could not find explicit package identifier" +
                            " in package manager list.");
                }

                // Grab app icon from PackageInstaller and convert it to a BitmapDrawable in bytes
                Drawable icon = priorities.getDrawableId();
                Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
                try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] bitmapData = stream.toByteArray();

                    Glide.with(mContext).load(bitmapData).centerCrop().into(prioritiesViewHolder
                            .mAppIcon);
                }
            } catch (Exception e) {
                // Suppress warning
            }
        } else {
            final HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            final PrioritiesHeader monthHeader = (PrioritiesHeader) prioritiesItem;
            headerViewHolder.mHeaderText.setText(monthHeader.getName());
        }
    }

    @Override
    public int getItemViewType(final int position) {
        return getData().get(position).getType().ordinal();
    }
}