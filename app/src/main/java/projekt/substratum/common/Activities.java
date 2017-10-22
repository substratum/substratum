package projekt.substratum.common;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import projekt.substratum.R;

public enum Activities {
    ;

    public static void launchActivityUrl(final Context context, final int resource) {
        try {
            final Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(context.getString(resource)));
            context.startActivity(i);
        } catch (final ActivityNotFoundException activityNotFoundException) {
            Toast.makeText(context,
                    context.getString(R.string.activity_missing_toast),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static void launchExternalActivity(final Context context,
                                              final String packageName,
                                              final String className) {
        final Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, packageName + '.' + className));
        context.startActivity(intent);
    }

    public static void launchInternalActivity(final Context context, final Class target) {
        final Intent intent = new Intent(context, target);
        context.startActivity(intent);
    }
}
