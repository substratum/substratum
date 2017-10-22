/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.util.helpers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

import projekt.substratum.common.References;

public class NotificationCreator {

    private final Context mContext;
    private final String content_title;
    private final String content_text;
    private final Boolean auto_cancel;
    private final PendingIntent intent;
    private final int small_icon;
    private final Bitmap big_icon;
    private final int notification_priority;
    private final int invoke_id;

    public NotificationCreator(final Context context,
                               final String content_title,
                               final String content_text,
                               final Boolean auto_cancel,
                               final PendingIntent intent,
                               final int small_icon,
                               final Bitmap big_icon,
                               final int notification_priority,
                               final int invoke_id) {
        super();
        this.mContext = context;
        this.content_title = content_title;
        this.content_text = content_text;
        this.auto_cancel = auto_cancel;
        this.intent = intent;
        this.small_icon = small_icon;
        this.big_icon = big_icon;
        this.notification_priority = notification_priority;
        this.invoke_id = invoke_id;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean createNotification() {
        try {
            final NotificationManager mNotifyManager = (NotificationManager) this.mContext
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            final NotificationCompat.Builder mBuilder = new
                    NotificationCompat.Builder(this.mContext, References
                    .DEFAULT_NOTIFICATION_CHANNEL_ID);

            if (this.content_title != null) mBuilder.setContentTitle(this.content_title);
            if (this.content_text != null) mBuilder.setContentText(this.content_text);
            if (this.auto_cancel != null) mBuilder.setAutoCancel(this.auto_cancel);
            if (this.intent != null) mBuilder.setContentIntent(this.intent);
            if (this.small_icon != 0) mBuilder.setSmallIcon(this.small_icon);
            if (this.big_icon != null) mBuilder.setLargeIcon(this.big_icon);

            mBuilder.setPriority(this.notification_priority);
            if (mNotifyManager != null) {
                mNotifyManager.notify(this.invoke_id, mBuilder.build());
            }
            return true;
        } catch (final Exception e) {
            // Suppress warning
        }
        return false;
    }
}