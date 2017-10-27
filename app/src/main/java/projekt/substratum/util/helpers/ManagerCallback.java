package projekt.substratum.util.helpers;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import java.util.List;

import projekt.substratum.adapters.fragments.manager.ManagerItem;

public class ManagerCallback extends DiffUtil.Callback {

    private List<ManagerItem> newOverlays;
    private List<ManagerItem> oldOverlays;

    public ManagerCallback(List<ManagerItem> newOverlays, List<ManagerItem> oldOverlays) {
        this.newOverlays = newOverlays;
        this.oldOverlays = oldOverlays;
    }

    @Override
    public int getOldListSize() {
        return oldOverlays.size();
    }

    @Override
    public int getNewListSize() {
        return oldOverlays.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldOverlays.get(oldItemPosition).equals(newOverlays.get(newItemPosition));
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldOverlays.get(oldItemPosition).equals(newOverlays.get(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}