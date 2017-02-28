package projekt.substratum.services;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import projekt.substratum.FloatUILaunchActivity;

@TargetApi(Build.VERSION_CODES.N)
public class FloatUiTile extends TileService {

    @Override
    public void onTileAdded() {
        switchState();
        super.onTileAdded();
    }

    @Override
    public void onStartListening() {
        switchState();
        super.onStartListening();
    }

    @Override
    public void onClick() {
        Intent intent = new Intent(this, FloatUILaunchActivity.class);
        startActivityAndCollapse(intent);
    }

    private void switchState() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        int state = prefs.getInt("float_tile", Tile.STATE_INACTIVE);
        Tile tile = getQsTile();
        tile.setState(state);
        tile.updateTile();
    }
}