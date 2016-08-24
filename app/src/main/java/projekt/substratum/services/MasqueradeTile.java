package projekt.substratum.services;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import projekt.substratum.R;
import projekt.substratum.config.References;

/**
 * @author Nicholas Chum (nicholaschum)
 */

@TargetApi(Build.VERSION_CODES.N)
public class MasqueradeTile extends TileService {

    @Override
    public void onClick() {
        if (References.isPackageInstalled(getApplicationContext(),
                "masquerade.substratum")) {
            Intent runCommand = new Intent();
            runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            runCommand.setAction("masquerade.substratum.COMMANDS");
            runCommand.putExtra("substratum-check", "masquerade-ball");
            getApplicationContext().sendBroadcast(runCommand);
        } else {
            if (References.checkOMS()) {
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string
                                .masquerade_check_not_installed),
                        Toast.LENGTH_SHORT);
                toast.show();
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string
                                .masquerade_check_not_supported),
                        Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }
}