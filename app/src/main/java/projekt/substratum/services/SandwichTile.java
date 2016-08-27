package projekt.substratum.services;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import projekt.substratum.R;

/**
 * @author Nicholas Chum (nicholaschum)
 */

@TargetApi(Build.VERSION_CODES.N)
public class SandwichTile extends TileService {

    @Override
    public void onTileAdded() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        if (!prefs.contains("sandwich_tile")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("sandwich_tile", 3).apply();
        }
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("sandwich_tile").apply();
        super.onTileRemoved();
    }

    @Override
    public void onClick() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        Tile tile = getQsTile();
        SharedPreferences.Editor editor = prefs.edit();

        switch (prefs.getInt("sandwich_tile", 0)) {
            case 0:
                editor.putInt("sandwich_tile", 1).apply();
                tile.setIcon(Icon.createWithResource(getApplicationContext(),
                        R.drawable.ic_one_bread));
                tile.setLabel(getApplicationContext().getString(R.string.one_bread));
                break;
            case 1:
                editor.putInt("sandwich_tile", 2).apply();
                tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable
                        .ic_two_breads));
                tile.setLabel(getApplicationContext().getString(R.string.two_bread));
                break;
            case 2:
                editor.putInt("sandwich_tile", 3).apply();
                tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable
                        .ic_three_breads));
                tile.setLabel(getApplicationContext().getString(R.string.three_bread));
                break;
            case 3:
                editor.putInt("sandwich_tile", 1).apply();
                tile.setIcon(Icon.createWithResource(getApplicationContext(),
                        R.drawable.ic_one_bread));
                tile.setLabel(getApplicationContext().getString(R.string.one_bread));
                break;
        }
        tile.updateTile();
    }
}