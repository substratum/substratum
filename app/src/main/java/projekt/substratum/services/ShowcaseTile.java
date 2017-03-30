package projekt.substratum.services;

import android.content.Intent;
import android.service.quicksettings.TileService;

import projekt.substratum.ShowcaseActivity;


public class ShowcaseTile extends TileService {
    @Override
    public void onClick() {
        Intent collapseIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        Intent intent = new Intent(this, ShowcaseActivity.class);
        sendBroadcast(collapseIntent);
        startActivity(intent);
    }
}
