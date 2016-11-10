package projekt.substratum.services;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

import projekt.substratum.LaunchActivity;
import projekt.substratum.LauncherActivity;

@TargetApi(Build.VERSION_CODES.N)
public class SubstratumTile extends TileService {

    @Override
    public void onClick() {
        try {
            Intent intent = new Intent(this, LauncherActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            // At this point, the app is most likely hidden and set to only open from Settings
            Intent intent = new Intent(this, LaunchActivity.class);
            startActivity(intent);
        }
    }
}