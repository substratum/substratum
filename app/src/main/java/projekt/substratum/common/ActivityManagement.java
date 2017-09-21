package projekt.substratum.common;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import projekt.substratum.R;

public class ActivityManagement {

    public static void launchActivityUrl(Context context, int resource) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(context.getString(resource)));
            context.startActivity(i);
        } catch (ActivityNotFoundException activityNotFoundException) {
            Toast.makeText(context,
                    context.getString(R.string.activity_missing_toast),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static void launchExternalActivity(Context context, String packageName, String
            className) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, packageName + "." + className));
        context.startActivity(intent);
    }

    public static void launchInternalActivity(Context context, Class target) {
        Intent intent = new Intent(context, target);
        context.startActivity(intent);
    }

}
