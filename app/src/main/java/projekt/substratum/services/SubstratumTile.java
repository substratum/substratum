package projekt.substratum.services;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

import projekt.substratum.LauncherActivity;

/**
 * @author Nicholas Chum (nicholaschum)
 */

@TargetApi(Build.VERSION_CODES.N)
public class SubstratumTile extends TileService {

    @Override
    public void onClick() {
        Intent intent = new Intent(this, LauncherActivity.class);
        startActivity(intent);
    }
}