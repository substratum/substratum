package projekt.substratum.services;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import projekt.substratum.R;
import projekt.substratum.config.References;

/**
 * @author Nicholas Chum (nicholaschum)
 */

@TargetApi(Build.VERSION_CODES.N)
public class CookieTile extends TileService {

    @Override
    public void onTileAdded() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        if (!prefs.contains("cookie_tile")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("cookie_tile", 1).apply();
        }
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("cookie_tile").apply();
        super.onTileRemoved();
    }

    @Override
    public void onClick() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        final Tile tile = getQsTile();
        final SharedPreferences.Editor editor = prefs.edit();

        switch (prefs.getInt("cookie_tile", 0)) {
            case 0:
                editor.putInt("cookie_tile", 1).apply();
                tile.setIcon(Icon.createWithResource(getApplicationContext(),
                        R.drawable.ic_cookie_non_bitten));
                tile.setLabel(getApplicationContext().getString(R.string.cookie_non_bitten));
                tile.updateTile();
                break;
            case 1:
                editor.putInt("cookie_tile", 2).apply();
                tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable
                        .ic_cookie_bitten));
                tile.setLabel(getApplicationContext().getString(R.string.cookie_bitten));
                tile.updateTile();
                break;
            case 2:
                editor.putInt("cookie_tile", 99).apply();
                tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable
                        .ic_char));
                tile.setLabel(getApplicationContext().getString(R.string.a_wild_char_has_appeared));
                tile.updateTile();
                References.clearAppCache(getApplicationContext());
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        editor.putInt("cookie_tile", 1).apply();
                        tile.setIcon(Icon.createWithResource(getApplicationContext(),
                                R.drawable.ic_cookie_non_bitten));
                        tile.setLabel(getApplicationContext().getString(
                                R.string.cookie_non_bitten));
                        tile.updateTile();
                    }
                }, 3500);
                break;
            case 99:
                break;
        }
    }
}