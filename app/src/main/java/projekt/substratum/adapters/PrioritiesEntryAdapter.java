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
import projekt.substratum.model.Priorities;
import projekt.substratum.model.PrioritiesHeader;
import projekt.substratum.model.PrioritiesItem;

public class PrioritiesEntryAdapter extends GestureAdapter<PrioritiesItem, GestureViewHolder> {

    private final Context mCtx;
    private final int mItemResId;

    public PrioritiesEntryAdapter(final Context ctx, @LayoutRes final int itemResId) {
        mCtx = ctx;
        mItemResId = itemResId;
    }

    @Override
    public GestureViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        if (viewType == PrioritiesItem.MonthItemType.MONTH.ordinal()) {
            final View itemView = LayoutInflater.from(parent.getContext()).inflate(mItemResId,
                    parent, false);
            return new PrioritiesViewHolder(itemView);
        } else {
            final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout
                    .header_item, parent, false);
            return new HeaderViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(final GestureViewHolder holder, final int position) {
        super.onBindViewHolder(holder, position);
        final PrioritiesItem prioritiesItem = getData().get(position);

        if (prioritiesItem.getType() == PrioritiesItem.MonthItemType.MONTH) {
            final PrioritiesViewHolder prioritiesViewHolder = (PrioritiesViewHolder) holder;
            final Priorities priorities = (Priorities) prioritiesItem;

            // Keep this value but do not display it to the user, instead, parse it
            try {
                ApplicationInfo applicationInfo = mCtx.getPackageManager().getApplicationInfo
                        (priorities.getName(), 0);
                String packageTitle = mCtx.getPackageManager().getApplicationLabel
                        (applicationInfo).toString();
                prioritiesViewHolder.mMonthText.setText(packageTitle + " (" + priorities.getName
                        () + ")");
            } catch (PackageManager.NameNotFoundException nnfe) {
                Log.e("SubstratumLogger", "Could not find explicit package identifier" +
                        " in package manager list.");
            }

            // Grab app icon from PackageInstaller and convert it to a BitmapDrawable in bytes
            Drawable icon = priorities.getDrawableId();
            Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] bitmapdata = stream.toByteArray();

            Glide.with(mCtx).load(bitmapdata).centerCrop().into(prioritiesViewHolder.mAppIcon);
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
